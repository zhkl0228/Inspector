package com.freakishfox.xAnSo;

import android.os.Environment;

import com.fuzhu8.inspector.Unpacker;
import com.fuzhu8.inspector.jni.Native;
import com.sun.jna.Platform;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * xAnSo unpacker
 * Created by zhkl0228 on 2017/10/23.
 */

public class xAnSoUnpacker extends Native implements Unpacker {

    private xAnSoUnpacker() {
        super("xAnSo");
    }

    private static final xAnSoUnpacker INSTANCE = new xAnSoUnpacker();

    public static Unpacker getInstance() {
        return INSTANCE;
    }

    /**
     * 脱壳
     */
    private static native int fix_section(String so_path, String out_path);

    @Override
    public ByteBuffer dumpByLinker(String so_path) throws IOException {
        checkSupported();

        if(!new File(so_path).canRead()) {
            throw new IOException("Load failed: " + so_path);
        }

        File dump = new File(Environment.getExternalStorageDirectory(), "dump.so");
        if (dump.exists() && !dump.delete()) {
            throw new IOException("delete last dump failed: " + dump);
        }
        switch (fix_section(so_path, dump.getAbsolutePath())) {
            case 0:
                break;
            case 1:
                throw new IOException("elf file section build fail, check your path: " + so_path);
            case 3:
                throw new IOException("save fixed elf file fail");
            default:
                throw new IOException("Unknown error, try again!");
        }

        byte[] data = FileUtils.readFileToByteArray(dump);
        return ByteBuffer.wrap(data);
    }

    @Override
    public String getName() {
        return "xAnSo";
    }

    @Override
    protected boolean hasSupported() {
        return !Platform.is64Bit() && Platform.isARM();
    }
}
