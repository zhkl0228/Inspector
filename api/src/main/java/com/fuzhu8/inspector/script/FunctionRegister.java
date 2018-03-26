package com.fuzhu8.inspector.script;

/**
 * lua function register
 * Created by zhkl0228 on 2018/1/7.
 */

public interface FunctionRegister {

    void registerFunction(String name) throws RegisterException;

}
