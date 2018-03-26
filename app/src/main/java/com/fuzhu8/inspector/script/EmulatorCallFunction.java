package com.fuzhu8.inspector.script;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.unicorn.Emulator;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

import java.util.Arrays;

/**
 * emulator.eFunc
 * Created by zhkl0228 on 2017/5/3.
 */

class EmulatorCallFunction extends InspectorFunction {

    EmulatorCallFunction(LuaState L, Inspector inspector) {
        super(L, inspector);
    }

    @Override
    public int execute() throws LuaException {
        int top = L.getTop();
        if (top < 3) {
            throw new LuaException("eFunc(emu, begin, ...);");
        }
        Emulator emulator = (Emulator) getParam(2).getObject();
        long begin = (long) getParam(3).getNumber();
        Object[] args = new Object[top - 3];
        for(int i = 4; i <= top; i++) {
            if (L.isNumber(i)) {
                int value = (int) L.toNumber(i);
                args[i - 4] = value;
                if (inspector.isDebug()) {
                    inspector.println("push integer: 0x" + Integer.toHexString(value) + ", index=" + i);
                }
            } else if (L.isString(i)) {
                String value = L.toString(i);
                args[i - 4] = value;
                if (inspector.isDebug()) {
                    inspector.println("push string: " + value + ", index=" + i);
                }
            } else {
                throw new LuaException("eFunc(emu, begin, ...); -- args invalid: " + i + ", type=" + L.type(i));
            }
        }
        if (inspector.isDebug()) {
            inspector.println("eFunc emulator=" + emulator + ", begin=0x" + Long.toHexString(begin) + ", args=" + Arrays.toString(args));
        }
        Number[] rets = emulator.eFunc(begin, args);
        for (Number number : rets) {
            L.pushNumber(number.intValue());
        }
        return rets.length;
    }

}
