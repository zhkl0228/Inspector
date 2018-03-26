package com.fuzhu8.inspector.dexposed;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.dex.ClassLoaderListener;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunction;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;
import com.taobao.android.dexposed.DexposedBridge;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

/**
 * @author zhkl0228
 *
 */
public class DexposedHookFunction extends HookFunction implements ClassLoaderListener {

	DexposedHookFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager, ModuleContext context) {
		super(L, inspector, dexFileManager, context);
	}

	@Override
	protected HookFunctionRequest createHookFunctionRequest(String clazz, String method, LuaObject callback,
			String[] params) {
		return new DexposedHookFunctionRequest(clazz, method, callback, context.getHooker(), params);
	}

	@Override
	protected void log(Throwable t) {
		DexposedBridge.log(t);
	}

}
