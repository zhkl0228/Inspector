package com.fuzhu8.inspector.xposed.permissions;

/**
 * @author zhkl0228
 *
 */
public interface FakeParsePackageResult {
	
	void fakePackageManagerService(Class<?> packageManagerServiceClass);

}
