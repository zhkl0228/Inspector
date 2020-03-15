package com.fuzhu8.inspector;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;

import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.plugin.MultiDex;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.inspector.plugin.PluginContext;
import com.fuzhu8.inspector.root.LineListener;
import com.fuzhu8.inspector.root.RootUtil;
import com.fuzhu8.inspector.root.RootUtilClient;
import com.fuzhu8.inspector.root.RootUtilServer;
import com.fuzhu8.inspector.root.SuperUserRootUtil;
import com.fuzhu8.inspector.script.LuaScriptManager;
import com.fuzhu8.inspector.sdk.Apk;
import com.fuzhu8.inspector.sdk.ApkWrapper;
import com.fuzhu8.inspector.sdk.PluginApk;
import com.fuzhu8.inspector.sdk.Sdk;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import cn.banny.utils.StringUtils;
import dalvik.system.PathClassLoader;

/**
 * @author zhkl0228
 *
 */
public class InspectorModuleContext implements FileFilter, ModuleContext {
	
	private static boolean debug;
	public static void setDebug() {
		debug = true;
	}
	public static boolean isDebug() {
		return debug;
	}
	
	private final ClassLoader classLoader;
	private final String processName;
	private final File moduleLibDir;
	private final File dataDir;
	private final LibraryAbi[] abis;
	private final ApplicationInfo appInfo;
	private final String modulePath;
	private final Sdk sdk;
	private final RootUtilServer rootUtilServer;
	
	public InspectorModuleContext(ClassLoader classLoader, String processName, File moduleDataDir, String dataDir,
								  ApplicationInfo appInfo, String modulePath, RootUtilServer rootUtilServer,
								  Hooker hooker, Sdk sdk) {
		super();
		this.classLoader = classLoader;
		this.processName = processName;
		this.moduleLibDir = new File(moduleDataDir, "lib");
		this.dataDir = dataDir == null ? null : new File(dataDir);
		this.abis = createAbi(this.dataDir);
		this.appInfo = appInfo;
		this.modulePath = modulePath;
		this.sdk = sdk;
		this.rootUtilServer = rootUtilServer;
		this.hooker = hooker;
	}

	private LibraryAbi[] createAbi(File dataDir) {
		List<LibraryAbi> abis = new ArrayList<>();
		if(dataDir != null) {
			File inspectorLibs = new File(dataDir, "inspector_libs/" + processName.replace(':', '_'));
			String abi = Build.CPU_ABI;
			abis.add(new LibraryAbi(new File(inspectorLibs, abi), abi));
			if("armeabi-v7a".equals(abi)) {
				abi = "armeabi";
				abis.add(new LibraryAbi(new File(inspectorLibs, abi), abi));
			}
		}
		
		return abis.toArray(new LibraryAbi[0]);
	}

	@Override
	public File getDataDir() {
		return dataDir;
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public String getProcessName() {
		return processName;
	}

	@Override
	public File getModuleLibDir() {
		return moduleLibDir;
	}
	
	private List<Plugin> plugins = Collections.emptyList();
	
	@Override
	public List<Plugin> getPlugins() {
		return Collections.unmodifiableList(plugins);
	}
	@Override
	public void discoverPlugins(DexFileManager dexFileManager, Inspector inspector, LuaScriptManager scriptManager, ClassLoader classLoader, Hooker hooker) {
		plugins = new ArrayList<>();
		ApkWrapper wrapper = sdk.searchApk(appInfo);
		Apk inspectorApk = wrapper.getInspectorApk();
		List<PluginApk> list = wrapper.getPluginApks();
		Apk targetApk = wrapper.getTargetApk();

		// inspector.println(targetApk);
		
		for(PluginApk pluginApk : list) {
			JarFile jarFile = null;
			InputStream is = null;
			try {
				File libPath = new File(Environment.getDataDirectory(), "data/" + pluginApk.getPackageName() + "/lib");
				File filesDir = new File(Environment.getDataDirectory(), "data/" + targetApk.getPackageName() + "/files");

				if (pluginApk.getPluginClassName() != null) {
					try {
						final String apkPath = pluginApk.getApkFile().getAbsolutePath();
						ClassLoader pluginClassLoader = new PathClassLoader(apkPath, libPath.getAbsolutePath(), classLoader);
						MultiDex.install(filesDir, apkPath, pluginClassLoader);

						Class<?> pluginClass = pluginClassLoader.loadClass(pluginApk.getPluginClassName());
						Thread.currentThread().setContextClassLoader(pluginClassLoader);

						if (!Plugin.class.isAssignableFrom(pluginClass)) {
							inspector.err_println("    This class doesn't implement any sub-interface of Plugin, skipping it: " + pluginClass);
							continue;
						}

						Constructor<?> constructor = pluginClass.getDeclaredConstructor(PluginContext.class);
						constructor.setAccessible(true);
						PluginContext pluginContext = new PluginContext(inspector, dexFileManager, scriptManager,
								this, pluginApk.getVersionName(), pluginApk.getVersionCode(), inspectorApk.getApkFile(), pluginApk.getApkFile(), hooker,
								targetApk.getVersionName(), targetApk.getVersionCode());
						final Plugin pluginInstance = (Plugin) constructor.newInstance(pluginContext);
						plugins.add(pluginInstance);
					} catch (Throwable t) {
						inspector.printStackTrace(t);
					}
					continue;
				}

				jarFile = new JarFile(pluginApk.getApkFile());
				JarEntry entry = jarFile.getJarEntry("assets/inspector_init");
				if(entry == null) {
					inspector.err_println("assets/inspector_init not found in the APK: " + pluginApk.getApkFile());
					return;
				}
				is = jarFile.getInputStream(entry);

				ClassLoader mcl = new PathClassLoader(pluginApk.getApkFile().getAbsolutePath(), libPath.getAbsolutePath(), classLoader);
				
				BufferedReader pluginClassesReader = new BufferedReader(new InputStreamReader(is));
				String pluginClassName;
				while ((pluginClassName = pluginClassesReader.readLine()) != null) {
					pluginClassName = pluginClassName.trim();
					if (StringUtils.isEmpty(pluginClassName) || pluginClassName.startsWith("#")) {
						continue;
					}

					try {
						Class<?> pluginClass = mcl.loadClass(pluginClassName);

						if (!Plugin.class.isAssignableFrom(pluginClass)) {
							inspector.err_println("    This class doesn't implement any sub-interface of Plugin, skipping it: " + pluginClass);
							continue;
						}

						Constructor<?> constructor = pluginClass.getConstructor(PluginContext.class);
						PluginContext pc = new PluginContext(inspector, dexFileManager, scriptManager,
								this, pluginApk.getVersionName(), pluginApk.getVersionCode(), inspectorApk.getApkFile(), pluginApk.getApkFile(), hooker,
								targetApk.getVersionName(), targetApk.getVersionCode());
						final Plugin pluginInstance = (Plugin) constructor.newInstance(pc);
						plugins.add(pluginInstance);
					} catch (Throwable t) {
						inspector.printStackTrace(t);
					}
				}
			} catch (IOException e) {
				inspector.printStackTrace(e);
			} finally {
				org.apache.commons.io.IOUtils.closeQuietly(is);
				if(jarFile != null) {
					try {
						jarFile.close();
					} catch(IOException ignored) {}
				}
			}
		}
		
		inspector.out_println("Discover plugins: " + plugins + " for " + targetApk);
	}

	@Override
	public boolean accept(File file) {
		if(file.isDirectory()) {
			return false;
		}
		
		String ext = FilenameUtils.getExtension(file.getName());
		return "jar".equalsIgnoreCase(ext) || "apk".equalsIgnoreCase(ext);
	}

	@Override
	public ApplicationInfo getAppInfo() {
		return appInfo;
	}

	@Override
	public String getModulePath() {
		return modulePath;
	}

	@Override
	public Sdk getSdk() {
		return sdk;
	}

	@Override
	public LibraryAbi[] getAbiDirectory() {
		return abis;
	}
	
	@Override
	public RootUtil createRootUtil(int watchdogTimeout, LineListener lineListener) {
		RootUtil rootUtil = new SuperUserRootUtil();
		if(rootUtil.startShell(watchdogTimeout, lineListener)) {
			return rootUtil;
		}
		
		if(rootUtilServer != null) {
			rootUtil = new RootUtilClient(rootUtilServer.getPort());
			if(rootUtil.startShell(watchdogTimeout, lineListener)) {
				return rootUtil;
			}
		}
		
		throw new UnsupportedOperationException();
	}
	
	private final Hooker hooker;
	
	@Override
	public Hooker getHooker() {
		return hooker;
	}

	private Application application;

	@Override
	public void onAttachApplication(Application application) {
		this.application = application;
	}

	@Override
	public Application getApplication() {
		return application;
	}
}
