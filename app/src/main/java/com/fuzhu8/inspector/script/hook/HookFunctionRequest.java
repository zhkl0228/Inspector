package com.fuzhu8.inspector.script.hook;

import com.fuzhu8.inspector.Hex;
import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.advisor.Unhook;
import com.fuzhu8.inspector.dex.DexFileManager;

import org.keplerproject.luajava.LuaObject;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author zhkl0228
 *
 */
public abstract class HookFunctionRequest<T> {

	private static final List<Method> objectMethods;
	static {
		List<Method> list = new ArrayList<>(10);
		Collections.addAll(list, Object.class.getDeclaredMethods());
		objectMethods = Collections.unmodifiableList(list);
	}

	private static boolean isObjectMethod(Method method) {
		for (Method m : objectMethods) {
			if (!m.getName().equals(method.getName())) {
				continue;
			}

			if (!Arrays.equals(m.getParameterTypes(), method.getParameterTypes())) {
				continue;
			}

			if (m.getReturnType() == method.getReturnType()) {
				return true;
			}
		}
		return false;
	}
	
	protected final String clazz, method;
	protected final LuaObject callback;
	protected final String[] params;
	
	protected final Hooker hooker;
	
	protected HookFunctionRequest(String clazz, String method, LuaObject callback, Hooker hooker, String[] params) {
		super();
		this.clazz = clazz;
		this.method = method;
		this.callback = callback;
		this.hooker = hooker;
		this.params = params;
	}
	
	final boolean tryHook(ClassLoader classLoader, Inspector inspector, DexFileManager dexFileManager, Set<Member> hookedSet) throws ClassNotFoundException, NoSuchMethodException {
		if (classLoader == null) {
			return false;
		}

		boolean hooked = false;
		Class<?> class1 = classLoader.loadClass(clazz);
		
		T callback = createCallback(inspector, dexFileManager);
		if("*".equals(method)) {
			for(Constructor<?> constructor : class1.getDeclaredConstructors()) {
				if(hookedSet.contains(constructor)) {
					continue;
				}
				hookedSet.add(constructor);
				hooker.getHookList().add(executeHook(constructor, classLoader, callback, inspector, hookedSet));
			}
			for(Method method : class1.getDeclaredMethods()) {
				if(hookedSet.contains(method) || isObjectMethod(method)) {
					continue;
				}
				hookedSet.add(method);
				hooker.getHookList().add(executeHook(method, classLoader, callback, inspector, hookedSet));
			}
			return true;
		}
		
		Class<?>[] paramClass = new Class<?>[params.length];
		for(int i = 0; i < params.length; i++) {
			paramClass[i] = findClass(classLoader, params[i]);
		}
		
		Member[] hookMethods = getHookMethods(class1, paramClass);
		for(Member hookMethod : hookMethods) {
			if(hookedSet.contains(hookMethod)) {
				continue;
			}
			hookedSet.add(hookMethod);
			hooker.getHookList().add(executeHook(hookMethod, classLoader, callback, inspector, hookedSet));
			hooked = true;
		}
		return hooked;
	}

	protected abstract T createCallback(Inspector inspector, DexFileManager dexFileManager);
	protected abstract Unhook executeHook(Member hookMethod, ClassLoader classLoader, T callback, Inspector inspector, Set<Member> hookedSet);

	private Member[] getHookMethods(Class<?> class1, Class<?>[] paramClass) throws NoSuchMethodException {
		try {
			return method == null ? new Member[] {
				class1.getDeclaredConstructor(paramClass)
			} : new Member[] {
				class1.getDeclaredMethod(method, paramClass)
			};
		} catch(NoSuchMethodException e) {
			List<Member> list = new ArrayList<>();
			if(method == null) {
				Collections.addAll(list, class1.getDeclaredConstructors());
			} else if ("@".equals(method)) {
				for (Constructor<?> constructor : class1.getDeclaredConstructors()) {
					if (Modifier.isNative(constructor.getModifiers())) {
						list.add(constructor);
					}
				}
				for (Method method : class1.getDeclaredMethods()) {
					if (Modifier.isNative(method.getModifiers())) {
						list.add(method);
					}
				}
			} else {
				for (Method method : class1.getDeclaredMethods()) {
					if (this.method.equals(method.getName())) {
						list.add(method);
					}
				}
			}
			if(list.isEmpty()) {
				throw e;
			}
			return list.toArray(new Member[0]);
		}
	}

	public static Class<?> findClass(ClassLoader classLoader, String className, int... dimensions) throws ClassNotFoundException {
		if(className.endsWith("[]")) {
			return findClass(classLoader, className.substring(0, className.length() - 2), new int[dimensions.length + 1]);
		}
		
		if("int".equalsIgnoreCase(className)) {
			return findClass(int.class, dimensions);
		}
		if("char".equalsIgnoreCase(className)) {
			return findClass(char.class, dimensions);
		}
		if("byte".equalsIgnoreCase(className)) {
			return findClass(byte.class, dimensions);
		}
		if("boolean".equalsIgnoreCase(className)) {
			return findClass(boolean.class, dimensions);
		}
		if("long".equalsIgnoreCase(className)) {
			return findClass(long.class, dimensions);
		}
		if("float".equalsIgnoreCase(className)) {
			return findClass(float.class, dimensions);
		}
		if("double".equalsIgnoreCase(className)) {
			return findClass(double.class, dimensions);
		}
		if("byte[]".equals(className)) {
			return byte[].class;
		}
		if("java.lang.String[]".equals(className)) {
			return String[].class;
		}
		if("java.lang.String_fuck".equals(className)) {
			return String.class;
		}
		
		return findClass(Class.forName(className, true, classLoader), dimensions);
	}

	private static Class<?> findClass(Class<?> clazz, int... dimensions) {
		if(dimensions == null || dimensions.length < 1) {
			return clazz;
		}
		
		return Array.newInstance(clazz, dimensions).getClass();
	}

	protected final void printHook(Member hookMethod, ClassLoader classLoader, Inspector inspector) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("hook ");
		buffer.append(hookMethod.getDeclaringClass().getCanonicalName());
		buffer.append("->");
		buffer.append((hookMethod instanceof Constructor) ? hookMethod.getDeclaringClass().getSimpleName() : hookMethod.getName());
		buffer.append('(');
		Class<?>[] paramType;
		if (hookMethod instanceof Constructor) {
			paramType = ((Constructor<?>) hookMethod).getParameterTypes();
		} else {
			paramType = ((Method) hookMethod).getParameterTypes();
		}
		if(paramType.length > 0) {
			for(Class<?> param : paramType) {
				buffer.append(param.getCanonicalName()).append(", ");
			}
			buffer.delete(buffer.length() - 2, buffer.length());
		}
		buffer.append(')');
		buffer.append(" from classloader ");
		buffer.append(classLoader);
		buffer.append(" successfully! ");
		inspector.println(buffer);
	}

	public static final String START_TIME_IN_MILLIS_KEY = "startExecuteTime";

	/**
	 * @param executeTimeInMillis 方法执行时间，单位毫秒，-1表示未知时间
	 */
	public static void afterHookedMethod(Inspector inspector, long executeTimeInMillis, Member method, Object thisObject, Object result, Object ... args) {
		Class<?> clazz = method.getDeclaringClass();
		StringBuffer buffer = new StringBuffer();
		buffer.append(clazz.getName());
		buffer.append('[');
		Class<?> thisClass = thisObject == null ? null : thisObject.getClass();
		appendObject(buffer, thisObject, thisClass);
		buffer.append(']');
		buffer.append("->");
		buffer.append(method.getName());
		buffer.append('(');
		if(args != null &&
				args.length > 0) {
			for (Object arg : args) {
				buffer.append("\n  ");
				appendParam(buffer, arg, inspector);

				if (arg instanceof Throwable) {
					inspector.println(arg);
				}
			}
			buffer.delete(buffer.length() - 2, buffer.length());
			buffer.append('\n');
		}
		buffer.append(')');
		
		if(result != null) {
			buffer.append(" ret: ");
			
			appendParam(buffer, result, inspector);
			
			buffer.delete(buffer.length() - 2, buffer.length());
		}
		
		if(method instanceof Constructor && String.class == method.getDeclaringClass()) {
			buffer.append(" string: ").append(thisObject);
		}
		
		buffer.append(" [").append(method).append(']');
		if (executeTimeInMillis != -1) {
			buffer.append(" offset=").append(executeTimeInMillis).append("ms");
		}

		if (executeTimeInMillis >= 1000) {
			inspector.err_println(buffer);
		} else {
			inspector.println(buffer);
		}
	}

	private static void appendParam(StringBuffer buffer, Object obj, Inspector inspector) {
		if(obj instanceof ByteArrayOutputStream) {
			obj = ((ByteArrayOutputStream) obj).toByteArray();
		}

		if(obj != null && obj.getClass().isArray()) {
			int len = Array.getLength(obj);
			if(obj instanceof byte[]) {
				byte[] data = (byte[]) obj;
				char[] hex = Hex.encodeHex(data);
				buffer.append(hex);
				
				inspector.inspect(data, new String(hex));
			}
			
			buffer.append('[');
			
			for(int i = 0; i < len; i++) {
				appendParam(buffer, Array.get(obj, i), inspector);
			}
			if(len > 0) {
				buffer.delete(buffer.length() - 2, buffer.length());
			}
			buffer.append("], ");
			return;
		}

		if (obj == null) {
			buffer.append((Object) null);
		} else {
			appendObject(buffer, obj, obj.getClass());
		}
		buffer.append(", ");
	}

	private static void appendObject(StringBuffer buffer, Object obj, Class<?> clazz) {
		boolean isStr = obj instanceof CharSequence;
		if(isStr) {
			buffer.append('"');
		}

		if (clazz == null | isStr || clazz.isEnum() || clazz.isPrimitive() || clazz.getName().startsWith("java")) {
			buffer.append(obj);
		} else {
			buffer.append(clazz.getName()).append("@");
			if (obj == null) {
				buffer.append("NULL");
			} else {
				buffer.append(Integer.toHexString(obj.hashCode()));
			}
		}

		if(isStr) {
			buffer.append('"');
		}
	}

}
