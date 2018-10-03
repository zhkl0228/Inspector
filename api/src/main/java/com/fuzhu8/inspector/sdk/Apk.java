package com.fuzhu8.inspector.sdk;

import java.io.File;

/**
 * apk
 * Created by zhkl0228 on 2017/5/5.
 */

public class Apk {

    private final String versionName;
    private final int versionCode;
    private final File apkFile;
    private final String packageName;

    Apk(String versionName, int versionCode, File apkFile, String packageName) {
        super();
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.apkFile = apkFile;
        this.packageName = packageName;
    }

    public String getVersionName() {
        return versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public File getApkFile() {
        return apkFile;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public String toString() {
        return packageName + '_' + versionCode;
    }

}
