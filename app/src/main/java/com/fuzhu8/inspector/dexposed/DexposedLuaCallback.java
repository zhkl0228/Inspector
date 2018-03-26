package com.fuzhu8.inspector.dexposed;

import org.keplerproject.luajava.LuaObject;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.script.hook.LuaCallback;
import com.taobao.android.dexposed.XC_MethodHookAlteration;
import com.taobao.android.dexposed.callbacks.XCMethodPointer;

/**
 * 
 * @author zhkl0228
 *
 */
class DexposedLuaCallback extends XC_MethodHookAlteration<Object, Object> {
	
	private final Inspector inspector;
	private final LuaObject callback;
	
	DexposedLuaCallback(Inspector inspector, LuaObject callback) {
		super();
		this.inspector = inspector;
		this.callback = callback;
	}

	@Override
	protected Object invoked(final XCMethodPointer<Object, Object> old, Object thisObj, MethodHookParam param)
			throws Throwable {
		return LuaCallback.invoked(new DexposedCallable(old), thisObj, param.method, param.args, callback, inspector);
	}

}
