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
public class PrintFunction extends InspectorFunction {

	public PrintFunction(LuaState L, Inspector inspector) {
		super(L, inspector);
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() throws LuaException {
		if(L.getTop() > 1) {
			inspector.println(L.getLuaObject(2));
		}
		return 0;
	}

}
