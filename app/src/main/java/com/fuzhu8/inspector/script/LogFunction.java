package com.fuzhu8.inspector.script;

import com.fuzhu8.inspector.Inspector;

import org.keplerproject.luajava.LuaState;

/**
 * @author zhkl0228
 *
 */
public class LogFunction extends InspectorFunction {

	LogFunction(LuaState L, Inspector inspector) {
		super(L, inspector);
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() {
		if(L.getTop() >= 2) {
			inspector.println(getParam(2));
		}
		return 0;
	}

}
