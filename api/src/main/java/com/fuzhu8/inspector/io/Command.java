package com.fuzhu8.inspector.io;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;

/**
 * @author zhkl0228
 *
 */
public interface Command {
	
	void execute(StringBuffer lua, Inspector inspector, ModuleContext context);
	
}
