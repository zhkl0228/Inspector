package com.fuzhu8.inspector.advisor;

import java.lang.reflect.Member;

/**
 * @author zhkl0228
 *
 */
public abstract class MethodHookParam {
	
	/** Description of the hooked method */
	public final Member method;
	/** The <code>this</code> reference for an instance method, or null for static methods */
	public final Object thisObject;
	/** Arguments to the method call */
	public final Object[] args;

	public MethodHookParam(Member method, Object thisObject, Object[] args) {
		super();
		this.method = method;
		this.thisObject = thisObject;
		this.args = args;
	}

	/** Returns the result of the method call */
	public abstract Object getResult();

	/**
	 * Modify the result of the method call. In a "before-method-call"
	 * hook, prevents the call to the original method.
	 * You still need to "return" from the hook handler if required.
	 */
	public abstract void setResult(Object result);

	/** Returns the <code>Throwable</code> thrown by the method, or null */
	public abstract Throwable getThrowable();

	/** Returns true if an exception was thrown by the method */
	public abstract boolean hasThrowable();

	/**
	 * Modify the exception thrown of the method call. In a "before-method-call"
	 * hook, prevents the call to the original method.
	 * You still need to "return" from the hook handler if required.
	 */
	public abstract void setThrowable(Throwable throwable);

	/** Returns the result of the method call, or throws the Throwable caused by it */
	public abstract Object getResultOrThrowable() throws Throwable;

	public abstract Object getObjectExtra(String key);
	public abstract void setObjectExtra(String key, Object o);

}
