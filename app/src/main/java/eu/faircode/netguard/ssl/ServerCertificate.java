package eu.faircode.netguard.ssl;

import android.util.Log;

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

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.security.auth.x500.X500Principal;

import eu.faircode.netguard.ServiceSinkhole;

public class ServerCertificate {

    private static final Map<X509Certificate, SSLContext> proxyCertMap = new ConcurrentHashMap<>();
    private static final Map<InetSocketAddress, SSLContext> serverSSLContextMap = new ConcurrentHashMap<>();

    private final X509Certificate peerCertificate;

    public ServerCertificate(X509Certificate peerCertificate) {
        this.peerCertificate = peerCertificate;
    }

    public static SSLContext getSSLContext(InetSocketAddress serverAddress) {
        return serverSSLContextMap.get(serverAddress);
    }

    public void createSSLContext(X509Certificate rootCert, PrivateKey privateKey, InetSocketAddress serverAddress) throws Exception {
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
        serverSSLContextMap.put(serverAddress, serverContext);
    }

    private X509Certificate generateV3Certificate(PublicKey publicKey, X509Certificate peerCertificate, X509Certificate rootCert, PrivateKey privateKey) throws CertificateException, OperatorCreationException {
        X500Principal principal = rootCert.getSubjectX500Principal();
        X500Name issuer = X500Name.getInstance(principal.getEncoded());

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        final long ONE_YEAR_IN_MS = TimeUnit.DAYS.toMillis(365);
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

}
