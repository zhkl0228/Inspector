package com.fuzhu8.inspector.dexposed;

import android.app.ActivityThread;
import android.content.Context;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.LoadLibraryFake;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.module.AbstractModuleStarter;
import com.fuzhu8.inspector.script.LuaScriptManager;
import com.taobao.android.dexposed.DexposedBridge;

/**
 * @author zhkl0228
 *
 */
public class DexposedModuleStarter extends AbstractModuleStarter {

	public DexposedModuleStarter(String modulePath, boolean debug, boolean trace_anti, boolean anti_thread_create,
			boolean trace_file, boolean trace_sys_call, boolean trace_trace, int patch_ssl, boolean broadcastPort) {
		super(modulePath, debug, trace_anti, anti_thread_create, trace_file, trace_sys_call, trace_trace, patch_ssl, broadcastPort);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.module.AbstractModuleStarter#log(java.lang.String)
	 */
	@Override
	protected void log(String msg) {
		DexposedBridge.log(msg);
	}

	@Override
	protected void log(Throwable t) {
		DexposedBridge.log(t);
	}

	@Override
	protected LoadLibraryFake createLoadLibraryFake(ModuleContext context) {
		return null;
	}

	@Override
	protected Context getPluginInitializeContext() {
		return ActivityThread.currentApplication();
	}

	@Override
	protected DexFileManager createDexFileManager(ModuleContext context) {
		return new DexposedDexFileManager(context);
	}

	@Override
	protected LuaScriptManager createLuaScriptManager(ModuleContext context) {
		return new DexposedLuaScriptManager(context);
	}

	@Override
	protected Inspector createInspector(ModuleContext context, DexFileManager dexFileManager,
                                        LuaScriptManager scriptManager) {
		return new DexposedInspector(context, dexFileManager, scriptManager, broadcastPort);
	}

	@Override
	protected Hooker createHooker() {
		return new DexposedHooker();
	}

	@Override
	protected void afterStartModule(ModuleContext context, Inspector inspector, final DexFileManager dexFileManager, LuaScriptManager luaScriptManager) {
		super.afterStartModule(context, inspector, dexFileManager, luaScriptManager);
	}

}
