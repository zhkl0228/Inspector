package com.fuzhu8.inspector.io;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;

/**
 * @author zhkl0228
 *
 */
public class LuaCommand implements Command {
	
	private final String lua;

	LuaCommand(String lua) {
		super();
		this.lua = lua;
	}

	@Override
	public void execute(StringBuffer lua, Inspector inspector, ModuleContext context) {
		inspector.evalLuaScript(this.lua);
	}

}
