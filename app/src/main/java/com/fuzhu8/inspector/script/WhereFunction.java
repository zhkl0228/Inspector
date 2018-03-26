/**
 * 
 */
package com.fuzhu8.inspector.script;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

import com.fuzhu8.inspector.Inspector;

/**
 * @author zhkl0228
 *
 */
public class WhereFunction extends InspectorFunction {

	public WhereFunction(LuaState L, Inspector inspector) {
		super(L, inspector);
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() throws LuaException {
		String label = "where";
		if(L.getTop() > 1) {
			label = String.valueOf(getParam(2));
		}
		inspector.println(new Exception(label));
		return 0;
	}

}
