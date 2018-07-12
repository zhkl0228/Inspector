package com.fuzhu8.inspector.dex;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.provider.DexFileProvider;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

/**
 * @author zhkl0228
 *
 */
public interface DexFileManager {
	
	/**
	 * dex文件列表
	 */
	Collection<DexFileProvider> dumpDexFiles(boolean includeBootClassPath);
	
	/**
	 * 获取odex文件内容
	 * @param collectAll 是否收集所有运行时的类信息
	 */
	DexFile getDexFileData(String dexPath, boolean collectAll, boolean dexHunter) throws IOException;
	
	SmaliFile[] baksmali(String className, boolean collectAll, boolean dexHunter) throws IOException;
	
	/**
	 * 通过class查找dex文件
	 */
	DexFile getDexFileByteClass(Class<?> clazz, boolean dexHunter, boolean deodex);
	
	/**
	 * 添加匿名dex
	 */
	void addAnonymousDex(byte[] data, String name);
	
	/**
	 * 获取已加载的类
	 */
	Collection<String> getDexClasses(String dexPath) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;
	
	/**
	 * 内存
	 */
	ByteBuffer dumpMemory(long startAddr, long length);
	
	/**
	 * 添加类路径替换映射
	 */
	void addClassPathMap(String originalPath, String newPath);
	
	void addClassLoaderListener(ClassLoaderListener listener);
	
	String[] requestHookDex(String dexPath, boolean hookConstructor, String invokeFilter, String classFilter) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;
	
	/**
	 * 收集dalvik字节码
	 * @return 返回key
	 */
	String collectBytecode(Member member, BytecodeMethod code);
	String collectBytecode(String key, BytecodeMethod code);
	
	/**
	 * 收集类文件的<clinit>字节码
	 * @return 返回key
	 */
	String collectBytecode(Class<?> clazz, BytecodeMethod code);
	boolean hasClassInitBytecode(Class<?> clazz);
	
	/**
	 * 读取方法字节码
	 */
	Map<String, ClassMethod> readClassMethodBytecode(Class<?> clazz);
	
	/**
	 * 读取方法字节码，如果是native或者是abstract的方法，则返回null
	 */
	ClassMethod readMethodBytecode(Member method, int methodId);
	
	/**
	 * 读取<clinit>字节码，如果没有的话返回null
	 */
	BytecodeMethod readClassInitBytecode(Class<?> clazz);
	
	/**
	 * anti反调试
	 * @param antiThreadCreate 阻止本地线程创建
	 * @param traceFile 跟踪文件
	 * @param traceSysCall 跟踪系统调用
	 * @param traceTrace 跟踪ptrace相关
	 * @param patchSSL SSL相关
	 */
	void traceAnti(String dataDir, boolean antiThreadCreate,
			boolean traceFile,
			boolean traceSysCall,
			boolean traceTrace,
			int patchSSL);

	/**
	 * 组装
	 */
	void setInspector(Inspector inspector);
	
	/**
	 * 发现ClassLoader
	 */
	void discoverClassLoader(ClassLoader classLoader);
	
	ClassLoader getCallingClassLoader() throws ClassNotFoundException;

}
