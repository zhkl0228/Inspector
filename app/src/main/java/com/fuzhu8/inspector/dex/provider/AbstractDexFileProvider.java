package com.fuzhu8.inspector.dex.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.vm.DexFile;
import com.fuzhu8.inspector.dex.vm.dvm.DexClassDef;


/**
 * @author zhkl0228
 *
 */
public abstract class AbstractDexFileProvider implements DexFileProvider {
	
	private ClassLoader classLoader;
	protected final String name;

	AbstractDexFileProvider(ClassLoader classLoader, String name) {
		super();
		this.classLoader = classLoader;
		this.name = name;
	}
	
	protected abstract char getPrefix();

	@Override
	public void print(Inspector inspector) {
		inspector.println("0x" + Long.toHexString(getCookie()).toUpperCase(Locale.CHINA) + '=' + getMyPath());
		if(classLoader != null) {
			inspector.println("\t" + classLoader);
		}
	}
	
	@Override
	public final String getMyPath() {
		return getPrefix() + name + '_' + "0x" + Long.toHexString(getCookie()).toUpperCase(Locale.CHINA);
	}

	/**
	 * 默认无cookie
	 */
	protected long getCookie() {
		return 0x88888888L;
	}

	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		if(classLoader == null) {
			throw new ClassNotFoundException();
		}
		
		try {
			return Class.forName(className, false, classLoader);
		} catch(Throwable t) {
			throw new ClassNotFoundException();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractDexFileProvider other = (AbstractDexFileProvider) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public boolean accept(String dexPath) {
		String path = getMyPath();
		if (path.equals(dexPath)) {
			return true;
		} else {
			if (dexPath.endsWith("*")) {
				return path.startsWith(dexPath.substring(0, dexPath.length() - 1));
			}

			return false;
		}
	}

	protected static Collection<String> getClassesFromDexFile(DexFile dexFile) {
		int classCount = dexFile.getHeader().getClassDefsSize();
		List<String> list = new ArrayList<String>(classCount);
		for(int i = 0; i < classCount; i++) {
			DexClassDef classDef = dexFile.getClassDef(i);
			String descriptor = dexFile.dexGetClassDescriptor(classDef);
			list.add(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
		}
		return list;
	}

	@Override
	public void discoverClassLoader(long cookie, ClassLoader classLoader) {
		if(cookie == getCookie() && this.classLoader == null) {
			this.classLoader = classLoader;
		}
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public void doTest() {
	}

}
