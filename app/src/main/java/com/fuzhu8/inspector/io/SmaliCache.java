package com.fuzhu8.inspector.io;

import android.os.Build;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * smali cache
 * Created by zhkl0228 on 2018/3/25.
 */

public class SmaliCache extends AbstractInspectCache implements InspectCache {

    private final byte[] fileData;
    private final String baseName;

    public SmaliCache(File smaliDir, String baseName) {
        super(0x5000, true);

        this.baseName = baseName;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 1024);
            DataOutputStream dos = new DataOutputStream(baos);
            addFile(dos, smaliDir, null);
            this.fileData = baos.toByteArray();
        } catch(IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void addFile(DataOutputStream dos, File file, String path) throws IOException {
        String name = path == null ? file.getName() : path + '/' + file.getName();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File sub : files) {
                    addFile(dos, sub, name);
                }
            }
            return;
        }

        dos.writeUTF(name);
        dos.writeInt((int) file.length());
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            IOUtils.copy(inputStream, dos);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    protected void outputBody(DataOutputStream out) throws IOException {
        out.writeInt(Build.VERSION.SDK_INT);
        out.writeUTF(baseName);
        out.writeInt(fileData.length);
        out.write(fileData);
    }

}
