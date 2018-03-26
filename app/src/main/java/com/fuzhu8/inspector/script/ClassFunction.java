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
public class ClassFunction extends InspectorFunction {
	
	private final DexFileManager dexFileManager;

	ClassFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager) {
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
			try {
				Class<?> cls = Class.forName(className, false, dexFileManager.getCallingClassLoader());
				L.pushJavaObject(cls);
				return 1;
			} catch(ClassNotFoundException e) {
				cnfe = e;
			}
			
			for(DexFileProvider dex : dexFileManager.dumpDexFiles(true)) {
				try {
					Class<?> cls = dex.loadClass(className);
					L.pushJavaObject(cls);
					return 1;
				} catch(ClassNotFoundException t) {
					cnfe = t;
				}
			}

			inspector.println(cnfe);
		}
		return 0;
	}

}
