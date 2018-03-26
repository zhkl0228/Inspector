package com.fuzhu8.inspector.stetho.httpclient;

import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.RequestBodyHelper;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nullable;

/**
 * inspect request
 * Created by zhkl0228 on 2018/1/5.
 */

public class HttpClientInspectorRequest implements NetworkEventReporter.InspectorRequest {
    private final String requestId;
    final HttpUriRequest request;
    final RequestBodyHelper requestBodyHelper;

    HttpClientInspectorRequest(String requestId, HttpUriRequest request, RequestBodyHelper requestBodyHelper) {
        this.requestId = requestId;
        this.request = request;
        this.requestBodyHelper = requestBodyHelper;
    }

    @Nullable
    @Override
    public Integer friendlyNameExtra() {
        return null;
    }

    @Override
    public String url() {
        return request.getURI().toString();
    }

    @Override
    public String method() {
        return request.getMethod();
    }

    @Nullable
    @Override
    public byte[] body() throws IOException {
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest enclosingRequest = (HttpEntityEnclosingRequest) request;
            HttpEntity entity = enclosingRequest.getEntity();
            if (entity == null) {
                return null;
            }
            ByteArrayEntity copy = StethoInterceptor.copyEntity(entity);
            enclosingRequest.setEntity(copy);

            Header contentEncoding = entity.getContentEncoding();
            OutputStream out = requestBodyHelper.createBodySink(contentEncoding == null ? null : contentEncoding.getValue());
            copy.writeTo(out);
            return requestBodyHelper.getDisplayBody();
        }
        return null;
    }

    @Override
    public String id() {
        return requestId;
    }

    @Override
    public String friendlyName() {
        return "httpclient";
    }

    @Override
    public int headerCount() {
        return request.getAllHeaders().length;
    }

    @Override
    public String headerName(int index) {
        return request.getAllHeaders()[index].getName();
    }

    @Override
    public String headerValue(int index) {
        return request.getAllHeaders()[index].getValue();
    }

    @Nullable
    @Override
    public String firstHeaderValue(String name) {
        return request.getFirstHeader(name).getValue();
    }
}
