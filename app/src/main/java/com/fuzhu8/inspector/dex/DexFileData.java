package com.fuzhu8.inspector.dex;

import android.os.Build;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ZipMultiDirectoryCompress;
import com.fuzhu8.inspector.dex.jf.Baksmali;
import com.fuzhu8.inspector.dex.provider.DexFileProvider;
import com.fuzhu8.inspector.dex.vm.dvm.DexOptHeader;
import com.fuzhu8.inspector.io.ByteBufferCache;
import com.fuzhu8.inspector.io.InspectCache;
import com.fuzhu8.inspector.io.SmaliCache;
import com.google.android.collect.Lists;
import com.google.common.collect.ImmutableList;
import com.sun.jna.Native;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.analysis.ClassPathResolver;
import org.jf.dexlib2.analysis.InlineMethodResolver;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;

/**
 * @author zhkl0228
 *
 */
public class DexFileData implements DexFile {
	
	public static DexFileData createFile(ByteBuffer buffer, String name, String label, Inspector inspector, com.fuzhu8.inspector.dex.vm.DexFile dexFile) {
		if(label == null) {
			label = "";
		}

		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.mark();
		try {
			if(inspector.isDebug()) {
				inspector.out_println("createDexFile name=" + name + ", label=" + label);
			}
			
			int magic = buffer.getInt();
			boolean isZip = magic == 0x04034B50;
			if(isZip) {
				return new DexFileData(buffer, name, label + ".jar");
			}
			
			int dexType = buffer.getInt();
			if(magic == 0xA786564 && dexType == 0x353330) {//dex
				return fixStruct(buffer, name, label, inspector, dexFile);
			}
			if(magic == 0xA796564 && dexType == 0x363330) {//odex
				if(inspector.isDebug()) {
					inspector.out_println("createDexFile try_deodex name=" + name + ", label=" + label);
				}
				
				return deodex(buffer, name, label, inspector, dexFile);
			}
			
			return new DexFileData(buffer, name, label + ".dat");
		} finally {
			buffer.reset();
		}
	}
	
	public static SmaliFile[] baksmali(ByteBuffer buffer, String name, String label, Inspector inspector, com.fuzhu8.inspector.dex.vm.DexFile dexFile, String className, DexFileProvider dex) {
		if(label == null) {
			label = "";
		}
		
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.mark();
		try {
			if(inspector.isDebug()) {
				inspector.out_println("baksmali name=" + name + ", label=" + label);
			}
			
			int magic = buffer.getInt();
			boolean isZip = magic == 0x04034B50;
			if(isZip) {
				throw new UnsupportedOperationException("baksmali isZip");
			}
			
			int dexType = buffer.getInt();
			if(magic == 0xA786564 && dexType == 0x353330) {//dex
				return fixStruct(inspector, dexFile, className, dex);
			}
			if(magic == 0xA796564 && dexType == 0x363330) {//odex
				if(inspector.isDebug()) {
					inspector.out_println("baksmali try_deodex name=" + name + ", label=" + label);
				}
				
				return deodex(buffer, inspector, dexFile, className, dex);
			}
			
			throw new UnsupportedOperationException("baksmali");
		} finally {
			buffer.reset();
		}
	}
	
	private static SmaliFile[] fixStruct(Inspector inspector, com.fuzhu8.inspector.dex.vm.DexFile dexFile, String className, DexFileProvider dex) {
		if(dexFile == null) {
			throw new UnsupportedOperationException("dexFile is null");
		}
		
		File dataDir = inspector.getAppDataDir();
		if(dataDir == null) {
			throw new UnsupportedOperationException("dataDir is null");
		}
		
		Locale backup = Locale.getDefault();
		File smaliDir;
		try {
			Locale locale = new Locale("en", "US");
	        Locale.setDefault(locale);

			Opcodes opcodes = Opcodes.forApi(Build.VERSION.SDK_INT);
			DexBackedDexFile fixDexFile = new DexBackedDexFile(opcodes, dexFile);
			
			smaliDir = new File(dataDir, "smali");
			FileUtils.deleteQuietly(smaliDir);
			
			BaksmaliOptions options = new BaksmaliOptions();
			options.deodex = false;
            options.apiLevel = Build.VERSION.SDK_INT;
            options.classPath = loadClassPathForDexFile(fixDexFile, getDefaultBootClassPathForApi(Build.VERSION.SDK_INT), ImmutableList.<String>of());
            options.inlineResolver = null;
            
            return Baksmali.disassembleDexFile(fixDexFile, smaliDir, options, className, dex);
		} catch(Throwable t) {
			inspector.printStackTrace(t);
			return null;
		} finally {
			Locale.setDefault(backup);
		}
	}
	
	private static DexFileData fixStruct(ByteBuffer buffer, String name, String label, Inspector inspector, com.fuzhu8.inspector.dex.vm.DexFile dexFile) {
		if(dexFile == null) {
			return new DexFileData(buffer, name, label + ".dex");
		}
		
		File dataDir = inspector.getAppDataDir();
		if(dataDir == null) {
			return new DexFileData(buffer, name, label + ".dex");
		}
		
		Locale backup = Locale.getDefault();
		boolean errorOccurred = true;
		File smaliDir = null;
		try {
			Locale locale = new Locale("en", "US");
	        Locale.setDefault(locale);

			long start = System.currentTimeMillis();
			inspector.out_println("Prepare fixStruct: " + name + label + ".dex with dexFile");
			Opcodes opcodes = Opcodes.forApi(Build.VERSION.SDK_INT);
			DexBackedDexFile fixDexFile = new DexBackedDexFile(opcodes, dexFile);
			
			smaliDir = new File(dataDir, "smali");
			FileUtils.deleteQuietly(smaliDir);
			
			BaksmaliOptions options = new BaksmaliOptions();
			options.deodex = false;
            options.apiLevel = Build.VERSION.SDK_INT;
			options.classPath = loadClassPathForDexFile(fixDexFile, getDefaultBootClassPathForApi(Build.VERSION.SDK_INT), ImmutableList.<String>of());
            options.inlineResolver = null;
            
            errorOccurred = !Baksmali.disassembleDexFile(fixDexFile, smaliDir, options, inspector);
            if(errorOccurred) {
            	throw new Exception("disassembleDexFile errorOccurred: " + name + label + ".dex");
            }
            
            // ByteBuffer dex = Smali.assembleSmaliFile(inspector, smaliDir);
			// DexFileData data = new DexFileData(dex, name, label + ".dex");
			DexFileData data = new DexFileData(new SmaliCache(smaliDir, name + label));
			FileUtils.deleteQuietly(smaliDir);

			inspector.out_println("fixStruct: " + name + label + ".dex with dexFile elapsed time: " + (System.currentTimeMillis() - start) + "ms");
			return data;
		} catch(Throwable t) {
			inspector.printStackTrace(t);
			
			DexFileData dex = new DexFileData(buffer, name, label + ".dex");
			
			if(!errorOccurred && smaliDir.exists()) {
				return joinSmali(smaliDir, dex, inspector, name, label + ".dex");
			}
			
			return dex;
		} finally {
			Locale.setDefault(backup);
		}
	}

    private static DexFileData joinSmali(File smaliDir, DexFileData dex, Inspector inspector, String baseName, String ext) {
    	//初始化支持多级目录压缩的ZipMultiDirectoryCompress
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipMultiDirectoryCompress zipCompress = new ZipMultiDirectoryCompress();
        ZipOutputStream zos = null;
        try {
            //创建一个Zip输出流
            zos = new ZipOutputStream(baos);
            //启动压缩进程
            zipCompress.startCompress(zos, baseName + ext, smaliDir);
            
            // zos.flush();
        } catch (IOException e){
			inspector.printStackTrace(e);

			return dex;
        } finally{
            IOUtils.closeQuietly(zos);
        }  
        
        return new MultiDexFileData(dex, new DexFileData(ByteBuffer.wrap(baos.toByteArray()), baseName, ".zip"));
	}

	@Nonnull
    private static List<String> getDefaultBootClassPathForApi(int apiLevel) {
        if (apiLevel < 9) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar");
        } else if (apiLevel < 12) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/core-junit.jar");
        } else if (apiLevel < 14) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/core-junit.jar");
        } else if (apiLevel < 16) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/filterfw.jar");
        } else if (apiLevel < 21) {
            // this is correct as of api 17/4.2.2
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar");
        } else { // api >= 21
            return Lists.newArrayList(
                    "/system/framework/core-libart.jar",
                    "/system/framework/conscrypt.jar",
                    "/system/framework/okhttp.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/ims-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/apache-xml.jar");
        }
    }

	private static DexFileData deodex(ByteBuffer buffer, String name, String label, Inspector inspector, com.fuzhu8.inspector.dex.vm.DexFile dexFile) {
		File dataDir = inspector.getAppDataDir();
		if(dataDir == null) {
			return new DexFileData(buffer, name, label + ".odex");
		}
		
		Locale backup = Locale.getDefault();
		File smaliDir = null;
		try {
			Locale locale = new Locale("en", "US");
	        Locale.setDefault(locale);

			long start = System.currentTimeMillis();
			inspector.out_println("Prepare deodex: " + name + label + ".odex with " + (dexFile == null ? "dexBuffer" : "dexFile"));
			
			Opcodes opcodes = Opcodes.forApi(Build.VERSION.SDK_INT);
			DexBackedOdexFile odexFile;
			if(dexFile == null) {
				buffer.reset();
				byte[] copy = new byte[buffer.remaining()];
				buffer.get(copy);
				odexFile = DexBackedOdexFile.fromInputStream(opcodes, new ByteArrayInputStream(copy));
			} else {
				buffer.reset();
				ByteBuffer directBuffer = ByteBuffer.allocateDirect(buffer.remaining());
				directBuffer.put(buffer);
				directBuffer.flip();
				odexFile = new DexBackedOdexFile(opcodes, new DexOptHeader(Native.getDirectBufferPointer(directBuffer)), dexFile);
			}
			
			smaliDir = new File(dataDir, "smali");
			FileUtils.deleteQuietly(smaliDir);
			
			BaksmaliOptions options = new BaksmaliOptions();
			options.deodex = true;
			List<String> bootClassPathEntries = odexFile.getDependencies();
            List<String> bootClassPathDirs = new ArrayList<>(2);
            bootClassPathDirs.add("/system/app");
            bootClassPathDirs.add("/system/framework");
			options.classPath = loadClassPathForDexFile(odexFile, bootClassPathEntries, bootClassPathDirs);
            options.apiLevel = Build.VERSION.SDK_INT;
            options.inlineResolver = InlineMethodResolver.createInlineMethodResolver(odexFile.getOdexVersion());
            
            boolean errorOccurred = !Baksmali.disassembleDexFile(odexFile, smaliDir, options, inspector);
            
            // ByteBuffer dex = Smali.assembleSmaliFile(inspector, smaliDir);
			// DexFileData data = new DexFileData(dex, name, label + ".dex");
			DexFileData data = new DexFileData(new SmaliCache(smaliDir, name + label));
			FileUtils.deleteQuietly(smaliDir);

			inspector.out_println("deodex: " + name + label + ".odex with " + (dexFile == null ? "dexBuffer" : "dexFile") + " elapsed time: " + (System.currentTimeMillis() - start) + "ms");
			if(errorOccurred) {//如果有错误发生，则把odex打包发送
				data = new MultiDexFileData(new DexFileData(buffer, name, label + ".odex"), data);
			}
			return data;
		} catch(InterruptedException e) {
			inspector.printStackTrace(e);
			return new DexFileData(buffer, name, label + ".odex");
		} catch(Throwable t) {
			inspector.printStackTrace(t);
			
			DexFileData odex = new DexFileData(buffer, name, label + ".odex");
			if(smaliDir != null && smaliDir.exists()) {
				return joinSmali(smaliDir, odex, inspector, name, label + ".odex");
			}
			
			return odex;
		} finally {
			Locale.setDefault(backup);
		}
	}

	private static SmaliFile[] deodex(ByteBuffer buffer, Inspector inspector, com.fuzhu8.inspector.dex.vm.DexFile dexFile, String className, DexFileProvider dex) {
		File dataDir = inspector.getAppDataDir();
		if(dataDir == null) {
			throw new UnsupportedOperationException("dataDir is null");
		}
		
		Locale backup = Locale.getDefault();
		File smaliDir;
		try {
			Locale locale = new Locale("en", "US");
	        Locale.setDefault(locale);
			
			Opcodes opcodes = Opcodes.forApi(Build.VERSION.SDK_INT);
			DexBackedOdexFile odexFile;
			if(dexFile == null) {
				buffer.reset();
				byte[] copy = new byte[buffer.remaining()];
				buffer.get(copy);
				odexFile = DexBackedOdexFile.fromInputStream(opcodes, new ByteArrayInputStream(copy));
			} else {
				buffer.reset();
				ByteBuffer directBuffer = ByteBuffer.allocateDirect(buffer.remaining());
				directBuffer.put(buffer);
				directBuffer.flip();
				odexFile = new DexBackedOdexFile(opcodes, new DexOptHeader(Native.getDirectBufferPointer(directBuffer)), dexFile);
			}
			
			smaliDir = new File(dataDir, "smali");
			FileUtils.deleteQuietly(smaliDir);
			
			BaksmaliOptions options = new BaksmaliOptions();
			options.deodex = true;
			List<String> bootClassPathEntries = odexFile.getDependencies();
			List<String> bootClassPathDirs = new ArrayList<>(2);
			bootClassPathDirs.add("/system/app");
            bootClassPathDirs.add("/system/framework");
			options.classPath = loadClassPathForDexFile(odexFile, bootClassPathEntries, bootClassPathDirs);
            options.apiLevel = Build.VERSION.SDK_INT;
            options.inlineResolver = InlineMethodResolver.createInlineMethodResolver(odexFile.getOdexVersion());
            
            return Baksmali.disassembleDexFile(odexFile, smaliDir, options, className, dex);
		} catch(Exception t) {
			inspector.printStackTrace(t);
			
			return null;
		} finally {
			Locale.setDefault(backup);
		}
	}

	private final InspectCache out;

	DexFileData(ByteBuffer buffer, String baseName, String ext) {
		this(new ByteBufferCache(baseName + ext, buffer));
	}

	private DexFileData(InspectCache out) {
		this.out = out;
	}

	@Override
	public void writeToConsole(Inspector inspector) {
		inspector.writeToConsole(out);
	}

	@Nonnull
	private static ClassPath loadClassPathForDexFile(@Nonnull org.jf.dexlib2.iface.DexFile dexFile, List<String> bootClassPath, List<String> classPathDirectories)
			throws IOException {
		ClassPathResolver resolver;

		if (bootClassPath == null) {
			// except that the oat version -> api mapping doesn't fully work yet
			resolver = new ClassPathResolver(classPathDirectories, ImmutableList.<String>of(), dexFile);
		} else {
			resolver = new ClassPathResolver(classPathDirectories, bootClassPath, ImmutableList.<String>of(), dexFile);
		}

		return new ClassPath(resolver.getResolvedClassProviders());
	}

}
