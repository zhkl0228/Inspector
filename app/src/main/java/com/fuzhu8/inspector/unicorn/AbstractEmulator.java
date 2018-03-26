package com.fuzhu8.inspector.unicorn;

import com.fuzhu8.inspector.Inspector;
import com.sun.jna.Pointer;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import unicorn.EventMemHook;
import unicorn.ReadHook;
import unicorn.Unicorn;
import unicorn.UnicornConst;
import unicorn.WriteHook;

/**
 * abstract emulator
 * Created by zhkl0228 on 2017/5/2.
 */

public abstract class AbstractEmulator implements Emulator {

    protected final Inspector inspector;
    protected final Unicorn unicorn;
    private final Alloc alloc;

    public AbstractEmulator(final Inspector inspector, int unicorn_arch, int unicorn_mode, SyscallHandler syscallHandler, Alloc alloc) {
        super();
        this.inspector = inspector;
        this.unicorn = new Unicorn(unicorn_arch, unicorn_mode);
        this.alloc = alloc;

        unicorn.hook_add(new EventMemHook() {
            @Override
            public boolean hook(Unicorn u, int type, long address, int size, long value, Object user) {
                final int pageAlign = getPageAlign();
                final long addr = alignAddr(address, pageAlign);
                if (addr < 0x40000000) {
                    inspector.err_println("memory map failed: address=0x" + Long.toHexString(address) + ", aligned=0x" + Long.toHexString(addr) + ", size=" + size + ", value=0x" + Long.toHexString(value));
                    showRegs();
                    return false;
                }

                u.mem_map(addr, pageAlign, UnicornConst.UC_PROT_ALL);
                u.mem_write(addr, new Pointer(addr).getByteArray(0, pageAlign));
                if (inspector.isDebug()) {
                    inspector.println("memory map: address=0x" + Long.toHexString(address) + ", size=" + size + ", value=0x" + Long.toHexString(value) + ", aligned=0x" + Long.toHexString(addr) + ", user=" + user);
                }
                return true;
            }
        }, UnicornConst.UC_HOOK_MEM_READ_UNMAPPED | UnicornConst.UC_HOOK_MEM_WRITE_UNMAPPED | UnicornConst.UC_HOOK_MEM_FETCH_UNMAPPED, null);

        if (syscallHandler != null) {
            unicorn.hook_add(syscallHandler, null);
        }

        this.readHook = new TraceMemoryHook(inspector);
        this.writeHook = new TraceMemoryHook(inspector);
        this.codeHook = new AssemblyCodeDumper(inspector, this, alloc);

        this.unicorn.hook_add(codeHook, 1, 0, null);
        // this.unicorn.hook_add((MemHook) codeHook, 1, 0, null);
    }

    protected final void setAllocHeap(long heap) {
        alloc.setHeap(heap);
    }

    private boolean traceMemoryRead, traceMemoryWrite;
    private long traceMemoryReadBegin, traceMemoryReadEnd;
    private long traceMemoryWriteBegin, traceMemoryWriteEnd;
    protected boolean traceInstruction;
    private long traceInstructionBegin, traceInstructionEnd;

    @Override
    public final Emulator traceRead(long begin, long end) {
        traceMemoryRead = true;
        traceMemoryReadBegin = begin;
        traceMemoryReadEnd = end;
        return this;
    }

    @Override
    public final Emulator traceWrite(long begin, long end) {
        traceMemoryWrite = true;
        traceMemoryWriteBegin = begin;
        traceMemoryWriteEnd = end;
        return this;
    }

    @Override
    public final Emulator traceRead() {
        return traceRead(1, 0);
    }

    @Override
    public final Emulator traceWrite() {
        return traceWrite(1, 0);
    }

    @Override
    public final Emulator traceCode() {
        return traceCode(1, 0);
    }

    @Override
    public final Emulator traceCode(long begin, long end) {
        traceInstruction = true;
        traceInstructionBegin = begin;
        traceInstructionEnd = end;
        return this;
    }

    @Override
    public CodeDumper dump(long begin, long end) {
        return codeHook.setDump(begin, end);
    }

    private final ReadHook readHook;
    private final WriteHook writeHook;
    private final AssemblyCodeDumper codeHook;

    /**
     * Emulate machine code in a specific duration of time.
     *
     * @param begin    Address where emulation starts
     * @param until    Address where emulation stops (i.e when this address is hit)
     * @param timeout  Duration to emulate the code (in microseconds). When this value is 0, we will emulate the code in infinite time, until the code is finished.
     * @param count    The number of instructions to be emulated. When this value is 0, we will emulate all the code available, until the code is finished.
     * @param alts     The function replacements
     */
    protected synchronized final void emulate(long begin, long until, long timeout, long count, Alt... alts) {
        try {
            unicorn.hook_del(readHook);
            unicorn.hook_del(writeHook);

            if (traceMemoryRead) {
                unicorn.hook_add(readHook, traceMemoryReadBegin, traceMemoryReadEnd, null);
            }
            if (traceMemoryWrite) {
                unicorn.hook_add(writeHook, traceMemoryWriteBegin, traceMemoryWriteEnd, null);
            }
            codeHook.initialize(traceInstruction, traceInstructionBegin, traceInstructionEnd, alts);

            unicorn.emu_start(begin, until, timeout, count);
        } catch (RuntimeException e) {
            showRegs();
            IOUtils.closeQuietly(this);
            throw e;
        } finally {
            traceMemoryRead = false;
            traceMemoryWrite = false;
            traceInstruction = false;
        }
    }

    @Override
    public final String cstring(long addr) {
        return cstring(unicorn, addr);
    }

    public static String cstring(Unicorn unicorn, long addr) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data;
        try {
            while ((data = unicorn.mem_read(addr++, 1))[0] != 0) {
                baos.write(data);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        data = baos.toByteArray();
        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return new String(data);
        }
    }

    private static long alignAddr(long addr, final long pageAlign) {
        return addr / pageAlign * pageAlign;
    }

    protected abstract int getPageAlign();

    private boolean closed;

    @Override
    public synchronized final void close() throws IOException {
        if (closed) {
            throw new IOException("Already closed.");
        }

        try {
            closeInternal();
            unicorn.closeAll();
        } finally {
            closed = true;
        }
    }

    protected abstract void closeInternal();

}
