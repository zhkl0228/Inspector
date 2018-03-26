package com.fuzhu8.inspector.dexposed;

import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.dex.AbstractDexFileManager;

/**
 * @author zhkl0228
 *
 */
public class DexposedDexFileManager extends AbstractDexFileManager {

	DexposedDexFileManager(ModuleContext context) {
		super(context);
	}

	@Override
	protected boolean canHookDefineClass() {
		return false;
	}

}
