package com.fuzhu8.inspector.dex.provider;

import java.util.Collection;
import java.util.Map;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.BytecodeMethod;
import com.fuzhu8.inspector.dex.DexFile;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.dex.SmaliFile;

/**
 * @author zhkl0228
 *
 */
public interface DexFileProvider {

	DexFile createDexFileData(Inspector inspector, DexFileManager dexFileManager, Map<String, BytecodeMethod> mainInstructionMap, boolean dexHunter);
	
	SmaliFile[] baksmali(Inspector inspector, DexFileManager dexFileManager, Map<String, BytecodeMethod> mainInstructionMap, boolean dexHunter, String className);

	void print(Inspector inspector);
	
	Class<?> loadClass(String className) throws ClassNotFoundException;
	
	boolean accept(String dexPath);
	
	Collection<String> getClasses();
	
	void discoverClassLoader(long cookie, ClassLoader classLoader);
	
	ClassLoader getClassLoader();
	
	void doTest();
	
	String getMyPath();

}
