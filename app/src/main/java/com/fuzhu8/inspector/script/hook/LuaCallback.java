package com.fuzhu8.inspector.script.hook;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

import com.fuzhu8.inspector.Inspector;

/**
 * @author zhkl0228
 *
 */
public class LuaCallback {

	/**
	 * demo(old, thisObj, args...);<br /><br />
	 * 
	 * hook("java.lang.String", "equalsIgnoreCase", "java.lang.String", function(old, this, anotherString)<br />
	 * &nbsp;&nbsp;&nbsp;&nbsp;local ret = old:invoke(this, anotherString);<br />
	 * &nbsp;&nbsp;&nbsp;&nbsp;log("equalsIgnoreCase this=" .. this .. ", anotherString=" .. anotherString .. ", ret=" .. tostring(ret));<br />
	 * &nbsp;&nbsp;&nbsp;&nbsp;return ret;<br />
	 * end);<br />
	 */
	public static Object invoked(Callable<Object, Object> old, Object thisObj, Member member, Object[] args, LuaObject callback, Inspector inspector)
			throws Throwable {
		try {
			Class<?> retType = void.class;
			if(member instanceof Method) {
				Method method = Method.class.cast(member);
				retType = method.getReturnType();
			}
			if(inspector.isDebug()) {
				inspector.println("invoked method=" + member + ", retType=" + retType);
			}
			int off = retType == void.class || retType == Void.class ? 0 : 1;
			Object[] new_args = new Object[args.length + 2];
			new_args[0] = old.getOriginal();
			new_args[1] = thisObj;
			System.arraycopy(args, 0, new_args, 2, args.length);
			Object[] values = callback.call(new_args, off);
			Object obj = null;
			if(off == 1 && values.length > 0) {
				obj = values[0];
			}
			if(inspector.isDebug()) {
				inspector.println("invoked ret=" + obj + ", retClass=" + (obj == null ? null : obj.getClass()));
			}
			
			if(retType == void.class) {
				return null;
			}
			
			if(obj == null && retType.isPrimitive()) {
				throw new LuaException("Must return value.");
			}
			
			if(retType == void.class || retType == Void.class) {
				return null;
			}
			return getResult(retType, obj);
		} catch(Exception t) {
			inspector.println(t);
			return old.invoke(thisObj, args);
		}
	}

	private static Object getResult(Class<?> retType, Object obj) {
		if(obj == null) {
			return null;
		}
		
		if(!(obj instanceof Double)) {
			return obj;
		}
		
		Double db = Double.class.cast(obj);
		return LuaState.convertLuaNumber(db, retType);
	}

}
