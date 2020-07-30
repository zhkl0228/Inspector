package eu.faircode.netguard;

import android.annotation.SuppressLint;
import android.net.VpnService;
import android.os.RemoteException;
import android.util.Log;

import com.fuzhu8.inspector.vpn.IPacketCapture;

import org.apache.commons.io.IOUtils;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import cn.banny.auxiliary.Inspector;

public class SSLProxy implements Runnable {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private static final int RECEIVE_BUFFER_SIZE = 0x2000;

    private final SSLSocket socket;
    private final SSLServerSocket serverSocket;
    private final Packet packet;
    private final IPacketCapture packetCapture;

    private static Map<X509Certificate, SSLContext> proxyCertMap = new ConcurrentHashMap<>();
    private X509Certificate peerCertificate;

    SSLProxy(VpnService vpnService, X509Certificate rootCert, PrivateKey privateKey, Packet packet, IPacketCapture packetCapture) throws Exception {
        this.packet = packet;
        this.packetCapture = packetCapture;

        Socket app = null;
        SSLSocket socket = null;
        SSLServerSocket serverSocket = null;
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
            app.setSoTimeout(10000);
            vpnService.protect(app);
            app.connect(new InetSocketAddress(InetAddress.getByName(packet.daddr), packet.dport), 5000);

            socket = (SSLSocket) context.getSocketFactory().createSocket(app, packet.daddr, packet.dport, true);
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            socket.addHandshakeCompletedListener(new HandshakeCompletedListener() {
                @Override
                public void handshakeCompleted(HandshakeCompletedEvent event) {
                    try {
                        peerCertificate = (X509Certificate) event.getPeerCertificates()[0];
                        countDownLatch.countDown();
                        Log.d(ServiceSinkhole.TAG, "handshakeCompleted event=" + event);
                    } catch (SSLPeerUnverifiedException e) {
                        Log.d(ServiceSinkhole.TAG, "handshakeCompleted failed", e);
                    }
                }
            });
            Log.d(ServiceSinkhole.TAG, "startHandshake socket=" + socket);
            socket.startHandshake();
            countDownLatch.await(10, TimeUnit.SECONDS);
            if (peerCertificate == null) {
                throw new IllegalStateException("handshake failed");
            }

            SSLContext serverContext = proxyCertMap.get(peerCertificate);
            if (serverContext == null) {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "SC");
                keyPairGenerator.initialize(0x400, new SecureRandom());
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                PublicKey publicKey = keyPair.getPublic();
                X509Certificate certificate = this.generateV3Certificate(publicKey, peerCertificate, rootCert, privateKey);
                Log.d(ServiceSinkhole.TAG, "generateV3Certificate certificate=" + certificate);
                certificate.checkValidity(new Date());
                certificate.verify(rootCert.getPublicKey());

                char[] password = "keypass".toCharArray();
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, password);
                keyStore.setKeyEntry("alias", keyPair.getPrivate(), password, new Certificate[]{certificate});

                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, password);

                serverContext = SSLContext.getInstance("TLS");
                serverContext.init(keyManagerFactory.getKeyManagers(), null, null);
                serverContext.getServerSessionContext().setSessionTimeout(10);
                proxyCertMap.put(peerCertificate, serverContext);
            }

            SSLServerSocketFactory factory = serverContext.getServerSocketFactory();
            serverSocket = (SSLServerSocket) factory.createServerSocket(0);
            serverSocket.setSoTimeout(10000);
            serverSocket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);

            this.socket = socket;
            this.serverSocket = serverSocket;
        } catch (Exception e) {
            IOUtils.closeQuietly(app);
            IOUtils.closeQuietly(socket);
            IOUtils.closeQuietly(serverSocket);
            throw e;
        }
    }

    private static final long ONE_YEAR_IN_MS = TimeUnit.DAYS.toMillis(365);

    private X509Certificate generateV3Certificate(PublicKey publicKey, X509Certificate peerCertificate, X509Certificate rootCert, PrivateKey privateKey) throws CertificateException, OperatorCreationException {
        X500Principal principal = rootCert.getSubjectX500Principal();
        X500Name issuer = X500Name.getInstance(principal.getEncoded());

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - ONE_YEAR_IN_MS / 2);
        Date notAfter = new Date(System.currentTimeMillis() + ONE_YEAR_IN_MS * 2);
        Locale dateLocal = Locale.ENGLISH;
        X500Principal subject = peerCertificate.getSubjectX500Principal();
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, dateLocal, X500Name.getInstance(subject.getEncoded()), SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));
        builder.copyAndAddExtension(Extension.subjectAlternativeName, false, new JcaX509CertificateHolder(peerCertificate));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider("SC").build(privateKey);
        X509CertificateHolder holder = builder.build(signer);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        converter.setProvider("SC");
        return converter.getCertificate(holder);
    }

    final Allowed startProxy() {
        Thread thread = new Thread(this, packet.toString());
        thread.setDaemon(true);
        thread.start();
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
            } catch (RemoteException ignored) {
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
            local.setSoTimeout(10000);

            InetSocketAddress client = (InetSocketAddress) local.getRemoteSocketAddress();
            InetSocketAddress server = (InetSocketAddress) socket.getRemoteSocketAddress();
            InputStream localIn = local.getInputStream();
            OutputStream localOut = local.getOutputStream();
            InputStream socketIn = socket.getInputStream();
            OutputStream socketOut = socket.getOutputStream();

            if (packetCapture != null) {
                try {
                    packetCapture.onSSLProxyEstablish(client.getHostString(), server.getHostString(), client.getPort(), server.getPort());
                } catch (RemoteException ignored) {
                }
            }
            new StreamForward(localIn, socketOut, local, true, client.getHostString(), server.getHostString(), client.getPort(), server.getPort());
            new StreamForward(socketIn, localOut, socket, false, client.getHostString(), server.getHostString(), client.getPort(), server.getPort());
        } catch (IOException e) {
            Log.d(ServiceSinkhole.TAG, "accept failed: " + packet, e);

            IOUtils.closeQuietly(socket);
            IOUtils.closeQuietly(local);
        } finally {
            IOUtils.closeQuietly(serverSocket);
        }
    }

}
