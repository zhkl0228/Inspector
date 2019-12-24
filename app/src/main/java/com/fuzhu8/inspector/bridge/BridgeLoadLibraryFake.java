package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.LibraryAbi;
import com.fuzhu8.inspector.LoadLibraryFake;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.MyModuleContext;
import com.fuzhu8.inspector.advisor.AbstractHookHandler;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import cn.android.bridge.AndroidBridge;

/**
 * @author zhkl0228
 *
 */
public class BridgeLoadLibraryFake extends AbstractHookHandler implements LoadLibraryFake {

	BridgeLoadLibraryFake(ModuleContext context) {
		super(context);

		Class<?> BaseDexClassLoader;
		try {
			BaseDexClassLoader = context.getClassLoader().loadClass("dalvik.system.BaseDexClassLoader");
		} catch(ClassNotFoundException e) {
			throw new UnsupportedOperationException(e);
		}
		try {
			hook(BaseDexClassLoader, "findLibrary", String.class);
		} catch (NoSuchMethodException e) {
			log(e);
		}
	}
	
	/**
	 * String findLibrary(String libName);
	 */
	protected final String findLibrary(Object thisObj, String libName, String path) {
		if(path != null &&
				new File(path).canExecute()) {
			return path;
		}
		
		final String libFile = "lib" + libName + ".so";
		File libDir = findLibsDir(context.getAbiDirectory(), context.getModulePath(), libFile);
		if(libDir == null) {
			libDir = context.getModuleLibDir();
		}

		File file = new File(libDir, libFile);
		if(file.canExecute()) {
			if(MyModuleContext.isDebug()) {
				log("findLibrary: " + file);
			}
			return file.getAbsolutePath();
		}
		
		return path;
	}
	
	private static File findLibsDir(LibraryAbi[] abis, String apkPath, String fileName) {
		return findLibsDir(abis, apkPath, fileName, null);
	}

	@SuppressWarnings("unused")
	public static File extractAssets(String apkPath, String assertName, File parentDir) {
		File apkFile = new File(apkPath);
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(apkFile);
			JarEntry entry = jarFile.getJarEntry(assertName);
			if(entry == null) {
				return null;
			}
			return writeJarEntry(parentDir, jarFile, entry, false);
		} catch(Exception t) {
			AndroidBridge.log(t);
			return null;
		} finally {
			if(jarFile != null) {
				try { jarFile.close(); } catch(IOException ignored) {}
			}
		}
	}

	private static File findLibsDir(LibraryAbi[] abis, String apkPath, String fileName, String sub) {
		for(LibraryAbi abi : abis) {
			File libDir = abi.getAppLibDir();
			if(sub != null && sub.trim().length() > 0) {
				libDir = new File(libDir, sub.trim());
			}
			
			File targetFile = new File(libDir, fileName);
			File apkFile = new File(apkPath);
			if(abi.lastApkModified == apkFile.lastModified() &&
					targetFile.canExecute()) {
				return libDir;
			}
			libDir.mkdirs();
			
			if(abi.lastApkModified == apkFile.lastModified()) {
				continue;
			}
			
			final String prefix = "lib/" + abi.getAbi() + '/';
			final String assetsPrefix = "assets/" + abi.getAbi() + '/';
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(apkFile);
				Enumeration<JarEntry> entries = jarFile.entries();
				while(entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					
					if(entry.getName().startsWith(assetsPrefix)) {
						writeJarEntry(libDir, jarFile, entry, true);
						continue;
					}
					
					if(!entry.getName().startsWith(prefix)) {
						continue;
					}
					
					writeJarEntry(libDir, jarFile, entry, true);
				}
			} catch(Throwable t) {
				AndroidBridge.log(t);
			} finally {
				if(jarFile != null) {
					try { jarFile.close(); } catch(IOException ignored) {}
				}
				abi.lastApkModified = apkFile.lastModified();
			}
			
			if(targetFile.canExecute()) {
				return libDir;
			}
		}
		
		return null;
	}

	private static File writeJarEntry(File parentDir, JarFile jarFile, JarEntry entry, boolean executable) {
		OutputStream outputStream = null;
		InputStream inputStream = null;
		try {
			File targetFile = new File(parentDir, FilenameUtils.getName(entry.getName()));
			inputStream = jarFile.getInputStream(entry);
			outputStream = new FileOutputStream(targetFile);
			IOUtils.copy(inputStream, outputStream);
			if(executable) {
				targetFile.setExecutable(true);
			}

			if(MyModuleContext.isDebug()) {
				AndroidBridge.log("extractLibrary: " + targetFile);
			}
			return targetFile;
		} catch(Exception t) {
			AndroidBridge.log(t);
			return null;
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(outputStream);
		}
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.advisor.AbstractHookHandler#getHandler()
	 */
	@Override
	protected Object getHandler() {
		return this;
	}

}
