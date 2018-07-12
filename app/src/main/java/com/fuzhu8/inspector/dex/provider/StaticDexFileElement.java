package com.fuzhu8.inspector.dex.provider;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.BytecodeMethod;
import com.fuzhu8.inspector.dex.ClassMethod;
import com.fuzhu8.inspector.dex.DexFileData;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.dex.SmaliFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import jf.dexlib.ClassDataItem;
import jf.dexlib.ClassDataItem.EncodedMethod;
import jf.dexlib.ClassDefItem;
import jf.dexlib.Code.Instruction;
import jf.dexlib.Code.InstructionIterator;
import jf.dexlib.CodeItem;
import jf.dexlib.IndexedSection;
import jf.dexlib.MethodIdItem;
import jf.dexlib.Util.ByteArrayAnnotatedOutput;

/**
 * @author zhkl0228
 *
 */
public class StaticDexFileElement extends AbstractDexFileProvider implements
		DexFileProvider {
	
	private final File dexFile;

	public StaticDexFileElement(ClassLoader classLoader, String fileName,
								File dexFile) {
		super(classLoader, fileName);
		
		this.dexFile = dexFile;
	}

	@Override
	protected char getPrefix() {
		return '#';
	}

	@Override
	public Collection<String> getClasses() {
		return Collections.emptyList();
	}

	@Override
	public DexFileData createDexFileData(Inspector inspector, DexFileManager dexFileManager, Map<String, BytecodeMethod> mainInstructionMap, boolean dexHunter) {
		try {
			ByteBuffer bb = ByteBuffer.wrap(FileUtils.readFileToByteArray(dexFile));
			if (dexFileManager != null && mainInstructionMap != null) {
				ByteBuffer buffer = fixDex(inspector, dexFileManager, bb.duplicate(), mainInstructionMap);
				return DexFileData.createFile(buffer, FilenameUtils.getBaseName(name), null, inspector, null);
			}

			return DexFileData.createFile(bb.duplicate(), FilenameUtils.getBaseName(name), null, inspector, null);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public SmaliFile[] baksmali(Inspector inspector, DexFileManager dexFileManager,
			Map<String, BytecodeMethod> mainInstructionMap, boolean dexHunter, String className) {
		try {
			ByteBuffer bb = ByteBuffer.wrap(FileUtils.readFileToByteArray(dexFile));
			if (dexFileManager != null && mainInstructionMap != null) {
				ByteBuffer buffer = fixDex(inspector, dexFileManager, bb.duplicate(), mainInstructionMap);
				return DexFileData.baksmali(buffer, FilenameUtils.getBaseName(name), null, inspector, null, className, this);
			}

			return DexFileData.baksmali(bb.duplicate(), FilenameUtils.getBaseName(name), null, inspector, null, className, this);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static ByteBuffer fixDex(Inspector inspector, DexFileManager dexFileManager, ByteBuffer buffer, Map<String, BytecodeMethod> mainInstructionMap) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.mark();
		boolean isZip = buffer.getInt() == 0x04034B50;
		buffer.reset();
		
		byte[] copy = new byte[buffer.remaining()];
		buffer.get(copy);
		JarInputStream jarIn = null;
		try {
			byte[] dexData = copy;
			if(isZip) {
				jarIn = new JarInputStream(new ByteArrayInputStream(copy), false);
				
				byte[] zipData = null;
				JarEntry entry;
				while((entry = jarIn.getNextJarEntry()) != null) {
					if(!"classes.dex".equals(entry.getName())) {
						continue;
					}
					
					zipData = IOUtils.toByteArray(jarIn);
					break;
				}
				
				if(zipData == null) {
					throw new RuntimeException("Extract classes.dex failed from jar.");
				}
				
				dexData = zipData;
			}
			jf.dexlib.DexFile dex = new jf.dexlib.DexFile(dexData);
			Map<String, BytecodeMethod> instructionMap = mainInstructionMap.isEmpty() ? new HashMap<String, BytecodeMethod>() : null;
			
			IndexedSection<ClassDefItem> classes = dex.ClassDefsSection;
			boolean update = false;
			for(ClassDefItem item : classes.getItems()) {
				ClassDataItem classData = item.getClassData();
				if(classData == null) {
					continue;
				}
				String clazz = item.getClassType().getTypeDescriptor();
				
				Class<?> loaded = instructionMap == null ? null : loadMyClass(dexFileManager, clazz.substring(1, clazz.length() - 1).replace('/', '.'));
				if(loaded != null) {
					Map<String, ClassMethod> methods = dexFileManager.readClassMethodBytecode(loaded);
					for(Map.Entry<String, ClassMethod> entry : methods.entrySet()) {
						if(BytecodeMethod.class.isInstance(entry.getValue())) {
							instructionMap.put(entry.getKey(), BytecodeMethod.class.cast(entry.getValue()));
						}
					}
				}
				
				for(EncodedMethod method : classData.getDirectMethods()) {
					if(updateDexMethod(inspector, dex, clazz, method, instructionMap == null ? mainInstructionMap : instructionMap)) {
						update = true;
					}
				}
				for(EncodedMethod method : classData.getVirtualMethods()) {
					if(updateDexMethod(inspector, dex, clazz, method, instructionMap == null ? mainInstructionMap : instructionMap)) {
						update = true;
					}
				}
			}
			if(!update) {
				return ByteBuffer.wrap(copy);
			}
			
			jf.dexlib.DexFile outDex = new jf.dexlib.DexFile();

	        for(ClassDefItem classDef : classes.getItems()){
	        	if(classDef.getClassType().getTypeDescriptor().startsWith("Landroid/support/")) {
	        		continue;
	        	}
	        	
	            classDef.internClassDefItem(outDex);
	        }
	        outDex.setSortAllItems(true);
	        outDex.place();

	        byte[] buf = new byte[outDex.getFileSize()];
	        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(buf);
	        outDex.writeTo(out);

	        jf.dexlib.DexFile.calcSignature(buf);
	        jf.dexlib.DexFile.calcChecksum(buf);
	        
	        return ByteBuffer.wrap(buf);
		} catch(Throwable t) {
			inspector.println(t);
			return ByteBuffer.wrap(copy);
		} finally {
			IOUtils.closeQuietly(jarIn);
		}
	}

	private static Class<?> loadMyClass(DexFileManager dexFileManager, String className) {
		for(DexFileProvider dex : dexFileManager.dumpDexFiles(false)) {
			if(!dex.getClasses().contains(className)) {
				continue;
			}
			
			try {
				return dex.loadClass(className);
			} catch(ClassNotFoundException ignored) {}
		}
		return null;
	}

	private static boolean updateDexMethod(Inspector inspector, jf.dexlib.DexFile dex, String clazz, EncodedMethod method, Map<String, BytecodeMethod> instructionMap) {
		MethodIdItem methodIdItem = method.method;
		String key = clazz + "->" + methodIdItem.getMethodName().getStringValue() + methodIdItem.getPrototype().getPrototypeString();
		BytecodeMethod newCode = instructionMap.get(key);
		short[] ins = newCode == null ? null : newCode.getInstructions();
		if(ins == null || ins.length < 1) {
			return false;
		}
		
		ByteBuffer bb = ByteBuffer.allocate(ins.length * 2);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for(short in : ins) {
			bb.putShort(in);
		}
		bb.flip();
		byte[] encodedInstructions = new byte[ins.length * 2];
		bb.get(encodedInstructions);
		
		final ArrayList<Instruction> instructionList = new ArrayList<>();
		try {
			InstructionIterator.IterateInstructions(dex, encodedInstructions, new InstructionIterator.ProcessInstructionDelegate() {
				public void ProcessInstruction(int codeAddress,
						Instruction instruction) {
					instructionList.add(instruction);
				}
			});
		} catch(Throwable t) {
			inspector.println(new Exception("processInstruction key=" + key + ", msg=" + t.getMessage(), t));
			return false;
		}
		
		if(instructionList.isEmpty()) {
			return false;
		}
		
		CodeItem code = method.codeItem;
		if (code == null) {
			return false;
		}
		
		code.registerCount = newCode.getRegistersSize();
        code.inWords = newCode.getInsSize();
        code.outWords = newCode.getOutsSize();
        
        code.tries = null;
        code.encodedCatchHandlers = null;
		
		inspector.println("fix dalvik bytecode: " + key);
		code.updateCode(instructionList.toArray(new Instruction[0]));
		return true;
	}

}
