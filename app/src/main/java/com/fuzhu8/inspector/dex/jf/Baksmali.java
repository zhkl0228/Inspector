/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.fuzhu8.inspector.dex.jf;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.SmaliFile;
import com.fuzhu8.inspector.dex.provider.DexFileProvider;
import com.google.common.collect.Ordering;

import org.apache.commons.io.FileUtils;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.util.ClassFileNameHandler;
import org.jf.util.IndentingWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Baksmali {

    public static SmaliFile[] disassembleDexFile(DexFile dexFile, File outputDir, final BaksmaliOptions options, String className, DexFileProvider dex) throws Exception {
    	List<? extends ClassDef> classDefs = setOptions(dexFile, outputDir);

        String prefix = 'L' + className.replace('.', '/');
        final String binaryName = prefix + ';';
        final String innerClass = prefix + '$';
        
        List<SmaliFile> smalies = new ArrayList<>();
        for (final ClassDef classDef: classDefs) {
        	/*
             * The path for the disassembly file is based on the package name
             * The class descriptor will look something like:
             * Ljava/lang/Object;
             * Where the there is leading 'L' and a trailing ';', and the parts of the
             * package name are separated by '/'
             */
            String classDescriptor = classDef.getType();

            //validate that the descriptor is formatted like we expect
            if (classDescriptor.charAt(0) != 'L' ||
                    classDescriptor.charAt(classDescriptor.length()-1) != ';') {
                throw new RuntimeException("Unrecognized class descriptor - " + classDescriptor);
            }
            
            if(binaryName.equals(classDescriptor) || classDescriptor.startsWith(innerClass)) { // me or inner
            	String name = classDescriptor.substring(1, classDescriptor.length() - 1).replace('/', '.');
            	try {
                    smalies.add(new SmaliFile(name, disassembleClass(classDef, options), dex));
                } catch(Exception ignored) {}
            }
        }
		return smalies.toArray(new SmaliFile[0]);
    }

    private static List<? extends ClassDef> setOptions(DexFile dexFile, File outputDirectoryFile) throws InterruptedException, IOException {
    	if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                throw new IOException("Can't create the output directory " + outputDirectoryFile);
            }
        }

        //sort the classes, so that if we're on a case-insensitive file system and need to handle classes with file
        //name collisions, then we'll use the same name for each class, if the dex file goes through multiple
        //baksmali/smali cycles for some reason. If a class with a colliding name is added or removed, the filenames
        //may still change of course

        return Ordering.natural().sortedCopy(dexFile.getClasses());
	}

	public static boolean disassembleDexFile(DexFile dexFile, File outputDirectoryFile, final BaksmaliOptions options, Inspector inspector) throws Exception {
    	if(inspector != null && inspector.isDebug()) {
    		inspector.println(options);
    	}
    	
        List<? extends ClassDef> classDefs = setOptions(dexFile, outputDirectoryFile);

        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                throw new IOException("Can't create the output directory " + outputDirectoryFile);
            }
        }
        final ClassFileNameHandler fileNameHandler = new ClassFileNameHandler(outputDirectoryFile, ".smali");

        int total = classDefs.size();
        if(inspector != null) {
        	inspector.println("Prepare disassembleDexFile total: " + total);
        }
        
        Thread thread = Thread.currentThread();
        int jobs = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(jobs > 4 ? 4 : jobs);
        List<Future<Void>> tasks = new ArrayList<>(classDefs.size());
        WorkerListener workerListener = new $WorkerListener(total, inspector);
        for (final ClassDef classDef : classDefs) {
            DisassembleClassWorker worker = new DisassembleClassWorker(thread, workerListener, classDef, fileNameHandler, options);
            tasks.add(executor.submit(worker));
        }

        try {
            for (Future<Void> task : tasks) {
                task.get();
            }
        } finally {
            executor.shutdownNow();
        }

        return true;
    }

    private static class DisassembleClassWorker implements Callable<Void> {
        private final Thread thread;
        private final WorkerListener workerListener;
        private final ClassDef classDef;
        private final ClassFileNameHandler fileNameHandler;
        private final BaksmaliOptions options;
        DisassembleClassWorker(Thread thread, WorkerListener workerListener, ClassDef classDef, ClassFileNameHandler fileNameHandler, BaksmaliOptions options) {
            this.thread = thread;
            this.workerListener = workerListener;
            this.classDef = classDef;
            this.fileNameHandler = fileNameHandler;
            this.options = options;
        }
        @Override
        public Void call() throws Exception {
            if(thread.isInterrupted()) {
                throw new InterruptedException("disassembleDexFile");
            }

            workerListener.notifyBegin("disassembleDexFile: " + classDef.getType());

            File smaliFile = null;
            try {
                /*
                 * The path for the disassembly file is based on the package name
                 * The class descriptor will look something like:
                 * Ljava/lang/Object;
                 * Where the there is leading 'L' and a trailing ';', and the parts of the
                 * package name are separated by '/'
                 */
                String classDescriptor = classDef.getType();

                //validate that the descriptor is formatted like we expect
                if (classDescriptor.charAt(0) != 'L' ||
                        classDescriptor.charAt(classDescriptor.length()-1) != ';') {
                    throw new RuntimeException("Unrecognized class descriptor - " + classDescriptor);
                }

                smaliFile = fileNameHandler.getUniqueFilenameForClass(classDescriptor);
                disassembleClass(classDef, smaliFile, options);
            } catch(Exception e) {
                workerListener.notifyException(new Exception("disassembleDexFile failed: " + classDef.getType(), e));

                FileUtils.deleteQuietly(smaliFile);
            }
            return null;
        }
    }

    private static void disassembleClass(ClassDef classDef, File smaliFile,
                                            BaksmaliOptions options) throws Exception {
        //write the disassembly
        IndentingWriter writer = null;
        try
        {
            File smaliParent = smaliFile.getParentFile();
            if (!smaliParent.exists()) {
                if (!smaliParent.mkdirs()) {
                    // check again, it's likely it was created in a different thread
                    if (!smaliParent.exists()) {
                        throw new RuntimeException("Unable to create directory " + smaliParent.toString() + " - skipping class");
                    }
                }
            }

            if (!smaliFile.exists()){
                if (!smaliFile.createNewFile()) {
                    throw new IOException("Unable to create file " + smaliFile.toString() + " - skipping class");
                }
            }

            BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(smaliFile), "UTF8"));

            writer = new IndentingWriter(bufWriter);
            disassembleClass(classDef, writer, options);
        }
        finally
        {
            if (writer != null) {
            	 writer.close();
            }
        }
    }

    private static String disassembleClass(ClassDef classDef, BaksmaliOptions options) throws Exception {
        //write the disassembly
        IndentingWriter writer = null;
        try
        {
        	Writer stringWriter = new StringWriter();
            BufferedWriter bufWriter = new BufferedWriter(stringWriter);

            writer = new IndentingWriter(bufWriter);
            disassembleClass(classDef, writer, options);
            writer.flush();
            return stringWriter.toString();
        }
        finally
        {
            if (writer != null) {
            	 writer.close();
            }
        }
    }

    private static void disassembleClass(ClassDef classDef, IndentingWriter indentingWriter,
                                            BaksmaliOptions options) throws Exception {
        //create and initialize the top level string template
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(indentingWriter);
    }

}
