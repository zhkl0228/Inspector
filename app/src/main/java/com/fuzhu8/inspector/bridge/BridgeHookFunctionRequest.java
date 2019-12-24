package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.advisor.Unhook;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;

import org.keplerproject.luajava.LuaObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.Set;

import cn.android.bridge.AndroidBridge;
import cn.android.bridge.XC_MethodHook;

/**
 * @author zhkl0228
 *
 */
class BridgeHookFunctionRequest extends HookFunctionRequest<XC_MethodHook> {
	
	/**
	 * @param method nil表示构造函数，*表示所有构造函数以及方法
	 */
	BridgeHookFunctionRequest(String clazz, String method, LuaObject callback, Hooker hooker, String...params) {
		super(clazz, method, callback, hooker, params);
	}

	@Override
	protected final XC_MethodHook createCallback(Inspector inspector, DexFileManager dexFileManager) {
		if(this.callback == null) {
			return new BridgeHookHandler(inspector, dexFileManager);
		}
		
		try {
			Class.forName("cn.android.bridge.XC_MethodHookAlteration");
			Constructor<?> constructor = BridgeLuaCallback.class.getConstructor(Inspector.class, LuaObject.class);
			return (XC_MethodHook) constructor.newInstance(inspector, this.callback);
		} catch(Exception e) {
			throw new UnsupportedOperationException("lua callback cannot supported.", e);
		}
	}

	@Override
	protected final Unhook executeHook(Member hookMethod, ClassLoader classLoader, XC_MethodHook callback, Inspector inspector, Set<Member> hookedSet) {
		XC_MethodHook.Unhook unhook = AndroidBridge.hookMethod(hookMethod, callback);
		
		printHook(hookMethod, classLoader, inspector);
		
		return new BridgeUnhook(true, unhook, hooker.getHookList(), hookedSet);
	}

}
