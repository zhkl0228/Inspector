/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import org.keplerproject.luajava.LuaObject;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.script.hook.LuaCallback;

import de.robv.android.xposed.XC_MethodHookAlteration;
import de.robv.android.xposed.callbacks.XCMethodPointer;

/**
 * 
 * @author zhkl0228
 *
 */
class XposedLuaCallback extends XC_MethodHookAlteration<Object, Object> {
	
	private final Inspector inspector;
	private final LuaObject callback;
	
	public XposedLuaCallback(Inspector inspector, LuaObject callback) {
		super();
		this.inspector = inspector;
		this.callback = callback;
	}

	@Override
	protected Object invoked(final XCMethodPointer<Object, Object> old, Object thisObj, MethodHookParam param)
			throws Throwable {
		return LuaCallback.invoked(new XposedCallable(old), thisObj, param.method, param.args, callback, inspector);
	}

}
