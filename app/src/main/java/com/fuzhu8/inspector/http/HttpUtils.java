package com.fuzhu8.inspector.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;

/**
 * @author zhkl0228
 *
 */
public class HttpUtils {
	
	private static final int CONNECT_TIMEOUT = 10000;
	private static final int READ_TIMEOUT = 15000;

	public static byte[] sendGet(String urlStr, NameValuePair...headers) throws IOException {
		HttpURLConnection conn = null;
		InputStream inputStream = null;
		try {
			URL url = new URL(urlStr);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(CONNECT_TIMEOUT);
			conn.setReadTimeout(READ_TIMEOUT);
			
			for(NameValuePair pair : headers) {
				conn.setRequestProperty(pair.getName(), pair.getValue());
			}
			
			inputStream = conn.getInputStream();
			
			return IOUtils.toByteArray(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
			
			if(conn != null) {
				conn.disconnect();
			}
		}
	}

	public static byte[] sendPost(String urlStr, byte[] data, NameValuePair...headers) throws IOException {
		HttpURLConnection conn = null;
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			URL url = new URL(urlStr);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(CONNECT_TIMEOUT);
			conn.setReadTimeout(READ_TIMEOUT);
			conn.setRequestProperty("Content-length", "" + data.length);
			
			for(NameValuePair pair : headers) {
				conn.setRequestProperty(pair.getName(), pair.getValue());
			}
			
			outputStream = conn.getOutputStream();
			outputStream.write(data);
			outputStream.flush();
			
			inputStream = conn.getInputStream();
			
			return IOUtils.toByteArray(inputStream);
		} finally {
			IOUtils.closeQuietly(outputStream);
			IOUtils.closeQuietly(inputStream);
			
			if(conn != null) {
				conn.disconnect();
			}
		}
	}

}
