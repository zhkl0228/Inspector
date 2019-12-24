package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.script.hook.AbstractCallable;

import java.lang.reflect.Member;

import cn.android.bridge.callbacks.XCMethodPointer;

/**
 * @author zhkl0228
 *
 */
public class BridgeCallable extends AbstractCallable {
	
	private final XCMethodPointer<Object, Object> old;

	BridgeCallable(XCMethodPointer<Object, Object> old) {
		super();
		this.old = old;
	}

	@Override
	public Object invoke(Object thisObj, Object... args) throws Throwable {
		return old.invoke(thisObj, args);
	}

	@Override
	public Class<?> getDeclaringClass() {
		return old.getDeclaringClass();
	}

	@Override
	public Class<?>[] getParameterTypes() {
		return old.getParameterTypes();
	}

	@Override
	public Member getMethod() {
		throw new UnsupportedOperationException();
	}

}
