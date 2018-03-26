package com.fuzhu8.inspector.dexposed;

import java.lang.reflect.Member;

import com.fuzhu8.inspector.script.hook.AbstractCallable;
import com.taobao.android.dexposed.callbacks.XCMethodPointer;

/**
 * @author zhkl0228
 *
 */
public class DexposedCallable extends AbstractCallable {
	
	private final XCMethodPointer<Object, Object> old;

	DexposedCallable(XCMethodPointer<Object, Object> old) {
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
		return old.getMethod();
	}

}
