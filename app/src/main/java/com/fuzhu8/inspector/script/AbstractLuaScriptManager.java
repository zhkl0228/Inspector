package com.fuzhu8.inspector.script;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractAdvisor;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunction;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractLuaScriptManager extends AbstractAdvisor
		implements LuaScriptManager {
	
	protected final LuaState luaState;

	public AbstractLuaScriptManager(ModuleContext context) {
		super(context);
		
		luaState = createLuaState();
	}
	
	protected Inspector inspector;
	
	@Override
	public void setInspector(Inspector inspector) {
		this.inspector = inspector;
	}

	@Override
	protected void executeHook() {
	}

	protected LuaState createLuaState() {
		LuaState luaState = LuaStateFactory.newLuaState();
		luaState.openBase();
		luaState.openLibs();
		luaState.openIo();
		luaState.openMath();
		luaState.openOs();
		luaState.openPackage();
		luaState.openTable();
		luaState.openString();
		
		luaState.LdoString("io.stdout:setvbuf(\"no\")");
		luaState.LdoString("io.stderr:setvbuf(\"no\")");
		
		return luaState;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.script.LuaScriptManager#eval(java.lang.String)
	 */
	@Override
	public void eval(String lua) throws LuaException {
		if(this.luaState.LdoString(lua) != 0) {
			throw new LuaException(luaState.toString(-1));
		}
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.script.LuaScriptManager#registerAll(com.fuzhu8.inspector.Inspector)
	 */
	@Override
	public final void registerAll(DexFileManager dexFileManager) throws Exception {
		registerFunction("log", new LogFunction(luaState, inspector));
		registerFunction("logd", new LogDFunction(luaState, inspector));
		registerFunction("where", new WhereFunction(luaState, inspector));
		registerFunction("inspect", new InspectFunction(luaState, inspector));
		registerFunction("dc", new DiscoverClassLoaderFunction(luaState, inspector, dexFileManager));
		// registerFunction("bindClass", new BindClassFunction(luaState, inspector, dexFileManager));
		// registerFunction("newInstance", new NewInstanceFunction(luaState, inspector, dexFileManager));
		registerFunction("class", new ClassFunction(luaState, inspector, dexFileManager));
		registerFunction("eFunc", new EmulatorCallFunction(luaState, inspector));
		registerFunction("json", new JSONFunction(luaState, inspector));
		
		registerGlobalObject("inspector", inspector);

		PrintFunction pf = new PrintFunction(luaState, inspector);
		luaState.newTable();
		luaState.pushString("print");
		luaState.pushJavaFunction(pf);
		luaState.setTable(-3);
		luaState.setGlobal("FZInspector");

		luaState.newTable();
		luaState.pushString("p");
		luaState.pushJavaFunction(pf);
		luaState.setTable(-3);
		luaState.setGlobal("fz");
		
		HookFunction hookFunction = createHookFunction(luaState, inspector, dexFileManager);
		registerFunction("hook", hookFunction);
		
		dexFileManager.addClassLoaderListener(hookFunction);
		
		eval("luajava.bindClass = bindClass;\n" +
				"luajava.newInstance = newInstance;\n");
	}
	
	protected abstract HookFunction createHookFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager);

	@Override
	public void registerGlobalObject(String name, Object obj) throws LuaException {
		if(name == null || obj == null) {
			throw new IllegalArgumentException();
		}
		
		luaState.pushObjectValue(obj);
		luaState.setGlobal(name);
	}

	@Override
	public void registerFunction(String name, FunctionRegister function)
			throws RegisterException {
		function.registerFunction(name);
	}

}
