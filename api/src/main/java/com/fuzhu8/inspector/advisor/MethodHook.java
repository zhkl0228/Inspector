package com.fuzhu8.inspector.advisor;

/**
 * @author zhkl0228
 *
 */
public interface MethodHook {
	
	/**
	 * Called before the invocation of the method.
	 * <p>Can use {@link MethodHookParam#setResult(Object)} and {@link MethodHookParam#setThrowable(Throwable)}
	 * to prevent the original method from being called.
	 */
	void beforeHookedMethod(MethodHookParam param) throws Throwable;
	
	/**
	 * Called after the invocation of the method.
	 * <p>Can use {@link MethodHookParam#setResult(Object)} and {@link MethodHookParam#setThrowable(Throwable)}
	 * to modify the return value of the original method.
	 */
	void afterHookedMethod(MethodHookParam param) throws Throwable;

}
