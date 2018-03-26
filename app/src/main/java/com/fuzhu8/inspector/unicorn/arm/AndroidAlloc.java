package com.fuzhu8.inspector.unicorn.arm;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.unicorn.Alloc;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

import unicorn.ArmConst;
import unicorn.Unicorn;

/**
 * android memory alloc
 * Created by zhkl0228 on 2017/5/11.
 */

class AndroidAlloc implements Alloc {

    private final Inspector inspector;
    private final long malloc, free;

    AndroidAlloc(Inspector inspector) {
        super();
        this.inspector = inspector;

        NativeLibrary libc = NativeLibrary.getInstance("c");
        Pointer malloc = libc.getGlobalVariableAddress("malloc");
        Pointer free = libc.getGlobalVariableAddress("free");
        if (inspector.isDebug()) {
            inspector.println("AndroidAlloc malloc=" + malloc + ", free=" + free);
        }
        this.malloc = Pointer.nativeValue(malloc);
        this.free = Pointer.nativeValue(free);
    }

    @Override
    public boolean replaceFunction(Unicorn unicorn, long address) {
        if ((address | 1) == (malloc | 1)) {
            this.malloc(unicorn);
            return true;
        }
        if ((address | 1) == (free | 1)) {
            this.free(unicorn);
            return true;
        }
        return false;
    }

    private long heap;

    @Override
    public void setHeap(long heap) {
        this.heap = heap;
    }

    private void malloc(Unicorn unicorn) {
        int size = ((Number) unicorn.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
        if (heap > 0) {
            int addr = virtual_malloc(size);
            if (inspector.isDebug()) {
                inspector.println("virtual malloc size=" + size + ", addr=0x" + Integer.toHexString(addr));
            }
            unicorn.reg_write(ArmConst.UC_ARM_REG_R0, addr);
            return;
        }

        Pointer ptr = CLibrary.INSTANCE.malloc(size);

        if (inspector.isDebug()) {
            ARM.showThumbRegs(unicorn, inspector);
            inspector.err_println("malloc size=" + size + ", ptr=" + ptr);
        }

        int addr = (int) Pointer.nativeValue(ptr);
        unicorn.reg_write(ArmConst.UC_ARM_REG_R0, addr);
    }

    private int virtual_malloc(int size) {
        int ret = (int) heap;
        heap += ARM.alignSize(size);
        return ret;
    }

    private void free(Unicorn unicorn) {
        int addr = ((Number) unicorn.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
        if (heap > 0) {
            virtual_free(addr);
            return;
        }

        CLibrary.INSTANCE.free(new Pointer(addr));

        if (inspector.isDebug()) {
            ARM.showThumbRegs(unicorn, inspector);
            inspector.err_println("free addr=0x" + Integer.toHexString(addr));
        }
    }

    private void virtual_free(int addr) {
        inspector.err_println("Unsupported virtual free: addr=0x" + Integer.toHexString(addr));
    }

    @Override
    public final void forceReturn(Unicorn u) {
        Object lr = u.reg_read(ArmConst.UC_ARM_REG_LR);
        u.reg_write(ArmConst.UC_ARM_REG_PC, lr);
    }

}
