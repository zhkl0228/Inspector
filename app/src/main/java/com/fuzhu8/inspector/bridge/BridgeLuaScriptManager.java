package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.AbstractLuaScriptManager;
import com.fuzhu8.inspector.script.LuaScriptManager;
import com.fuzhu8.inspector.script.hook.HookFunction;

import org.keplerproject.luajava.LuaState;

/**
 * @author zhkl0228
 *
 */
public class BridgeLuaScriptManager extends AbstractLuaScriptManager implements
		LuaScriptManager {

	BridgeLuaScriptManager(ModuleContext context) {
		super(context);
	}

	@Override
	protected HookFunction createHookFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager) {
		return new BridgeHookFunction(L, inspector, dexFileManager, context);
	}

}
