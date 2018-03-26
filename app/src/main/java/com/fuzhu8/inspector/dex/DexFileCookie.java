/**
 * 
 */
package com.fuzhu8.inspector.dex;

/**
 * @author zhkl0228
 *
 */
public class DexFileCookie {
	
	private final long cookie;
	private final String fileName;
	private final ClassLoader classLoader;
	
	public DexFileCookie(long cookie, String fileName, ClassLoader classLoader) {
		super();
		this.cookie = cookie;
		this.fileName = fileName;
		this.classLoader = classLoader;
	}

	public long getCookie() {
		return cookie;
	}

	public String getFileName() {
		return fileName;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public String toString() {
		return "DexFileCookie [cookie=" + cookie + ", fileName=" + fileName + "]";
	}

}
