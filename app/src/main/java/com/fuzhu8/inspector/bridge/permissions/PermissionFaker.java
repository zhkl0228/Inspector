package com.fuzhu8.inspector.bridge.permissions;

import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractAdvisor;

import java.util.List;

import cn.android.bridge.XSharedPreferences;

/**
 * @author zhkl0228
 *
 */
public abstract class PermissionFaker extends AbstractAdvisor {
	
	protected final XSharedPreferences pref;
	final List<String> permissionsAdd;

	PermissionFaker(ModuleContext context,
					final XSharedPreferences pref, List<String> permissionsAdd) {
		super(context);
		
		this.pref = pref;
		this.permissionsAdd = permissionsAdd;
	}

	@Override
	protected final void executeHook() {
	}
	
	public abstract void fakeParsePackage();

	public abstract void fakePackageManagerService(Class<?> packageManagerServiceClass);

}
