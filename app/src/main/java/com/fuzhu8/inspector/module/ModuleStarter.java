package com.fuzhu8.inspector.module;

import java.io.File;

import android.content.pm.ApplicationInfo;

/**
 * @author zhkl0228
 *
 */
public interface ModuleStarter {
	
	void startModule(ApplicationInfo appInfo, String processName, File moduleDataDir, String collect_bytecode_text, ClassLoader classLoader);

}
