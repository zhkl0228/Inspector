package com.fuzhu8.inspector;

import android.app.Application;
import android.content.pm.ApplicationInfo;

import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.inspector.root.LineListener;
import com.fuzhu8.inspector.root.RootUtil;
import com.fuzhu8.inspector.script.LuaScriptManager;
import com.fuzhu8.inspector.sdk.Sdk;

import java.io.File;
import java.util.List;

/**
 * module context
 * Created by zhkl0228 on 2018/1/7.
 */

public interface ModuleContext {

    File getDataDir();

    ClassLoader getClassLoader();

    String getProcessName();

    File getModuleLibDir();

    List<Plugin> getPlugins();

    List<String> getPluginApkList();

    void discoverPlugins(DexFileManager dexFileManager, Inspector inspector, LuaScriptManager scriptManager, ClassLoader classLoader, Hooker hooker);

    ApplicationInfo getAppInfo();

    String getModulePath();

    Sdk getSdk();

    LibraryAbi[] getAbiDirectory();

    RootUtil createRootUtil(int watchdogTimeout, LineListener lineListener);

    Hooker getHooker();

    void onAttachApplication(Application application);
    Application getApplication();

}
