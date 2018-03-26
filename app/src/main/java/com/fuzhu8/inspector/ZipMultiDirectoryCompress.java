package com.fuzhu8.inspector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
public class ZipMultiDirectoryCompress {
   
    public void startCompress(ZipOutputStream zos, String oppositePath, File file) throws IOException {
        if(!file.isDirectory()) {
            addFile(zos, oppositePath, file);
        	return;
        }
        
        for(File f : file.listFiles()) {
        	if(!f.isDirectory()) {
        		addFile(zos, oppositePath, f);
        		continue;
        	}
        	
        	addDirectory(zos, oppositePath, f);
        	startCompress(zos, oppositePath + '/' + f.getName(), f);
        }
    }
   
    private void addFile(ZipOutputStream zos, String oppositePath, File file) throws IOException {
        ZipEntry entry = new ZipEntry(oppositePath + '/' + file.getName());
        InputStream is = null;
        try{
            zos.putNextEntry(entry);
            is = new FileInputStream(file);           
            IOUtils.copy(is, zos);
            zos.flush();
            zos.closeEntry();
        } finally {
            IOUtils.closeQuietly(is);
        }       
    }
   
    private void addDirectory(ZipOutputStream zos, String oppositePath, File file) throws IOException {
        ZipEntry entry = new ZipEntry(oppositePath + '/' + file.getName() + "/");
        zos.putNextEntry(entry);
        zos.closeEntry();
    }
}