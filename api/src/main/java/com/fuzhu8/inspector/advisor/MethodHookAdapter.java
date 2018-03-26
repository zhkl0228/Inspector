package com.fuzhu8.inspector.advisor;

/**
 * MethodHook adapter
 * Created by zhkl0228 on 2017/4/11.
 */

public abstract class MethodHookAdapter implements MethodHook {
    @Override
    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    @Override
    public void afterHookedMethod(MethodHookParam param) throws Throwable {
    }
}
