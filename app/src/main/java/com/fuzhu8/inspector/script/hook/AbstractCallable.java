package com.fuzhu8.inspector.script.hook;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractCallable implements Callable<Object, Object> {

	public AbstractCallable() {
		super();
	}

	public final Object call(Object thisObj, Object... args) throws Throwable {
		return invoke(thisObj, args);
	}

	@Override
	public final Object getOriginal() {
		return this;
	}

}
