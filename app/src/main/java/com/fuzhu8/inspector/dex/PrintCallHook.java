package com.fuzhu8.inspector.dex;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.advisor.MethodHook;
import com.fuzhu8.inspector.advisor.MethodHookParam;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;

/**
 * @author zhkl0228
 *
 */
public class PrintCallHook implements MethodHook {
	
	protected final Inspector inspector;
	private final boolean printInvoke;
	
	PrintCallHook(Inspector inspector, boolean printInvoke) {
		super();
		this.inspector = inspector;
		this.printInvoke = printInvoke;
	}

	@Override
	public void afterHookedMethod(MethodHookParam param) throws Throwable {
		if(!printInvoke) {
			return;
		}

		Long startTimeInMillis = (Long) param.getObjectExtra(HookFunctionRequest.START_TIME_IN_MILLIS_KEY);
		HookFunctionRequest.afterHookedMethod(inspector, (startTimeInMillis == null ? -1 : System.currentTimeMillis() - startTimeInMillis), param.method, param.thisObject, param.getResult(), param.args);
	}

	@Override
	public void beforeHookedMethod(MethodHookParam param) throws Throwable {
		param.setObjectExtra(HookFunctionRequest.START_TIME_IN_MILLIS_KEY, System.currentTimeMillis());
	}

}
