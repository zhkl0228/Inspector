package com.fuzhu8.inspector.script;

import com.fuzhu8.inspector.Inspector;

import org.keplerproject.luajava.LuaState;

/**
 * @author zhkl0228
 *
 */
public class PrintFunction extends InspectorFunction {

	PrintFunction(LuaState L, Inspector inspector) {
		super(L, inspector);
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() {
		if(L.getTop() > 1) {
			inspector.println(L.getLuaObject(2));
		}
		return 0;
	}

}
