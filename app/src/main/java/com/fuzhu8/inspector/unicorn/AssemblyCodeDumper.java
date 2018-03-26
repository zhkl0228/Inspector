package com.fuzhu8.inspector.unicorn;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.io.ByteBufferCache;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import unicorn.CodeHook;
import unicorn.Unicorn;

/**
 * my code hook
 * Created by zhkl0228 on 2017/5/2.
 */

class AssemblyCodeDumper implements CodeHook, CodeDumper {

    private final Inspector inspector;
    private final Emulator emulator;
    private final Alloc alloc;

    AssemblyCodeDumper(Inspector inspector, Emulator emulator, Alloc alloc) {
        super();

        this.inspector = inspector;
        this.emulator = emulator;
        this.alloc = alloc;
    }

    private boolean traceInstruction;
    private long traceBegin, traceEnd;

    private final List<Alt> alts = new ArrayList<>();

    void initialize(boolean traceInstruction, long begin, long end, Alt... alts) {
        this.traceInstruction = traceInstruction;
        this.traceBegin = begin;
        this.traceEnd = end;

        this.alts.clear();
        this.alts.add(alloc);
        Collections.addAll(this.alts, alts);
    }

    /*@Override
    public void hook(Unicorn u, long address, int size, long value, Object user) {
        if (dumpBuffer != null && address >= dumpBegin && address + size < dumpEnd) {
            byte[] data = new Pointer(address).getByteArray(0, size);
            int offset = (int) (address - dumpBegin);
            System.arraycopy(data, 0, dumpBuffer, offset, size);
        }
    }*/

    private boolean canTrace(long address) {
        return traceInstruction && (traceBegin > traceEnd || address >= traceBegin && address <= traceEnd);
    }

    private long dumpBegin, dumpEnd;
    private byte[] dumpBuffer;

    @Override
    public CodeDumper setDump(long begin, long end) {
        if (end <= begin) {
            throw new IllegalArgumentException("end must great than begin.");
        }

        long size = end - begin;
        if (size > 0x1000000) { // 16M
            throw new IllegalArgumentException("dump size must less than 16M.");
        }

        if (begin == dumpBegin && end == dumpEnd && dumpBuffer != null) {
            return this;
        }

        dumpBegin = begin;
        dumpEnd = end;
        dumpBuffer = new byte[(int) size];
        return this;
    }

    @Override
    public void dump() {
        if (dumpBuffer == null) {
            throw new IllegalStateException("must setDump first.");
        }
        inspector.writeToConsole(new ByteBufferCache("dump_0x" + Long.toHexString(dumpBegin) + "_0x" + Long.toHexString(dumpEnd) + ".bin", ByteBuffer.wrap(dumpBuffer)));
    }

    @Override
    public void hook(Unicorn u, long address, int size, Object user) {
        for (Alt alt : alts) {
            if (alt.replaceFunction(u, address)) {
                alt.forceReturn(u);
                return;
            }
        }

        try {
            if (canTrace(address)) {
                if (!emulator.disassemble(address, size)) {
                    inspector.err_println("### Trace Instruction at 0x" + Long.toHexString(address) + ", size = " + size);
                }
            }
        } finally {
            if (dumpBuffer != null && address >= dumpBegin && address + size < dumpEnd) {
                byte[] code = u.mem_read(address, size);
                int offset = (int) (address - dumpBegin);
                System.arraycopy(code, 0, dumpBuffer, offset, size);
            }
        }
    }

}
