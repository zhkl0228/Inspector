package com.fuzhu8.inspector;

import java.io.File;
import java.lang.reflect.Field;

/**
 * @author zhkl0228
 *
 */
public abstract class Module {
	
	public static File getModuleDataDir(String dataDir, String modulePackage) {
		return getModuleDataDir(new File(dataDir), modulePackage);
	}
	
	private static File getModuleDataDir(File dataDir, String modulePackage) {
		return new File(dataDir.getParentFile(), modulePackage);
	}
	
	public static Object getFieldOjbect(Class<?> clazz, Object obj, String filedName) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field field = clazz.getDeclaredField(filedName);
		field.setAccessible(true);
		return field.get(obj);
	}
	
	public static int getFieldInt(Class<?> clazz, Object obj, String filedName) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
		Field field = clazz.getDeclaredField(filedName);
		field.setAccessible(true);
		return field.getInt(obj);
	}
	
	public static long getFieldLong(Class<?> clazz, Object obj, String filedName) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
		Field field = clazz.getDeclaredField(filedName);
		field.setAccessible(true);
		return field.getLong(obj);
	}

}
