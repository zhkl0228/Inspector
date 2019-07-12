package com.fuzhu8.inspector.script;

import com.fuzhu8.inspector.Inspector;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

public class JSONFunction extends InspectorFunction {

    JSONFunction(LuaState L, Inspector inspector) {
        super(L, inspector);
    }

    @Override
    public int execute() throws LuaException {
        if(L.getTop() > 1) {
            inspector.println(com.alibaba.fastjson.JSON.toJSONString(getParam(2).getObject()));
        }
        return 0;
    }

}
