/**
 * 
 */
package com.fuzhu8.inspector.bean;

/**
 * @author zhkl0228
 *
 */
public class InstalledPackage {
	
	private final String name;
	private final String packageName;
	private final String versionName;
	private final int versionCode;
	
	public InstalledPackage(String name, String packageName,
			String versionName, int versionCode) {
		super();
		this.name = name;
		this.packageName = packageName;
		this.versionName = versionName;
		this.versionCode = versionCode;
	}

	public String getName() {
		return name;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getVersionName() {
		return versionName;
	}

	public int getVersionCode() {
		return versionCode;
	}

}
