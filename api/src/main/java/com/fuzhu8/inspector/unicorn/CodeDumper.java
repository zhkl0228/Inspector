package com.fuzhu8.inspector.unicorn;

/**
 * asm code dumper
 * Created by zhkl0228 on 2017/5/11.
 */

interface CodeDumper {

    CodeDumper setDump(long begin, long end);
    void dump();

}
