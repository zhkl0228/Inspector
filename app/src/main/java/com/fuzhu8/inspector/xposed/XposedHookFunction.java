package com.fuzhu8.inspector.xposed;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.dex.ClassLoaderListener;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunction;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

import de.robv.android.xposed.XposedBridge;

/**
 * @author zhkl0228
 *
 */
public class XposedHookFunction extends HookFunction implements ClassLoaderListener {

	XposedHookFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager,
					   ModuleContext context) {
		super(L, inspector, dexFileManager, context);
	}

	@Override
	protected HookFunctionRequest createHookFunctionRequest(String clazz, String method, LuaObject callback,
			String[] params) {
		return new XposedHookFunctionRequest(clazz, method, callback, context.getHooker(), params);
	}

	@Override
	protected void log(Throwable t) {
		XposedBridge.log(t);
	}

}
