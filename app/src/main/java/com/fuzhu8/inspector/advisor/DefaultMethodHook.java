/**
 * 
 */
package com.fuzhu8.inspector.advisor;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * @author zhkl0228
 *
 */
public class DefaultMethodHook implements MethodHook {
	
	private final Hookable handler;

	public DefaultMethodHook(Hookable advisor) {
		super();
		this.handler = advisor;
	}

	@Override
	public void beforeHookedMethod(MethodHookParam param) throws Throwable {
		try {
			Member hooked = param.method;
			
			Object value = handler.handleBefore(hooked, param.thisObject, param.args);
			if(value != null) {
				if(Method.class.isInstance(hooked) &&
						Method.class.cast(hooked).getReturnType() != void.class) {
					param.setResult(value);
				} else {
					param.setResult(null);
				}
			}
		} catch(Throwable t) {
			handler.log(t);
		}
	}

	@Override
	public void afterHookedMethod(MethodHookParam param) throws Throwable {
		try {
			if(param.hasThrowable()) {
				return;
			}
			
			Member hooked = param.method;
			
			Object result = handler.handleAfter(hooked, param.thisObject, param.args, param.getResult());
			if(Method.class.isInstance(hooked) &&
					Method.class.cast(hooked).getReturnType() != void.class) {
				param.setResult(result);
			}
		} catch(Throwable t) {
			handler.log(t);
		}
	}

}
