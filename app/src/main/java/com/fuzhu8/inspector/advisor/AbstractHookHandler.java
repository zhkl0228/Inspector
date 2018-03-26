package com.fuzhu8.inspector.advisor;

import com.fuzhu8.inspector.ModuleContext;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexFile;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractHookHandler implements Hookable {
	
	protected final ModuleContext context;

	public AbstractHookHandler(ModuleContext context) {
		super();
		this.context = context;
	}

	/**
	 *
	 * @param method null表示hook构造函数
	 */
	@Override
	public final void hook(String clazz, String method, Class<?>...params) throws NoSuchMethodException, ClassNotFoundException {
		hook(Class.forName(clazz, true, context.getClassLoader()), method, params);
	}
	
	@Override
	public final void hookMethod(Class<?> clazz, final Member member) {
		context.getHooker().hook(clazz, member, this, false);
	}
	
	/**
	 *
	 * @param method null表示构造函数
	 */
	@Override
	public final void hook(Class<?> clazz, final String method, Class<?>...params) throws NoSuchMethodException {
		context.getHooker().hook(clazz, method, this, false, params);
	}

	private final Map<Member, Method> beforeMethodMap = new HashMap<>();
	
	@Override
	public Object handleBefore(Member hooked, Object thisObj, Object[] args) {
		try {
			Object handler = getHandler();
			if(handler == null) {
				throw new IllegalArgumentException("handler is null");
			}
			
			if(beforeMethodMap.containsKey(hooked)) {
				Method method = beforeMethodMap.get(hooked);
				if(method == null) {
					return null;
				}
				
				return invokeBeforeMethod(method, hooked, thisObj, args, handler);
			}
			
			Class<?>[] types = getParameterTypes(hooked);
			Class<?>[] paramClass = new Class<?>[types.length + 2];
			System.arraycopy(types, 0, paramClass, 1, types.length);
			paramClass[paramClass.length - 1] = Object[].class;
			
			if(Method.class.isInstance(hooked) && Modifier.isStatic(hooked.getModifiers())) {
				paramClass[0] = Class.class;
			} else {
				paramClass[0] = hooked.getDeclaringClass();
			}
			Method method = findTargetMethod(handler.getClass(), "before_" + getName(hooked, false), paramClass, hooked);
			paramClass[0] = Object.class;
			if(method == null) {
				method = findTargetMethod(handler.getClass(), "before_" + getName(hooked, false), paramClass, hooked);
			}
			if(method == null) {
				for(int i = 0; i < paramClass.length - 1; i++) {
					paramClass[i] = getObjectClass(paramClass[i]);
				}
				method = findTargetMethod(handler.getClass(), "before_" + getName(hooked, false), paramClass, hooked);
			}
			beforeMethodMap.put(hooked, method);
			if(method == null) {
				return null;
			}

			method.setAccessible(true);
			return invokeBeforeMethod(method, hooked, thisObj, args, handler);
		} catch(Throwable t) {
			log(t);
		}
		
		return null;
	}

	private static Class<?> getObjectClass(Class<?> class1) {
		String str = class1.getCanonicalName();
		if(str.startsWith("java") ||
				/*str.startsWith("android") ||*/
				str.indexOf('.') == -1) {
			return class1;
		}
		return Object.class;
	}
	
	private Object invokeBeforeMethod(Method target, Member hooked, Object thisObj, Object[] args, Object handler) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (args == null) {
			args = new Object[0];
		}
		Object[] newArgs = new Object[args.length + 2];
		newArgs[0] = thisObj;
		if(Method.class.isInstance(hooked) && Modifier.isStatic(hooked.getModifiers())) {
			newArgs[0] = hooked.getDeclaringClass();
		}
		System.arraycopy(args, 0, newArgs, 1, args.length);
		newArgs[newArgs.length - 1] = args;
		return target.invoke(handler, newArgs);
	}

	private Class<?>[] getParameterTypes(Member hooked) {
		if(Method.class.isInstance(hooked)) {
			return Method.class.cast(hooked).getParameterTypes();
		}
		return Constructor.class.cast(hooked).getParameterTypes();
	}

	private String getName(Member hooked, boolean fix) {
		if(Method.class.isInstance(hooked)) {
			if(!fix) {
				return Method.class.cast(hooked).getName();
			}
			
			return fixKeyword(Method.class.cast(hooked).getName());
		}
		return Constructor.class.cast(hooked).getDeclaringClass().getSimpleName();
	}

	private String fixKeyword(String name) {
		if(isKeywords(name)) {
			return '_' + name;
		}
		
		return name;
	}
	
	private static final Set<String> KEYWORDS_SET;
	static {
		Set<String> set = new HashSet<>();
		set.add("abstract");
		set.add("assert");
		set.add("boolean");
		set.add("break");
		set.add("byte");
		set.add("case");
		set.add("catch");
		set.add("char");
		set.add("class");
		set.add("const");
		set.add("continue");
		set.add("default");
		set.add("do");
		set.add("double");
		set.add("else");
		set.add("enum");
		set.add("extends");
		set.add("final");
		set.add("finally");
		set.add("float");
		set.add("for");
		set.add("goto");
		set.add("if");
		set.add("implements");
		set.add("import");
		set.add("instanceof");
		set.add("int");
		set.add("interface");
		set.add("long");
		set.add("native");
		set.add("new");
		set.add("package");
		set.add("private");
		set.add("protected");
		set.add("public");
		set.add("return");
		set.add("strictfp");
		set.add("short");
		set.add("static");
		set.add("super");
		set.add("switch");
		set.add("synchronized");
		set.add("this");
		set.add("throw");
		set.add("throws");
		set.add("transient");
		set.add("try");
		set.add("void");
		set.add("volatile");
		set.add("while");
		set.add("true");
		set.add("false");
		KEYWORDS_SET = Collections.unmodifiableSet(set);
	}

	private boolean isKeywords(String name) {
		return KEYWORDS_SET.contains(name);
	}

	private Method findTargetMethod(Class<?> clazz,
			String name, Class<?>[] paramClass, Member hooked) {
		if(clazz == null) {
			return null;
		}
		
		if(("defineClassNative".equals(hooked.getName()) || "defineClass".equals(hooked.getName())) && hooked.getDeclaringClass() == DexFile.class) {//fix StackOverflowError
			try {
				return clazz.getDeclaredMethod(name, paramClass);
			} catch(NoSuchMethodException e) {
				return findTargetMethod(clazz.getSuperclass(), name, paramClass, hooked);
			}
		}
		
		for(Method method : clazz.getDeclaredMethods()) {
			Class<?>[] paramTypes = method.getParameterTypes();
			if(paramTypes.length != paramClass.length) {
				continue;
			}
			
			if(!method.getName().equals(name)) {
				continue;
			}
			
			boolean flag = true;
			for(int i = 0; i < paramClass.length; i++) {
				if(paramClass[i] == paramTypes[i] ||
						paramTypes[i].isAssignableFrom(paramClass[i])) {
					continue;
				}
				flag = false;
				break;
			}
			if(flag) {
				return method;
			}
		}
		
		return findTargetMethod(clazz.getSuperclass(), name, paramClass, hooked);
	}

	private final Map<Member, Method> afterMethodMap = new HashMap<>();

	@Override
	public Object handleAfter(Member hooked, Object thisObj,
			Object[] args, Object ret) {
		try {
			Object handler = getHandler();
			if(handler == null) {
				throw new IllegalArgumentException("handler is null");
			}
			
			if(afterMethodMap.containsKey(hooked)) {
				Method method = afterMethodMap.get(hooked);
				if(method == null) {
					return ret;
				}
				
				return invokeAfterMethod(method, hooked, thisObj, ret, args, handler);
			}
			
			Class<?>[] types = getParameterTypes(hooked);
			Class<?>[] paramClass = new Class<?>[types.length + (getReturnType(hooked) != void.class ? 2 : 1)];
			System.arraycopy(types, 0, paramClass, 1, types.length);
			if(getReturnType(hooked) != void.class) {
				paramClass[paramClass.length - 1] = getReturnType(hooked);
			}

			if(Method.class.isInstance(hooked) && Modifier.isStatic(hooked.getModifiers())) {
				paramClass[0] = Class.class;
			} else {
				paramClass[0] = hooked.getDeclaringClass();
			}
			String paramArgs = Arrays.toString(paramClass);
			
			Method method = findTargetMethod(handler.getClass(), getName(hooked, true), paramClass, hooked);
			if(method == null) {
				paramClass[0] = Object.class;
				method = findTargetMethod(handler.getClass(), getName(hooked, true), paramClass, hooked);
			}
			if(method == null) {
				for(int i = 0; i < paramClass.length; i++) {
					paramClass[i] = getObjectClass(paramClass[i]);
				}
				method = findTargetMethod(handler.getClass(), getName(hooked, true), paramClass, hooked);
			}
			afterMethodMap.put(hooked, method);
			if(method == null) {
				log("handleHook target method not found: " + hooked + " in " + getClass() + " with args: " + paramArgs + ", with replacement: " + Arrays.toString(paramClass));
				return ret;
			}

			method.setAccessible(true);
			return invokeAfterMethod(method, hooked, thisObj, ret, args, handler);
		} catch(Throwable t) {
			log(t);
		}
		return ret;
	}
	
	private Object invokeAfterMethod(Method target, Member hooked, Object thisObj, Object ret, Object[] args, Object handler) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (args == null) {
			args = new Object[0];
		}
		Object[] newArgs = new Object[args.length + (getReturnType(hooked) != void.class ? 2 : 1)];
		newArgs[0] = thisObj;
		if(Method.class.isInstance(hooked) && Modifier.isStatic(hooked.getModifiers())) {
			newArgs[0] = hooked.getDeclaringClass();
		}
		System.arraycopy(args, 0, newArgs, 1, args.length);
		if(getReturnType(hooked) != void.class) {
			newArgs[newArgs.length - 1] = ret;
		}
		return target.invoke(handler, newArgs);
	}

	private Class<?> getReturnType(Member hooked) {
		if(Method.class.isInstance(hooked)) {
			return Method.class.cast(hooked).getReturnType();
		}
		return void.class;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.advisor.Hookable#getAppDataDir()
	 */
	@Override
	public final File getAppDataDir() {
		return context.getDataDir();
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.advisor.Hookable#getModuleLibDir()
	 */
	@Override
	public final File getModuleLibDir() {
		return context.getModuleLibDir();
	}
	
	/**
	 * hook handler
	 */
	protected abstract Object getHandler();

	@Override
	public void log(Object msg) {
		context.getHooker().log(msg);
	}

}
