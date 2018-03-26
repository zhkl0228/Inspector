package com.fuzhu8.inspector.stetho.urlconnection;

import com.facebook.stetho.urlconnection.ByteArrayRequestEntity;
import com.facebook.stetho.urlconnection.StethoURLConnectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * hook implementation
 * Created by zhkl0228 on 2018/1/4.
 */

class StethoURLConnectionManagerWrapper {

    private final StethoURLConnectionManager stethoManager;

    StethoURLConnectionManagerWrapper() {
        super();

        this.stethoManager = new StethoURLConnectionManager("urlconnection");
    }

    private boolean preConnect;

    InspectorOutputStream outputStream;

    void notifyPreConnect(final HttpURLConnection connection, Map<String, List<String>> requestProperties) {
        if (preConnect) {
            return;
        }
        this.stethoManager.preConnect(new HttpURLConnectionDelegate(connection, requestProperties), outputStream == null ? null : new ByteArrayRequestEntity(outputStream.toByteArray()));
        preConnect = true;
    }

    void notifyPostConnect() throws IOException {
        this.stethoManager.postConnect();
    }

    void notifyHttpExchangeFailed(IOException ex) {
        if (preConnect) {
            this.stethoManager.httpExchangeFailed(ex);
        }
    }

    InputStream interpretResponseStream(InputStream responseStream) {
        return this.stethoManager.interpretResponseStream(responseStream);
    }

}
