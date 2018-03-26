package com.fuzhu8.inspector.dexposed;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;
import com.taobao.android.dexposed.XC_MethodHook;

/**
 * @author zhkl0228
 *
 */
public class DexposedHookHandler extends XC_MethodHook {
	
	private final Inspector inspector;
	protected final DexFileManager dexFileManager;

	DexposedHookHandler(Inspector inspector, DexFileManager dexFileManager) {
		super();
		this.inspector = inspector;
		this.dexFileManager = dexFileManager;
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		super.afterHookedMethod(param);

		Long startTimeInMillis = (Long) param.getObjectExtra(HookFunctionRequest.START_TIME_IN_MILLIS_KEY);
		HookFunctionRequest.afterHookedMethod(inspector, (startTimeInMillis == null ? -1 : System.currentTimeMillis() - startTimeInMillis), param.method, param.thisObject, param.getResult(), param.args);
	}

	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		param.setObjectExtra(HookFunctionRequest.START_TIME_IN_MILLIS_KEY, System.currentTimeMillis());
	}
}
