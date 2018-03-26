package com.fuzhu8.inspector.unicorn.arm;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.unicorn.AbstractEmulator;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import capstone.Capstone;
import unicorn.ArmConst;
import unicorn.UnicornConst;

import static unicorn.ArmConst.UC_ARM_REG_C13_C0_3;
import static unicorn.ArmConst.UC_ARM_REG_C1_C0_2;
import static unicorn.ArmConst.UC_ARM_REG_CPSR;
import static unicorn.ArmConst.UC_ARM_REG_FPEXC;

/**
 * default arm emulator
 * Created by zhkl0228 on 2017/5/2.
 */

public class DefaultARMEmulator extends AbstractEmulator implements ARMEmulator {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMicros(30);

    private static final int STACK_BASE = 0x7ff000;
    private static final int STACK_SIZE = 64;

    private final boolean thumb;
    private final long sp;
    private final long lr;

    private static final int HEAP_BASE = 0x10000000;

    private final Capstone capstoneArm, capstoneThumb;

    public DefaultARMEmulator(Inspector inspector, boolean thumb) {
        super(inspector,
                UnicornConst.UC_ARCH_ARM, thumb ? UnicornConst.UC_MODE_THUMB : UnicornConst.UC_MODE_ARM,
                new AndroidSyscallHandler(inspector),
                new AndroidAlloc(inspector));
        this.thumb = thumb;
        this.lr = 0x100000;
        switchUserMode();

        // init stack
        unicorn.mem_map(STACK_BASE, (STACK_SIZE + 1) * PAGE_ALIGN, UnicornConst.UC_PROT_READ | UnicornConst.UC_PROT_WRITE);
        sp = STACK_BASE + STACK_SIZE * PAGE_ALIGN;

        byte[] nop;
        if (thumb) {
            nop = new byte[] { 0x00, (byte) 0xbf };
        } else {
            nop = new byte[] { 0x00, (byte) 0xf0, 0x20, (byte) 0xe3 };
        }
        unicorn.mem_map(lr, PAGE_ALIGN, UnicornConst.UC_PROT_WRITE);
        unicorn.mem_write(lr, nop);
        unicorn.mem_protect(lr, PAGE_ALIGN, UnicornConst.UC_PROT_READ | UnicornConst.UC_PROT_EXEC);

        unicorn.mem_map(HEAP_BASE, PAGE_ALIGN * 2, UnicornConst.UC_PROT_READ | UnicornConst.UC_PROT_WRITE);

        this.capstoneArm = new Capstone(Capstone.CS_ARCH_ARM, Capstone.CS_MODE_ARM);
        this.capstoneThumb = new Capstone(Capstone.CS_ARCH_ARM, Capstone.CS_MODE_THUMB);
    }

    @Override
    public boolean disassemble(long address, int size) {
        final Capstone capstone = ARM.isThumb(unicorn) ? capstoneThumb : capstoneArm;

        byte[] code = unicorn.mem_read(address, size);
        Capstone.CsInsn[] insns = capstone.disasm(code, address);
        StringBuilder sb = new StringBuilder();
        sb.append("### Trace Instruction ");
        for (Capstone.CsInsn ins : insns) {
            sb.append(String.format("0x%x: %s %s", ins.address, ins.mnemonic, ins.opStr));
        }
        inspector.err_println(sb.toString());
        return true;
    }

    @Override
    protected void closeInternal() {
        capstoneThumb.close();
        capstoneArm.close();
    }

    @Override
    protected int getPageAlign() {
        return PAGE_ALIGN;
    }

    private int writeCString(final long alloc, String str) {
        byte[] data;
        try {
            data = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            data = str.getBytes();
        }
        unicorn.mem_write(alloc, Arrays.copyOf(data, data.length + 1));
        return ARM.alignSize(data.length + 1);
    }

    @Override
    public Number[] eFunc(long begin, Object... args) {
        final List<Number> rets = new ArrayList<Number>(10);
        int i = 0;
        int[] regArgs = ARM.getRegArgs();
        long heap = HEAP_BASE;
        while (args != null && i < args.length && i < regArgs.length) {
            if (args[i] instanceof String) {
                int size = writeCString(heap, (String) args[i]);
                if (inspector.isDebug()) {
                    inspector.println("map arg" + (i+1) + ": 0x" + Long.toHexString(heap) + " -> " + args[i] + ", size=" + size);
                }
                unicorn.reg_write(regArgs[i], heap);
                rets.add(heap);
                heap += size;
            } else {
                unicorn.reg_write(regArgs[i], args[i]);
            }
            i++;
        }
        long sp = this.sp;
        while (args != null && i < args.length) {
            if (args[i] instanceof String) {
                int size = writeCString(heap, (String) args[i]);
                byte[] array = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) heap).array();
                unicorn.mem_write(sp, array);
                if (inspector.isDebug()) {
                    inspector.println("map arg" + (i+1) + ": 0x" + Long.toHexString(heap) + " -> " + args[i] + ", size=" + size);
                }
                rets.add(heap);
                heap += size;
            } else {
                Number number = (Number) args[i];
                byte[] array = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(number.intValue()).array();
                unicorn.mem_write(sp, array);
            }
            i++;
            sp += 4;
        }

        initialize(heap);
        emulate(thumb ? (begin | 1) : (begin >> 1 << 1), lr, TIMEOUT, 0);
        rets.add(0, (Number) unicorn.reg_read(ArmConst.UC_ARM_REG_R0));
        return rets.toArray(new Number[0]);
    }

    private void initialize(long heap) {
        unicorn.reg_write(UC_ARM_REG_C13_C0_3, heap); // errno
        setAllocHeap(heap + ARM.ALIGN_SIZE_BASE);

        unicorn.reg_write(ArmConst.UC_ARM_REG_SP, sp);
        unicorn.reg_write(ArmConst.UC_ARM_REG_LR, lr);

        enableVFP();
    }

    private void switchUserMode() {
        int value = ((Number) unicorn.reg_read(UC_ARM_REG_CPSR)).intValue();
        value &= 0x7ffffff0;
        unicorn.reg_write(UC_ARM_REG_CPSR, value);
    }

    private void enableVFP() {
        int value = ((Number) unicorn.reg_read(UC_ARM_REG_C1_C0_2)).intValue();
        value |= (0xf << 20);
        unicorn.reg_write(UC_ARM_REG_C1_C0_2, value);
        unicorn.reg_write(UC_ARM_REG_FPEXC, 0x40000000);
    }

    @Override
    public void eBlock(long begin, long until) {
        initialize(HEAP_BASE);
        emulate(thumb ? (begin | 1) : (begin >> 1 << 1), until < 1 ? lr : until, traceInstruction ? 0 : TIMEOUT, 0);
        showRegs();
    }

    @Override
    public void showRegs() {
        this.showRegs((int[]) null);
    }

    @Override
    public void showRegs(int... regs) {
        ARM.showRegs(unicorn, inspector, regs);
    }

}
