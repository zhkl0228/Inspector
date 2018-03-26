package com.fuzhu8.inspector.dex.provider;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.BytecodeMethod;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.dex.DexFileData;
import com.fuzhu8.inspector.dex.SmaliFile;
import com.fuzhu8.inspector.dex.vm.dvm.DvmDex;
import com.fuzhu8.inspector.dex.vm.dvm.DvmDexFile;
import com.fuzhu8.inspector.raw.dex.smali.DexFileHeadersPointer;
import com.sun.jna.Pointer;

/**
 * @author zhkl0228
 *
 */
public class BootClassPathElement extends AbstractDexFileProvider implements
		DexFileProvider {
	
	private final DvmDexFile dexFile;

	public BootClassPathElement(ClassLoader classLoader, String fileName, DvmDex dvmDex) {
		super(classLoader, fileName);
		
		this.dexFile = dvmDex.getDexFile();
	}

	@Override
	protected char getPrefix() {
		return '@';
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.AbstractDexFileProvider#getCookie()
	 */
	@Override
	protected long getCookie() {
		return Pointer.nativeValue(dexFile.getPointer());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dexFile == null) ? 0 : dexFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BootClassPathElement other = (BootClassPathElement) obj;
		if (dexFile == null) {
			if (other.dexFile != null)
				return false;
		} else if (!dexFile.equals(other.dexFile))
			return false;
		return true;
	}

	@Override
	public Collection<String> getClasses() {
		return getClassesFromDexFile(dexFile);
	}

	@Override
	public DexFileData createDexFileData(Inspector inspector, DexFileManager dexFileManager, Map<String, BytecodeMethod> mainInstructionMap, boolean dexHunter) {
		return DexFileData.createFile(dexFile.getDex(), FilenameUtils.getBaseName(name), null, inspector, dexHunter ? null : dexFile);
	}
	
	@Override
	public SmaliFile[] baksmali(Inspector inspector, DexFileManager dexFileManager,
			Map<String, BytecodeMethod> mainInstructionMap, boolean dexHunter, String className) {
		return DexFileData.baksmali(dexFile.getDex(), FilenameUtils.getBaseName(name), null, inspector, dexHunter ? null : dexFile, className, this);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexFileProvider#print(java.lang.String, com.fuzhu8.inspector.Inspector)
	 */
	@Override
	public void print(Inspector inspector) {
		super.print(inspector);
		
		DexFileHeadersPointer dexFileHeadersPointer = dexFile.readDexFileHeadersPointer();
		if(dexFileHeadersPointer != null) {
			inspector.println("\t" + dexFileHeadersPointer);
		}
	}

}
