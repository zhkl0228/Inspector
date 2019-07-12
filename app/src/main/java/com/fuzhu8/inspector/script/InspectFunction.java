package com.fuzhu8.inspector.script;

import com.fuzhu8.inspector.Inspector;

import org.keplerproject.luajava.LuaState;

/**
 * @author zhkl0228
 *
 */
public class InspectFunction extends InspectorFunction {

	InspectFunction(LuaState L, Inspector inspector) {
		super(L, inspector);
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() {
		int top = L.getTop();
		byte[] data = null;
		String label = getClass().getSimpleName();
		if(top >= 2 && L.isString(2)) {
			data = L.toByteArray(2);
		}
		if(top >= 3) {
			label = L.toString(3);
		}
		inspector.inspect(data, label);
		return 0;
	}

}
