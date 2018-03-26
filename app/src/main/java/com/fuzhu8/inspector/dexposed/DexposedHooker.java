/**
 * 
 */
package com.fuzhu8.inspector.dexposed;

import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Set;

import com.fuzhu8.inspector.advisor.AbstractHooker;
import com.fuzhu8.inspector.advisor.DefaultMethodHook;
import com.fuzhu8.inspector.advisor.Hookable;
import com.fuzhu8.inspector.advisor.MethodHook;
import com.taobao.android.dexposed.DexposedBridge;
import com.taobao.android.dexposed.XC_MethodHook;
import com.taobao.android.dexposed.XC_MethodHook.Unhook;

/**
 * @author zhkl0228
 *
 */
public class DexposedHooker extends AbstractHooker {

	@Override
	public com.fuzhu8.inspector.advisor.Unhook hook(Class<?> clazz, String method, Hookable handler, boolean userHook,
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
	public com.fuzhu8.inspector.advisor.Unhook hook(Class<?> clazz, Member member, Hookable handler, boolean userHook) {
		return hookMethod(member, new DefaultMethodHook(handler), userHook);
	}

	@Override
	protected void log(String msg) {
		DexposedBridge.log(msg);
	}

	@Override
	protected void log(Throwable t) {
		DexposedBridge.log(t);
	}

	@Override
	public Set<com.fuzhu8.inspector.advisor.Unhook> hookAllConstructors(Class<?> clazz, final MethodHook callback, boolean userHook) {
		Set<Unhook> set = DexposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				callback.beforeHookedMethod(new DexposedMethodHookParam(param));
			}
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				callback.afterHookedMethod(new DexposedMethodHookParam(param));
			}
		});
		Set<com.fuzhu8.inspector.advisor.Unhook> ret = new HashSet<com.fuzhu8.inspector.advisor.Unhook>(set.size());
		for(Unhook unhook : set) {
			com.fuzhu8.inspector.advisor.Unhook uh = new DexposedUnhook(userHook, unhook, getHookList(), null);
			getHookList().add(uh);
			ret.add(uh);
		}
		return ret;
	}

	@Override
	public com.fuzhu8.inspector.advisor.Unhook hookMethod(Member method, final MethodHook callback, boolean userHook) {
		Unhook unhook = DexposedBridge.hookMethod(method, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				callback.beforeHookedMethod(new DexposedMethodHookParam(param));
			}
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				callback.afterHookedMethod(new DexposedMethodHookParam(param));
			}
		});
		com.fuzhu8.inspector.advisor.Unhook uh = new DexposedUnhook(userHook, unhook, getHookList(), null);
		getHookList().add(uh);
		return uh;
	}

}
