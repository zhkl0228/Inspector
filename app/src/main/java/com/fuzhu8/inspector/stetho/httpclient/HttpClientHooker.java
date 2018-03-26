package com.fuzhu8.inspector.stetho.httpclient;

import com.facebook.stetho.inspector.network.NetworkEventReporter;
import com.facebook.stetho.inspector.network.NetworkEventReporterImpl;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.advisor.MethodHookAdapter;
import com.fuzhu8.inspector.advisor.MethodHookParam;
import com.fuzhu8.inspector.stetho.StethoInspectorHooker;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultRequestDirector;
import org.apache.http.protocol.HttpContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * httpclient hooker
 * Created by zhkl0228 on 2018/1/5.
 */

public class HttpClientHooker implements StethoInspectorHooker {

    @Override
    public void hook(ClassLoader classLoader, final Hooker hooker) throws ClassNotFoundException, NoSuchMethodException {
        Constructor<?>[] constructors = AbstractHttpClient.class.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            hooker.hookMethod(constructor, new MethodHookAdapter() {
                @Override
                public void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    StethoInterceptor interceptor = new StethoInterceptor();
                    AbstractHttpClient httpClient = (AbstractHttpClient) param.thisObject;
                    httpClient.addRequestInterceptor(interceptor);
                    httpClient.addResponseInterceptor(interceptor);
                }
            });
        }

        Method execute = DefaultRequestDirector.class.getDeclaredMethod("execute", HttpHost.class, HttpRequest.class, HttpContext.class);
        hooker.hookMethod(execute, new MethodHookAdapter() {
            @Override
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                HttpContext context = (HttpContext) param.args[2];
                HttpClientInspectorRequest inspectorRequest = (HttpClientInspectorRequest) context.getAttribute(StethoInterceptor.INSPECTOR_REQUEST_KEY);
                if (inspectorRequest == null) {
                    return;
                }

                Throwable throwable = param.getThrowable();
                if (throwable != null) {
                    if (throwable instanceof RuntimeException) {
                        hooker.log(throwable);
                    }
                    NetworkEventReporter eventReporter = NetworkEventReporterImpl.get();
                    if (eventReporter.isEnabled()) {
                        eventReporter.httpExchangeFailed(inspectorRequest.id(), throwable.toString());
                    }
                }
            }
        });
    }

}
