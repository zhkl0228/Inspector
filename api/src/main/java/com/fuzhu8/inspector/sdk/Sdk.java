package com.fuzhu8.inspector.sdk;

import com.fuzhu8.inspector.advisor.Hookable;
import com.fuzhu8.inspector.advisor.Hooker;

import android.content.pm.ApplicationInfo;

/**
 * @author zhkl0228
 *
 */
public interface Sdk {
	
	/**
	 * DexFile.defineClass
	 */
	void hook_defineClass(Hooker hooker, Hookable handler);

	/**
	 * DexFile.openDexFile
	 */
	void hook_openDexFile(Hooker hooker, Hookable handler);
	
	/**
	 * ISms.Stub.Proxy.sendText
	 */
	void hook_sendTextMessage(Hooker hooker, Hookable handler, Class<?> ISmsClass) throws NoSuchMethodException;
	
	/**
	 * 整合searchInspectorApk、searchPluginsApk、targetApk
	 * @return wrapper
	 */
	ApkWrapper searchApk(ApplicationInfo applicationInfo);

}
