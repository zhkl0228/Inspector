package com.fuzhu8.inspector.script;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.fuzhu8.inspector.DigestUtils;

/**
 * @author zhkl0228
 *
 */
public class MethodHashUtils {
	
	private static String fix(String hash) {
		if(Character.isDigit(hash.charAt(0))) {
			return 'v' + hash.substring(1);
		}
		
		return hash;
	}
	
	public static String hashMethod(Method method) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(method.getReturnType().getName());
		buffer.append('*').append(method.getName());
		buffer.append('(');
		for(Class<?> clazz : method.getParameterTypes()) {
			buffer.append(clazz.getName());
		}
		buffer.append(')');
		return fix(DigestUtils.md5Hex(buffer.toString()));
	}

	public static String hashConstructor(Constructor constructor) {
		StringBuilder buffer = new StringBuilder();
		buffer.append('(');
		for(Class<?> clazz : constructor.getParameterTypes()) {
			buffer.append(clazz.getName());
		}
		buffer.append(')');
		return fix(DigestUtils.md5Hex(buffer.toString()));
	}

	public static String hashField(Field field) {
		String buffer = field.getType().getName() +
				'*' + field.getName();
		return fix(DigestUtils.md5Hex(buffer));
	}

}
