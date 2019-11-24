package com.fuzhu8.inspector.io;

import java.io.DataOutputStream;
import java.io.IOException;

public class CommandCache extends AbstractInspectCache {

    private final String type;
    private final String data;

    public CommandCache(String type, String data) {
        super(0x6000, true);
        this.type = type;
        this.data = data;
    }

    @Override
    protected void outputBody(DataOutputStream out) throws IOException {
        out.writeUTF(type);
        out.writeUTF(data);
    }

}
