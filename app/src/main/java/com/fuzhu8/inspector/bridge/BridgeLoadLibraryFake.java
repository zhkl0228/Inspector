package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.BuildConfig;
import com.fuzhu8.inspector.InspectorModuleContext;
import com.fuzhu8.inspector.LibraryAbi;
import com.fuzhu8.inspector.LoadLibraryFake;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractHookHandler;
import com.fuzhu8.inspector.ApkPath;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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
	@SuppressWarnings("unused")
	protected final String findLibrary(Object thisObj, String libName, String path) {
		if(path != null &&
				!path.contains(InspectorModuleContext.INSPECTOR_LIB_DIR) &&
				new File(path).canExecute()) {
			return path;
		}
		
		final String libFile = "lib" + libName + ".so";
		List<ApkPath> list = new ArrayList<>();
		list.add(new ApkPath(BuildConfig.APPLICATION_ID, context.getModulePath()));
		list.addAll(context.getPluginApkList());
		for(ApkPath apk : list) {
			File libDir = findLibsDir(context.getAbiDirectory(), apk.path, libFile, apk.packageName);
//			log("findLibrary path=" + apk.path + ", libFile=" + libFile + ", packageName=" + apk.packageName + ", libDir=" + libDir);
			if(libDir == null) {
				libDir = context.getModuleLibDir();
			}

			File file = new File(libDir, libFile);
			if(file.canExecute()) {
				if(InspectorModuleContext.isDebug()) {
					log("findLibrary: " + file);
				}
				return file.getAbsolutePath();
			}
		}
		
		return path;
	}

	@SuppressWarnings("unused")
	public static File extractAssets(String apkPath, String assertName, File parentDir) {
		File apkFile = new File(apkPath);
		try (JarFile jarFile = new JarFile(apkFile)) {
			JarEntry entry = jarFile.getJarEntry(assertName);
			if (entry == null) {
				return null;
			}
			return writeJarEntry(parentDir, jarFile, entry, false);
		} catch (Exception t) {
			AndroidBridge.log(t);
			return null;
		}
	}

	private File findLibsDir(LibraryAbi[] abis, String apkPath, String fileName, String packageName) {
		for(LibraryAbi abi : abis) {
			if (!abi.lastApkModified.containsKey(packageName)) {
				abi.lastApkModified.put(packageName, -1L);
			}

			File libDir = abi.getAppLibDir();
			if(packageName != null && packageName.trim().length() > 0) {
				libDir = new File(libDir, packageName.trim());
			}
			
			File targetFile = new File(libDir, fileName);
			File apkFile = new File(apkPath);
//			log("findLibsDir apkPath=" + apkPath + ", fileName=" + fileName + ", packageName=" + packageName + ", lastApkModified=" + abi.lastApkModified + ", lastModified=" + apkFile.lastModified());

			if(abi.lastApkModified.get(packageName) == apkFile.lastModified() &&
					targetFile.canExecute()) {
				return libDir;
			}
			libDir.mkdirs();
			
			if(abi.lastApkModified.get(packageName) == apkFile.lastModified()) {
				continue;
			}
			
			final String prefix = "lib/" + abi.getAbi() + '/';
			final String assetsPrefix = "assets/" + abi.getAbi() + '/';
			try (JarFile jarFile = new JarFile(apkFile)) {
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();

					if (entry.getName().startsWith(assetsPrefix)) {
						writeJarEntry(libDir, jarFile, entry, true);
						continue;
					}

					if (!entry.getName().startsWith(prefix)) {
						continue;
					}

					writeJarEntry(libDir, jarFile, entry, true);
				}
			} catch (Throwable t) {
				AndroidBridge.log(t);
			} finally {
				abi.lastApkModified.put(packageName, apkFile.lastModified());
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

			if(InspectorModuleContext.isDebug()) {
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
