package eu.faircode.netguard;

import android.net.VpnService;
import android.util.Log;

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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
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
import javax.security.auth.x500.X500Principal;

import cn.banny.auxiliary.Inspector;

public class SSLProxy implements Runnable {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private final SSLSocket socket;
    private final SSLServerSocket serverSocket;
    private final Packet packet;

    private X509Certificate peerCertificate;

    SSLProxy(VpnService vpnService, X509Certificate rootCert, PrivateKey privateKey, Packet packet) throws IOException, InterruptedException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, NoSuchProviderException, SignatureException, InvalidKeyException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        this.packet = packet;

        Socket app = new Socket();
        app.bind(null);
        vpnService.protect(app);
        app.connect(new InetSocketAddress(packet.daddr, packet.dport), 5000);
        socket = (SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket(app, packet.daddr, packet.dport, true);
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
        countDownLatch.await();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "SC");
        keyPairGenerator.initialize(0x400, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        X509Certificate certificate = this.generateV3Certificate(publicKey, peerCertificate, rootCert, privateKey);
        certificate.checkValidity(new Date());
        certificate.verify(rootCert.getPublicKey());

        char[] password = "keypass".toCharArray();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, password);
        keyStore.setKeyEntry("alias", keyPair.getPrivate(), password, new Certificate[]{certificate});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        sslContext.getServerSessionContext().setSessionTimeout(10);

        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        serverSocket = (SSLServerSocket) factory.createServerSocket(0);
        serverSocket.setSoTimeout(10000);
        serverSocket.setReceiveBufferSize(0x2000);
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

    private class StreamForward implements Runnable {
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final Socket socket;
        StreamForward(InputStream inputStream, OutputStream outputStream, Socket socket) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.socket = socket;

            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
        @Override
        public void run() {
            byte[] buf = new byte[0x2000];
            int read;
            try {
                while ((read = inputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, read);

                    Log.d(ServiceSinkhole.TAG, Inspector.inspectString(Arrays.copyOf(buf, read), socket.toString()));
                }
            } catch (IOException ignored) {
            } finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
                IOUtils.closeQuietly(socket);
            }
        }
    }

    @Override
    public void run() {
        final SSLSocket local;
        try {
            local = (SSLSocket) serverSocket.accept();

            new StreamForward(local.getInputStream(), socket.getOutputStream(), local);
            new StreamForward(socket.getInputStream(), local.getOutputStream(), socket);
        } catch (IOException e) {
            Log.d(ServiceSinkhole.TAG, "accept failed", e);
        } finally {
            IOUtils.closeQuietly(serverSocket);
        }
    }

}
