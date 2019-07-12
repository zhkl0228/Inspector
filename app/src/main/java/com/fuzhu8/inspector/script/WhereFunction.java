package com.fuzhu8.inspector.script;

import com.fuzhu8.inspector.Inspector;

import org.keplerproject.luajava.LuaState;

/**
 * @author zhkl0228
 *
 */
public class WhereFunction extends InspectorFunction {

	WhereFunction(LuaState L, Inspector inspector) {
		super(L, inspector);
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() {
		String label = "where";
		if(L.getTop() > 1) {
			label = String.valueOf(getParam(2));
		}
		inspector.println(new Exception(label));
		return 0;
	}

}
