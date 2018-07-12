package com.fuzhu8.inspector.dex.provider;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.BytecodeMethod;
import com.fuzhu8.inspector.dex.ClassMethod;
import com.fuzhu8.inspector.dex.DexFileData;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.dex.SmaliFile;
import com.fuzhu8.inspector.dex.vm.DexFile;
import com.fuzhu8.inspector.dex.vm.DexFileFactory;
import com.fuzhu8.inspector.jni.DexHunter;
import com.fuzhu8.inspector.raw.dex.ClassData;
import com.fuzhu8.inspector.raw.dex.ClassDef;
import com.fuzhu8.inspector.raw.dex.Code;
import com.fuzhu8.inspector.raw.dex.Dex;
import com.fuzhu8.inspector.raw.dex.MethodId;
import com.fuzhu8.inspector.raw.dex.ProtoId;
import com.fuzhu8.inspector.raw.dex.TypeList;
import com.fuzhu8.inspector.raw.dex.smali.DexFileHeadersPointer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhkl0228
 *
 */
public class DexPathListElement extends AbstractDexFileProvider implements
		DexFileProvider {
	
	private final long cookie;
	private final DexFile dexFile;
	private final File outFile;
	
	private final DexHunter dexHunter = DexHunter.getInstance();

	/**
	 * BaseDexClassLoader
	 */
	public DexPathListElement(ClassLoader classLoader, long cookie, String fileName) {
		this(classLoader, cookie, fileName, null);
	}
	
	/**
	 * openDexFileNative
	 */
	public DexPathListElement(long cookie, String fileName, File outFile, String dataDir) {
		this(null, cookie, fileName, outFile);
		
		dexHunter.saveDexFileByCookie(cookie, dataDir);
	}

	private DexPathListElement(ClassLoader classLoader, long cookie, String fileName, File outFile) {
		super(classLoader, fileName);
		
		this.cookie = cookie;
		this.dexFile = DexFileFactory.createDexFileByCookie(cookie);
		this.dexFileHeadersPointer = dexFile.readDexFileHeadersPointer();
		this.outFile = outFile;
	}

	@Override
	protected char getPrefix() {
		return outFile == null ? '&' : '*';
	}
	
	private final DexFileHeadersPointer dexFileHeadersPointer;

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexFileProvider#print(java.lang.String, com.fuzhu8.inspector.Inspector)
	 */
	@Override
	public void print(Inspector inspector) {
		super.print(inspector);
		
		if(dexFileHeadersPointer != null) {
			inspector.println("\t" + dexFileHeadersPointer);
		}
	}

	@Override
	protected long getCookie() {
		return cookie;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (cookie ^ (cookie >>> 32));
		result = prime * result + ((dexFile == null) ? 0 : dexFile.hashCode());
		result = prime * result + ((dexFileHeadersPointer == null) ? 0 : dexFileHeadersPointer.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DexPathListElement other = (DexPathListElement) obj;
		if (cookie != other.cookie)
			return false;
		if (dexFile == null) {
			if (other.dexFile != null)
				return false;
		} else if (!dexFile.equals(other.dexFile))
			return false;
		if (dexFileHeadersPointer == null) {
			if (other.dexFileHeadersPointer != null)
				return false;
		} else if (!dexFileHeadersPointer.equals(other.dexFileHeadersPointer))
			return false;
		return true;
	}

	@Override
	public Collection<String> getClasses() {
		return getClassesFromDexFile(dexFile);
	}

	private ByteBuffer getDexData(boolean dexHunter) {
		ByteBuffer buffer;
		ClassLoader classLoader;
		if(dexHunter &&
				(classLoader = getClassLoader()) != null &&
				(buffer = this.dexHunter.dumpDexFileByCookie(cookie, classLoader)) != null) {
			return buffer;
		}

		if(this.outFile == null) {
			return dexFile.getDex();
		}

		try {
			return ByteBuffer.wrap(FileUtils.readFileToByteArray(outFile));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public DexFileData createDexFileData(Inspector inspector, DexFileManager dexFileManager, Map<String, BytecodeMethod> mainInstructionMap, boolean dexHunter) {
		ByteBuffer dexData = getDexData(dexHunter);
		if(dexFileManager != null && mainInstructionMap != null) {
			dexData = collectAll(inspector, dexFileManager, dexData, mainInstructionMap);
		}
		
		return DexFileData.createFile(dexData, FilenameUtils.getBaseName(name), null, inspector, dexHunter ? null : dexFile);
	}

	@Override
	public SmaliFile[] baksmali(Inspector inspector, DexFileManager dexFileManager,
			Map<String, BytecodeMethod> mainInstructionMap, boolean dexHunter, String className) {
		ByteBuffer dexData = getDexData(dexHunter);
		if(dexFileManager != null && mainInstructionMap != null) {
			dexData = collectAll(inspector, dexFileManager, dexData, mainInstructionMap);
		}
		
		return DexFileData.baksmali(dexData, FilenameUtils.getBaseName(name), null, inspector, dexHunter ? null : dexFile, className, this);
	}

	private ByteBuffer collectAll(Inspector inspector, DexFileManager dexFileManager, ByteBuffer buffer, Map<String, BytecodeMethod> mainInstructionMap) {
		Map<String, BytecodeMethod> instructionMap = new HashMap<>(mainInstructionMap);
		try {
			for(String className : this.getClasses()) {
				Class<?> clazz = this.loadClass(className);
				if(clazz == null) {
					continue;
				}
				
				Map<String, ClassMethod> methods = dexFileManager.readClassMethodBytecode(clazz);
				for(Map.Entry<String, ClassMethod> entry : methods.entrySet()) {
					if(BytecodeMethod.class.isInstance(entry.getValue())) {
						instructionMap.put(entry.getKey(), BytecodeMethod.class.cast(entry.getValue()));
					}
				}
			}
			
			/*BytecodeMap bytecode = InspectorNative.getBytecodeMap();
			while(bytecode != null) {
				String key = bytecode.getKey();
				if(inspector.isDebug()) {
					bytecode.dump("collect instructions: " + key + ", methodId=0x" + Long.toHexString(Pointer.nativeValue(bytecode.getMethod().getPointer())).toUpperCase(Locale.CHINA), inspector);
				}
				instructionMap.put(key, bytecode);
				bytecode = bytecode.getNext();
			}*/
		} catch(Throwable t) {
			inspector.println(t);
		}
		return fixDalvikBytecode(instructionMap, buffer, inspector);
	}

	private static ByteBuffer fixDalvikBytecode(Map<String, BytecodeMethod> instructionMap, ByteBuffer dexData, Inspector inspector) {
		try {
			if(instructionMap.isEmpty()) {
				return dexData;
			}

			dexData.mark();
			Dex dex = Dex.create(dexData);
			for(ClassDef def : dex.classDefs()) {
				if(def.getClassDataOffset() < 1) {
					continue;
				}

				String classType = dex.typeNames().get(def.getTypeIndex());
				try {
					ClassData classData = dex.readClassData(def);
					for(com.fuzhu8.inspector.raw.dex.ClassData.Method method : classData.allMethods()) {
						MethodId methodId = dex.methodIds().get(method.getMethodIndex());
						ProtoId protoId = dex.protoIds().get(methodId.getProtoIndex());
						String retType = dex.typeNames().get(protoId.getReturnTypeIndex());
						TypeList typeList = dex.readTypeList(protoId.getParametersOffset());
						
						String key = classType + "->" + dex.strings().get(methodId.getNameIndex()) + typeList + retType;
						try {
							BytecodeMethod code = instructionMap.get(key);
							short[] ins = code == null ? null : code.getInstructions();
							Code myCode = method.getCodeOffset() < 1 ? null : dex.readCode(method);
							short[] my = myCode == null ? null : myCode.getInstructions();
							if(ins != null && myCode != null && !Arrays.equals(ins, my) &&
									dex.writeMethodInstructions(def, method, ins, myCode)) {
								inspector.println("fix dalvik bytecode: " + key);
							}
						} catch(Throwable t) {
							inspector.println(new Exception("failed fix dalvik bytecode: " + key, t));
						}
					}
				} catch(Throwable t) {
					inspector.println(new Exception("failed fix dalvik bytecode: " + classType, t));
				}
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			dex.writeTo(baos);
			return ByteBuffer.wrap(baos.toByteArray());
		} catch (Throwable e) {
			inspector.println(e);
			dexData.reset();
			return dexData;
		}
	}

}
