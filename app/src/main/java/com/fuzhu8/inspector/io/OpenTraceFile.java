package com.fuzhu8.inspector.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * open trace file
 * Created by zhkl0228 on 2017/9/17.
 */

public class OpenTraceFile extends AbstractInspectCache implements InspectCache {

    private final String keywords;
    private final String title;
    private final InputStream inputStream;
    private final int length;

    public OpenTraceFile(String keywords, String title, InputStream inputStream, int length) {
        super(0x4000);

        this.keywords = keywords;
        this.title = title;
        this.inputStream = inputStream;
        this.length = length;
    }

    @Override
    protected void outputBody(DataOutputStream out) throws IOException {
        out.writeUTF(keywords);
        out.writeUTF(title);

        out.writeInt(length);

        byte[] buf = new byte[1024];
        int len;
        while((len = inputStream.read(buf)) != -1) {
            out.write(buf, 0, len);
            out.flush();
        }

        out.flush();
    }

    @Override
    public boolean canCache() {
        return false;
    }

}
