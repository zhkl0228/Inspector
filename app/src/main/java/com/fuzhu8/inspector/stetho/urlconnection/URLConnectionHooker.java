package com.fuzhu8.inspector.stetho.urlconnection;

import android.annotation.SuppressLint;

import com.facebook.stetho.common.Util;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.advisor.MethodHook;
import com.fuzhu8.inspector.advisor.MethodHookAdapter;
import com.fuzhu8.inspector.advisor.MethodHookParam;
import com.fuzhu8.inspector.stetho.StethoInspectorHooker;
import com.taobao.android.dexposed.XposedHelpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * URLConnection hooker
 * Created by zhkl0228 on 2018/1/5.
 */

public class URLConnectionHooker implements StethoInspectorHooker {

    private final Map<Integer, StethoURLConnectionManagerWrapper> managerMap = new ConcurrentHashMap<>();

    @SuppressLint("PrivateApi")
    @Override
    public void hook(ClassLoader classLoader, Hooker hooker) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> HttpURLConnectionImpl = classLoader.loadClass("com.android.okhttp.internal.http.HttpURLConnectionImpl");
        Constructor<?>[] constructors = HttpURLConnectionImpl.getConstructors();
        MethodHook createManager = new MethodHookAdapter() {
            @Override
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                managerMap.put(param.thisObject.hashCode(), new StethoURLConnectionManagerWrapper());
            }
        };
        for (Constructor<?> constructor : constructors) {
            hooker.hookMethod(constructor, createManager);
        }

        Method disconnect = HttpURLConnectionImpl.getDeclaredMethod("disconnect");
        hooker.hookMethod(disconnect, new MethodHookAdapter() {
            @Override
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                managerMap.remove(param.thisObject.hashCode());
            }
        });

        Method execute = HttpURLConnectionImpl.getDeclaredMethod("execute", boolean.class);
        hooker.hookMethod(execute, new MethodHookAdapter() {
            @Override
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                HttpURLConnection connection = (HttpURLConnection) param.thisObject;
                StethoURLConnectionManagerWrapper wrapper = managerMap.get(connection.hashCode());
                if (wrapper == null) {
                    return;
                }

                boolean readResponse = (Boolean) param.args[0];
                if (readResponse) {
                    try {
                        Object httpEngine = XposedHelpers.getObjectField(connection, "httpEngine");
                        XposedHelpers.callMethod(httpEngine, "sendRequest");
                        Object requestHeaders = XposedHelpers.getObjectField(httpEngine, "requestHeaders");
                        Object headers = XposedHelpers.getObjectField(requestHeaders, "headers");
                        @SuppressWarnings("unchecked") Map<String, List<String>> requestProperties = (Map<String, List<String>>) XposedHelpers.callMethod(headers, "toMultimap", false);
                        wrapper.notifyPreConnect(connection, requestProperties);
                    } catch (XposedHelpers.InvocationTargetError error) {
                        param.setThrowable(error.getCause());
                    }
                }
            }

            @Override
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                StethoURLConnectionManagerWrapper wrapper = managerMap.get(param.thisObject.hashCode());
                if (wrapper == null) {
                    return;
                }

                Throwable ex = param.getThrowable();
                if (ex instanceof IOException) {
                    wrapper.notifyHttpExchangeFailed((IOException) ex);
                }
                boolean readResponse = (Boolean) param.args[0];
                if (readResponse && ex == null) {
                    wrapper.notifyPostConnect();
                }
            }
        });

        Method getOutputStream = HttpURLConnectionImpl.getDeclaredMethod("getOutputStream");
        hooker.hookMethod(getOutputStream, new MethodHookAdapter() {
            @Override
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                StethoURLConnectionManagerWrapper wrapper = managerMap.get(param.thisObject.hashCode());
                OutputStream outputStream = (OutputStream) param.getResult();
                if (wrapper != null && outputStream != null) {
                    if (wrapper.outputStream == null) {
                        wrapper.outputStream = new InspectorOutputStream(outputStream);
                    }
                    param.setResult(wrapper.outputStream);
                }
            }
        });

        Method getInputStream = HttpURLConnectionImpl.getDeclaredMethod("getInputStream");
        hooker.hookMethod(getInputStream, new MethodHookAdapter() {
            @Override
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                HttpURLConnection connection = (HttpURLConnection) param.thisObject;
                StethoURLConnectionManagerWrapper wrapper = managerMap.get(connection.hashCode());
                InputStream rawStream = (InputStream) param.getResult();
                if (wrapper != null && rawStream != null) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try {
                        // Let Stetho see the raw, possibly compressed stream.
                        rawStream = wrapper.interpretResponseStream(rawStream);
                        InputStream decompressedStream = applyDecompressionIfApplicable(connection, rawStream);
                        if (decompressedStream != null) {
                            Util.copy(decompressedStream, out, new byte[1024]);
                        }
                    } finally {
                        if (rawStream != null) {
                            rawStream.close();
                        }
                    }
                    param.setResult(new ByteArrayInputStream(out.toByteArray()));
                }
            }
        });
    }

    private static final String GZIP_ENCODING = "gzip";

    private static InputStream applyDecompressionIfApplicable(
            HttpURLConnection conn, InputStream in) throws IOException {
        if (in != null && GZIP_ENCODING.equals(conn.getContentEncoding())) {
            return new GZIPInputStream(in);
        }
        return in;
    }

}
