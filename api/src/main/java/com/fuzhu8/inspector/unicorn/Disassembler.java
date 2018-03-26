package com.fuzhu8.inspector.unicorn;

/**
 * disassembler
 * Created by zhkl0228 on 2017/5/9.
 */

public interface Disassembler {

    boolean disassemble(long address, int size);

}
