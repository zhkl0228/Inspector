package com.fuzhu8.inspector.sdk;

import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.fuzhu8.inspector.advisor.Hookable;
import com.fuzhu8.inspector.advisor.Hooker;
import com.taobao.android.dexposed.XposedHelpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * android 7.1.2
 * Created by zhkl0228 on 2017/7/19.
 */

public class Sdk25 extends UnknownSdk {
    @Override
    public void hook_openDexFile(Hooker hooker, Hookable handler) {
    }

    @Override
    public void hook_defineClass(Hooker hooker, Hookable handler) {
    }

    @Override
    public void hook_sendTextMessage(Hooker hooker, Hookable handler, Class<?> ISmsClass) throws NoSuchMethodException {
    }

    @Override
    protected Map<String, PackageInfo> getInstalledPackages(IPackageManager pm) {
        try {
            Object list = XposedHelpers.callMethod(pm, "getInstalledPackages", PackageManager.GET_META_DATA, 0);
            List<PackageInfo> pis = (List<PackageInfo>) XposedHelpers.callMethod(list, "getList");
            Map<String, PackageInfo> map = new HashMap<String, PackageInfo>(pis.size());
            for(PackageInfo pi : pis) {
                map.put(pi.packageName, pi);
            }
            return map;
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }
}
