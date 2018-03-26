package com.fuzhu8.inspector.stetho;

import com.facebook.stetho.Stetho;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.stetho.httpclient.HttpClientHooker;
import com.fuzhu8.inspector.stetho.urlconnection.URLConnectionHooker;

import java.util.Arrays;
import java.util.List;

/**
 * Stetho initializer
 * Created by zhkl0228 on 2018/1/4.
 */

public class StethoInitializer {

    private final List<StethoInspectorHooker> hookers;

    public StethoInitializer() {
        super();

        StethoInspectorHooker urlConnectionHooker = new URLConnectionHooker();
        StethoInspectorHooker httpClientHooker = new HttpClientHooker();
        this.hookers = Arrays.asList(urlConnectionHooker, httpClientHooker);
    }

    public void initialize(Stetho.Initializer initializer, ModuleContext context) throws ClassNotFoundException, NoSuchMethodException {
        Stetho.initialize(initializer);

        ClassLoader classLoader = context.getClassLoader();
        Hooker hooker = context.getHooker();

        for (StethoInspectorHooker inspectorHooker : hookers) {
            inspectorHooker.hook(classLoader, hooker);
        }
    }

}
