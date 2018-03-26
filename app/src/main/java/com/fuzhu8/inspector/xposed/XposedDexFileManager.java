package com.fuzhu8.inspector.xposed;

import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.dex.AbstractDexFileManager;
import com.fuzhu8.inspector.dex.DexFileManager;


/**
 * @author zhkl0228
 *
 */
final class XposedDexFileManager extends AbstractDexFileManager implements DexFileManager {

	XposedDexFileManager(ModuleContext context) {
		super(context);
	}

}
