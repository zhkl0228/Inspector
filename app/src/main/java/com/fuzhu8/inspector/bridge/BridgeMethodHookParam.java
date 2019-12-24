/**
 * 
 */
package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.advisor.MethodHookParam;

import cn.android.bridge.XC_MethodHook;

/**
 * @author zhkl0228
 *
 */
public class BridgeMethodHookParam extends MethodHookParam {
	
	private final XC_MethodHook.MethodHookParam param;

	public BridgeMethodHookParam(XC_MethodHook.MethodHookParam param) {
		super(param.method, param.thisObject, param.args);
		this.param = param;
	}

	@Override
	public Object getResult() {
		return param.getResult();
	}

	@Override
	public void setResult(Object result) {
		param.setResult(result);
	}

	@Override
	public Throwable getThrowable() {
		return param.getThrowable();
	}

	@Override
	public boolean hasThrowable() {
		return param.hasThrowable();
	}

	@Override
	public void setThrowable(Throwable throwable) {
		param.setThrowable(throwable);
	}

	@Override
	public Object getResultOrThrowable() throws Throwable {
		return param.getResultOrThrowable();
	}

	@Override
	public Object getObjectExtra(String key) {
		return param.getObjectExtra(key);
	}

	@Override
	public void setObjectExtra(String key, Object o) {
		param.setObjectExtra(key, o);
	}
}
