package com.fuzhu8.inspector.unicorn.arm;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.unicorn.AbstractEmulator;
import com.fuzhu8.inspector.unicorn.SyscallHandler;

import unicorn.ArmConst;
import unicorn.Unicorn;
import unicorn.UnicornException;

/**
 * android syscall handler
 * Created by zhkl0228 on 2017/5/9.
 */

class AndroidSyscallHandler implements SyscallHandler {

    private final Inspector inspector;
    private final Os os;

    AndroidSyscallHandler(Inspector inspector) {
        super();
        this.inspector = inspector;
        this.os = new AndroidOs(inspector);
    }

    @Override
    public void hook(Unicorn u, int intno, Object user) {
        if (inspector.isDebug()) {
            ARM.showThumbRegs(u, inspector);
        }

        int NR = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R7)).intValue();
        String syscall = null;
        if (intno == 2) {
            switch (NR) {
                case  5:
                    u.reg_write(ArmConst.UC_ARM_REG_R0, open(u));
                    return;
                case  6:
                    u.reg_write(ArmConst.UC_ARM_REG_R0, close(u));
                    return;
                case 78:
                    u.reg_write(ArmConst.UC_ARM_REG_R0, gettimeofday(u));
                    return;
                case 91:
                    u.reg_write(ArmConst.UC_ARM_REG_R0, munmap(u));
                    return;
                case 146:
                    u.reg_write(ArmConst.UC_ARM_REG_R0, writev(u));
                    return;
                case 192:
                    u.reg_write(ArmConst.UC_ARM_REG_R0, mmap(u));
                    return;
                case 220:
                    u.reg_write(ArmConst.UC_ARM_REG_R0, madvise(u));
                    return;
                case 240:
                    throw new UnsupportedOperationException("futex");
                case 248:
                    exit_group(u);
                    return;
                case 67:
                    syscall = "sigaction";
                    break;
                case 126:
                    syscall = "sigprocmask";
                    break;
            }
        }

        int pc = ((Number) u.reg_read(ArmConst.UC_ARM_REG_PC)).intValue();
        inspector.err_println("handleInterrupt intno=" + intno + ", NR=" + NR + ", PC=0x" + Integer.toHexString(pc) + ", syscall=" + syscall);
    }

    private int madvise(Unicorn u) {
        int addr = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
        int len = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R1)).intValue();
        int advise = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R2)).intValue();
        int ret = os.madvise(addr, len, advise);
        if (inspector.isDebug()) {
            inspector.println("madvise addr=0x" + Integer.toHexString(addr) + ", len=" + len + ", advise=0x" + Integer.toHexString(advise) + ", ret=" + ret);
        }
        return ret;
    }

    private int munmap(Unicorn u) {
        int start = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
        int length = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R1)).intValue();
        int ret = os.munmap(start, length);
        if (inspector.isDebug()) {
            inspector.println("munmap start=0x" + Integer.toHexString(start) + ", length=" + length + ", ret=" + ret);
        }
        return ret;
    }

    private void exit_group(Unicorn u) throws UnicornException {
        int status = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
        throw new UnicornException("exit with code: " + status);
    }

    private int mmap(Unicorn u) {
        int start = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
        int length = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R1)).intValue();
        int prot = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R2)).intValue();
        int flags = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R3)).intValue();
        int fd = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R4)).intValue();
        int offset = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R5)).intValue();
        long ret = os.mmap(start, length, prot, flags, fd, offset);
        if (inspector.isDebug()) {
            inspector.println("mmap start=0x" + Integer.toHexString(start) + ", length=" + length + ", prot=0x" + Integer.toHexString(prot) + ", flags=0x" + Integer.toHexString(flags) + ", fd=" + fd + ", offset=" + offset + ", ret=0x" + Long.toHexString(ret));
        }
        return (int) ret;
    }

    private int writev(Unicorn u) {
        int fd = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
        int iov = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R1)).intValue();
        int iovcnt = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R2)).intValue();
        return os.writev(u, fd, iov, iovcnt);
    }

    private int open(Unicorn u) {
        int pathname_p = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
        int oflags = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R1)).intValue();
        int mode = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R2)).intValue();
        String pathname = AbstractEmulator.cstring(u, pathname_p);
        int fd = os.open(pathname, oflags, mode);
        if (inspector.isDebug()) {
            inspector.println("open pathname=" + pathname + ", oflags=0x" + Integer.toHexString(oflags) + ", mode=" + Integer.toHexString(mode) + ", fd=0x" + Integer.toHexString(fd));
        }
        return fd;
    }

    private int close(Unicorn u) {
        int fd = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
        int ret = os.close(fd);
        if (inspector.isDebug()) {
            inspector.println("close fd=0x" + Integer.toHexString(fd) + ", ret=" + ret);
        }
        return ret;
    }

    private int gettimeofday(Unicorn u) {
        int tv = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
        int tz = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R1)).intValue();

        if (tv != 0 && inspector.isDebug()) {
            byte[] before = u.mem_read(tv, 4);
            inspector.inspect(before, "gettimeofday");
        }

        int ret = os.gettimeofday(u, tv, tz);

        if (tv != 0 && inspector.isDebug()) {
            byte[] after = u.mem_read(tv, 4);
            inspector.inspect(after, "gettimeofday ret=" + ret);
        }
        return ret;
    }

}
