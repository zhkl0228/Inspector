package com.fuzhu8.inspector.xposed;

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

import de.robv.android.xposed.XposedBridge;

/**
 * @author zhkl0228
 *
 */
public class XposedModuleStarter extends AbstractModuleStarter {

	public XposedModuleStarter(String modulePath, boolean debug, boolean trace_anti, boolean anti_thread_create,
			boolean trace_file, boolean trace_sys_call, boolean trace_trace, boolean broadcastPort) {
		super(modulePath, debug, trace_anti, anti_thread_create, trace_file, trace_sys_call, trace_trace, broadcastPort);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.module.AbstractModuleStarter#log(java.lang.String)
	 */
	@Override
	protected void log(String msg) {
		XposedBridge.log(msg);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.module.AbstractModuleStarter#log(java.lang.Throwable)
	 */
	@Override
	protected void log(Throwable t) {
		XposedBridge.log(t);
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
		
		return new XposedLoadLibraryFake(context);
	}

	@Override
	protected Context getPluginInitializeContext() {
		return null;
	}

	@Override
	protected DexFileManager createDexFileManager(ModuleContext context) {
		return new XposedDexFileManager(context);
	}

	@Override
	protected LuaScriptManager createLuaScriptManager(ModuleContext context) {
		return new XposedLuaScriptManager(context);
	}

	@Override
	protected Inspector createInspector(ModuleContext context, DexFileManager dexFileManager,
                                        LuaScriptManager scriptManager) {
		return new XposedInspector(context, dexFileManager, scriptManager, broadcastPort);
	}

	@Override
	protected Hooker createHooker() {
		return new XposedHooker();
	}

}
