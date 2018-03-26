package com.fuzhu8.inspector.stetho.urlconnection;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;

public class HttpURLConnectionDelegate extends HttpURLConnection {

    private final HttpURLConnection connection;
    private final Map<String, List<String>> requestProperties;

    HttpURLConnectionDelegate(HttpURLConnection connection, Map<String, List<String>> requestProperties) {
        super(connection.getURL());
        this.connection = connection;
        this.requestProperties = requestProperties;
    }

    @Override
    public String getHeaderFieldKey(int n) {
        return connection.getHeaderFieldKey(n);
    }

    @Override
    public void setFixedLengthStreamingMode(int contentLength) {
        connection.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setFixedLengthStreamingMode(long contentLength) {
        connection.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setChunkedStreamingMode(int chunklen) {
        connection.setChunkedStreamingMode(chunklen);
    }

    @Override
    public String getHeaderField(int n) {
        return connection.getHeaderField(n);
    }

    @Override
    public void setInstanceFollowRedirects(boolean followRedirects) {
        connection.setInstanceFollowRedirects(followRedirects);
    }

    @Override
    public boolean getInstanceFollowRedirects() {
        return connection.getInstanceFollowRedirects();
    }

    @Override
    public void setRequestMethod(String method) throws ProtocolException {
        connection.setRequestMethod(method);
    }

    @Override
    public String getRequestMethod() {
        return connection.getRequestMethod();
    }

    @Override
    public int getResponseCode() throws IOException {
        return connection.getResponseCode();
    }

    @Override
    public String getResponseMessage() throws IOException {
        return connection.getResponseMessage();
    }

    @Override
    public long getHeaderFieldDate(String name, long Default) {
        return connection.getHeaderFieldDate(name, Default);
    }

    @Override
    public void disconnect() {
        connection.disconnect();
    }

    @Override
    public boolean usingProxy() {
        return connection.usingProxy();
    }

    @Override
    public Permission getPermission() throws IOException {
        return connection.getPermission();
    }

    @Override
    public InputStream getErrorStream() {
        return connection.getErrorStream();
    }

    @Override
    public void connect() throws IOException {
        connection.connect();
    }

    @Override
    public void setConnectTimeout(int timeout) {
        connection.setConnectTimeout(timeout);
    }

    @Override
    public int getConnectTimeout() {
        return connection.getConnectTimeout();
    }

    @Override
    public void setReadTimeout(int timeout) {
        connection.setReadTimeout(timeout);
    }

    @Override
    public int getReadTimeout() {
        return connection.getReadTimeout();
    }

    @Override
    public URL getURL() {
        return connection.getURL();
    }

    @Override
    public int getContentLength() {
        return connection.getContentLength();
    }

    @SuppressLint("NewApi")
    @Override
    public long getContentLengthLong() {
        return connection.getContentLengthLong();
    }

    @Override
    public String getContentType() {
        return connection.getContentType();
    }

    @Override
    public String getContentEncoding() {
        return connection.getContentEncoding();
    }

    @Override
    public long getExpiration() {
        return connection.getExpiration();
    }

    @Override
    public long getDate() {
        return connection.getDate();
    }

    @Override
    public long getLastModified() {
        return connection.getLastModified();
    }

    @Override
    public String getHeaderField(String name) {
        return connection.getHeaderField(name);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return connection.getHeaderFields();
    }

    @Override
    public int getHeaderFieldInt(String name, int Default) {
        return connection.getHeaderFieldInt(name, Default);
    }

    @SuppressLint("NewApi")
    @Override
    public long getHeaderFieldLong(String name, long Default) {
        return connection.getHeaderFieldLong(name, Default);
    }

    @Override
    public Object getContent() throws IOException {
        return connection.getContent();
    }

    @Override
    public Object getContent(Class[] classes) throws IOException {
        return connection.getContent(classes);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return connection.getOutputStream();
    }

    @Override
    public String toString() {
        return connection.toString();
    }

    @Override
    public void setDoInput(boolean doinput) {
        connection.setDoInput(doinput);
    }

    @Override
    public boolean getDoInput() {
        return connection.getDoInput();
    }

    @Override
    public void setDoOutput(boolean dooutput) {
        connection.setDoOutput(dooutput);
    }

    @Override
    public boolean getDoOutput() {
        return connection.getDoOutput();
    }

    @Override
    public void setAllowUserInteraction(boolean allowuserinteraction) {
        connection.setAllowUserInteraction(allowuserinteraction);
    }

    @Override
    public boolean getAllowUserInteraction() {
        return connection.getAllowUserInteraction();
    }

    @Override
    public void setUseCaches(boolean usecaches) {
        connection.setUseCaches(usecaches);
    }

    @Override
    public boolean getUseCaches() {
        return connection.getUseCaches();
    }

    @Override
    public void setIfModifiedSince(long ifmodifiedsince) {
        connection.setIfModifiedSince(ifmodifiedsince);
    }

    @Override
    public long getIfModifiedSince() {
        return connection.getIfModifiedSince();
    }

    @Override
    public boolean getDefaultUseCaches() {
        return connection.getDefaultUseCaches();
    }

    @Override
    public void setDefaultUseCaches(boolean defaultusecaches) {
        connection.setDefaultUseCaches(defaultusecaches);
    }

    @Override
    public void setRequestProperty(String key, String value) {
        connection.setRequestProperty(key, value);
    }

    @Override
    public void addRequestProperty(String key, String value) {
        connection.addRequestProperty(key, value);
    }

    @Override
    public String getRequestProperty(String key) {
        return connection.getRequestProperty(key);
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return requestProperties;
    }

}
