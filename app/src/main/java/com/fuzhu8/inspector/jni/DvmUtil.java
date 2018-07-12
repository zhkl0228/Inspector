package com.fuzhu8.inspector.jni;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import com.fuzhu8.inspector.Module;
import com.fuzhu8.inspector.dex.vm.dvm.ClassObject;
import com.fuzhu8.inspector.dex.vm.dvm.ClassPathEntry;
import com.fuzhu8.inspector.dex.vm.dvm.DvmDex;
import com.fuzhu8.inspector.dex.vm.dvm.DvmGlobals;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;



/**
 * @author zhkl0228
 *
 */
public class DvmUtil extends Native {
	
	private static final DvmUtil INSTANCE = new DvmUtil();
	
	private DvmUtil() {
		super("dvmutil");
	}
	
	public static DvmUtil getInstance() {
		return INSTANCE;
	}
	
	public ClassObject getDexClassObject(Class<?> clazz) {
		checkSupported("getDexClassObject class=" + clazz);
		
		int classId = findClassId(clazz);
		// Log.d("DvmUtil", "getDexClassObject classId=" + classId);
		if(classId == 0) {
			return null;
		}
		
		return new ClassObject(new Pointer(classId));
	}
	
	/**
	 * 根据class找到内存地址
	 */
	private native int findClassId(Class<?> clazz);
	
	/**
	 * 根据方法找到内存地址
	 */
	public int findMethodId(Member method) {
		checkSupported("findMethodId method=" + method);
		
		if (!(method instanceof Method) && !(method instanceof Constructor<?>)) {
			throw new IllegalArgumentException("Only methods and constructors can be hooked: " + method);
		}
		
		try {
			int slot = Module.getFieldInt(method.getClass(), method, "slot");
			return findMethodId(method.getDeclaringClass(), method, slot);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public DvmGlobals getDvmGlobals() {
		checkSupported("getDvmGlobals");
		
		return new DvmGlobals(new Pointer(getDvmGlobalsPointer()));
	}
	
	private native int getDvmGlobalsPointer();
	
	/**
	 * 根据方法的slot查找内存地址
	 */
	private native int findMethodId(Class<?> clazz, Member method, int slot);
	
	public String findBootClassDvmDexName(DvmDex dvmDex) {
		DvmGlobals gDvm = getDvmGlobals();
		for(ClassPathEntry cpe : gDvm.getBootClassPath()) {
			if(dvmDex.equals(cpe.getDvmDex())) {
				return cpe.getFileName();
			}
		}
		return null;
	}

	@Override
	protected boolean hasSupported() {
		return !Platform.is64Bit() && Feature.isDvm();
	}

}
