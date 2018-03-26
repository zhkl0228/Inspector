/**
 * 
 */
package com.fuzhu8.inspector.dex.vm.dvm;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

import android.os.Build;

/**
 * @author zhkl0228
 *
 */
public class DvmGlobals extends PointerType {
	
	private int getBootClassPathEntryOffset() {
		switch (Build.VERSION.SDK_INT) {
		case 15:
			return 0x8C;
		case 16:
			return 0x90;
		case 17:
		case 18:
		case 19:
			return 0xA0;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public DvmGlobals(Pointer p) {
		super(p);
	}
	
	public Iterable<ClassPathEntry> getBootClassPath() {
		return new Iterable<ClassPathEntry>() {
			@Override
			public Iterator<ClassPathEntry> iterator() {
				return new ClassPathEntryIterator();
			}
		};
	}
	
	private class ClassPathEntryIterator implements Iterator<ClassPathEntry> {
		ClassPathEntry next = new ClassPathEntry(getPointer().getPointer(getBootClassPathEntryOffset()));
		@Override
		public boolean hasNext() {
			return next.getKind() != ClassPathEntryKind.kCpeLastEntry && next.getKind() != ClassPathEntryKind.kCpeUnknown;
		}
		@Override
		public ClassPathEntry next() {
			if (!hasNext()) {
                throw new NoSuchElementException();
            }
			ClassPathEntry next = this.next;
			Pointer pointer = next.getPointer().share(ClassPathEntry.SIZE_OF_CLASS_PATH_ENTRY);
			this.next = new ClassPathEntry(pointer);
			return next;
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
