package com.fuzhu8.inspector.advisor;

import java.lang.reflect.Member;
import java.util.Set;


/**
 * @author zhkl0228
 *
 */
public interface Hooker extends HookManager {
	
	com.fuzhu8.inspector.advisor.Unhook hook(Class<?> clazz, final String method, Hookable handler, boolean userHook, Class<?>...params) throws NoSuchMethodException;
	com.fuzhu8.inspector.advisor.Unhook hook(Class<?> clazz, final Member member, Hookable handler, boolean userHook);

	void log(Object msg);
	
	Set<Unhook> hookAllConstructors(Class<?> clazz, MethodHook callback, boolean userHook);
	Unhook hookMethod(Member method, MethodHook callback, boolean userHook);
	Unhook hookMethod(Member method, MethodHook callback);

}
