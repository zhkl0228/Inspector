package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.dex.AbstractDexFileManager;
import com.fuzhu8.inspector.dex.DexFileManager;


/**
 * @author zhkl0228
 *
 */
final class BridgeDexFileManager extends AbstractDexFileManager implements DexFileManager {

	BridgeDexFileManager(ModuleContext context) {
		super(context);
	}

}
