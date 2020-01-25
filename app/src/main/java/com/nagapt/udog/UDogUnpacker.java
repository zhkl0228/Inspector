package com.nagapt.udog;

import android.os.Environment;

import com.fuzhu8.inspector.Unpacker;
import com.fuzhu8.inspector.jni.Native;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author zhkl0228
 *
 */
public class UDogUnpacker extends Native implements Unpacker {

	private UDogUnpacker() {
		super("udog");
	}
	
	private static final UDogUnpacker INSTANCE = new UDogUnpacker();
	
	public static Unpacker getInstance() {
		return INSTANCE;
	}
	
	/**
	 * 脱壳
	 */
    private static native int doDumpByLinker(String so_path, String out_path);

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.Unpacker#dumpByLinker(java.lang.String)
	 */
	@Override
	public synchronized ByteBuffer dumpByLinker(String so_path) throws IOException {
		checkSupported("dumpByLinker so_path=" + so_path);

    	if(!new File(so_path).canRead()) {
    		throw new IOException("Load failed: " + so_path);
    	}
		
		File dump = new File(Environment.getExternalStorageDirectory(), "dump.so");
		if (dump.exists() && !dump.delete()) {
			throw new IOException("delete last dump failed: " + dump);
		}
		switch (doDumpByLinker(so_path, dump.getAbsolutePath())) {
    	case 0:
    		break;
		case 1:
			throw new IOException("Load SO File failed, check your path: " + so_path);
		case 3:
			throw new IOException("Write Dump SO into SDCARD failed!");
		default:
			throw new IOException("Unknown error, try again!");
		}
    	
    	byte[] data = FileUtils.readFileToByteArray(dump);
    	return ByteBuffer.wrap(data);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.jni.Native#hasSupported()
	 */
	@Override
	protected boolean hasSupported() {
		return false;
	}

	@Override
	public String getName() {
		return "udog";
	}

}
