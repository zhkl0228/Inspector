package com.fuzhu8.inspector.advisor;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractHooker implements Hooker, HookManager {
	
	protected abstract void log(String msg);
	protected abstract void log(Throwable t);

	@Override
	public void log(Object msg) {
		if(msg instanceof Throwable) {
			log(Throwable.class.cast(msg));
		} else if(msg != null) {
			log(msg.toString());
		}
	}
	
	private final List<Unhook> hooks = new ArrayList<Unhook>();
	
	@Override
	public final List<Unhook> getHookList() {
		return hooks;
	}
	@Override
	public final Unhook getHook(int index) {
		return index >= 0 && index < hooks.size() ? hooks.get(index) : null;
	}
	@Override
	public Unhook hookMethod(Member method, MethodHook callback) {
		return hookMethod(method, callback, false);
	}

}
