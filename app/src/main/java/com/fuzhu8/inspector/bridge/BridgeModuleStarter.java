package com.fuzhu8.inspector.bridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.LoadLibraryFake;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.module.AbstractModuleStarter;
import com.fuzhu8.inspector.script.LuaScriptManager;

import cn.android.bridge.AndroidBridge;

/**
 * @author zhkl0228
 *
 */
public class BridgeModuleStarter extends AbstractModuleStarter {

	public BridgeModuleStarter(String modulePath, boolean debug, boolean trace_anti, boolean anti_thread_create,
							   boolean trace_file, boolean trace_sys_call, boolean trace_trace, int patch_ssl, boolean broadcastPort) {
		super(modulePath, debug, trace_anti, anti_thread_create, trace_file, trace_sys_call, trace_trace, patch_ssl, broadcastPort);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.module.AbstractModuleStarter#log(java.lang.String)
	 */
	@Override
	protected void log(String msg) {
		AndroidBridge.log(msg);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.module.AbstractModuleStarter#log(java.lang.Throwable)
	 */
	@Override
	protected void log(Throwable t) {
		AndroidBridge.log(t);
	}

	@Override
	protected void afterStartModule(ModuleContext context, Inspector inspector, DexFileManager dexFileManager, LuaScriptManager luaScriptManager) {
		super.afterStartModule(context, inspector, dexFileManager, luaScriptManager);

		// new BaiduProtectKiller(context, inspector, dexFileManager);
		
		// DexposedBridge.canDexposed();
	}

	@SuppressLint("ObsoleteSdkInt")
	@Override
	protected LoadLibraryFake createLoadLibraryFake(ModuleContext context) {
		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
			return null;
		}
		
		return new BridgeLoadLibraryFake(context);
	}

	@Override
	protected Context getPluginInitializeContext() {
		return null;
	}

	@Override
	protected DexFileManager createDexFileManager(ModuleContext context) {
		return new BridgeDexFileManager(context);
	}

	@Override
	protected LuaScriptManager createLuaScriptManager(ModuleContext context) {
		return new BridgeLuaScriptManager(context);
	}

	@Override
	protected Inspector createInspector(ModuleContext context, DexFileManager dexFileManager,
                                        LuaScriptManager scriptManager) {
		return new BridgeInspector(context, dexFileManager, scriptManager, broadcastPort);
	}

	@Override
	protected Hooker createHooker() {
		return new BridgeHooker();
	}

}
