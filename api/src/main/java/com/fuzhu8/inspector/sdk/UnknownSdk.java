package com.fuzhu8.inspector.sdk;

import android.app.ActivityThread;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhkl0228
 *
 */
abstract class UnknownSdk implements Sdk {

	protected abstract Map<String, PackageInfo> getInstalledPackages(IPackageManager pm);

	@Override
	public ApkWrapper searchApk(ApplicationInfo applicationInfo) {
		IPackageManager pm = ActivityThread.getPackageManager();
		Map<String, PackageInfo> map = getInstalledPackages(pm);

		List<PluginApk> pluginApks = new ArrayList<>();
		Apk inspectorApk = null;
		Apk targetApk = null;
		for(PackageInfo pi : map.values()) {
			ApplicationInfo app = pi.applicationInfo;
			if(applicationInfo.packageName.equals(app.packageName)) {
				targetApk = new Apk(pi.versionName, pi.versionCode, new File(app.publicSourceDir), app.packageName);
				continue;
			}
			if("com.fuzhu8.inspector".equals(app.packageName)) {
				inspectorApk = new Apk(pi.versionName, pi.versionCode, new File(app.publicSourceDir), app.packageName);
				continue;
			}
			
			if (!app.enabled)
				continue;

			if(app.metaData == null ||
					Objects.requireNonNull(getClass().getPackage()).getName().equals(app.packageName)) {
				continue;
			}
			
			if(!app.metaData.containsKey(INSPECTOR_PLUGIN_META_KEY)) {
				continue;
			}
			
			String packages = app.metaData.getString("plugin_packages");
			if(packages == null) {
				continue;
			}

			String[] packagePlugins = packages.split(",");
			for (String packageName : packagePlugins) {
				int index = packageName.indexOf(':');
				String pluginClassName = null;
				if (index != -1) {
					pluginClassName = packageName.substring(index + 1);
					packageName = packageName.substring(0, index);
				}

				if (packageName.equals(applicationInfo.packageName)) {
					pluginApks.add(new PluginApk(pi.versionName, pi.versionCode, new File(app.publicSourceDir), pi.packageName, pluginClassName));
					break;
				}
			}
		}
		
		if(inspectorApk == null) {
			throw new IllegalStateException("searchApk find inspector apk failed.");
		}
		if(targetApk == null) {
			throw new IllegalStateException("searchApk find target apk failed.");
		}
		
		return new ApkWrapper(inspectorApk, pluginApks, targetApk);
	}
}
