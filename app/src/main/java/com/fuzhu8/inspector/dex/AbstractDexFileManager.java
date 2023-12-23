package com.fuzhu8.inspector.dex;

import android.os.Build;
import android.util.Log;

import com.fuzhu8.inspector.DigestUtils;
import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.InspectorModuleContext;
import com.fuzhu8.inspector.Module;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractAdvisor;
import com.fuzhu8.inspector.advisor.MethodHook;
import com.fuzhu8.inspector.dex.provider.DexFileProvider;
import com.fuzhu8.inspector.dex.provider.DexPathListElement;
import com.fuzhu8.inspector.dex.provider.StaticDexFileElement;
import com.fuzhu8.inspector.dex.vm.dvm.DexProtoId;
import com.fuzhu8.inspector.dex.vm.dvm.DexTypeList;
import com.fuzhu8.inspector.jni.DexHunter;
import com.fuzhu8.inspector.jni.TraceAnti;
import com.fuzhu8.inspector.plugin.Plugin;
import com.sun.jna.Pointer;
import com.taobao.android.dexposed.XposedHelpers;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.banny.utils.StringUtils;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractDexFileManager extends AbstractAdvisor implements
		DexFileManager {

	private final Set<DexFileProvider> dynamicLoadedDexInfo = new LinkedHashSet<>();
	private final Set<DexFileProvider> staticLoadedDex = new LinkedHashSet<>();

	public AbstractDexFileManager(ModuleContext context) {
		super(context);
	}
	
	protected Inspector inspector;

	@Override
	public void setInspector(Inspector inspector) {
		this.inspector = inspector;
	}
	
	private final Set<ClassLoader> discoveredClassLoaders = new LinkedHashSet<>();

	@Override
	public final void discoverClassLoader(ClassLoader classLoader) {
		try {
			if (classLoader != null) {
				discoveredClassLoaders.add(classLoader);
			}
			Class<?> VMStack = Class.forName("dalvik.system.VMStack");
			for (Method method : VMStack.getDeclaredMethods()) {
				if ("getClasses".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
					Class<?>[] classes = (Class<?>[]) method.invoke(null, -1);
					for(Class<?> clazz : classes) {
						ClassLoader loader = clazz.getClassLoader();
						if(discoveredClassLoaders.add(loader)) {
							inspector.println("discoverClassLoader: " + loader);
							notifyClassLoader(loader);
						}
					}
					break;
				}
			}
		} catch(Throwable e) {
			inspector.println(e);
		}
	}

	@Override
	public ClassLoader getCallingClassLoader() throws ClassNotFoundException {
		Class<?> VMStack = Class.forName("dalvik.system.VMStack");
		return (ClassLoader) XposedHelpers.callStaticMethod(VMStack, "getCallingClassLoader");
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexFileManager#dumpDexFileInfo()
	 */
	@Override
	public Collection<DexFileProvider> dumpDexFiles(boolean includeBootClassPath) {
		Set<DexFileProvider> set;
		synchronized (this.staticLoadedDex) {
			set = new LinkedHashSet<>(this.staticLoadedDex);
		}
		try {
			readDexs(set, context.getClassLoader());
		} catch(Throwable t) {
			log(t);
		}
		synchronized (this.dynamicLoadedDexInfo) {
			set.addAll(this.dynamicLoadedDexInfo);
		}
		for(ClassLoader classLoader : discoveredClassLoaders) {
			try {
				readDexs(set, classLoader);
			} catch(Throwable t) {
				log(t);
			}
		}
		return set;
	}

	private void readDexs(Set<DexFileProvider> dexs, ClassLoader loader) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			return;
		}

		if(inspector.isDebug()) {
			inspector.println("readDexs loaderClass=" + loader.getClass() + ", loader=" + loader);
		}
		
		for(DexFileCookie cookie : readDexFileCookies(loader)) {
			DexFileProvider dex = new DexPathListElement(cookie.getClassLoader(), cookie.getCookie(), cookie.getFileName());
			dexs.add(dex);

			if(inspector.isDebug()) {
				inspector.println("readDexs cookie=" + cookie + ", dex=" + dex);
			}
		}
	}
	
	private List<DexFileCookie> readDexFileCookies(ClassLoader classLoader) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ClassNotFoundException {
		Class<?> BaseDexClassLoader;
		try {
			BaseDexClassLoader = classLoader.loadClass("dalvik.system.BaseDexClassLoader");
		} catch(ClassNotFoundException e) {
			throw new UnsupportedOperationException(e);
		}
		
		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
			return Collections.emptyList();
		}
		
		ClassLoader parent = classLoader;
		while(parent != null && !(BaseDexClassLoader.isInstance(parent))) {
			parent = parent.getParent();
		}
		
		if(parent == null) {
			return Collections.emptyList();
		}
		
		List<DexFileCookie> list = new ArrayList<DexFileCookie>();
		Object dexPathList = Module.getFieldOjbect(BaseDexClassLoader, parent, "pathList");
		Object[] dexElements = (Object[]) Module.getFieldOjbect(Class.forName("dalvik.system.DexPathList"), dexPathList, "dexElements");
		for (Object dexElement : dexElements) {
			dalvik.system.DexFile dexFile = (dalvik.system.DexFile) Module.getFieldOjbect(Class.forName("dalvik.system.DexPathList$Element"), dexElement, "dexFile");
			if (dexFile == null) {
				continue;
			}

			String fileName = (String) Module.getFieldOjbect(dalvik.system.DexFile.class, dexFile, "mFileName");
			Field field = dalvik.system.DexFile.class.getDeclaredField("mCookie");
			field.setAccessible(true);
			Object cookieObj = field.get(dexFile);
			if (Long.class.isInstance(cookieObj) || long.class.isInstance(cookieObj)) {
				long cookie = Long.class.cast(cookieObj);
				list.add(new DexFileCookie(cookie, fileName, parent));
			} else if (Integer.class.isInstance(cookieObj) || int.class.isInstance(cookieObj)) {
				int cookie = Integer.class.cast(cookieObj);
				list.add(new DexFileCookie(cookie, fileName, parent));
			} else {
				long[] cookies = (long[]) cookieObj;
				for (long cookie : cookies) {
					list.add(new DexFileCookie(cookie, fileName, parent));
				}
			}
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexFileManager#getDexFileData(java.lang.String, boolean, boolean)
	 */
	@Override
	public DexFile getDexFileData(String dexPath, boolean collectAll, boolean dexHunter) throws IOException {
		DexFileProvider dex = null;
		for(DexFileProvider dexFile : dumpDexFiles(true)) {
			if(dexFile.accept(dexPath)) {
				dex = dexFile;
				break;
			}
		}
		if(dex == null) {
			return null;
		}
		
		if(collectAll) {
			return dex.createDexFileData(inspector, this, this.instructionMap, dexHunter);
		}
		
		return dex.createDexFileData(inspector, null, null, dexHunter);
	}

	@Override
	public SmaliFile[] baksmali(String className, boolean collectAll, boolean dexHunter) throws IOException {
		DexFileProvider dex = null;
		for(DexFileProvider dexFile : dumpDexFiles(true)) {
			boolean found = false;
			for(String c : dexFile.getClasses()) {
				if(c.equals(className)) {
					found = true;
					break;
				}
			}
			if(found) {
				dex = dexFile;
				break;
			}
		}
		if(dex == null) {
			return null;
		}
		
		if(collectAll) {
			return dex.baksmali(inspector, this, this.instructionMap, dexHunter, className);
		}
		
		return dex.baksmali(inspector, null, null, dexHunter, className);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexFileManager#dumpLoadedClass(java.lang.String)
	 */
	@Override
	public Collection<String> getDexClasses(String dexPath)
			throws NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		for(DexFileProvider dex : dumpDexFiles(true)) {
			if(dex.accept(dexPath)) {
				return dex.getClasses();
			}
		}
		
		return Collections.emptyList();
	}

	@Override
	public String[] requestHookDex(String dexPath, boolean hookConstructor, String invokeFilter, String classFilter) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		List<DexFileProvider> dexList = new ArrayList<>(5);
		for(DexFileProvider dexFileProvider : dumpDexFiles(true)) {
			if(dexFileProvider.accept(dexPath)) {
				dexList.add(dexFileProvider);
			}
		}
		if(dexList.isEmpty()) {
			return new String[0];
		}
		
		Set<String> hooked = new LinkedHashSet<String>();
		for (DexFileProvider dex : dexList) {
			for(String className : dex.getClasses()) {
				try {
					if(classFilter != null && !className.contains(classFilter)) {
						continue;
					}

					Class<?> clazz = dex.loadClass(className);

					hookAllMember(clazz, inspector, hookConstructor, invokeFilter != null && className.contains(invokeFilter));
					hooked.add(className);
				} catch(ClassNotFoundException ignored) {
				} catch(Throwable t) {
					inspector.println(t);
				}
			}
		}
		
		return hooked.toArray(new String[0]);
	}

	private void hookAllMember(Class<?> clazz, Inspector inspector, boolean hookConstructor, boolean printInvoke) {
		MethodHook callback = new BytecodeCollector(inspector, this, printInvoke);
		MethodHook dumpCallback = printInvoke ? new PrintCallHook(inspector, true) : null;
		if(hookConstructor) {
			context.getHooker().hookAllConstructors(clazz, dumpCallback, false);
		}

		for(Method method : clazz.getDeclaredMethods()) {
			if(Modifier.isAbstract(method.getModifiers())) {
				continue;
			}
			
			if("toString".equals(method.getName())) {
				continue;
			}
			
			if(Modifier.isNative(method.getModifiers())) {
				if(dumpCallback != null) {
					context.getHooker().hookMethod(method, dumpCallback, true);
				}
				continue;
			}
			
			if(dumpCallback != null) {
				context.getHooker().hookMethod(method, dumpCallback, true);
				continue;
			}
			
			context.getHooker().hookMethod(method, callback, true);
		}
	}

	@Override
	public DexFileData getDexFileByteClass(Class<?> clazz, boolean dexHunter, boolean deodex) {
		throw new UnsupportedOperationException();
	}
	
	private final Map<String, BytecodeMethod> instructionMap = new HashMap<String, BytecodeMethod>();

	@Override
	public String collectBytecode(Member member, BytecodeMethod code) {
		String key = getMemberKey(member);
		return collectBytecode(key, code);
	}

	@Override
	public String collectBytecode(Class<?> clazz, BytecodeMethod code) {
		String key = getBinaryClassName(clazz) + "-><clinit>()V";
		return collectBytecode(key, code);
	}

	@Override
	public String collectBytecode(String key, BytecodeMethod code) {
		if(instructionMap.put(key, code) == null) {
			return key;
		}
		return null;
	}

	@Override
	public boolean hasClassInitBytecode(Class<?> clazz) {
		String key = getBinaryClassName(clazz) + "-><clinit>()V";
		return instructionMap.containsKey(key);
	}

	static String getMemberKey(Member member) {
		if(member instanceof Field) {
			throw new UnsupportedOperationException("getMemberKey for field: " + member);
		}
		
		StringBuilder buffer = new StringBuilder();
		buffer.append(getBinaryClassName(member.getDeclaringClass()));
		buffer.append("->");
		boolean isConstructor = member instanceof Constructor;
		Class<?>[] paramTypes;
		Class<?> returnType;
		if(isConstructor) {
			buffer.append("<init>");
			Constructor<?> constructor = Constructor.class.cast(member);
			paramTypes = constructor.getParameterTypes();
			returnType = void.class;
		} else {
			buffer.append(member.getName());
			Method method = Method.class.cast(member);
			paramTypes = method.getParameterTypes();
			returnType = method.getReturnType();
		}
		buffer.append('(');
		for(Class<?> clazz : paramTypes) {
			buffer.append(getBinaryClassName(clazz));
		}
		buffer.append(')');
		buffer.append(getBinaryClassName(returnType));
		
		return buffer.toString();
	}

	private static String getBinaryClassName(Class<?> clazz) {
		if(clazz == void.class) {
			return "V";
		}
		
		if(clazz.isArray()) {
			return clazz.getName();
		}
		
		if(!clazz.isPrimitive()) {
			return 'L' + clazz.getName().replace('.', '/') + ';';
		}
		
		clazz = Array.newInstance(clazz, 0).getClass();
		return getBinaryClassName(clazz).substring(1).replace('.', '/');
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexFileManager#dumpMemory(long, long)
	 */
	@Override
	public ByteBuffer dumpMemory(long startAddr, long length) {
		if(startAddr < 1) {
			throw new IllegalArgumentException("dumpMemory startAddr=" + startAddr);
		}
		
		return new Pointer(startAddr).getByteBuffer(0, length);
	}

	/**
	 * 此处会引起基地新版本退出
	 * @see com.fuzhu8.inspector.advisor.AbstractAdvisor#executeHook()
	 */
	@Override
	protected void executeHook() {
		// if(System.out != null) return;
		
		try {
			hook(PathClassLoader.class, null, String.class, ClassLoader.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		try {
			hook(PathClassLoader.class, null, String.class, String.class, ClassLoader.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		
		try {
			hook(DexClassLoader.class, null, String.class, String.class, String.class, ClassLoader.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		context.getSdk().hook_openDexFile(context.getHooker(), this);
		
		if(canHookDefineClass()) {
			context.getSdk().hook_defineClass(context.getHooker(), this);
		}
	}
	
	protected boolean canHookDefineClass() {
		return true;
	}

	@SuppressWarnings("unused")
	void PathClassLoader(Object thisObj, String path, ClassLoader parent) {
		PathClassLoader(thisObj, path, null, parent);
	}

	@SuppressWarnings("unused")
	void before_PathClassLoader(Object thisObj, String path, ClassLoader parent, Object[] args) {
		String newClassPath = replaceClassPath(path);
		if(newClassPath != null) {
			args[0] = newClassPath;
		}
		
		// args[1] = getClass().getClassLoader();
	}
	
	private final List<ClassLoaderListener> listeners = new ArrayList<ClassLoaderListener>();
	
	@Override
	public void addClassLoaderListener(ClassLoaderListener listener) {
		this.listeners.add(listener);
	}

	private void PathClassLoader(Object thisObj, String path, String libPath, ClassLoader parent) {
		if (InspectorModuleContext.isDebug()) {
			log("PathClassLoader path=" + path + ", parent=" + parent);
		}
		PathClassLoader classLoader = PathClassLoader.class.cast(thisObj);
		
		try {
			for(String p : path.split(File.pathSeparator)) {
				if(StringUtils.isEmpty(p)) {
					continue;
				}
				
				File file = new File(p);
				if(file.isDirectory() || !file.exists()) {
					continue;
				}

				File dataDir = context.getDataDir();
				if (dataDir == null) {
					dataDir = new File("/data/local/tmp");
				}
				File outFile = new File(dataDir.getAbsolutePath() + file.getAbsolutePath());
				FileUtils.copyFile(file, outFile);
				
				DexFileProvider dex = new StaticDexFileElement(classLoader, p, outFile);
				synchronized (staticLoadedDex) {
					staticLoadedDex.add(dex);
				}
			}
			
			DexHunter dexHunter = DexHunter.getInstance();
			for(DexFileCookie cookie : readDexFileCookies(classLoader)) {
				dexHunter.saveDexFileByCookie(cookie.getCookie(), context.getDataDir().getAbsolutePath());
			}
		} catch(Throwable t) {
			log(t);
		} finally {
			notifyClassLoader(classLoader);
		}
	}
	
	private void notifyClassLoader(ClassLoader classLoader) {
		for(ClassLoaderListener listener : this.listeners) {
			listener.notifyClassLoader(classLoader);
		}
	}

	@SuppressWarnings("unused")
	void before_PathClassLoader(Object thisObj, String path, String libPath, ClassLoader parent, Object[] args) {
		String newClassPath = replaceClassPath(path);
		if(newClassPath != null) {
			args[0] = newClassPath;
		}
		
		// args[2] = getClass().getClassLoader();
	}

	@SuppressWarnings("unused")
	void DexClassLoader(Object thisObj, String dexPath, String dexOutputDir, String libPath, ClassLoader parent) {
		if (InspectorModuleContext.isDebug()) {
			log("DexClassLoader dexPath=" + dexPath + ", parent=" + parent);
		}
		DexClassLoader classLoader = DexClassLoader.class.cast(thisObj);
		
		try {
			for(String path : dexPath.split(File.pathSeparator)) {
				File file = new File(path);
				if(file.isDirectory()) {
					file = new File(file, "classes.dex");
				}
				if(!file.isFile() || !file.canRead()) {
					continue;
				}
				
				File dataDir = context.getDataDir();
				if (dataDir == null) {
					dataDir = new File("/data/local/tmp");
				}
				File outFile = new File(dataDir.getAbsolutePath() + file.getAbsolutePath());
				FileUtils.copyFile(file, outFile);

				DexFileProvider dex = new StaticDexFileElement(classLoader, path, outFile);
				synchronized (staticLoadedDex) {
					staticLoadedDex.add(dex);
				}
			}
			
			DexHunter dexHunter = DexHunter.getInstance();
			for(DexFileCookie cookie : readDexFileCookies(classLoader)) {
				dexHunter.saveDexFileByCookie(cookie.getCookie(), context.getDataDir().getAbsolutePath());
			}
		} catch(Throwable t) {
			log(t);
		} finally {
			notifyClassLoader(classLoader);
		}
	}

	@SuppressWarnings("unused")
	void before_DexClassLoader(Object thisObj, String dexPath, String dexOutputDir, String libPath, ClassLoader parent, Object[] args) {
		String newClassPath = replaceClassPath(dexPath);
		if(newClassPath != null) {
			args[0] = newClassPath;
		}
		
		// args[3] = getClass().getClassLoader();
	}

	@Override
	public void addAnonymousDex(byte[] data, String name) {
		try {
			byte[] copy = new byte[data.length];
			System.arraycopy(data, 0, copy, 0, copy.length);
			String md5 = name + '_' + DigestUtils.md5Hex(data).toUpperCase(Locale.CHINA);
			File dataDir = context.getDataDir();
			if (dataDir == null) {
				dataDir = new File("/data/local/tmp");
			}
			File outFile = new File(dataDir, md5 + ".dex");
			FileUtils.writeByteArrayToFile(outFile, data);
			DexFileProvider dex = new StaticDexFileElement(null, md5, outFile);
			synchronized (staticLoadedDex) {
				staticLoadedDex.add(dex);
			}
		} catch (IOException e) {
			log(e);
		}
	}

	/**
	 * int openDexFileNative(String sourceName, String outputName, int flags);
	 */
	private int openDexFileNative(Class<?> thisObj, String sourceName, String outputName, int flags, int cookie) {
		return (int) openDexFileNative(thisObj, sourceName, outputName, flags, (long) cookie);
	}
	
	private long openDexFileNative(Class<?> thisObj, String sourceName, String outputName, int flags, long cookie) {
		try {
			if (InspectorModuleContext.isDebug()) {
				log("openDexFileNative sourceName=" + sourceName + ", outputName=" + outputName + ", cookie=0x" + Long.toHexString(cookie).toUpperCase(Locale.CHINA));
			}
			File dataDir = context.getDataDir();
			if (dataDir == null) {
				dataDir = new File("/data/local/tmp");
			}
			File outFile = new File(dataDir.getAbsolutePath() + sourceName);
			FileUtils.copyFile(new File(sourceName), outFile);
			DexFileProvider dex = new StaticDexFileElement(null, sourceName, outFile);
			synchronized (staticLoadedDex) {
				staticLoadedDex.add(dex);
			}
		} catch(Throwable t) {
			if (InspectorModuleContext.isDebug()) {
				log(t);
			}
		}
		
		try {
			/*
			 * 会因为当前dex被释放后，cookie指向错误的内存指针，从而导致崩溃
			 */
			if(cookie > 0 && outputName != null) {
				File inFile = new File(outputName);
				File dataDir = context.getDataDir();
				if (dataDir == null) {
					dataDir = new File("/data/local/tmp");
				}
				File outFile = new File(dataDir.getAbsolutePath() + outputName);
				FileUtils.copyFile(inFile, outFile);
				DexFileProvider dexFile = new DexPathListElement(cookie, outputName, outFile, dataDir.getAbsolutePath());
				// DexOrJar dexOrJar = new DexOrJar(new Pointer(cookie));
				// DvmDex dex = dexOrJar.isDex() ? dexOrJar.getRawDexFile().getDvmDex() : dexOrJar.getJarFile().getDvmDex();
				// info.setDexHunter(new DexHunter(dex, inspector));
				synchronized (dynamicLoadedDexInfo) {
					dynamicLoadedDexInfo.add(dexFile);
				}
			}
		} catch(Throwable t) {
			if (InspectorModuleContext.isDebug()) {
				log(t);
			} else {
				Log.w("Inspector", t);
			}
		}
		return cookie;
	}

	@SuppressWarnings("unused")
	int openDexFile(Class<?> thisObj, String sourceName, String outputName, int flags, int cookie) {
		return openDexFileNative(thisObj, sourceName, outputName, flags, cookie);
	}

	@SuppressWarnings("unused")
	long openDexFile(Class<?> thisObj, String sourceName, String outputName, int flags, long cookie) {
		return openDexFileNative(thisObj, sourceName, outputName, flags, cookie);
	}
	
	/**
	 * Class<?> defineClassNative(String name, ClassLoader loader, int cookie)
	 */
	private Class<?> defineClassNative(Object thisObj, String name, final ClassLoader loader, final int cookie, Class<?> clazz) {
		return defineClassNative(thisObj, name, loader, (long) cookie, clazz);
	}

	@SuppressWarnings("unused")
	Class<?> defineClass(Object thisObj, String name, ClassLoader loader, int cookie, Class<?> clazz) {
		return defineClassNative(thisObj, name, loader, cookie, clazz);
	}

	@SuppressWarnings("unused")
	Class<?> defineClass(Object thisObj, String name, ClassLoader loader, Object cookie, dalvik.system.DexFile dexFile, List<Throwable> suppressed, Class<?> clazz) {
		return defineClassNative(thisObj, name, loader, cookie, dexFile, clazz);
	}

	private Class<?> defineClassNative(Object thisObj, String name, ClassLoader loader, Object cookie, dalvik.system.DexFile dexFile, Class<?> clazz) {
		if (loader != null && clazz != null) {
			for (Plugin plugin : context.getPlugins()) {
				plugin.defineClass(loader, clazz);
			}
		}
		return clazz;
	}
	
	private Class<?> defineClassNative(Object thisObj, String name, final ClassLoader loader, final long cookie, Class<?> clazz) {
		return defineClassNative(thisObj, name, loader, new long[] { cookie }, clazz);
	}

	@SuppressWarnings("unused")
	Class<?> defineClass(Object thisObj, String name, ClassLoader loader, long cookie, Class<?> clazz) {
		return defineClassNative(thisObj, name, loader, cookie, clazz);
	}
	
	private Class<?> defineClassNative(Object thisObj, String name, final ClassLoader loader, final Object cookie, Class<?> clazz) {
		try {
			if (loader != null && clazz != null) {
				for (Plugin plugin : context.getPlugins()) {
					plugin.defineClass(loader, clazz);
				}
			}

			if(name.contains("com.fuzhu8")) {
				return clazz;
			}

			long[] cookies = (long[]) cookie;
			synchronized (this.dynamicLoadedDexInfo) {
				for(long ck : cookies) {
					for(DexFileProvider dexFile : this.dynamicLoadedDexInfo) {
						dexFile.discoverClassLoader(ck, loader);
					}
				}
			}
		} catch(Throwable t) {
			log(t);
		}
		return clazz;
	}

	@SuppressWarnings("unused")
	Class<?> defineClass(Object thisObj, String name, ClassLoader loader, Object cookie, Class<?> clazz) {
		return defineClassNative(thisObj, name, loader, cookie, clazz);
	}
	
	private final Map<String, String> classPathMap = new HashMap<String, String>();

	@Override
	public void addClassPathMap(String originalPath, String newPath) {
		this.classPathMap.put(originalPath, newPath);
	}

	private String replaceClassPath(String originalClassPath) {
		return this.classPathMap.get(originalClassPath);
	}

	@Override
	public ClassMethod readMethodBytecode(Member method, int methodId) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 如果是native或者是abstract的方法，则返回null
	 */
	private static ClassMethod readMethodBytecode(int methodId) {
		if(methodId == 0) {
			return null;
		}
		
		return new com.fuzhu8.inspector.dex.vm.dvm.Method(new Pointer(methodId)).getInsns();
	}

	@Override
	public BytecodeMethod readClassInitBytecode(Class<?> clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, ClassMethod> readClassMethodBytecode(Class<?> clazz) {
		throw new UnsupportedOperationException();
	}
	
	private void addInsMap(com.fuzhu8.inspector.dex.vm.DexFile dexFile, com.fuzhu8.inspector.dex.vm.dvm.Method method, String classBinaryName, Map<String, ClassMethod> insMap) {
		int protoIdx = method.getDexProto().getProtoIdx();
		DexProtoId dexProtoId = dexFile.getProtoId(protoIdx);
		
		DexTypeList dexTypeList = dexFile.dexGetProtoParameters(dexProtoId);
		String retType = dexFile.dexStringByTypeIdx(dexProtoId.getReturnTypeIdx());
		
		String key = classBinaryName + "->" + method.getName() + (dexTypeList == null ? "()" : dexTypeList) + retType;
		insMap.put(key, method.getInsns());
	}

	@Override
	public void traceAnti(String dataDir, boolean antiThreadCreate,
			boolean traceFile,
			boolean traceSysCall,
			boolean traceTrace,
			int patchSSL) {
		try {
			hook("dalvik.system.VMDebug", "isDebuggerConnected");
		} catch (Exception e) {
			log(e);
		}
		
		TraceAnti.getInstance().traceAnti(dataDir, antiThreadCreate, traceFile, traceSysCall, traceTrace, patchSSL);
	}

	@SuppressWarnings("unused")
	boolean isDebuggerConnected(Object thisObj, boolean ret) {
		Log.d("TK", "[*] Traced-anti-IsDebuggerConnected");
		return ret;
	}

}
