package com.fuzhu8.inspector.stetho;

import com.fuzhu8.inspector.advisor.Hooker;

/**
 * stetho inspector hooker
 * Created by zhkl0228 on 2018/1/5.
 */

public interface StethoInspectorHooker {

    void hook(ClassLoader classLoader, Hooker hooker) throws ClassNotFoundException, NoSuchMethodException;

}
