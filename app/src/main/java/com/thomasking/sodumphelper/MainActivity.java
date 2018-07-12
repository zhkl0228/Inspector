package com.thomasking.sodumphelper;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.io.FileUtils;

import com.fuzhu8.inspector.Unpacker;
import com.fuzhu8.inspector.jni.Native;
import com.sun.jna.Platform;

import android.os.Environment;

/**
 * @author zhkl0228
 *
 */
public class MainActivity extends Native implements Unpacker {

	private MainActivity() {
		super("dumpee");
	}
	
	private static final MainActivity INSTANCE = new MainActivity();
	
	public static Unpacker getDumpee() {
		return INSTANCE;
	}
	
	private native int doDumpByDvm(String arg1);

	/**
	 * 脱壳
	 */
    private static native int doDumpByLinker(String so_path);
    
    /**
     * so脱壳
     */
    @Override
    public synchronized ByteBuffer dumpByLinker(String so_path) throws IOException {
		checkSupported("dumpByLinker so_path=" + so_path);
    	
    	if(!new File(so_path).canRead()) {
    		throw new IOException("Load failed: " + so_path);
    	}

		File dump = new File(Environment.getExternalStorageDirectory(), "dump.so");
		FileUtils.deleteQuietly(dump);
    	switch (doDumpByLinker(so_path)) {
    	case 0:
    		break;
		case 1:
			throw new IOException("Load SO File failed, check your path: " + so_path);
		case 2:
			throw new IOException("DIY SO File, not supported now!");
		case 3:
			throw new IOException("Write Dump SO into SDCARD failed!");
		default:
			throw new IOException("Unknown error, try again!");
		}
    	
    	byte[] data = FileUtils.readFileToByteArray(dump);
    	return ByteBuffer.wrap(data);
    }

	@Override
	protected boolean hasSupported() {
		return !Platform.is64Bit() && Platform.isARM();
	}

	@Override
	public String getName() {
		return "dumpee";
	}

}
