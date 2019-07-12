package com.fuzhu8.inspector.script;

import android.util.Log;

import com.fuzhu8.inspector.Inspector;

import org.keplerproject.luajava.LuaState;

/**
 * @author zhkl0228
 *
 */
public class LogDFunction extends InspectorFunction {

	LogDFunction(LuaState L, Inspector inspector) {
		super(L, inspector);
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() {
		if(L.getTop() > 1) {
			Log.d("Inspector", String.valueOf(getParam(2)));
		}
		return 0;
	}

}
