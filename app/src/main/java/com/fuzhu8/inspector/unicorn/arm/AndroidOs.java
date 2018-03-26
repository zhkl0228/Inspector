package com.fuzhu8.inspector.unicorn.arm;

import com.fuzhu8.inspector.Inspector;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import unicorn.Unicorn;

/**
 * libcore.io.Os wrapper
 * Created by zhkl0228 on 2017/5/9.
 */

class AndroidOs implements Os {

    private final Inspector inspector;

    private final Object os;
    private final Method writev;

    private final Method FileDescriptor_setInt$;

    AndroidOs(Inspector inspector) {
        super();
        this.inspector = inspector;

        try {
            Class<?> Libcore = Class.forName("libcore.io.Libcore");
            Field os = Libcore.getDeclaredField("os");
            this.os = os.get(null);

            Class<?> osClass = Class.forName("libcore.io.Os");
            writev = osClass.getDeclaredMethod("writev", FileDescriptor.class, Object[].class, int[].class, int[].class);

            FileDescriptor_setInt$ = FileDescriptor.class.getDeclaredMethod("setInt$", int.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int gettimeofday(Unicorn unicorn, int tv, int tz) {
        Pointer tvp = null;
        Pointer tzp = null;
        try {
            if (tv != 0) {
                tvp = new Pointer(Native.malloc(8));
            }
            if (tz != 0) {
                tzp = new Pointer(Native.malloc(8));
            }
            int ret = CLibrary.INSTANCE.gettimeofday(tvp, tzp);
            if (ret == 0) {
                if (tvp != null) {
                    unicorn.mem_write(tv, tvp.getByteArray(0, 4));
                    unicorn.mem_write(tv + 4, tvp.getByteArray(4, 4));
                }

                if (tzp != null) {
                    unicorn.mem_write(tz, tzp.getByteArray(0, 4));
                    unicorn.mem_write(tz + 4, tzp.getByteArray(4, 4));
                }
            }
            return ret;
        } finally {
            if (tvp != null) {
                Native.free(Pointer.nativeValue(tvp));
            }
            if (tzp != null) {
                Native.free(Pointer.nativeValue(tzp));
            }
        }
    }

    private FileDescriptor createFileDescriptor(int fd) throws InvocationTargetException, IllegalAccessException {
        FileDescriptor descriptor = new FileDescriptor();
        FileDescriptor_setInt$.invoke(descriptor, fd);
        return descriptor;
    }

    @Override
    public int madvise(int addr, int len, int advise) {
        return CLibrary.INSTANCE.madvise(new Pointer(addr), len, advise);
    }

    @Override
    public int munmap(int start, int length) {
        return CLibrary.INSTANCE.munmap(new Pointer(start), length);
    }

    @Override
    public long mmap(int start, int length, int prot, int flags, int fd, int offset) {
        Pointer pointer = CLibrary.INSTANCE.mmap(new Pointer(start), length, prot, flags, fd, offset);
        return Pointer.nativeValue(pointer);
    }

    @Override
    public int open(String path, int flags, int mode) {
        return CLibrary.INSTANCE.open(path, flags, mode);
    }

    @Override
    public int close(int fd) {
        return CLibrary.INSTANCE.close(fd);
    }

    @Override
    public int writev(Unicorn unicorn, int fd, int iov, int iovcnt) {
        try {
            FileDescriptor descriptor = createFileDescriptor(fd);
            Object[] buffers = new Object[iovcnt];
            int[] offsets = new int[iovcnt];
            int[] byteCounts = new int[iovcnt];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for(int i = 0; i < iovcnt; i++) {
                long base = iov + i * 8;
                long iov_base = ByteBuffer.wrap(unicorn.mem_read(base, 4)).order(ByteOrder.LITTLE_ENDIAN).getInt();
                int iov_len = ByteBuffer.wrap(unicorn.mem_read(base + 4, 4)).order(ByteOrder.LITTLE_ENDIAN).getInt();

                if (inspector.isDebug()) {
                    inspector.println("writev i=" + i + ", base=0x" + Long.toHexString(base) + ", iov_base=0x" + Long.toHexString(iov_base) + ", iov_len=" + iov_len);
                }

                byte[] buffer = unicorn.mem_read(iov_base, iov_len);
                ByteBuffer bb = ByteBuffer.allocateDirect(iov_len);
                bb.put(buffer);
                bb.flip();
                buffers[i] = bb;
                offsets[i] = 0;
                byteCounts[i] = iov_len;

                baos.write(buffer);
            }
            int total = (Integer) writev.invoke(os, descriptor, buffers, offsets, byteCounts);
            if (inspector.isDebug()) {
                inspector.inspect(baos.toByteArray(), "writev total=" + total);
            }
            return total;
        } catch (InvocationTargetException e) {
            inspector.println(e);
            return -1;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
