/**
 * 
 */
package com.fuzhu8.inspector.script;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

import com.fuzhu8.inspector.Inspector;

import android.util.Log;

/**
 * @author zhkl0228
 *
 */
public class LogDFunction extends InspectorFunction {

	public LogDFunction(LuaState L, Inspector inspector) {
		super(L, inspector);
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() throws LuaException {
		if(L.getTop() > 1) {
			Log.d("Inspector", String.valueOf(getParam(2)));
		}
		return 0;
	}

}
