package com.fuzhu8.inspector.script;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.dex.provider.DexFileProvider;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author zhkl0228
 *
 */
@Deprecated
public class NewInstanceFunction extends InspectorFunction {
	
	private final DexFileManager dexFileManager;

	public NewInstanceFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager) {
		super(L, inspector);
		
		this.dexFileManager = dexFileManager;
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() {
		int count = L.getTop();
		if(count >= 2) {
			String className = getParam(2).getString();
			LuaObject[] params = new LuaObject[count - 2];
			for(int i = 0; i < params.length; i++) {
				params[i] = getParam(3 + i);
			}
			
			Exception cnfe = null;
			for(DexFileProvider dex : dexFileManager.dumpDexFiles(true)) {
				try {
					Object obj = newInstance(className, params, dex.getClassLoader());
					if(obj != null) {
						L.pushObjectValue(obj);
						return 1;
					}
				} catch(Exception t) {
					cnfe = t;
				}
			}
			
			if(cnfe != null) {
				inspector.println(cnfe);
			}
		}
		return 0;
	}

	private Object newInstance(String className, LuaObject[] params, ClassLoader classLoader) throws ClassNotFoundException, LuaException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> clazz = HookFunctionRequest.findClass(classLoader, className);
		for(Constructor<?> constructor : clazz.getDeclaredConstructors()) {
			Class<?>[] types = constructor.getParameterTypes();
			if(types.length != params.length) {
				continue;
			}

			Object[] args = new Object[types.length];
			boolean valid = true;
			for(int i = 0; i < types.length; i++) {
				Class<?> c = types[i];
				if(c == String.class && params[i].isString()) {
					args[i] = params[i].getString();
					continue;
				}
				
				if((c == boolean.class || c == Boolean.class) && params[i].isBoolean()) {
					args[i] = params[i].getBoolean();
					continue;
				}
				
				if(Number.class.isAssignableFrom(c) && params[i].isNumber()) {
					args[i] = LuaState.convertLuaNumber(params[i].getNumber(), c);
					continue;
				}
				
				if(params[i].isNil()) {
					args[i] = null;
					continue;
				}
				
				if(!params[i].isJavaObject()) {
					valid = false;
					break;
				}
				
				args[i] = params[i].getObject();
			}
			if(!valid) {
				continue;
			}
			
			constructor.setAccessible(true);
			return constructor.newInstance(args);
		}
		return null;
	}

}
