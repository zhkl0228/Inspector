package com.fuzhu8.inspector.module;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Process;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.LoadLibraryFake;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.MyModuleContext;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.jni.Feature;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.inspector.script.LuaScriptManager;
import com.fuzhu8.inspector.sdk.Sdk;
import com.fuzhu8.inspector.sdk.Sdk17;
import com.fuzhu8.inspector.sdk.Sdk19;
import com.fuzhu8.inspector.sdk.Sdk21;
import com.fuzhu8.inspector.sdk.Sdk23;
import com.fuzhu8.inspector.sdk.Sdk25;
import com.fuzhu8.inspector.sdk.Sdk26;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;

import cn.banny.utils.StringUtils;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractModuleStarter implements ModuleStarter {
	
	private final String modulePath;
	private final boolean debug, trace_anti, anti_thread_create, trace_file, trace_sys_call, trace_trace;
	private final int patch_ssl;
	protected final boolean broadcastPort;

	public AbstractModuleStarter(String modulePath, boolean debug, boolean trace_anti, boolean anti_thread_create,
								 boolean trace_file, boolean trace_sys_call, boolean trace_trace, int patch_ssl, boolean broadcastPort) {
		super();
		this.modulePath = modulePath;
		this.debug = debug;
		this.trace_anti = trace_anti;
		this.anti_thread_create = anti_thread_create;
		this.trace_file = trace_file;
		this.trace_sys_call = trace_sys_call;
		this.trace_trace = trace_trace;
		this.patch_ssl = patch_ssl;
		this.broadcastPort = broadcastPort;
	}
	
	protected abstract void log(String msg);
	protected abstract void log(Throwable t);

	@SuppressLint({"PrivateApi", "Assert"})
	@Override
	public final void startModule(final ApplicationInfo appInfo, String processName, File moduleDataDir, String collect_bytecode_text, ClassLoader classLoader) {
		if (debug) {
			log("Preparing inspect [" + processName + "][" + Process.myPid() + "][" + Build.CPU_ABI + "] with moduleDataDir: " + moduleDataDir);
		}

		final Hooker hooker = createHooker();
		ClassLoader myLoader = AbstractModuleStarter.class.getClassLoader();
		ClassLoader moduleClassLoader = new ModuleClassLoader(myLoader, classLoader);
		final ModuleContext context = new MyModuleContext(moduleClassLoader,
				processName,
				moduleDataDir,
				appInfo.dataDir,
				appInfo, modulePath, null, hooker, createSdk());
		
		createLoadLibraryFake(context);
		
		DexFileManager dexFileManager = createDexFileManager(context);
		LuaScriptManager scriptManager = createLuaScriptManager(context);
		
		Inspector inspector = createInspector(context, dexFileManager, scriptManager);
		if (!Platform.isAndroid()) {
			throw new IllegalStateException();
		}
		Native.getLastError();//初始化JNA
		Thread thread = new Thread(inspector);
		thread.start();

		scriptManager.setInspector(inspector);
		dexFileManager.setInspector(inspector);
		dexFileManager.discoverClassLoader(classLoader);
		
		try {
			scriptManager.registerAll(dexFileManager);
		} catch(Throwable t) {
			log(t);
		}

		try {
			context.discoverPlugins(dexFileManager, inspector, scriptManager, moduleClassLoader, hooker);
		} catch(Throwable t) {
			log(t);
		}

		if(debug) {
			MyModuleContext.setDebug();
		}
		
		if(Feature.supportDvm() && trace_anti) {
			dexFileManager.traceAnti(appInfo.dataDir,
					anti_thread_create,
					trace_file,
					trace_sys_call,
					trace_trace,
					patch_ssl);
		}

		if(Feature.supportDvm() && !StringUtils.isEmpty(collect_bytecode_text)) {
			/*
			 * 收集所有字节码
			 */
			inspector.enableCollectBytecode(collect_bytecode_text);
			inspector.println("Enable collect bytecode successfully! ");
		}
		
		Context initializeContext = getPluginInitializeContext();
		if(initializeContext != null) {
			for(Plugin plugin : context.getPlugins()) {
				plugin.initialize(initializeContext);
			}
		}

		// new SSLTrustKiller(context);
		afterStartModule(context, inspector, dexFileManager, scriptManager);

		inspector.println("Inspect process [" + processName + "][" + Process.myPid() + "] successfully! ");
	}

	protected void afterStartModule(ModuleContext context, Inspector inspector, DexFileManager dexFileManager, LuaScriptManager luaScriptManager) {
	}

	protected abstract Hooker createHooker();

	protected abstract Inspector createInspector(ModuleContext context, DexFileManager dexFileManager,
												 LuaScriptManager scriptManager);

	protected abstract LuaScriptManager createLuaScriptManager(ModuleContext context);

	protected abstract DexFileManager createDexFileManager(ModuleContext context);

	protected abstract LoadLibraryFake createLoadLibraryFake(ModuleContext context);
	
	protected abstract Context getPluginInitializeContext();

	@SuppressLint("ObsoleteSdkInt")
	public static Sdk createSdk() {
		if (Build.VERSION.SDK_INT >= 26) {
			return new Sdk26(); // Oreo
		}

		if (Build.VERSION.SDK_INT >= 25) { // Nougat
			return new Sdk25();
		}

		if(Build.VERSION.SDK_INT >= 23) { // Marshmallow
			return new Sdk23();
		}

		if(Build.VERSION.SDK_INT >= 21) { // LOLLIPOP
			return new Sdk21();
		}

		if(Build.VERSION.SDK_INT >= 19) { // KITKAT
			return new Sdk19();
		}

		if(Build.VERSION.SDK_INT >= 16) { // JELLY_BEAN
			return new Sdk17();
		}

		throw new UnsupportedOperationException("sdk=" + Build.VERSION.SDK_INT);
	}

}
