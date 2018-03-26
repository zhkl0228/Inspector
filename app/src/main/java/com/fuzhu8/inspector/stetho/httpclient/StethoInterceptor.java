package com.fuzhu8.inspector.stetho.httpclient;

import com.facebook.stetho.inspector.network.DefaultResponseHandler;
import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;
import com.facebook.stetho.inspector.network.RequestBodyHelper;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StethoInterceptor implements HttpResponseInterceptor, HttpRequestInterceptor {

    static final String INSPECTOR_REQUEST_KEY = "inspectorRequest";

    private final NetworkEventReporter eventReporter = NetworkEventReporterImpl.get();

    static ByteArrayEntity copyEntity(HttpEntity entity) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        entity.writeTo(baos);
        ByteArrayEntity copy = new ByteArrayEntity(baos.toByteArray());
        copy.setContentEncoding(entity.getContentEncoding());
        copy.setContentType(entity.getContentType());
        copy.setChunked(entity.isChunked());
        return copy;
    }

    @Override
    public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        if (httpRequest instanceof HttpUriRequest) {
            String requestId = eventReporter.nextRequestId();
            HttpUriRequest request = (HttpUriRequest) httpRequest;

            RequestBodyHelper requestBodyHelper;
            if (eventReporter.isEnabled()) {
                requestBodyHelper = new RequestBodyHelper(eventReporter, requestId);
                HttpClientInspectorRequest inspectorRequest = new HttpClientInspectorRequest(requestId, request, requestBodyHelper);
                eventReporter.requestWillBeSent(inspectorRequest);
                httpContext.setAttribute(INSPECTOR_REQUEST_KEY, inspectorRequest);
            }
        }
    }

    @Override
    public void process(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
        HttpClientInspectorRequest inspectorRequest = (HttpClientInspectorRequest) httpContext.getAttribute(INSPECTOR_REQUEST_KEY);
        if (inspectorRequest == null) {
            return;
        }

        String requestId = inspectorRequest.id();
        if (eventReporter.isEnabled()) {
            RequestBodyHelper requestBodyHelper = inspectorRequest.requestBodyHelper;
            if (requestBodyHelper.hasBody()) {
                requestBodyHelper.reportDataSent();
            }

            eventReporter.responseHeadersReceived(
                    new HttpClientInspectorResponse(
                            requestId,
                            inspectorRequest.request,
                            httpResponse));

            HttpEntity body = httpResponse.getEntity();
            Header contentType = body.getContentType();
            Header contentEncoding = body.getContentEncoding();

            HttpEntity copy = copyEntity(body);
            httpResponse.setEntity(copy);

            // Log.d(getClass().getSimpleName(), Inspector.inspectString(IOUtils.toByteArray(copy.getContent()), "processResponse url=" + inspectorRequest.url() + ", contentType=" + contentType + ", contentEncoding=" + contentEncoding));

            eventReporter.interpretResponseStream(
                    requestId,
                    contentType != null ? contentType.getValue() : null,
                    contentEncoding != null ? contentEncoding.getValue() : null,
                    copy.getContent(),
                    new DefaultResponseHandler(eventReporter, requestId));
        }
    }

}
