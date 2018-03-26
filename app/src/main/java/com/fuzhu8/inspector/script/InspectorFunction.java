package com.fuzhu8.inspector.script;

import org.keplerproject.luajava.JavaFunction;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

import com.fuzhu8.inspector.Inspector;

/**
 * @author zhkl0228
 *
 */
public abstract class InspectorFunction extends JavaFunction implements FunctionRegister {
	
	protected final Inspector inspector;

	public InspectorFunction(LuaState L, Inspector inspector) {
		super(L);
		this.inspector = inspector;
	}

	@Override
	public void registerFunction(String name) throws RegisterException {
		try {
			register(name);
		} catch (LuaException e) {
			throw new RegisterException(e);
		}
	}
}
