package com.fuzhu8.inspector.stetho.urlconnection;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Inspector output stream
 * Created by zhkl0228 on 2018/1/4.
 */

public class InspectorOutputStream extends FilterOutputStream {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    InspectorOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);

        baos.write(b);
    }

    public byte[] toByteArray() {
        return baos.toByteArray();
    }
}
