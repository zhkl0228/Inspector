/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fuzhu8.inspector.maps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import android.util.Log;

/**
 * Memory address to library mapping for native libraries.
 * <p/>
 * Each instance represents a single native library and its start and end memory addresses. 
 */
public final class NativeLibraryMapInfo {
	
    private long mStartAddr;
    private long mEndAddr;

    private String mLibrary;

    /**
     * Constructs a new native library map info.
     * @param startAddr The start address of the library.
     * @param endAddr The end address of the library.
     * @param library The name of the library.
     */
    NativeLibraryMapInfo(long startAddr, long endAddr, String library) {
        this.mStartAddr = startAddr;
        this.mEndAddr = endAddr;
        this.mLibrary = library;
    }
    
    /**
     * Returns the name of the library.
     */
    public String getLibraryName() {
        return mLibrary;
    }
    
    /**
     * Returns the start address of the library.
     */
    public long getStartAddress() {
        return mStartAddr;
    }
    
    /**
     * Returns the end address of the library.
     */
    public long getEndAddress() {
        return mEndAddr;
    }
    
    private static final int PAGESIZE = 4096;
    
    /**
     * make writable
     * @param offset
     * @param len
     * @return a direct ByteBuffer that accesses the memory
     * @throws IOException 
     */
    public ByteBuffer mprotect(long offset, int len) throws IOException {
    	if(len < 1) {
    		throw new IllegalArgumentException("mprotect len must positive.");
    	}
    	
    	/* Align to a multiple of PAGESIZE, assumed to be a power of two */
    	long p = ((mStartAddr + offset + PAGESIZE-1) & ~(PAGESIZE-1)) - PAGESIZE;
    	
    	len = (int) (mStartAddr + offset - p) + len;
    	len = len % PAGESIZE == 0 ? len : (len / PAGESIZE * PAGESIZE + PAGESIZE);
    	if(CLibrary.INSTANCE.mprotect(new Pointer(p), len, CLibrary.PROT_READ | CLibrary.PROT_WRITE | CLibrary.PROT_EXEC) == 0) {
    		return new Pointer(mStartAddr).getByteBuffer(offset, len);
    	}
    	
    	int errno = Native.getLastError();
    	switch (errno) {
    	case 12://ENOMEM
    		throw new IOException("mprotect: Internal kernel structures could not be allocated.");
		case 13://EACCES
			throw new IOException("mprotect: The memory cannot be given the specified access.");
		case 14://EFAULT
			throw new IOException("mprotect: The memory cannot be accessed.");
		case 22://EINVAL
			throw new IOException("mprotect: 0x" + Long.toHexString(p) + " is not a valid pointer, or not a multiple of PAGESIZE.");
		default:
			throw new IOException("mprotect failed: " + errno);
		}
    }

    /**
     * Returns whether the specified address is inside the library.
     * @param address The address to test.
     * @return <code>true</code> if the address is between the start and end address of the library.
     * @see #getStartAddress()
     * @see #getEndAddress()
     */
    public boolean isWithinLibrary(long address) {
        return address >= mStartAddr && address <= mEndAddr;
    }

    @Override
	public String toString() {
    	StringBuffer buffer = new StringBuffer();
    	buffer.append("[0x").append(Long.toHexString(mStartAddr));
    	buffer.append('-').append("0x").append(Long.toHexString(mEndAddr));
    	buffer.append("]").append(mLibrary);
		return buffer.toString();
	}
    
    public static NativeLibraryMapInfo readNativeLibrary(String path) throws IOException {
    	List<NativeLibraryMapInfo> infos = readNativeLibraryMapInfo();
    	for(NativeLibraryMapInfo info : infos) {
    		if(info.mLibrary.equals(path)) {
    			return info;
    		}
    	}
    	return null;
    }

	public static List<NativeLibraryMapInfo> readNativeLibraryMapInfo() throws IOException {
    	InputStream inputStream = null;
        InputStreamReader input = null;
        BufferedReader reader = null;

        String line;

        List<NativeLibraryMapInfo> list = new ArrayList<NativeLibraryMapInfo>();
        try {
        	inputStream = new FileInputStream("/proc/self/maps");
        	input = new InputStreamReader(inputStream);
        	reader = new BufferedReader(input);
        	
            // most libraries are defined on several lines, so we need to make sure we parse
            // all the library lines and only add the library at the end
            long startAddr = 0;
            long endAddr = 0;
            String library = null;

            while ((line = reader.readLine()) != null) {
                // Log.d("ddms", "line: " + line);
                if (line.length() < 16) {
                    continue;
                }

                try {
                	int split = line.indexOf('-');
                	if(split == -1) {
                		continue;
                	}
                	int end = line.indexOf(' ', split + 1);
                	if(end == -1) {
                		continue;
                	}
                	
                    long tmpStart = Long.parseLong(line.substring(0, split), 16);
                    long tmpEnd = Long.parseLong(line.substring(split + 1, end), 16);

                    int index = line.indexOf('/');

                    if (index == -1)
                        continue;

                    String tmpLib = line.substring(index);

                    if (library == null ||
                            (library != null && tmpLib.equals(library) == false)) {

                        if (library != null) {
                            addNativeLibraryMapInfo(startAddr, endAddr, library, list);
                            // Log.d("ddms", library + "(" + Long.toHexString(startAddr) + " - " + Long.toHexString(endAddr) + ")");
                        }

                        // now init the new library
                        library = tmpLib;
                        startAddr = tmpStart;
                        endAddr = tmpEnd;
                    } else {
                        // add the new end
                        endAddr = tmpEnd;
                    }
                } catch (NumberFormatException e) {
                    Log.d("Inspector", e.getMessage(), e);
                }
            }

            if (library != null) {
                addNativeLibraryMapInfo(startAddr, endAddr, library, list);
                // Log.d("ddms", library + "(" + Long.toHexString(startAddr) + " - " + Long.toHexString(endAddr) + ")");
            }
            
            return list;
        } finally {
        	IOUtils.closeQuietly(reader);
        	IOUtils.closeQuietly(input);
        	IOUtils.closeQuietly(inputStream);
        }
    }

	private static void addNativeLibraryMapInfo(long startAddr, long endAddr, String library,
			List<NativeLibraryMapInfo> list) {
		list.add(new NativeLibraryMapInfo(startAddr, endAddr, library));
	}
}
