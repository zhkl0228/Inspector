package com.fuzhu8.inspector.module;

/**
 * module class loader
 * Created by zhkl0228 on 2018/1/9.
 */

public class ModuleClassLoader extends ClassLoader {

    private final ClassLoader last;

    ModuleClassLoader(ClassLoader parent, ClassLoader last) {
        super(parent);
        this.last = last;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return last.loadClass(name);
    }

}
