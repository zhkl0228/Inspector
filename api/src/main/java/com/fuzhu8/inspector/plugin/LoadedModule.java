package com.fuzhu8.inspector.plugin;

public class LoadedModule {

    public final String filename;
    public final long base;
    public final int size;

    public LoadedModule(String filename, long base, int size) {
        this.filename = filename;
        this.base = base;
        this.size = size;
    }

}
