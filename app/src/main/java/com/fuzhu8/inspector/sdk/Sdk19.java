package com.fuzhu8.inspector.sdk;

import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.taobao.android.dexposed.XposedHelpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhkl0228
 *
 */
public class Sdk19 extends Sdk17 implements Sdk {

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
		} catch (Exception e) {
			throw new UnsupportedOperationException(e);
		}
	}
}
