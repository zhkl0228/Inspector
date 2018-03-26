package com.fuzhu8.inspector.unicorn;

import java.io.Closeable;

/**
 * cpu emulator
 * Created by zhkl0228 on 2017/5/2.
 */

public interface Emulator extends Closeable, Disassembler {

    /**
     * trace memory read
     */
    Emulator traceRead();
    Emulator traceRead(long begin, long end);

    /**
     * trace memory write
     */
    Emulator traceWrite();
    Emulator traceWrite(long begin, long end);

    /**
     * trace instruction
     */
    Emulator traceCode();
    Emulator traceCode(long begin, long end);

    CodeDumper dump(long begin, long end);

    Number[] eFunc(long begin, Object... args);

    /**
     * emulate block
     * @param begin start address
     * @param until stop address
     */
    void eBlock(long begin, long until);

    /**
     * show all registers
     */
    void showRegs();

    /**
     * show registers
     */
    void showRegs(int... regs);

    /**
     * read c string
     */
    String cstring(long addr);

}
