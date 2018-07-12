package com.fuzhu8.inspector.script;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.DexFileManager;

/**
 * @author zhkl0228
 *
 */
public class DiscoverClassLoaderFunction extends InspectorFunction {
	
	private final DexFileManager dexFileManager;

	DiscoverClassLoaderFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager) {
		super(L, inspector);
		
		this.dexFileManager = dexFileManager;
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() {
		dexFileManager.discoverClassLoader(null);
		return 0;
	}

}
