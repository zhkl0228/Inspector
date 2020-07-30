package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.dex.ClassLoaderListener;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunction;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

import cn.android.bridge.AndroidBridge;
import cn.android.bridge.XC_MethodHook;

/**
 * @author zhkl0228
 *
 */
public class BridgeHookFunction extends HookFunction<XC_MethodHook> implements ClassLoaderListener {

	BridgeHookFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager,
					   ModuleContext context) {
		super(L, inspector, dexFileManager, context);
	}

	@Override
	protected HookFunctionRequest<XC_MethodHook> createHookFunctionRequest(String clazz, String method, LuaObject callback,
			String[] params) {
		return new BridgeHookFunctionRequest(clazz, method, callback, context.getHooker(), params);
	}

	@Override
	protected void log(Throwable t) {
		AndroidBridge.log(t);
	}

}
