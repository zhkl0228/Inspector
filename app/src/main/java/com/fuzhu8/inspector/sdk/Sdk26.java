package com.fuzhu8.inspector.sdk;

import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.fuzhu8.inspector.advisor.Hookable;
import com.fuzhu8.inspector.advisor.Hooker;
import com.taobao.android.dexposed.XposedHelpers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * android 8.0.0
 * Created by zhkl0228 on 2018/5/9.
 */

public class Sdk26 extends UnknownSdk {
    
    @Override
    public void hook_openDexFile(Hooker hooker, Hookable handler) {
    }

    @Override
    public void hook_defineClass(Hooker hooker, Hookable handler) {
    }

    @Override
    protected Map<String, PackageInfo> getInstalledPackages(IPackageManager pm) {
        try {
            Object list = XposedHelpers.callMethod(pm, "getInstalledPackages", PackageManager.GET_META_DATA, 0);
            List<?> pis = (List<?>) XposedHelpers.callMethod(list, "getList");
            Map<String, PackageInfo> map = new HashMap<>(pis.size());
            for(Object obj : pis) {
                PackageInfo pi = (PackageInfo) obj;
                map.put(pi.packageName, pi);
            }
            return map;
        } catch (Throwable e) {
            throw new UnsupportedOperationException("getInstalledPackages failed: pm=" + Arrays.toString(pm.getClass().getDeclaredMethods()), e);
        }
    }
}
