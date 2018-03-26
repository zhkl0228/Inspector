package com.fuzhu8.inspector.xposed;

import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Set;

import com.fuzhu8.inspector.advisor.AbstractHooker;
import com.fuzhu8.inspector.advisor.DefaultMethodHook;
import com.fuzhu8.inspector.advisor.Hookable;
import com.fuzhu8.inspector.advisor.MethodHook;
import com.fuzhu8.inspector.advisor.Unhook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * @author zhkl0228
 *
 */
public class XposedHooker extends AbstractHooker {

	@Override
	public com.fuzhu8.inspector.advisor.Unhook hook(Class<?> clazz, String method, Hookable handler,
			boolean userHook,
			Class<?>... params) throws NoSuchMethodException {
		Member member;
		if(method == null) {
			member = clazz.getDeclaredConstructor(params);
		} else {
			member = clazz.getDeclaredMethod(method, params);
		}
		
		return hook(clazz, member, handler, userHook);
	}

	@Override
	public com.fuzhu8.inspector.advisor.Unhook hook(Class<?> clazz, Member member, Hookable handler,
			boolean userHook) {
		return hookMethod(member, new DefaultMethodHook(handler), userHook);
	}

	@Override
	protected void log(String msg) {
		XposedBridge.log(msg);
	}

	@Override
	protected void log(Throwable t) {
		XposedBridge.log(t);
	}

	@Override
	public Set<com.fuzhu8.inspector.advisor.Unhook> hookAllConstructors(Class<?> clazz, final MethodHook callback, boolean userHook) {
		Set<de.robv.android.xposed.XC_MethodHook.Unhook> set = XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				callback.beforeHookedMethod(new XposedMethodHookParam(param));
			}
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				callback.afterHookedMethod(new XposedMethodHookParam(param));
			}
		});
		Set<com.fuzhu8.inspector.advisor.Unhook> ret = new HashSet<com.fuzhu8.inspector.advisor.Unhook>(set.size());
		for(de.robv.android.xposed.XC_MethodHook.Unhook unhook : set) {
			Unhook uh = new XposedUnhook(userHook, unhook, getHookList(), null);
			getHookList().add(uh);
			ret.add(uh);
		}
		return ret;
	}

	@Override
	public com.fuzhu8.inspector.advisor.Unhook hookMethod(Member method, final MethodHook callback, boolean userHook) {
		de.robv.android.xposed.XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(method, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				callback.beforeHookedMethod(new XposedMethodHookParam(param));
			}
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				callback.afterHookedMethod(new XposedMethodHookParam(param));
			}
		});
		Unhook uh = new XposedUnhook(userHook, unhook, getHookList(), null);
		getHookList().add(uh);
		return uh;
	}

}
