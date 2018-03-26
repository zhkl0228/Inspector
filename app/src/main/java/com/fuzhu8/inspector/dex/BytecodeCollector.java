package com.fuzhu8.inspector.dex;

import java.util.Locale;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.advisor.MethodHookParam;

/**
 * @author zhkl0228
 *
 */
public class BytecodeCollector extends PrintCallHook {
	
	private final DexFileManager dexFileManager;
	
	public BytecodeCollector(Inspector inspector,
			DexFileManager dexFileManager, boolean printInvoke) {
		super(inspector, printInvoke);
		this.dexFileManager = dexFileManager;
	}

	@Override
	public void beforeHookedMethod(MethodHookParam param) throws Throwable {
		super.beforeHookedMethod(param);
		
		try {
			ClassMethod code = dexFileManager.readMethodBytecode(param.method, 0);
			if(code == null) {
				return;
			}
			
			if(!code.isBytecodeMethod()) {
				code.dump("print native method: " + AbstractDexFileManager.getMemberKey(param.method) + ", methodId=0x" + Integer.toHexString(0).toUpperCase(Locale.CHINA), inspector);
				return;
			}
			
			String key;
			if((key = dexFileManager.collectBytecode(param.method, BytecodeMethod.class.cast(code))) != null) {
				code.dump("collect instructions: " + key + ", methodId=0x" + Integer.toHexString(0).toUpperCase(Locale.CHINA), inspector);
			}
			
			BytecodeMethod bytecode;
			final Class<?> clazz = param.method.getDeclaringClass();
			if(!dexFileManager.hasClassInitBytecode(clazz) &&
					(bytecode = dexFileManager.readClassInitBytecode(clazz)) != null &&
					(key = dexFileManager.collectBytecode(clazz, bytecode)) != null) {
				bytecode.dump("collect instructions: " + key + ", methodId=0x" + Integer.toHexString(0).toUpperCase(Locale.CHINA), inspector);
			}
		} catch(Throwable t) {
			inspector.println(t);
		}
	}

}
