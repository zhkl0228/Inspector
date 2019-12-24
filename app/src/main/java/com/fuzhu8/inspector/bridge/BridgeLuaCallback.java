/**
 * 
 */
package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.script.hook.LuaCallback;

import org.keplerproject.luajava.LuaObject;

import cn.android.bridge.XC_MethodHook;
import cn.android.bridge.XC_MethodHookAlteration;
import cn.android.bridge.callbacks.XCMethodPointer;

/**
 * 
 * @author zhkl0228
 *
 */
class BridgeLuaCallback extends XC_MethodHookAlteration<Object, Object> {
	
	private final Inspector inspector;
	private final LuaObject callback;
	
	public BridgeLuaCallback(Inspector inspector, LuaObject callback) {
		super();
		this.inspector = inspector;
		this.callback = callback;
	}

	@Override
	protected Object invoked(final XCMethodPointer<Object, Object> old, Object thisObj, XC_MethodHook.MethodHookParam param)
			throws Throwable {
		return LuaCallback.invoked(new BridgeCallable(old), thisObj, param.method, param.args, callback, inspector);
	}

}
