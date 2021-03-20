package eu.faircode.netguard;

import android.annotation.SuppressLint;
import android.os.RemoteException;
import android.util.Log;

import com.fuzhu8.inspector.vpn.IPacketCapture;
import com.fuzhu8.inspector.vpn.InspectorVpn;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cn.banny.auxiliary.Inspector;
import eu.faircode.netguard.ssl.ServerCertificate;

public class SSLProxy implements Runnable {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private enum HandshakeStatus {
        handshaking, // 正在握手
        failed1, // 握手失败一次
        failed2, // 握手失败两次
        success // 握手成功
    }

    private SSLSocket socket;
    private final SSLServerSocket serverSocket;
    private final Packet packet;
    private final InspectorVpn vpn;

    private static final Map<InetSocketAddress, HandshakeStatus> handshakeStatusMap = new ConcurrentHashMap<>();

    static Allowed create(final InspectorVpn vpn, final X509Certificate rootCert, final PrivateKey privateKey, final Packet packet, final int timeout) {
        try {
            final InetSocketAddress server = packet.createServerAddress();
            final HandshakeStatus status = handshakeStatusMap.get(server);
            if (status == null || status == HandshakeStatus.failed1) {
                handshakeStatusMap.put(server, HandshakeStatus.handshaking);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try (Socket socket = connectServer(new ServerCertificateNotifier() {
                            @Override
                            public void handshakeCompleted(ServerCertificate serverCertificate) {
                                try {
                                    serverCertificate.createSSLContext(rootCert, privateKey, server);
                                } catch (Exception e) {
                                    Log.w(ServiceSinkhole.TAG, "create ssl context failed", e);
                                }
                            }
                        }, vpn, timeout, packet)) {
                            Log.d(ServiceSinkhole.TAG, "handshake success: socket=" + socket);
                            handshakeStatusMap.put(server, HandshakeStatus.success);
                        } catch (Exception e) {
                            Log.w(ServiceSinkhole.TAG, "handshake failed: " + server, e);
                            handshakeStatusMap.put(server, status == null ? HandshakeStatus.failed1 : HandshakeStatus.failed2);
                        }
                    }
                });
                thread.setDaemon(true);
                thread.start();
                return null;
            }
            switch (status) {
                case handshaking: // 正在进行SSL握手
                    return null;
                case failed2: // 握手两次失败：连接失败，或者不是SSL协议
                    return new Allowed();
                case success: // 握手成功
                    SSLContext serverContext = ServerCertificate.getSSLContext(server);
                    if (serverContext != null) {
                        return new SSLProxy(vpn, serverContext, packet, timeout).redirect();
                    }
                default:
                    throw new IllegalStateException("server=" + server + ", status=" + status);
            }
        } catch (IOException e) {
            Log.w(ServiceSinkhole.TAG, "mitm failed", e);
            return null;
        }
    }

    private final int timeout;

    private SSLProxy(InspectorVpn vpn, SSLContext serverContext, Packet packet, int timeout) throws IOException {
        this.vpn = vpn;
        this.packet = packet;
        this.timeout = timeout;
        this.serverSocket = startSSLServerSocket(serverContext, this, packet);
    }

    private static final int SERVER_SO_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);

    private static SSLServerSocket startSSLServerSocket(SSLContext serverContext, SSLProxy proxy, Packet packet) throws IOException {
        SSLServerSocketFactory factory = serverContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(0);
        serverSocket.setSoTimeout(SERVER_SO_TIMEOUT);

        Thread thread = new Thread(proxy, packet.toString());
        thread.setDaemon(true);
        thread.start();
        return serverSocket;
    }

    private interface ServerCertificateNotifier {
        void handshakeCompleted(ServerCertificate serverCertificate);
    }

    private static SSLSocket connectServer(final ServerCertificateNotifier notifier, InspectorVpn vpn, int timeout, Packet packet) throws Exception {
        Socket app = null;
        SSLSocket socket = null;
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) {
                }
                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) {
                }
            };
            context.init(null, new TrustManager[]{trustManager}, new SecureRandom());

            app = new Socket();
            app.bind(null);
            app.setSoTimeout(timeout);
            vpn.protect(app);
            app.connect(packet.createServerAddress(), 5000);

            socket = (SSLSocket) context.getSocketFactory().createSocket(app, packet.daddr, packet.dport, true);
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            socket.addHandshakeCompletedListener(new HandshakeCompletedListener() {
                @Override
                public void handshakeCompleted(HandshakeCompletedEvent event) {
                    try {
                        X509Certificate peerCertificate = (X509Certificate) event.getPeerCertificates()[0];
                        if (notifier != null) {
                            notifier.handshakeCompleted(new ServerCertificate(peerCertificate));
                        }
                        countDownLatch.countDown();
                        Log.d(ServiceSinkhole.TAG, "handshakeCompleted event=" + event);
                    } catch (SSLPeerUnverifiedException e) {
                        Log.d(ServiceSinkhole.TAG, "handshakeCompleted failed", e);
                    }
                }
            });
            Log.d(ServiceSinkhole.TAG, "connectServer socket=" + socket);
            socket.startHandshake();
            if (!countDownLatch.await(timeout, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("handshake failed");
            }
            app.setSoTimeout(0);
            return socket;
        } catch (Exception e) {
            IOUtils.closeQuietly(app);
            IOUtils.closeQuietly(socket);
            throw e;
        }
    }

    final synchronized Allowed redirect() {
        return new Allowed("127.0.0.1", serverSocket.getLocalPort());
    }

    private Throwable socketException;

    private class StreamForward implements Runnable {
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final boolean send;
        private final String clientIp, serverIp;
        private final int clientPort, serverPort;
        private final CountDownLatch countDownLatch;
        private final Socket socket;
        StreamForward(InputStream inputStream, OutputStream outputStream, boolean send, String clientIp, String serverIp, int clientPort, int serverPort, CountDownLatch countDownLatch, Socket socket) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.send = send;
            this.clientIp = clientIp;
            this.serverIp = serverIp;
            this.clientPort = clientPort;
            this.serverPort = serverPort;
            this.countDownLatch = countDownLatch;
            this.socket = socket;

            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
        @Override
        public void run() {
            doForward();
        }

        private void doForward() {
            try {
                byte[] buf = new byte[socket.getReceiveBufferSize()];
                int read;
                while (socketException == null) {
                    try {
                        while ((read = inputStream.read(buf)) != -1) {
                            outputStream.write(buf, 0, read);
                            outputStream.flush();

                            IPacketCapture packetCapture = vpn.getPacketCapture();
                            if (packetCapture != null) {
                                try {
                                    if (send) {
                                        packetCapture.onSSLProxyTX(clientIp, serverIp, clientPort, serverPort, Arrays.copyOf(buf, read));
                                    } else {
                                        packetCapture.onSSLProxyRX(clientIp, serverIp, clientPort, serverPort, Arrays.copyOf(buf, read));
                                    }
                                } catch (RemoteException ignored) {
                                }
                            } else {
                                Log.d(ServiceSinkhole.TAG, Inspector.inspectString(Arrays.copyOf(buf, read), socket.toString()));
                            }
                        }
                        break;
                    } catch(SocketTimeoutException ignored) {}
                }
            } catch (Throwable e) {
                Log.w(ServiceSinkhole.TAG, "stream forward exception: socket=" + socket, e);
                socketException = e;
            } finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
                countDownLatch.countDown();
            }
        }
    }

    private SSLSocket local;

    @Override
    public void run() {
        Runnable runnable = null;
        try {
            local = (SSLSocket) serverSocket.accept();
            local.setSoTimeout(1000);
            this.socket = connectServer(null, vpn, timeout, packet);

            final InetSocketAddress client = (InetSocketAddress) local.getRemoteSocketAddress();
            final InetSocketAddress server = (InetSocketAddress) socket.getRemoteSocketAddress();
            InputStream localIn = local.getInputStream();
            OutputStream localOut = local.getOutputStream();
            InputStream socketIn = socket.getInputStream();
            OutputStream socketOut = socket.getOutputStream();

            IPacketCapture packetCapture = vpn.getPacketCapture();
            if (packetCapture != null) {
                try {
                    packetCapture.onSSLProxyEstablish(client.getHostString(), server.getHostString(), client.getPort(), server.getPort());
                } catch (RemoteException ignored) {
                }
            }
            final CountDownLatch countDownLatch = new CountDownLatch(2);
            new StreamForward(localIn, socketOut, true, client.getHostString(), server.getHostString(), client.getPort(), server.getPort(), countDownLatch, local);
            new StreamForward(socketIn, localOut, false, client.getHostString(), server.getHostString(), client.getPort(), server.getPort(), countDownLatch, socket);
            runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        countDownLatch.await();

                        IPacketCapture packetCapture = vpn.getPacketCapture();
                        if (packetCapture != null) {
                            packetCapture.onSSLProxyFinish(client.getHostString(), server.getHostString(), client.getPort(), server.getPort());
                        }
                    } catch (InterruptedException | RemoteException ignored) {
                    } finally {
                        IOUtils.closeQuietly(socket);
                        IOUtils.closeQuietly(local);
                    }
                }
            };
        } catch (Exception e) {
            Log.d(ServiceSinkhole.TAG, "accept failed: " + packet + ", local_port=" + serverSocket.getLocalPort(), e);

            IOUtils.closeQuietly(socket);
            IOUtils.closeQuietly(local);
        } finally {
            IOUtils.closeQuietly(serverSocket);
        }

        if (runnable != null) {
            runnable.run();
        }
    }

}
