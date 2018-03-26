package com.fuzhu8.inspector.stetho.httpclient;

import com.facebook.stetho.inspector.network.NetworkEventReporter;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import javax.annotation.Nullable;

/**
 * inspect response
 * Created by zhkl0228 on 2018/1/5.
 */

class HttpClientInspectorResponse implements NetworkEventReporter.InspectorResponse {
    private final String requestId;
    private final HttpUriRequest request;
    private final HttpResponse response;

    HttpClientInspectorResponse(String requestId, HttpUriRequest request, HttpResponse response) {
        this.requestId = requestId;
        this.request = request;
        this.response = response;
    }

    @Override
    public String url() {
        return request.getURI().toString();
    }

    @Override
    public boolean connectionReused() {
        return false;
    }

    @Override
    public int connectionId() {
        return request.hashCode();
    }

    @Override
    public boolean fromDiskCache() {
        return false;
    }

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public int statusCode() {
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public String reasonPhrase() {
        return response.getStatusLine().getReasonPhrase();
    }

    @Override
    public int headerCount() {
        return response.getAllHeaders().length;
    }

    @Override
    public String headerName(int index) {
        return response.getAllHeaders()[index].getName();
    }

    @Override
    public String headerValue(int index) {
        return response.getAllHeaders()[index].getValue();
    }

    @Nullable
    @Override
    public String firstHeaderValue(String name) {
        Header header = response.getFirstHeader(name);
        return header == null ? null : header.getValue();
    }
}
