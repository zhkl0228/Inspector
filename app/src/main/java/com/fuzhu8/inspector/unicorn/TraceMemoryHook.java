package com.fuzhu8.inspector.unicorn;

import com.fuzhu8.inspector.Inspector;

import unicorn.MemHook;
import unicorn.Unicorn;

/**
 * trace memory read
 * Created by zhkl0228 on 2017/5/2.
 */

class TraceMemoryHook implements MemHook {

    private final Inspector inspector;

    TraceMemoryHook(Inspector inspector) {
        this.inspector = inspector;
    }

    @Override
    public void hook(Unicorn u, long address, int size, Object user) {
        inspector.println("### Memory READ at 0x" + Long.toHexString(address) + ", data size = " + size);
    }

    @Override
    public void hook(Unicorn u, long address, int size, long value, Object user) {
        inspector.println("### Memory WRITE at 0x" + Long.toHexString(address) + ", data size = " + size + ", data value = 0x" + Long.toHexString(value));
    }

}
