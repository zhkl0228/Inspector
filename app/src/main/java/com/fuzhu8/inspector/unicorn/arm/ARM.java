package com.fuzhu8.inspector.unicorn.arm;

import com.fuzhu8.inspector.Inspector;

import java.util.Locale;

import unicorn.ArmConst;
import unicorn.Unicorn;

/**
 * arm utils
 * Created by zhkl0228 on 2017/5/11.
 */

public class ARM {

    private static int getBit(long value, int offset) {
        long mask = 1L << offset;
        return (value & mask) != 0 ? 1 : 0;
    }

    private static final int CPSR_THUMB_BIT = 5;

    static boolean isThumb(Unicorn unicorn) {
        Number cpsr = (Number) unicorn.reg_read(ArmConst.UC_ARM_REG_CPSR);
        return getBit(cpsr.intValue(), ARM.CPSR_THUMB_BIT) == 1;
    }

    static void showThumbRegs(Unicorn unicorn, Inspector inspector) {
        showRegs(unicorn, inspector, ARM.THUMB_REGS);
    }

    static void showRegs(Unicorn unicorn, Inspector inspector, int[] regs) {
        if (regs == null || regs.length < 1) {
            regs = ARM.getAllRegisters(isThumb(unicorn));
        }
        StringBuilder builder = new StringBuilder();
        builder.append(">>>");
        for (int reg : regs) {
            Number number;
            int value;
            switch (reg) {
                case ArmConst.UC_ARM_REG_CPSR:
                    number = (Number) unicorn.reg_read(ArmConst.UC_ARM_REG_CPSR);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " cpsr: N=%d, Z=%d, C=%d, V=%d, T=%d, mode=",
                            getBit(value, 31),
                            getBit(value, 30),
                            getBit(value, 29),
                            getBit(value, 28),
                            getBit(value, CPSR_THUMB_BIT))).append(Integer.toBinaryString(value & 0x1f));
                    break;
                case ArmConst.UC_ARM_REG_R0:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " r0=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_R1:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " r1=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_R2:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " r2 = 0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_R3:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, ", r3=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_R4:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " r4=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_R5:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " r5=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_R6:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " r6=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_R7:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " r7=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_R8:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " r8=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_R9:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " r9=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_R10:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " r10=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_FP:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " fp=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_IP:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " ip=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_SP:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " sp=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_LR:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " lr=0x%x", value));
                    break;
                case ArmConst.UC_ARM_REG_PC:
                    number = (Number) unicorn.reg_read(reg);
                    value = number.intValue();
                    builder.append(String.format(Locale.US, " pc=0x%x", value));
                    break;
            }
        }
        inspector.println(builder.toString());
    }

    private static final int[] ARG_REGS = new int[] {
            ArmConst.UC_ARM_REG_R0,
            ArmConst.UC_ARM_REG_R1,
            ArmConst.UC_ARM_REG_R2,
            ArmConst.UC_ARM_REG_R3
    };

    private static final int[] THUMB_REGS = new int[] {
            ArmConst.UC_ARM_REG_R0,
            ArmConst.UC_ARM_REG_R1,
            ArmConst.UC_ARM_REG_R2,
            ArmConst.UC_ARM_REG_R3,
            ArmConst.UC_ARM_REG_R4,
            ArmConst.UC_ARM_REG_R5,
            ArmConst.UC_ARM_REG_R6,
            ArmConst.UC_ARM_REG_R7,

            ArmConst.UC_ARM_REG_SP,
            ArmConst.UC_ARM_REG_LR,
            ArmConst.UC_ARM_REG_PC,
            ArmConst.UC_ARM_REG_CPSR
    };
    private static final int[] ARM_REGS = new int[] {
            ArmConst.UC_ARM_REG_R0,
            ArmConst.UC_ARM_REG_R1,
            ArmConst.UC_ARM_REG_R2,
            ArmConst.UC_ARM_REG_R3,
            ArmConst.UC_ARM_REG_R4,
            ArmConst.UC_ARM_REG_R5,
            ArmConst.UC_ARM_REG_R6,
            ArmConst.UC_ARM_REG_R7,
            ArmConst.UC_ARM_REG_R8,
            ArmConst.UC_ARM_REG_R9,
            ArmConst.UC_ARM_REG_R10,

            ArmConst.UC_ARM_REG_FP,
            ArmConst.UC_ARM_REG_IP,

            ArmConst.UC_ARM_REG_SP,
            ArmConst.UC_ARM_REG_LR,
            ArmConst.UC_ARM_REG_PC,
            ArmConst.UC_ARM_REG_CPSR
    };

    static int[] getRegArgs() {
        return ARG_REGS;
    }

    private static int[] getAllRegisters(boolean thumb) {
        return thumb ? THUMB_REGS : ARM_REGS;
    }

    static final int ALIGN_SIZE_BASE = 16;

    static int alignSize(int size) {
        return (size / ALIGN_SIZE_BASE + 1) * ALIGN_SIZE_BASE;
    }

}
