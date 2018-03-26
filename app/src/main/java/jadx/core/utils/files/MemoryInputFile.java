/**
 * 
 */
package jadx.core.utils.files;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.android.dex.Dex;

import jadx.core.utils.exceptions.DecodeException;

/**
 * @author zhkl0228
 *
 */
public class MemoryInputFile extends InputFile {
	
	public static InputFile load(ByteBuffer buffer) throws DecodeException, IOException {
		return new MemoryInputFile(Dex.create(buffer));
	}

	private MemoryInputFile(Dex dex) throws DecodeException, IOException {
		super();
		
		addDexFile(dex);
	}

	@Override
	public File getFile() {
		return null;
	}

	@Override
	public String toString() {
		throw new UnsupportedOperationException();
	}

}
