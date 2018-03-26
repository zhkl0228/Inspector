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

import android.os.Build;

import com.fuzhu8.inspector.Inspector;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.smali.LexerErrorInterface;
import org.jf.smali.smaliFlexLexer;
import org.jf.smali.smaliParser;
import org.jf.smali.smaliTreeWalker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import cn.banny.utils.IOUtils;

/**
 * Main class for smali. It recognizes enough options to be able to dispatch
 * to the right "actual" main.
 */
public class Smali {

    public static ByteBuffer assembleSmaliFile(Inspector inspector, String ... smalies) throws Exception {
        int apiLevel = Build.VERSION.SDK_INT;

        final DexBuilder dexBuilder = new DexBuilder(Opcodes.forApi(apiLevel));

        for(String smali : smalies) {
            boolean errors = !assembleSmaliFile(inspector, new StringReader(smali), null, dexBuilder, apiLevel);
        	if(errors) {
        		throw new RuntimeException("assembleSmaliFile failed.");
        	}
        }

        MemoryDataStore dataStore = new MemoryDataStore();
        dexBuilder.writeTo(dataStore);
        byte[] data = dataStore.getData();
        return ByteBuffer.wrap(data, 0, data.length);
    }

    @SuppressWarnings("unused")
    public static ByteBuffer assembleSmaliFile(Inspector inspector, File dir) throws Exception {
        int apiLevel = Build.VERSION.SDK_INT;

        LinkedHashSet<File> filesToProcess = new LinkedHashSet<>();

        if (!dir.exists()) {
            throw new IOException("Cannot find file or directory \"" + dir + "\"");
        }
        if(!dir.isDirectory()) {
        	throw new IOException("dir not exists: " + dir);
        }

        getSmaliFilesInDir(dir, filesToProcess);

        final DexBuilder dexBuilder = new DexBuilder(Opcodes.forApi(apiLevel));

        int total = filesToProcess.size();
        int dirLen = dir.getAbsolutePath().length();
        inspector.println("Prepare assembleSmaliFile total: " + total);
        Thread thread = Thread.currentThread();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Void>> tasks = new ArrayList<>(total);
        WorkerListener workerListener = new $WorkerListener(total, inspector);
        for (final File file: filesToProcess) {
        	AssembleSmaliWorker worker = new AssembleSmaliWorker(thread, workerListener, file, dirLen, inspector, dexBuilder, apiLevel);
            tasks.add(executor.submit(worker));
        }

        try {
            for (Future<Void> task : tasks) {
                task.get();
            }
        } finally {
            executor.shutdownNow();
        }

        MemoryDataStore dataStore = new MemoryDataStore();
        dexBuilder.writeTo(dataStore);
        byte[] data = dataStore.getData();
        return ByteBuffer.wrap(data, 0, data.length);
    }

    private static class AssembleSmaliWorker implements Callable<Void> {
        private final Thread thread;
        private final WorkerListener workerListener;
        private final File file;
        private final int dirLen;
        private final Inspector inspector;
        private final DexBuilder dexBuilder;
        private final int apiLevel;
        AssembleSmaliWorker(Thread thread, WorkerListener workerListener, File file, int dirLen, Inspector inspector, DexBuilder dexBuilder, int apiLevel) {
            this.thread = thread;
            this.workerListener = workerListener;
            this.file = file;
            this.dirLen = dirLen;
            this.inspector = inspector;
            this.dexBuilder = dexBuilder;
            this.apiLevel = apiLevel;
        }
        @Override
        public Void call() throws Exception {
            if(thread.isInterrupted()) {
                throw new InterruptedException("assembleSmaliFile");
            }

            workerListener.notifyBegin("assembleSmaliFile: " + file.getAbsolutePath().substring(dirLen));

            boolean errors = !assembleSmaliFile(inspector, file, dexBuilder, apiLevel);
            if(errors) {
                RuntimeException e = new RuntimeException("assembleSmaliFile failed: " + file.getAbsolutePath().substring(dirLen));
                workerListener.notifyException(e);
            }
            return null;
        }
    }

    private static void getSmaliFilesInDir(@Nonnull File dir, @Nonnull Set<File> smaliFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for(File file: files) {
                if (file.isDirectory()) {
                    getSmaliFilesInDir(file, smaliFiles);
                } else if (file.getName().endsWith(".smali")) {
                    smaliFiles.add(file);
                }
            }
        }
    }

    private static boolean assembleSmaliFile(Inspector inspector, File smaliFile, DexBuilder dexBuilder,
                                             int apiLevel)
            throws Exception {
        FileInputStream fis = null;
        InputStreamReader reader = null;
        try {
            fis = new FileInputStream(smaliFile.getAbsolutePath());
            reader = new InputStreamReader(fis, "UTF-8");
            return assembleSmaliFile(inspector, reader, smaliFile, dexBuilder, apiLevel);
        } finally {
            IOUtils.close(reader);
            IOUtils.close(fis);
        }
    }

    private static boolean assembleSmaliFile(Inspector inspector, Reader reader, File smaliFile, DexBuilder dexBuilder,
                                             int apiLevel)
            throws Exception {
        CommonTokenStream tokens;

        LexerErrorInterface lexer;

        lexer = new smaliFlexLexer(reader);
        ((smaliFlexLexer)lexer).setSourceFile(smaliFile);
        tokens = new CommonTokenStream((TokenSource)lexer);

        if (inspector != null && inspector.isDebug()) {
            tokens.getTokens();

            for (int i=0; i<tokens.size(); i++) {
                Token token = tokens.get(i);
                if (token.getChannel() == smaliParser.HIDDEN) {
                    continue;
                }

                inspector.println(smaliParser.tokenNames[token.getType()] + ": " + token.getText());
            }
        }

        smaliParser parser = new smaliParser(tokens);
        parser.setVerboseErrors(false);
        parser.setAllowOdex(false);
        parser.setApiLevel(apiLevel);

        smaliParser.smali_file_return result = parser.smali_file();

        if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
            return false;
        }

        CommonTree t = result.getTree();

        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
        treeStream.setTokenStream(tokens);

        if (inspector != null && inspector.isDebug()) {
            inspector.println(t.toStringTree());
        }

        smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);
        dexGen.setApiLevel(apiLevel);

        dexGen.setVerboseErrors(false);
        dexGen.setDexBuilder(dexBuilder);
        dexGen.smali_file();

        return dexGen.getNumberOfSyntaxErrors() == 0;
    }
}