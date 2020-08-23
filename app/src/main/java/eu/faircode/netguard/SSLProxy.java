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

    private static final int RECEIVE_BUFFER_SIZE = 0x2000;

    private SSLSocket socket;
    private final SSLServerSocket serverSocket;
    private final Packet packet;
    private final InspectorVpn vpn;

    private static Map<InetSocketAddress, SSLProxy> proxyMap = new ConcurrentHashMap<>();

    static SSLProxy create(InspectorVpn vpn, X509Certificate rootCert, PrivateKey privateKey, Packet packet) throws Exception {
        InetSocketAddress clientSocketAddress = packet.createClientAddress();
        SSLProxy proxy = proxyMap.get(clientSocketAddress);
        if (proxy != null) {
            return proxy;
        }

        SSLContext serverContext = ServerCertificate.getSSLContext(packet.createServerAddress());
        if (serverContext != null) {
            return new SSLProxy(vpn, serverContext, packet);
        }

        return new SSLProxy(vpn, rootCert, privateKey, packet);
    }

    private SSLProxy(InspectorVpn vpn, SSLContext serverContext, Packet packet) throws IOException {
        this.vpn = vpn;
        this.packet = packet;
        InetSocketAddress clientAddress = packet.createClientAddress();
        proxyMap.put(clientAddress, this);

        synchronized (this) {
            this.socket = null;

            SSLServerSocketFactory factory = serverContext.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(0);
            serverSocket.setSoTimeout(30000);
            serverSocket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
            this.serverSocket = serverSocket;

            Thread thread = new Thread(this, packet.toString());
            thread.setDaemon(true);
            thread.start();
        }
    }

    private ServerCertificate serverCertificate;

    private SSLProxy(InspectorVpn vpn, X509Certificate rootCert, PrivateKey privateKey, Packet packet) throws Exception {
        this.vpn = vpn;
        this.packet = packet;
        InetSocketAddress clientAddress = packet.createClientAddress();
        proxyMap.put(clientAddress, this);

        synchronized (this) {
            SSLServerSocket serverSocket = null;
            try {
                this.socket = connectServer();

                SSLContext serverContext = serverCertificate.createSSLContext(rootCert, privateKey, packet.createServerAddress());
                SSLServerSocketFactory factory = serverContext.getServerSocketFactory();
                serverSocket = (SSLServerSocket) factory.createServerSocket(0);
                serverSocket.setSoTimeout(30000);
                serverSocket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
                this.serverSocket = serverSocket;

                Thread thread = new Thread(this, packet.toString());
                thread.setDaemon(true);
                thread.start();
            } catch (Exception e) {
                proxyMap.remove(clientAddress);

                IOUtils.closeQuietly(serverSocket);
                throw e;
            }
        }
    }

    private SSLSocket connectServer() throws Exception {
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
            app.setSoTimeout(15000);
            vpn.protect(app);
            app.connect(packet.createServerAddress(), 15000);

            socket = (SSLSocket) context.getSocketFactory().createSocket(app, packet.daddr, packet.dport, true);
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            socket.addHandshakeCompletedListener(new HandshakeCompletedListener() {
                @Override
                public void handshakeCompleted(HandshakeCompletedEvent event) {
                    try {
                        X509Certificate peerCertificate = (X509Certificate) event.getPeerCertificates()[0];
                        serverCertificate = new ServerCertificate(peerCertificate);
                        countDownLatch.countDown();
                        Log.d(ServiceSinkhole.TAG, "handshakeCompleted event=" + event);
                    } catch (SSLPeerUnverifiedException e) {
                        Log.d(ServiceSinkhole.TAG, "handshakeCompleted failed", e);
                    }
                }
            });
            Log.d(ServiceSinkhole.TAG, "startHandshake socket=" + socket);
            socket.startHandshake();
            countDownLatch.await(30, TimeUnit.SECONDS);
            if (serverCertificate == null) {
                throw new IllegalStateException("handshake failed");
            }
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

    private boolean canStop;

    private class StreamForward implements Runnable {
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final Socket socket;
        private final boolean send;
        private final String clientIp, serverIp;
        private final int clientPort, serverPort;
        StreamForward(InputStream inputStream, OutputStream outputStream, Socket socket, boolean send, String clientIp, String serverIp, int clientPort, int serverPort) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.socket = socket;
            this.send = send;
            this.clientIp = clientIp;
            this.serverIp = serverIp;
            this.clientPort = clientPort;
            this.serverPort = serverPort;

            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
        @Override
        public void run() {
            try {
                doForward();
            } catch (Throwable ignored) {
            }
        }

        private void doForward() throws RemoteException {
            try {
                byte[] buf = new byte[RECEIVE_BUFFER_SIZE];
                int read;
                while (!canStop) {
                    try {
                        while ((read = inputStream.read(buf)) != -1) {
                            outputStream.write(buf, 0, read);

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
                    } catch (SocketTimeoutException ignored) {
                    }
                }
            } catch (IOException ignored) {
            } finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
                IOUtils.closeQuietly(socket);

                canStop = true;
                IPacketCapture packetCapture = vpn.getPacketCapture();
                if (packetCapture != null) {
                    packetCapture.onSSLProxyFinish(clientIp, serverIp, clientPort, serverPort, send);
                }
            }
        }
    }

    @Override
    public void run() {
        SSLSocket local = null;
        try {
            local = (SSLSocket) serverSocket.accept();
            local.setSoTimeout(30000);

            if (this.socket == null) {
                this.socket = connectServer();
            }

            InetSocketAddress client = (InetSocketAddress) local.getRemoteSocketAddress();
            InetSocketAddress server = (InetSocketAddress) socket.getRemoteSocketAddress();
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
            new StreamForward(localIn, socketOut, local, true, client.getHostString(), server.getHostString(), client.getPort(), server.getPort());
            new StreamForward(socketIn, localOut, socket, false, client.getHostString(), server.getHostString(), client.getPort(), server.getPort());
        } catch (Exception e) {
            Log.d(ServiceSinkhole.TAG, "accept failed: " + packet + ", local_port=" + serverSocket.getLocalPort(), e);

            IOUtils.closeQuietly(socket);
            IOUtils.closeQuietly(local);
        } finally {
            InetSocketAddress clientSocketAddress = new InetSocketAddress(packet.saddr, packet.sport);
            proxyMap.remove(clientSocketAddress);
            IOUtils.closeQuietly(serverSocket);
        }
    }

}
