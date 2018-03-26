package com.fuzhu8.inspector.dexposed;

import java.lang.reflect.Member;
import java.util.Set;

import org.keplerproject.luajava.LuaObject;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.advisor.Unhook;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;
import com.taobao.android.dexposed.DexposedBridge;
import com.taobao.android.dexposed.XC_MethodHook;

/**
 * @author zhkl0228
 *
 */
class DexposedHookFunctionRequest extends HookFunctionRequest<XC_MethodHook> {
	
	/**
	 * @param method nil表示构造函数，*表示所有构造函数以及方法
	 */
	DexposedHookFunctionRequest(String clazz, String method, LuaObject callback, Hooker hooker, String...params) {
		super(clazz, method, callback, hooker, params);
	}

	@Override
	protected final XC_MethodHook createCallback(Inspector inspector, DexFileManager dexFileManager) {
		if(this.callback == null) {
			return new DexposedHookHandler(inspector, dexFileManager);
		}
		
		return new DexposedLuaCallback(inspector, callback);
	}

	@Override
	protected final Unhook executeHook(Member hookMethod, ClassLoader classLoader, XC_MethodHook callback, Inspector inspector, Set<Member> hookedSet) {
		com.taobao.android.dexposed.XC_MethodHook.Unhook unhook = DexposedBridge.hookMethod(hookMethod, callback);
		
		printHook(hookMethod, classLoader, inspector);
		
		return new DexposedUnhook(true, unhook, hooker.getHookList(), hookedSet);
	}

}
