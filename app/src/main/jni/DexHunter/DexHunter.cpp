//
// Created by 陈伟腾 on 15/8/5.
//

#include "DexHunter.h"
#include <jni.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/mman.h>

static struct CookieMap *allCookie = NULL;

static void saveCookie(JNIEnv *env, jobject obj, jint cookie, jstring dataDir);
static jobject DumpClass(JNIEnv *env, jobject obj, jint cookie, jobject classLoader);

static JNINativeMethod gMethods[] = {
    { "_saveDexFileByCookie", "(ILjava/lang/String;)V", (void *) saveCookie },
    { "_dumpDexFileByCookie", "(ILjava/lang/ClassLoader;)Ljava/nio/ByteBuffer;", (void *) DumpClass },
};

/*
* 为某一个类注册本地方法
*/
static int registerNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *gMethods,
                                 int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}


/*
* 为所有类注册本地方法
*/
static int registerNatives(JNIEnv *env) {
    const char *kClassName = "com/fuzhu8/inspector/jni/DexHunter";//指定要注册的类
    return registerNativeMethods(env, kClassName, gMethods,
                                 sizeof(gMethods) / sizeof(gMethods[0]));
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    jint result = -1;

    LOGE("in jni onload");
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }

    LOGE("register natives");
    if (!registerNatives(env)) {//注册
        LOGE("register failed");
        return -1;
    }
    //成功
    result = JNI_VERSION_1_4;
    LOGE("register success");
    return result;
}

static void saveHeaderAndData(CookieMap *cookieMap, const char *name) {
	DexFile *pDexFile = cookieMap->pDvmDex->pDexFile;
	MemMapping *mem = &cookieMap->pDvmDex->memMap;

	const u1 *addr = (const u1*) mem->addr;
	int length = int(pDexFile->baseAddr + pDexFile->pHeader->classDefsOff - addr);
    cookieMap->header = malloc(length);
    memcpy(cookieMap->header, pDexFile->pOptHeader, length);
    cookieMap->header_len = length;
	LOGE("saveHeaderAndData, header=%p, length=%u, name=%s", cookieMap->header, cookieMap->header_len, name);
    if(length <= 1024) {
        inspect((void *)pDexFile->pOptHeader, length, "saveHeader: %s", name);
    }

    addr = pDexFile->baseAddr + pDexFile->pHeader->classDefsOff + sizeof(DexClassDef) * pDexFile->pHeader->classDefsSize;
    length = int((const u1*) mem->addr + mem->length - addr);
    cookieMap->data = malloc(length);
    memcpy(cookieMap->data, addr, length);
    cookieMap->data_len = length;
    LOGE("saveHeaderAndData, data=%p, length=%u, name=%s", cookieMap->data, cookieMap->data_len, name);
    // inspect(addr, length, "saveData: %s", name);
}

static const char *tempDir = NULL;

static void saveCookie(JNIEnv *env, jobject obj, jint cookie, jstring dataDir) {
	if(tempDir == NULL && dataDir != NULL) {
		tempDir = env->GetStringUTFChars(dataDir, NULL);
	}

    if (cookie == 0)
        return;

    LOGE("cookie: %p, tempDir=%s", (void *)cookie, tempDir);
    DexOrJar *pDexorJar = (DexOrJar *) cookie;
    LOGE("filename: %s", pDexorJar->fileName);
    DvmDex *pDvmDex = NULL;
    const char *name = NULL;
    if (pDexorJar->isDex) {
    	name = pDexorJar->pRawDexFile->cacheFileName;
        LOGE("cache filename : %s", name);
        pDvmDex = pDexorJar->pRawDexFile->pDvmDex;
        LOGE("it's a dex file, addr: %p", pDvmDex->pDexFile);
    } else {
    	name = pDexorJar->pJarFile->cacheFileName;
        LOGE("cache filename : %s", name);
        pDvmDex = pDexorJar->pJarFile->pDvmDex;
        LOGE("it's a jar file, addr: %p", pDvmDex->pDexFile);
    }
    if (memcmp(pDvmDex->pDexFile->pOptHeader->magic, DEX_OPT_MAGIC, 4) == 0) {
        if (memcmp(pDvmDex->pDexFile->pOptHeader->magic + 4, DEX_OPT_MAGIC_VERS, 4) != 0) {
            LOGE("bad opt version");
            goto bad;
        }
        if (allCookie == NULL) {
            allCookie = (struct CookieMap *) malloc(sizeof(CookieMap));
            if (allCookie == 0)
                goto bad;

            allCookie->cookie = (int) cookie;
            allCookie->pDvmDex = pDvmDex;
            allCookie->dex = NULL;
            saveHeaderAndData(allCookie, name);

            allCookie->next = NULL;
        } else {
            struct CookieMap *next = allCookie;
            if (next->cookie == cookie)
                goto bad;
            while (next->next != NULL) {
                if (next->cookie == cookie)
                    goto bad;
                next = next->next;
            }
            next->next = new CookieMap();
            next = next->next;
            next->cookie = (int) cookie;
            next->pDvmDex = pDvmDex;
            next->dex = NULL;
            saveHeaderAndData(next, name);
            next->next = NULL;
        }
    }
    bad:
    return;
}

static struct CookieMap *findCookie(int cookie) {
    struct CookieMap *next = allCookie;
    while (next != NULL) {
        if (next->cookie == cookie)
            break;
        LOGE("next cookie=%p", (void *)next->cookie);
        next = next->next;
    }
    if (next == NULL) {
        LOGE("unsaved cookie");
        return NULL;
    } else {
        LOGE("find cookie!");
    }
    return next;
}

static void ReadClassDataHeader(const uint8_t** pData,
        DexClassDataHeader *pHeader) {
    pHeader->staticFieldsSize = readUnsignedLeb128(pData);
    pHeader->instanceFieldsSize = readUnsignedLeb128(pData);
    pHeader->directMethodsSize = readUnsignedLeb128(pData);
    pHeader->virtualMethodsSize = readUnsignedLeb128(pData);
}

static void ReadClassDataField(const uint8_t** pData, DexField* pField) {
    pField->fieldIdx = readUnsignedLeb128(pData);
    pField->accessFlags = readUnsignedLeb128(pData);
}

static void ReadClassDataMethod(const uint8_t** pData, DexMethod* pMethod) {
    pMethod->methodIdx = readUnsignedLeb128(pData);
    pMethod->accessFlags = readUnsignedLeb128(pData);
    pMethod->codeOff = readUnsignedLeb128(pData);
}

static DexClassData* ReadClassData(const uint8_t** pData) {

    DexClassDataHeader header;

    if (*pData == NULL) {
        return NULL;
    }

    ReadClassDataHeader(pData,&header);

    size_t resultSize = sizeof(DexClassData) + (header.staticFieldsSize * sizeof(DexField)) + (header.instanceFieldsSize * sizeof(DexField)) + (header.directMethodsSize * sizeof(DexMethod)) + (header.virtualMethodsSize * sizeof(DexMethod));

    DexClassData* result = (DexClassData*) malloc(resultSize);

    if (result == NULL) {
        return NULL;
    }

    uint8_t* ptr = ((uint8_t*) result) + sizeof(DexClassData);

    result->header = header;

    if (header.staticFieldsSize != 0) {
        result->staticFields = (DexField*) ptr;
        ptr += header.staticFieldsSize * sizeof(DexField);
    } else {
        result->staticFields = NULL;
    }

    if (header.instanceFieldsSize != 0) {
        result->instanceFields = (DexField*) ptr;
        ptr += header.instanceFieldsSize * sizeof(DexField);
    } else {
        result->instanceFields = NULL;
    }

    if (header.directMethodsSize != 0) {
        result->directMethods = (DexMethod*) ptr;
        ptr += header.directMethodsSize * sizeof(DexMethod);
    } else {
        result->directMethods = NULL;
    }

    if (header.virtualMethodsSize != 0) {
        result->virtualMethods = (DexMethod*) ptr;
    } else {
        result->virtualMethods = NULL;
    }

    for (uint32_t i = 0; i < header.staticFieldsSize; i++) {
        ReadClassDataField(pData, &result->staticFields[i]);
    }

    for (uint32_t i = 0; i < header.instanceFieldsSize; i++) {
        ReadClassDataField(pData, &result->instanceFields[i]);
    }

    for (uint32_t i = 0; i < header.directMethodsSize; i++) {
        ReadClassDataMethod(pData, &result->directMethods[i]);
    }

    for (uint32_t i = 0; i < header.virtualMethodsSize; i++) {
        ReadClassDataMethod(pData, &result->virtualMethods[i]);
    }

    return result;
}

static void writeLeb128(uint8_t ** ptr, uint32_t data)
{
    while (true) {
        uint8_t out = data & 0x7f;
        if (out != data) {
            *(*ptr)++ = out | 0x80;
            data >>= 7;
        } else {
            *(*ptr)++ = out;
            break;
        }
    }
}

static uint8_t* EncodeClassData(DexClassData *pData, int& len) {
    len = 0;

    len += unsignedLeb128Size(pData->header.staticFieldsSize);
    len += unsignedLeb128Size(pData->header.instanceFieldsSize);
    len += unsignedLeb128Size(pData->header.directMethodsSize);
    len += unsignedLeb128Size(pData->header.virtualMethodsSize);

    if (pData->staticFields) {
        for (uint32_t i = 0; i < pData->header.staticFieldsSize; i++) {
            len += unsignedLeb128Size(pData->staticFields[i].fieldIdx);
            len += unsignedLeb128Size(pData->staticFields[i].accessFlags);
        }
    }

    if (pData->instanceFields) {
        for (uint32_t i = 0; i < pData->header.instanceFieldsSize; i++) {
            len += unsignedLeb128Size(pData->instanceFields[i].fieldIdx);
            len += unsignedLeb128Size(pData->instanceFields[i].accessFlags);
        }
    }

    if (pData->directMethods) {
        for (uint32_t i=0; i<pData->header.directMethodsSize; i++) {
            len += unsignedLeb128Size(pData->directMethods[i].methodIdx);
            len += unsignedLeb128Size(pData->directMethods[i].accessFlags);
            len += unsignedLeb128Size(pData->directMethods[i].codeOff);
        }
    }

    if (pData->virtualMethods) {
        for (uint32_t i=0; i<pData->header.virtualMethodsSize; i++) {
            len += unsignedLeb128Size(pData->virtualMethods[i].methodIdx);
            len += unsignedLeb128Size(pData->virtualMethods[i].accessFlags);
            len += unsignedLeb128Size(pData->virtualMethods[i].codeOff);
        }
    }

    uint8_t * store = (uint8_t *) malloc(len);

    if (!store) {
        return NULL;
    }

    uint8_t * result=store;

    writeLeb128(&store,pData->header.staticFieldsSize);
    writeLeb128(&store,pData->header.instanceFieldsSize);
    writeLeb128(&store,pData->header.directMethodsSize);
    writeLeb128(&store,pData->header.virtualMethodsSize);

    if (pData->staticFields) {
        for (uint32_t i = 0; i < pData->header.staticFieldsSize; i++) {
            writeLeb128(&store,pData->staticFields[i].fieldIdx);
            writeLeb128(&store,pData->staticFields[i].accessFlags);
        }
    }

    if (pData->instanceFields) {
        for (uint32_t i = 0; i < pData->header.instanceFieldsSize; i++) {
            writeLeb128(&store,pData->instanceFields[i].fieldIdx);
            writeLeb128(&store,pData->instanceFields[i].accessFlags);
        }
    }

    if (pData->directMethods) {
        for (uint32_t i=0; i<pData->header.directMethodsSize; i++) {
            writeLeb128(&store,pData->directMethods[i].methodIdx);
            writeLeb128(&store,pData->directMethods[i].accessFlags);
            writeLeb128(&store,pData->directMethods[i].codeOff);
        }
    }

    if (pData->virtualMethods) {
        for (uint32_t i=0; i<pData->header.virtualMethodsSize; i++) {
            writeLeb128(&store,pData->virtualMethods[i].methodIdx);
            writeLeb128(&store,pData->virtualMethods[i].accessFlags);
            writeLeb128(&store,pData->virtualMethods[i].codeOff);
        }
    }

    free(pData);
    return result;
}

static uint8_t* codeitem_end(const u1** pData)
{
    uint32_t num_of_list = readUnsignedLeb128(pData);
    for (;num_of_list>0;num_of_list--) {
        int32_t num_of_handlers=readSignedLeb128(pData);
        int num=num_of_handlers;
        if (num_of_handlers<=0) {
            num=-num_of_handlers;
        }
        for (; num > 0; num--) {
            readUnsignedLeb128(pData);
            readUnsignedLeb128(pData);
        }
        if (num_of_handlers<=0) {
            readUnsignedLeb128(pData);
        }
    }
    return (uint8_t*)(*pData);
}

struct BytecodeFix {
	u4 codeOff;
	BytecodeMap *bytecode;
	BytecodeFix *next;
};

static BytecodeFix *createFix(u4 codeOff, BytecodeMap *bytecode) {
	BytecodeFix *fix = (BytecodeFix *) malloc(sizeof(BytecodeFix));
	if(fix == NULL) {
		return NULL;
	}

	fix->codeOff = codeOff;
	fix->bytecode = bytecode;
	fix->next = NULL;
	return fix;
}

#define ODEX_HEADER_SIZE	40

static jobject DumpClass(JNIEnv *env, jobject obj, jint cookie, jobject classLoader) {
	struct CookieMap *cookieMap = findCookie(cookie);
	if(cookieMap == NULL) {
		return NULL;
	}

	DvmDex* pDvmDex = cookieMap->pDvmDex;
	Object *loader =  (Object*) dvmDecodeIndirectRef(dvmThreadSelf(), classLoader);
	DexFile* pDexFile = pDvmDex->pDexFile;
	MemMapping * mem = &pDvmDex->memMap;

	u4 time = dvmGetRelativeTimeMsec();
	LOGE("GOT IT begin: %d ms", time);

	char classdef_tmp[100];
	memset(classdef_tmp, 0, 100);
	strcat(classdef_tmp, tempDir);
	strcat(classdef_tmp, "/classdef_XXXXXX");
	int classdef_fd;
	if((classdef_fd = mkstemp(classdef_tmp)) == -1) {
		LOGE("mkstemp classdef failed: %s", classdef_tmp);
		return NULL;
	}
	LOGE("mkstemp classdef successfully: %s", classdef_tmp);
	unlink(classdef_tmp);

	char extra_tmp[100];
	memset(extra_tmp, 0, 100);
	strcat(extra_tmp, tempDir);
	strcat(extra_tmp, "/extra_XXXXXX");
	int extra_fd;
	if((extra_fd = mkstemp(extra_tmp)) == -1) {
		LOGE("mkstemp extra failed: %s", extra_tmp);
		return NULL;
	}
	LOGE("mkstemp extra successfully: %s", extra_tmp);
	unlink(extra_tmp);

	uint32_t mask = 0x3ffff;
	char padding = 0;
	const char* header = "Landroid";
	unsigned int num_class_defs = pDexFile->pHeader->classDefsSize;
	uint32_t total_pointer = mem->length - uint32_t(pDexFile->baseAddr - (const u1*) mem->addr);
	uint32_t rec = total_pointer;

	while (total_pointer & 3) {
		total_pointer++;
	}

	int inc = total_pointer - rec;
	uint32_t start = pDexFile->pHeader->classDefsOff + sizeof(DexClassDef) * num_class_defs;
	uint32_t end = (uint32_t) ((const u1*) mem->addr + mem->length - pDexFile->baseAddr);

	struct BytecodeFix *fix = NULL;

	for (size_t i = 0; i < num_class_defs; i++) {
		bool need_extra = false;
		ClassObject * clazz = NULL;
		const u1* data = NULL;
		DexClassData* pData = NULL;
		bool pass = false;
		const DexClassDef *pClassDef = dexGetClassDef(pDvmDex->pDexFile, i);
		const char *descriptor = dexGetClassDescriptor(pDvmDex->pDexFile, pClassDef);

		if (!strncmp(header, descriptor, 8) || !pClassDef->classDataOff) {
			pass = true;
			goto classdef;
		}

		clazz = dvmDefineClass(pDvmDex, descriptor, loader);

		if (!clazz) {
			continue;
		}

		LOGE("GOT IT class: %s", descriptor);

		if (!dvmIsClassInitialized(clazz)) {
			if (dvmInitClass(clazz)) {
				LOGE("GOT IT init: %s", descriptor);
			}
		}

		if (pClassDef->classDataOff < start || pClassDef->classDataOff > end) {
			need_extra = true;
		}

		data = dexGetClassData(pDexFile, pClassDef);
		pData = ReadClassData(&data);

		if (!pData) {
			continue;
		}

		if (pData->directMethods) {
			for (uint32_t i = 0; i < pData->header.directMethodsSize; i++) {
				Method *method = &(clazz->directMethods[i]);
				uint32_t accessFlags = (method->accessFlags) & mask;

				LOGE("GOT IT direct method name %s->%s", descriptor, method->name);

				if (!method->insns || (accessFlags & ACC_NATIVE)) {
					if (pData->directMethods[i].codeOff) {
						need_extra = true;
						pData->directMethods[i].accessFlags = accessFlags;
						pData->directMethods[i].codeOff = 0;
					}
					continue;
				}

				u4 codeitem_off = u4((const u1*) method->insns - 16 - pDexFile->baseAddr);

				BytecodeMap *map;
				if(codeitem_off == pData->directMethods[i].codeOff &&
						(map = fixMethodBytecode(descriptor, method)) != NULL) {
					if(fix == NULL) {
						fix = createFix(codeitem_off, map);
					} else {
						BytecodeFix *next = fix;
						while(next->next != NULL) {
							next = next->next;
						}
						next->next = createFix(codeitem_off, map);
					}
				}

				if (accessFlags != pData->directMethods[i].accessFlags) {
					LOGE("GOT IT method accessFlags");
					need_extra = true;
					pData->directMethods[i].accessFlags = accessFlags;
				}

				if (codeitem_off != pData->directMethods[i].codeOff
						&& ((codeitem_off >= start && codeitem_off <= end) || codeitem_off == 0)) {
					LOGE("GOT IT method code");
					need_extra = true;
					pData->directMethods[i].codeOff = codeitem_off;
				}

				if (((codeitem_off < start || codeitem_off > end) && codeitem_off != 0)) {
					need_extra = true;
					pData->directMethods[i].codeOff = total_pointer;
					DexCode *code = (DexCode*) ((const u1*) method->insns - 16);
					uint8_t *item = (uint8_t *) code;
					int code_item_len = 0;
					if (code->triesSize) {
						const u1 * handler_data = dexGetCatchHandlerData(code);
						const u1** phandler = (const u1**) &handler_data;
						uint8_t * tail = codeitem_end(phandler);
						code_item_len = (int) (tail - item);
					} else {
						code_item_len = 16 + code->insnsSize * 2;
					}

					LOGE("GOT IT method code changed");

					write(extra_fd, item, code_item_len);
					total_pointer += code_item_len;
					while (total_pointer & 3) {
						write(extra_fd, &padding, 1);
						total_pointer++;
					}
				}
			}
		}

		if (pData->virtualMethods) {
			for (uint32_t i = 0; i < pData->header.virtualMethodsSize; i++) {
				Method *method = &(clazz->virtualMethods[i]);
				uint32_t accessFlags = (method->accessFlags) & mask;

				LOGE("GOT IT virtual method name %s->%s", descriptor, method->name);

				if (!method->insns || (accessFlags & ACC_NATIVE)) {
					if (pData->virtualMethods[i].codeOff) {
						need_extra = true;
						pData->virtualMethods[i].accessFlags = accessFlags;
						pData->virtualMethods[i].codeOff = 0;
					}
					continue;
				}

				u4 codeitem_off = u4((const u1 *) method->insns - 16 - pDexFile->baseAddr);

				BytecodeMap *map;
				if(codeitem_off == pData->directMethods[i].codeOff &&
						(map = fixMethodBytecode(descriptor, method)) != NULL) {
					if(fix == NULL) {
						fix = createFix(codeitem_off, map);
					} else {
						BytecodeFix *next = fix;
						while(next->next != NULL) {
							next = next->next;
						}
						next->next = createFix(codeitem_off, map);
					}
				}

				if (accessFlags != pData->virtualMethods[i].accessFlags) {
					LOGE("GOT IT method accessFlags");
					need_extra = true;
					pData->virtualMethods[i].accessFlags = accessFlags;
				}

				if (codeitem_off != pData->virtualMethods[i].codeOff && ((codeitem_off >= start && codeitem_off <= end)
								|| codeitem_off == 0)) {
					LOGE("GOT IT method code");
					need_extra = true;
					pData->virtualMethods[i].codeOff = codeitem_off;
				}

				if (((codeitem_off < start || codeitem_off > end) && codeitem_off != 0)) {
					need_extra = true;
					pData->virtualMethods[i].codeOff = total_pointer;
					DexCode *code = (DexCode*) ((const u1*) method->insns - 16);
					uint8_t *item = (uint8_t *) code;
					int code_item_len = 0;
					if (code->triesSize) {
						const u1 *handler_data = dexGetCatchHandlerData(code);
						const u1** phandler = (const u1**) &handler_data;
						uint8_t * tail = codeitem_end(phandler);
						code_item_len = (int) (tail - item);
					} else {
						code_item_len = 16 + code->insnsSize * 2;
					}

					LOGE("GOT IT method code changed");

					write(extra_fd, item, code_item_len);
					total_pointer += code_item_len;
					while (total_pointer & 3) {
						write(extra_fd, &padding, 1);
						total_pointer++;
					}
				}
			}
		}

		classdef:
		DexClassDef temp = *pClassDef;
		uint8_t *p = (uint8_t *) &temp;

		if (need_extra) {
			LOGE("GOT IT classdata before");
			int class_data_len = 0;
			uint8_t *out = EncodeClassData(pData, class_data_len);
			if (!out) {
				continue;
			}
			temp.classDataOff = total_pointer;
			write(extra_fd, out, class_data_len);
			total_pointer += class_data_len;
			while (total_pointer & 3) {
				write(extra_fd, &padding, 1);
				total_pointer++;
			}
			free(out);
			LOGE("GOT IT classdata written");
		} else {
			if (pData) {
				free(pData);
			}
		}

		if (pass) {
			temp.classDataOff = 0;
			temp.annotationsOff = 0;
		}

		LOGE("GOT IT classdef");
		write(classdef_fd, p, sizeof(DexClassDef));
	}

	int ret;
	char *addr = NULL;

	struct stat classdef_st;
	lseek(classdef_fd, 0, SEEK_SET);
	ret = fstat(classdef_fd, &classdef_st);
	if(ret == -1){
		LOGE("fstat classdef_fd failed: %d", classdef_fd);
		return NULL;
	}
	LOGE("classdef_fd stat: fd=%d, size=%llu", classdef_fd, classdef_st.st_size);

	struct stat extra_st;
	lseek(extra_fd, 0, SEEK_SET);
	ret = fstat(extra_fd, &extra_st);
	if(ret == -1){
		LOGE("fstat extra_fd failed: %d", extra_fd);
		return NULL;
	}
	LOGE("extra_fd stat: fd=%d, size=%llu", extra_fd, extra_st.st_size);

	int offset = 0;
	int total = cookieMap->header_len + classdef_st.st_size + cookieMap->data_len + extra_st.st_size;
	u1 *dex = (u1 *) malloc(total);

	memcpy(dex + offset, cookieMap->header, cookieMap->header_len);
	offset += cookieMap->header_len;

	addr = (char*) mmap(NULL, classdef_st.st_size, PROT_READ, MAP_PRIVATE, classdef_fd, 0);
	memcpy(dex + offset, addr, classdef_st.st_size);
	munmap(addr, classdef_st.st_size);
	offset += classdef_st.st_size;

	BytecodeFix *next = fix;
	while(next != NULL) {
		u4 codeOff = next->codeOff;
		BytecodeMap *bytecode = next->bytecode;
		u4 off = codeOff - offset;
		if(off >= 0 && off + 16 + ODEX_HEADER_SIZE < cookieMap->data_len &&
				bytecode != NULL && bytecode->insns != NULL) {
			u1 *data = (u1 *) cookieMap->data;
			LOGE("fixBytecode off=%u, len=%u, size=%u, data=%p, bytecode=%p", off, cookieMap->data_len, bytecode->insnsSize * 2, data, bytecode);
			memcpy(data + off + 16 + ODEX_HEADER_SIZE, bytecode->insns, bytecode->insnsSize * 2);
		}

		next = next->next;
	}

	memcpy(dex + offset, cookieMap->data, cookieMap->data_len);
	offset += cookieMap->data_len;

	addr = (char*) mmap(NULL, extra_st.st_size, PROT_READ, MAP_PRIVATE, extra_fd, 0);
	memcpy(dex + offset, addr, extra_st.st_size);
	munmap(addr, extra_st.st_size);
	offset += extra_st.st_size;

	close(classdef_fd);
	close(extra_fd);

	u4 last = dvmGetRelativeTimeMsec();
	LOGE("GOT IT end: %d ms, offset: %d ms", last, last - time);

	if(cookieMap->dex != NULL) {
		free(cookieMap->dex);
	}
	cookieMap->dex = dex;

	next = fix;
	while(next != NULL) {
		BytecodeFix *temp = next->next;
		free(next);
		next = temp;
	}

	return env->NewDirectByteBuffer(dex, total);
}

static pthread_mutex_t lock_collectBytecode;

struct BytecodeMap *allBytecodeMap = NULL;

static BytecodeMap* createBytecode(const Method *method, const DexCode *code) {
	struct BytecodeMap *bytecode = (struct BytecodeMap *) malloc(sizeof(BytecodeMap));
	if(bytecode == NULL) {
		return NULL;
	}

	bytecode->method = method;
	bytecode->insnsSize = code->insnsSize;
	bytecode->insns = (u2 *) malloc(code->insnsSize * 2);
	if(bytecode->insns == NULL) {
		return NULL;
	}
	memcpy(bytecode->insns, code->insns, code->insnsSize * 2);
	bytecode->next = NULL;

	DexStringCache stringCache;
	dexStringCacheInit(&stringCache);
	const char* methodDesc = dexProtoGetMethodDescriptor(&method->prototype, &stringCache);

	LOGE("dvmReportInvoke tid=%d, methodId=%p, odexHeader=%p, insns=%p, insnsSize=%u, %s->%s%s, allBytecodeMap=%p", gettid(), method, method->clazz->pDvmDex->pDexFile->pOptHeader, code, code->insnsSize, method->clazz->descriptor, method->name, methodDesc, allBytecodeMap);

	return bytecode;
}

struct BytecodeMap* fixMethodBytecode(const char *descriptor, const Method *method) {
	struct BytecodeMap *map = allBytecodeMap;
	while(map != NULL) {
		if(method == map->method &&
				memcmp((void *) method->insns, map->insns, map->insnsSize * 2) != 0) {
			memcpy((void *) method->insns, map->insns, map->insnsSize * 2);
			LOGE("fixMethodBytecode: %s->%s", descriptor, method->name);
			return map;
		}

		map = map->next;
	}
	return NULL;
}

void collectBytecode(const Method* m) {
	const DexCode *code = dvmGetMethodCode(m);
	if(code == NULL) {
		return;
	}

	pthread_mutex_lock(&lock_collectBytecode);
	if(allBytecodeMap == NULL) {
		allBytecodeMap = createBytecode(m, code);
	} else {
		struct BytecodeMap *next = allBytecodeMap;
		if(next->method == m) {
			goto finally;
		}
		while(next->next != NULL) {
			if(next->method == m) {
				goto finally;
			}

			next = next->next;
		}
		next->next = createBytecode(m, code);
	}

	finally:
	pthread_mutex_unlock(&lock_collectBytecode);
}
