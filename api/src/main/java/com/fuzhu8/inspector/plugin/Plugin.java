package com.fuzhu8.inspector.plugin;

import android.app.Application;
import android.content.Context;

import com.fuzhu8.inspector.ClientConnectListener;
import com.fuzhu8.tcpcap.handler.SessionHandler;

/**
 * @author zhkl0228
 *
 */
public interface Plugin extends ClientConnectListener {
	
	/**
	 * 初始化Context
	 */
	@Deprecated
	void initialize(Context context);

	void onAttachApplication(Application application);

	void onNativeLoad(LoadedModule module, ClassLoader loader);

	void defineClass(ClassLoader classLoader, Class<?> clazz);

	String getHelpContent();

	SessionHandler createSessionHandler();

	String toString();

}
