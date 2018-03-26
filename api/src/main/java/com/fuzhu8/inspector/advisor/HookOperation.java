/**
 * 
 */
package com.fuzhu8.inspector.advisor;

import java.lang.reflect.Member;

/**
 * @author zhkl0228
 *
 */
public interface HookOperation {
	
	void hook(Class<?> clazz, final String method, Class<?>...params) throws NoSuchMethodException;
	
	void hook(String clazz, String method, Class<?>...params) throws NoSuchMethodException, ClassNotFoundException;
	
	void hookMethod(Class<?> clazz, final Member member);

}
