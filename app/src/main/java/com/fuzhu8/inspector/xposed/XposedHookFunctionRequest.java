package com.fuzhu8.inspector.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.Set;

import org.keplerproject.luajava.LuaObject;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.advisor.Unhook;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * @author zhkl0228
 *
 */
class XposedHookFunctionRequest extends HookFunctionRequest<XC_MethodHook> {
	
	/**
	 * @param method nil表示构造函数，*表示所有构造函数以及方法
	 */
	XposedHookFunctionRequest(String clazz, String method, LuaObject callback, Hooker hooker, String...params) {
		super(clazz, method, callback, hooker, params);
	}

	@Override
	protected final XC_MethodHook createCallback(Inspector inspector, DexFileManager dexFileManager) {
		if(this.callback == null) {
			return new XposedHookHandler(inspector, dexFileManager);
		}
		
		try {
			Class.forName("de.robv.android.xposed.XC_MethodHookAlteration");
			Constructor<?> constructor = XposedLuaCallback.class.getConstructor(Inspector.class, LuaObject.class);
			return (XC_MethodHook) constructor.newInstance(inspector, this.callback);
		} catch(Exception e) {
			throw new UnsupportedOperationException("lua callback cannot supported.", e);
		}
	}

	@Override
	protected final Unhook executeHook(Member hookMethod, ClassLoader classLoader, XC_MethodHook callback, Inspector inspector, Set<Member> hookedSet) {
		de.robv.android.xposed.XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(hookMethod, callback);
		
		printHook(hookMethod, classLoader, inspector);
		
		return new XposedUnhook(true, unhook, hooker.getHookList(), hookedSet);
	}

}
