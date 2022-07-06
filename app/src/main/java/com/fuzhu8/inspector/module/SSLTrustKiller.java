package com.fuzhu8.inspector.module;

import android.annotation.SuppressLint;

import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractAdvisor;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author zhkl0228
 *
 */
@SuppressLint("CustomX509TrustManager")
public class SSLTrustKiller extends AbstractAdvisor implements X509TrustManager {

	public SSLTrustKiller(ModuleContext context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.advisor.AbstractAdvisor#executeHook()
	 */
	@Override
	public void executeHook() {
		try {
			hook(javax.net.ssl.TrustManagerFactory.class, "getTrustManagers");
		} catch (Throwable e) {
			log(e);
		}
		
		try {
			hook(javax.net.ssl.HttpsURLConnection.class, "setSSLSocketFactory", javax.net.ssl.SSLSocketFactory.class);
		} catch (Throwable e) {
			log(e);
		}
		
		try {
			hook(javax.net.ssl.SSLContext.class, "init", KeyManager[].class, TrustManager[].class, SecureRandom.class);
		} catch (Throwable e) {
			log(e);
		}
		
		try {
			hook(SSLSocketFactory.class, "setHostnameVerifier", X509HostnameVerifier.class);
		} catch (Throwable e) {
			log(e);
		}
		
		try {
			hook(SSLSocketFactory.class, "isSecure", Socket.class);
		} catch (Throwable e) {
			log(e);
		}
		
		try {
			hook(javax.net.ssl.HttpsURLConnection.class, "setDefaultHostnameVerifier", HostnameVerifier.class);
		} catch (Throwable e) {
			log(e);
		}
	}

	@SuppressWarnings("unused")
	void before_setDefaultHostnameVerifier(Object thisObj, Object[] args) {
		args[0] = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
	}
	@SuppressWarnings("unused")
	void setDefaultHostnameVerifier(Object thisObj, HostnameVerifier verifier) {
	}

	@SuppressWarnings("unused")
	boolean isSecure(Object thisObj, Socket socket, boolean ret) {
		return true;
	}

	@SuppressWarnings("unused")
	void before_setHostnameVerifier(Object thisObj, Object[] args) {
		args[0] = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
	}
	@SuppressWarnings("unused")
	void setHostnameVerifier(Object thisObj, X509HostnameVerifier verifier) {
	}

	@SuppressWarnings("unused")
	void before_init(Object thisObj, Object[] args) {
		args[0] = null;
		args[1] = getTrustManagers(null, null);
		args[2] = null;
	}
	@SuppressWarnings("unused")
	void init(Object thisObj, KeyManager[] km, TrustManager[] tm, SecureRandom sr) {
	}

	@SuppressWarnings("unused")
	void before_setSSLSocketFactory(Object thisObj, Object[] args) {
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, getTrustManagers(null, null), null);
			args[0] = context.getSocketFactory();
		} catch(Exception e) {
			log(e);
		}
	}
	@SuppressWarnings("unused")
	void setSSLSocketFactory(Object thisObj, javax.net.ssl.SSLSocketFactory sslSocketFactory) {
	}

	private TrustManager[] getTrustManagers(Object thisObj, TrustManager[] ret) {
		return new TrustManager[] { this };
	}

	@SuppressLint("TrustAllX509TrustManager")
	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) {
	}

	@SuppressLint("TrustAllX509TrustManager")
	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) {
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}

}
