package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.advisor.AbstractHooker;
import com.fuzhu8.inspector.advisor.DefaultMethodHook;
import com.fuzhu8.inspector.advisor.Hookable;
import com.fuzhu8.inspector.advisor.MethodHook;
import com.fuzhu8.inspector.advisor.Unhook;

import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Set;

import cn.android.bridge.AndroidBridge;
import cn.android.bridge.XC_MethodHook;

/**
 * @author zhkl0228
 *
 */
public class BridgeHooker extends AbstractHooker {

	@Override
	public Unhook hook(Class<?> clazz, String method, Hookable handler,
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
	public Unhook hook(Class<?> clazz, Member member, Hookable handler,
			boolean userHook) {
		return hookMethod(member, new DefaultMethodHook(handler), userHook);
	}

	@Override
	protected void log(String msg) {
		AndroidBridge.log(msg);
	}

	@Override
	protected void log(Throwable t) {
		AndroidBridge.log(t);
	}

	@Override
	public Set<Unhook> hookAllConstructors(Class<?> clazz, final MethodHook callback, boolean userHook) {
		Set<XC_MethodHook.Unhook> set = AndroidBridge.hookAllConstructors(clazz, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				callback.beforeHookedMethod(new BridgeMethodHookParam(param));
			}
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				callback.afterHookedMethod(new BridgeMethodHookParam(param));
			}
		});
		Set<Unhook> ret = new HashSet<Unhook>(set.size());
		for(XC_MethodHook.Unhook unhook : set) {
			Unhook uh = new BridgeUnhook(userHook, unhook, getHookList(), null);
			getHookList().add(uh);
			ret.add(uh);
		}
		return ret;
	}

	@Override
	public Unhook hookMethod(Member method, final MethodHook callback, boolean userHook) {
		XC_MethodHook.Unhook unhook = AndroidBridge.hookMethod(method, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				callback.beforeHookedMethod(new BridgeMethodHookParam(param));
			}
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				callback.afterHookedMethod(new BridgeMethodHookParam(param));
			}
		});
		Unhook uh = new BridgeUnhook(userHook, unhook, getHookList(), null);
		getHookList().add(uh);
		return uh;
	}

}
