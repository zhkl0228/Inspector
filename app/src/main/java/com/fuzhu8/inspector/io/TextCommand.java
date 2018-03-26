package com.fuzhu8.inspector.io;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;

/**
 * @author zhkl0228
 *
 */
public class TextCommand implements Command {
	
	private final String text;

	TextCommand(String text) {
		super();
		this.text = text;
	}

	@Override
	public void execute(StringBuffer lua, Inspector inspector, ModuleContext context) {
		if (isHelp()) {
			inspector.printHelp();
			return;
		}

		if("eval".equals(text)) {
			try {
				inspector.evalLuaScript(lua.toString());
			} finally {
				lua.setLength(0);
			}
			return;
		}
		
		lua.append(text).append('\n');
	}

	private boolean isHelp() {
		return "help".equalsIgnoreCase(text) || "?".equals(text);
	}

}
