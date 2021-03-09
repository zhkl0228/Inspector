package com.fuzhu8.inspector;

import java.io.File;
import java.lang.reflect.Method;

import android.os.Environment;
import android.util.Log;
import dalvik.system.PathClassLoader;

/**
 * @author zhkl0228
 *
 */
public class DexposedLoader implements Runnable {

	private static final String TAG = "DexposedLoader";
	
	private final String modulePath;
	
	private final boolean debug, trace_anti, anti_thread_create, trace_file, trace_sys_call, trace_trace;
	
	public DexposedLoader(String modulePath, boolean debug,
			boolean trace_anti, boolean anti_thread_create, boolean trace_file,
			boolean trace_sys_call, boolean trace_trace) {
		super();
		this.modulePath = modulePath;
		
		this.debug = debug;
		this.trace_anti = trace_anti;
		this.anti_thread_create = anti_thread_create;
		this.trace_file = trace_file;
		this.trace_sys_call = trace_sys_call;
		this.trace_trace = trace_trace;
	}

	public static void load(String modulePath) {
		new Thread(new DexposedLoader(modulePath, false, false, false, false, false, false), DexposedLoader.class.getSimpleName()).start();
	}

	@Override
	public void run() {
		try {
			while(true) {
				ClassLoader classLoader = new PathClassLoader(modulePath, new File(Environment.getDataDirectory(), "data/" + DexposedLoader.class.getPackage().getName() + "/lib").getAbsolutePath(), ClassLoader.getSystemClassLoader());
				Class<?> clazz = classLoader.loadClass("com.fuzhu8.inspector.DexposedModule");
				Method method = clazz.getDeclaredMethod("start", String.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class);
				Boolean restart = (Boolean) method.invoke(null, modulePath, debug, trace_anti, anti_thread_create, trace_file, trace_sys_call, trace_trace);
				if(!restart) {
					break;
				}
				
				Log.d(TAG, "Waiting 10 seconds to restart the module.");
				Thread.sleep(10000);
			}
		} catch(Throwable t) {
			Log.d(TAG, t.getMessage(), t);
		}
	}

}
