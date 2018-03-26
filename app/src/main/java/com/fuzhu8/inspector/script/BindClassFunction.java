/**
 * 
 */
package com.fuzhu8.inspector.script;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.dex.provider.DexFileProvider;

/**
 * @author zhkl0228
 *
 */
@Deprecated
public class BindClassFunction extends InspectorFunction {
	
	private final DexFileManager dexFileManager;

	public BindClassFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager) {
		super(L, inspector);
		
		this.dexFileManager = dexFileManager;
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() throws LuaException {
		if(L.getTop() > 1) {
			String className = getParam(2).getString();
			
			ClassNotFoundException cnfe = null;
			for(DexFileProvider dex : dexFileManager.dumpDexFiles(true)) {
				try {
					Class<?> cls = dex.loadClass(className);
					L.pushObjectValue(cls);
					return 1;
				} catch(ClassNotFoundException t) {
					cnfe = t;
				}
			}
			
			if(cnfe != null) {
				inspector.println(cnfe);
			}
		}
		return 0;
	}

}
