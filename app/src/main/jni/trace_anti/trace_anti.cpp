#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <sys/inotify.h>
#include <string.h>
#include "solist.h"
#include "com_fuzhu8_inspector_jni_TraceAnti.h"
#include "arm.h"

#include "inspector.h"
#include "trace_anti.h"

static Object* new_dvmInvokeMethod(Object* obj, const Method* m, ArrayObject* argList, ArrayObject* params, ClassObject* returnType, bool noAccessCheck) {
	// LOGD("dvmInvokeMethod methodId=%p, descriptor=%s, name=%s, shorty=%s", m, m->clazz->descriptor, m->name, m->shorty);
	return old_dvmInvokeMethod(obj, m, argList, params, returnType, noAccessCheck);
}

static void new_dvmInterpret(Thread* self, const Method* m, JValue* pResult) {
	// LOGD("dvmInterpret methodId=%p, descriptor=%s, name=%s, shorty=%s", m, m->clazz->descriptor, m->name, m->shorty);
	old_dvmInterpret(self, m, pResult);
}

static jboolean enable_collect_bytecode = JNI_FALSE;
static const char *bytecodeFilter = NULL;

static pthread_mutex_t lock_dvmReportInvoke;
static pthread_mutex_t lock_dvmReportExceptionThrow;
static pthread_mutex_t lock_dvmReportPreNativeInvoke;
static pthread_mutex_t lock_dvmReportPostNativeInvoke;
static pthread_mutex_t lock_dvmReportReturn;

void collectBytecode(const Method* m);

static void new_dvmReportReturn(Thread* self) {
	if(enable_collect_bytecode) {
		return;
	}

	pthread_mutex_lock(&lock_dvmReportReturn);
	old_dvmReportReturn(self);
	pthread_mutex_unlock(&lock_dvmReportReturn);
}

static void new_dvmReportPostNativeInvoke(const Method* methodToCall, Thread* self, u4* fp) {
	if(enable_collect_bytecode) {
		return;
	}

	pthread_mutex_lock(&lock_dvmReportPostNativeInvoke);
	old_dvmReportPostNativeInvoke(methodToCall, self, fp);
	pthread_mutex_unlock(&lock_dvmReportPostNativeInvoke);
}

static void new_dvmReportPreNativeInvoke(const Method* methodToCall, Thread* self, u4* fp) {
	if(enable_collect_bytecode) {
		return;
	}

	pthread_mutex_lock(&lock_dvmReportPreNativeInvoke);
	old_dvmReportPreNativeInvoke(methodToCall, self, fp);
	pthread_mutex_unlock(&lock_dvmReportPreNativeInvoke);
}

static void new_dvmReportExceptionThrow(Thread* self, Object* exception) {
	if(enable_collect_bytecode) {
		return;
	}

	pthread_mutex_lock(&lock_dvmReportExceptionThrow);
	old_dvmReportExceptionThrow(self, exception);
	pthread_mutex_unlock(&lock_dvmReportExceptionThrow);
}

static void new_dvmReportInvoke(Thread* self, const Method* m) {
	if(strstr(m->clazz->descriptor, "Ljava/") == m->clazz->descriptor ||
			strstr(m->clazz->descriptor, "Ljavax/") == m->clazz->descriptor ||
			strstr(m->clazz->descriptor, "Landroid/") == m->clazz->descriptor ||
			strstr(m->clazz->descriptor, "Lcom/android/") == m->clazz->descriptor ||
			strstr(m->clazz->descriptor, "Lsun/") == m->clazz->descriptor ||
			strstr(m->clazz->descriptor, "Lcom/sun/") == m->clazz->descriptor ||
			strstr(m->clazz->descriptor, "Llibcore/") == m->clazz->descriptor ||
			strstr(m->clazz->descriptor, "Ldalvik/") == m->clazz->descriptor ||
			strstr(m->clazz->descriptor, "Lorg/apache/") == m->clazz->descriptor ||
			strstr(m->clazz->descriptor, "Lde/robv/") == m->clazz->descriptor ||
			strstr(m->clazz->descriptor, "miui") != NULL ||
			strstr(m->clazz->descriptor, "banny") != NULL ||
			strstr(m->clazz->descriptor, "fuzhu8") != NULL) {
		if(enable_collect_bytecode) {
			return;
		}

		pthread_mutex_lock(&lock_dvmReportInvoke);
		old_dvmReportInvoke(self, m);
		pthread_mutex_unlock(&lock_dvmReportInvoke);
		return;
	}

	if(enable_collect_bytecode) {
		if(dvmIsNativeMethod(m) ||
			dvmIsAbstractMethod(m)) {
			return;
		}

		if(bytecodeFilter == NULL) {
			collectBytecode(m);
			return;
		}

		if(strstr(m->clazz->descriptor, bytecodeFilter) != NULL) {
			collectBytecode(m);
		}
		return;
	}

	pthread_mutex_lock(&lock_dvmReportInvoke);
	old_dvmReportInvoke(self, m);
	pthread_mutex_unlock(&lock_dvmReportInvoke);
}

static int new_inotify_add_watch(int fd, const char *pathname, uint32_t mask) {
	if (strstr(pathname, "mem") != NULL) {
		LOGD("[*] inotify_add_watch --> patch mem");
		return old_inotify_add_watch(fd, pathname, 0x00000200); //mem永远不会被删除，改为0也可以
	} else if (strstr(pathname, "task") != NULL) { //监控打开和读事件，防获取反调试线程信息
		LOGD("[*] inotify_add_watch --> patch task");
		return old_inotify_add_watch(fd, pathname, 0x00000200);
	} else if (strstr(pathname, "pagemap") != NULL) {
		LOGD("[*] inotify_add_watch --> patch pagemap");
		return old_inotify_add_watch(fd, pathname, 0x00000200);
	}

	unsigned lr;
	GETLR(lr);
	LOGD("[*] Traced-inotify_add_watch Call function: %p", (void *)lr);

	LOGD("[*] Traced-inotify_add_watch --> %s, 0x%X", pathname, mask);
	return old_inotify_add_watch(fd, pathname, mask);
}

static pthread_mutex_t lock_dexFileParse;

static DexFile* new_dexFileParse(const u1 *data, size_t length, int flags) {
	pthread_mutex_lock(&lock_dexFileParse);
	DexFile *ret = old_dexFileParse(data, length, flags);
	pthread_mutex_unlock(&lock_dexFileParse);
	if(ret) {
		collectDexFile((void *)data, length, "dexFileParse");
	}
	return ret;
}

static int new_munmap(void *start, size_t length) {
	unsigned lr;
	GETLR(lr);
	LOGD("[*] Traced-munmap Call function: %p, start=%p, length=%d", (void *)lr, start, length);
	return old_munmap(start, length);
}

static int new_pthread_create(pthread_t *tidp, const pthread_attr_t *attr, void* (*start_rtn)(void *), void *arg) {
	unsigned lr;
	GETLR(lr);
	/*LOGD("[*] Patched-pthread_create Call function: %p, attr=%p, start_rtn=%p, arg=%p", (void *)lr, attr, start_rtn, arg);
	return 0;*/
	LOGD("[*] Traced-pthread_create Call function: %p, attr=%p, start_rtn=%p, arg=%p", (void *)lr, attr, start_rtn, arg);
	return old_pthread_create(tidp, attr, start_rtn, arg);
}

static int new_sigaction(int signum, const struct sigaction *act, struct sigaction *oldact) {
	unsigned lr;
	GETLR(lr);
	LOGD("[*] Traced-sigaction Call function: %p, signum=%d, act=%p, oldact=%p", (void *)lr, signum, act, oldact);
	return old_sigaction(signum, act, oldact);
}

static int new_stat(const char *path, struct stat *buf){
	unsigned lr;
	GETLR(lr);
	LOGD("[*] Traced-stat Call function: %p, path=%s, buf=%p", (void *)lr, path, buf);
	return old_stat(path, buf);
}

static int new_fstat(int filedes, struct stat *buf){
	unsigned lr;
	GETLR(lr);
	LOGD("[*] Traced-fstat Call function: %p, filedes=%d, buf=%p", (void *)lr, filedes, buf);
	return old_fstat(filedes, buf);
}

static int new_lstat(const char *path, struct stat *buf){
	unsigned lr;
	GETLR(lr);
	LOGD("[*] Traced-lstat Call function: %p, path=%s, buf=%p", (void *)lr, path, buf);
	return old_lstat(path, buf);
}

static pid_t new_fork() {
	unsigned lr;
	GETLR(lr);

	pid_t pid = old_fork();
	LOGD("[*] Traced-fork Call function: %p, pid=%d", (void *)lr, pid);
	return pid;
}

static pthread_mutex_t lock__fork;

static pid_t new__fork() {
	unsigned lr;
	GETLR(lr);

	// LOGD("[*] Patched-__fork Call function: %p", (void *)lr);
	// return getpid();

	pthread_mutex_lock(&lock__fork);
	pid_t pid = old__fork();
	pthread_mutex_unlock(&lock__fork);
	LOGD("[*] Traced-__fork Call function: %p, pid=%d", (void *)lr, pid);
	return pid;
}

static pthread_mutex_t lock__exit;

static void new__exit(int status) {
	unsigned lr;
	GETLR(lr);

	LOGD("[*] Traced-_exit Call function: %p, status=%d", (void *)lr, status);
	pthread_mutex_lock(&lock__exit);
	old__exit(status);
	pthread_mutex_unlock(&lock__exit);
}

static int new_raise(int sig) {
	unsigned lr;
	GETLR(lr);

	LOGD("[*] Traced-raise Call function: %p, sig=%d", (void *)lr, sig);
	return old_raise(sig);
}

static pthread_mutex_t lock_dlopen;

typedef void* (*proto_ssl_create_cipher_list_v101)(void *ssl_method, void *cipher_list, void *cipher_list_by_id, const char *rule_str);
typedef void* (*proto_ssl_create_cipher_list)(void *ssl_method, void *cipher_list, void *cipher_list_by_id, const char *rule_str, void *c);
typedef int (*proto_RAND_bytes)(unsigned char *buf, int num);

static int ssl_mask = 0;

static proto_ssl_create_cipher_list old_ssl_create_cipher_list1 = NULL;
static proto_ssl_create_cipher_list old_ssl_create_cipher_list2 = NULL;
static proto_ssl_create_cipher_list old_ssl_create_cipher_list3 = NULL;

static proto_ssl_create_cipher_list_v101 old_ssl_create_cipher_list1_v101 = NULL;
static proto_ssl_create_cipher_list_v101 old_ssl_create_cipher_list2_v101 = NULL;
static proto_ssl_create_cipher_list_v101 old_ssl_create_cipher_list3_v101 = NULL;

static pthread_mutex_t lock_ssl_create_cipher_list1;
static pthread_mutex_t lock_ssl_create_cipher_list2;
static pthread_mutex_t lock_ssl_create_cipher_list3;

static proto_RAND_bytes old_RAND_bytes1 = NULL;
static proto_RAND_bytes old_RAND_bytes2 = NULL;
static proto_RAND_bytes old_RAND_bytes3 = NULL;

static pthread_mutex_t lock_RAND_bytes1;
static pthread_mutex_t lock_RAND_bytes2;
static pthread_mutex_t lock_RAND_bytes3;

#define SSL_DEFAULT_CIPHER_LIST        "ALL:!aNULL:!eNULL:!SSLv2:!aECDH:!kEECDH:!kEDH:!kDHr:!kDHd:!kECDHr:!kPSK:!kKRB5:!kSRP:!EXPORT:!AESGCM"
#define SSL_RSA_PRE_MASTER_SIZE        46

static jint settings_patch_ssl = 0;

static void *new_ssl_create_cipher_list1_v101(void *ssl_method, void *cipher_list, void *cipher_list_by_id, const char *rule_str) {
    unsigned lr;
    GETLR(lr);

    pthread_mutex_lock(&lock_ssl_create_cipher_list1);
    void *ret = old_ssl_create_cipher_list1_v101(ssl_method, cipher_list, cipher_list_by_id, SSL_DEFAULT_CIPHER_LIST);
    pthread_mutex_unlock(&lock_ssl_create_cipher_list1);
    LOGD("[*] Patched-ssl_create_cipher_list1_v101 Call function: %p, rule_str=%s", (void *)lr, rule_str);
    return ret;
}
static void *new_ssl_create_cipher_list2_v101(void *ssl_method, void *cipher_list, void *cipher_list_by_id, const char *rule_str) {
    unsigned lr;
    GETLR(lr);

    pthread_mutex_lock(&lock_ssl_create_cipher_list2);
    void *ret = old_ssl_create_cipher_list2_v101(ssl_method, cipher_list, cipher_list_by_id, SSL_DEFAULT_CIPHER_LIST);
    pthread_mutex_unlock(&lock_ssl_create_cipher_list2);
    LOGD("[*] Patched-ssl_create_cipher_list2_v101 Call function: %p, rule_str=%s", (void *)lr, rule_str);
    return ret;
}
static void *new_ssl_create_cipher_list3_v101(void *ssl_method, void *cipher_list, void *cipher_list_by_id, const char *rule_str) {
    unsigned lr;
    GETLR(lr);

    pthread_mutex_lock(&lock_ssl_create_cipher_list3);
    void *ret = old_ssl_create_cipher_list3_v101(ssl_method, cipher_list, cipher_list_by_id, SSL_DEFAULT_CIPHER_LIST);
    pthread_mutex_unlock(&lock_ssl_create_cipher_list3);
    LOGD("[*] Patched-ssl_create_cipher_list3_v101 Call function: %p, rule_str=%s", (void *)lr, rule_str);
    return ret;
}

static void *new_ssl_create_cipher_list1(void *ssl_method, void *cipher_list, void *cipher_list_by_id, const char *rule_str, void *c) {
	unsigned lr;
	GETLR(lr);

	pthread_mutex_lock(&lock_ssl_create_cipher_list1);
	void *ret = old_ssl_create_cipher_list1(ssl_method, cipher_list, cipher_list_by_id, SSL_DEFAULT_CIPHER_LIST, c);
	pthread_mutex_unlock(&lock_ssl_create_cipher_list1);
	LOGD("[*] Patched-ssl_create_cipher_list1 Call function: %p, rule_str=%s", (void *)lr, rule_str);
	return ret;
}
static void *new_ssl_create_cipher_list2(void *ssl_method, void *cipher_list, void *cipher_list_by_id, const char *rule_str, void *c) {
	unsigned lr;
	GETLR(lr);

	pthread_mutex_lock(&lock_ssl_create_cipher_list2);
	void *ret = old_ssl_create_cipher_list2(ssl_method, cipher_list, cipher_list_by_id, SSL_DEFAULT_CIPHER_LIST, c);
	pthread_mutex_unlock(&lock_ssl_create_cipher_list2);
	LOGD("[*] Patched-ssl_create_cipher_list2 Call function: %p, rule_str=%s", (void *)lr, rule_str);
	return ret;
}
static void *new_ssl_create_cipher_list3(void *ssl_method, void *cipher_list, void *cipher_list_by_id, const char *rule_str, void *c) {
	unsigned lr;
	GETLR(lr);

	pthread_mutex_lock(&lock_ssl_create_cipher_list3);
	void *ret = old_ssl_create_cipher_list3(ssl_method, cipher_list, cipher_list_by_id, SSL_DEFAULT_CIPHER_LIST, c);
	pthread_mutex_unlock(&lock_ssl_create_cipher_list3);
	LOGD("[*] Patched-ssl_create_cipher_list3 Call function: %p, rule_str=%s", (void *)lr, rule_str);
	return ret;
}

static int new_RAND_bytes1(unsigned char *buf, int num) {
	unsigned lr;
	GETLR(lr);

	if(num == SSL_RSA_PRE_MASTER_SIZE) { // fake rsa pre master
		memset(buf, 0, SSL_RSA_PRE_MASTER_SIZE);
		LOGD("[*] Patched-RAND_bytes1 Call function: %p, buf=%p, num=%d", (void *)lr, (void *) buf, num);
		return 1;
	}

	pthread_mutex_lock(&lock_RAND_bytes1);
	int ret = old_RAND_bytes1(buf, num);
	pthread_mutex_unlock(&lock_RAND_bytes1);
	LOGD("[*] Traced-RAND_bytes1 Call function: %p, buf=%p, num=%d, ret=%d", (void *)lr, (void *) buf, num, ret);
	return ret;
}
static int new_RAND_bytes2(unsigned char *buf, int num) {
	unsigned lr;
	GETLR(lr);

	if(num == SSL_RSA_PRE_MASTER_SIZE) { // fake rsa pre master
		memset(buf, 0, SSL_RSA_PRE_MASTER_SIZE);
		LOGD("[*] Patched-RAND_bytes2 Call function: %p, buf=%p, num=%d", (void *)lr, (void *) buf, num);
		return 1;
	}

	pthread_mutex_lock(&lock_RAND_bytes2);
	int ret = old_RAND_bytes2(buf, num);
	pthread_mutex_unlock(&lock_RAND_bytes2);
	LOGD("[*] Traced-RAND_bytes2 Call function: %p, buf=%p, num=%d, ret=%d", (void *)lr, (void *) buf, num, ret);
	return ret;
}
static int new_RAND_bytes3(unsigned char *buf, int num) {
	unsigned lr;
	GETLR(lr);

	if(num == SSL_RSA_PRE_MASTER_SIZE) { // fake rsa pre master
		memset(buf, 0, SSL_RSA_PRE_MASTER_SIZE);
		LOGD("[*] Patched-RAND_bytes3 Call function: %p, buf=%p, num=%d", (void *)lr, (void *) buf, num);
		return 1;
	}

	pthread_mutex_lock(&lock_RAND_bytes3);
	int ret = old_RAND_bytes3(buf, num);
	pthread_mutex_unlock(&lock_RAND_bytes3);
	LOGD("[*] Traced-RAND_bytes3 Call function: %p, buf=%p, num=%d, ret=%d", (void *)lr, (void *) buf, num, ret);
	return ret;
}

static void fake_openssl(const char *pathname, void *ssl_create_cipher_list, void *RAND_bytes, const char *version) {
	const char *v101 = strstr(version, "1.0.1"); // compare openssl version
	LOGD("[*] Traced-dlopen openssl: path=%s, ssl_create_cipher_list=%p, RAND_bytes=%p, version=%s", pathname, ssl_create_cipher_list, RAND_bytes, version);
	if((ssl_mask & 0x1) == 0) {
        if ((settings_patch_ssl & 0x1) != 0) {
            inline_mshook(RAND_bytes, (void*)new_RAND_bytes1, "RAND_bytes1", (void **)&old_RAND_bytes1);
            if (v101) {
                inline_mshook(ssl_create_cipher_list, (void*)new_ssl_create_cipher_list1_v101, "ssl_create_cipher_list1_v101", (void **)&old_ssl_create_cipher_list1_v101);
            } else {
                inline_mshook(ssl_create_cipher_list, (void*)new_ssl_create_cipher_list1, "ssl_create_cipher_list1", (void **)&old_ssl_create_cipher_list1);
            }
        }
		ssl_mask |= 0x1;
	} else if((ssl_mask & 0x2) == 0) {
        if ((settings_patch_ssl & 0x2) != 0) {
            inline_mshook(RAND_bytes, (void*)new_RAND_bytes2, "RAND_bytes2", (void **)&old_RAND_bytes2);
            if (v101) {
                inline_mshook(ssl_create_cipher_list, (void*)new_ssl_create_cipher_list2_v101, "ssl_create_cipher_list2_v101", (void **)&old_ssl_create_cipher_list2_v101);
            } else {
                inline_mshook(ssl_create_cipher_list, (void*)new_ssl_create_cipher_list2, "ssl_create_cipher_list2", (void **)&old_ssl_create_cipher_list2);
            }
        }
		ssl_mask |= 0x2;
	} else if((ssl_mask & 0x4) == 0) {
        if ((settings_patch_ssl & 0x4) != 0) {
            inline_mshook(RAND_bytes, (void*)new_RAND_bytes3, "RAND_bytes3", (void **)&old_RAND_bytes3);
            if (v101) {
                inline_mshook(ssl_create_cipher_list, (void*)new_ssl_create_cipher_list3_v101, "ssl_create_cipher_list3_v101", (void **)&old_ssl_create_cipher_list3_v101);
            } else {
                inline_mshook(ssl_create_cipher_list, (void*)new_ssl_create_cipher_list3, "ssl_create_cipher_list3", (void **)&old_ssl_create_cipher_list3);
            }
        }
		ssl_mask |= 0x4;
	} else {
		LOGD("[E] Traced-dlopen ssl: path=%s, ssl_create_cipher_list=%p, RAND_bytes=%p failed.", pathname, ssl_create_cipher_list, RAND_bytes);
	}
}

#define SSLEAY_VERSION    0
#define SSLEAY_OPTIONS    1
#define SSLEAY_CFLAGS     2
#define SSLEAY_BUILT_ON   3
#define SSLEAY_PLATFORM   4
#define SSLEAY_DIR        5

typedef const char *(*proto_SSLeay_version)(int t);

static void* ssl_dlopen(const char *pathname, int mode) {
	pthread_mutex_lock(&lock_dlopen);
	void *handler = old_dlopen(pathname, mode);
	pthread_mutex_unlock(&lock_dlopen);

	if(handler) {
		void *ssl_create_cipher_list = dlsym(handler, "ssl_create_cipher_list");
		void *RAND_bytes = dlsym(handler, "RAND_bytes");
        void *SSLeay_version_sym = dlsym(handler, "SSLeay_version");

		if(ssl_create_cipher_list && RAND_bytes && SSLeay_version_sym) {
            proto_SSLeay_version SSLeay_version = (proto_SSLeay_version) SSLeay_version_sym;
            const char *version = SSLeay_version(SSLEAY_VERSION);

            fake_openssl(pathname, ssl_create_cipher_list, RAND_bytes, version);

            const char *options = SSLeay_version(SSLEAY_OPTIONS);
            const char *cflags = SSLeay_version(SSLEAY_CFLAGS);
            const char *built_on = SSLeay_version(SSLEAY_BUILT_ON);
            const char *platform = SSLeay_version(SSLEAY_PLATFORM);
            const char *dir = SSLeay_version(SSLEAY_DIR);

            LOGD("[*] %s, options=%s, cflags=%s, built_on=%s, platform=%s, dir=%s, pathname=%s", version, options, cflags, built_on, platform, dir, pathname);
		} else if (ssl_create_cipher_list ||
                   RAND_bytes ||
                   SSLeay_version_sym) {
            LOGD("[*] Traced-ssl_dlopen: path=%s, ssl_create_cipher_list=%p, RAND_bytes=%p, SSLeay_version=%p", pathname, ssl_create_cipher_list, RAND_bytes, SSLeay_version_sym);
        }
	}

	return handler;
}

static void* new_dlopen(const char *pathname, int mode) {
	unsigned lr;
	GETLR(lr);

	if(strstr(pathname, "htc") != NULL) {
		char re_path[256];
		if (myDataDir != NULL) {
			sprintf(re_path, "%s%s", myDataDir, pathname);
		} else {
			sprintf(re_path, "/data/local/tmp%s", pathname);
		}

		FILE *src = old_fopen(pathname, "r");
		if (src == NULL) {
			LOGD("[E] dlopen re-path [%s]failed for src file", pathname);
			pthread_mutex_lock(&lock_dlopen);
			void *ret = old_dlopen(pathname, mode);
			pthread_mutex_unlock(&lock_dlopen);
			return ret;
		}
		FILE *dest = createFile(re_path);
		if (dest == NULL) {
			LOGD("[E] dlopen re-path [%s]failed for create dest file", pathname);
			pthread_mutex_lock(&lock_dlopen);
			void *ret = old_dlopen(pathname, mode);
			pthread_mutex_unlock(&lock_dlopen);
			return ret;
		}

		LOGD("[*] Traced-dlopen Call function: %p, src=%s, dest=%s", (void *)lr, pathname, re_path);
		int c;
		while ((c = fgetc(src)) != EOF) {
			fputc(c, dest);
		}

		fclose(src);
		fclose(dest);
	}

	void *handler = ssl_dlopen(pathname, mode);
	LOGD("[*] Traced-dlopen Call function: %p, path=%s, mode=%d, handler=%p", (void *)lr, pathname, mode, handler);
	return handler;
}

static void* new__mmap2(void* start, size_t length, int prot, int flags, int fd, size_t offset) {
	unsigned lr;
	GETLR(lr);
	void *base = old__mmap2(start, length, prot, flags, fd, offset);
	LOGD("[*] Traced-__mmap2 Call function: %p, Ret address: %p, start=%p, length=%d, prot=0x%X", (void *)lr, base, start, length, prot);
	return base;
}

static void new_dvmDbgActive() {
	LOGD("[*] Traced mock dvmDbgActive");
}

static void anti_hook(int r0, int r1, int r2, int r3) {
	unsigned lr;
	GETLR(lr);

	LOGD("[*] Traced anti_hook Call function: %p, R0=0x%X, R1=0x%X, R2=0x%X, R3=0x%X", (void *)lr, r0, r1, r2, r3);
}

typedef int (*proto__open)(const char *pathname, int native_flags, int mode);
static proto__open old__open = NULL;

static int new__open(const char *path, int flags, int rev) {
	unsigned lr;
	GETLR(lr);
	LOGD("[*] Traced-__open Call function: %p, path=%s", (void *)lr, path);
	return old__open(path, flags, rev);
}

static int new_open(const char *pathname, int oflag, mode_t mode) {
	unsigned lr;
	GETLR(lr);
	LOGD("[*] Traced-open Call function: %p, path=%s", (void *)lr, pathname);
	return old_open(pathname, oflag, mode);
}

extern "C" void*  __mmap2(void*, size_t, int, int, int, size_t);

static pthread_mutex_t lock_dlsym;

static void* new_dlsym(void* handle, const char* symbol) {
	unsigned lr;
	GETLR(lr);

	pthread_mutex_lock(&lock_dlsym);
	void *ret = old_dlsym(handle, symbol);
	pthread_mutex_unlock(&lock_dlsym);

	/*if(strstr(symbol, "ptrace") != NULL && ret != NULL) {
		LOGD("[*] Patched-dlsym Call function: %p, handle=%p, symbol=%s, address=%p", (void *)lr, handle, symbol, anti_hook);
		return (void *)anti_hook;
	}*/

	if(strstr(symbol, "dvmDbgActive") != NULL && ret != NULL) {
		LOGD("[*] Patched-dlsym Call function: %p, handle=%p, symbol=%s", (void *)lr, handle, symbol);
		return (void *)new_dvmDbgActive;
	}

	/*if(strstr(symbol, "__open_fake") != NULL && ret != NULL && strstr(symbol, "__openat") == NULL && old__open == NULL) {
		inline_hook((void *)ret, (void *)new__open, "__open", (void **)&old__open);
		LOGD("[*] Patched-__open Call function: %p, handle=%p, symbol=%s", (void *)lr, handle, symbol);
		return ret;
	}*/

	/*if(strstr(symbol, "munmap") != NULL && ret == old_munmap) {
		LOGD("[*] Patched-munmap Call function: %p, handle=%p, symbol=%s", (void *)lr, handle, symbol);
		return (void *)new_munmap;
	}*/

	if(strstr(symbol, "__mmap2") != NULL && ret != NULL && lr % 4096 != 0xDC3) {
		old__mmap2 = (proto__mmap2) ret;
		LOGD("[*] Patched-__mmap2 Call function: %p, handle=%p, symbol=%s, test=0x%X", (void *)lr, handle, symbol, lr % 4096);
		return (void *)new__mmap2;
	}

	/*if(strstr(symbol, "fork") != NULL && ret != NULL && old_fork != NULL) {
		LOGD("[*] Patched-fork Call function: %p, handle=%p, symbol=%s", (void *)lr, handle, symbol);
		return (void *)new_fork;
	}*/

	if(ret != NULL) {
		// inspect(ret, 16, "[*] Traced-dlsym Call function: %p, handle=%p, symbol=%s, ret=%p", (void *)lr, handle, symbol, ret);
	}
	LOGD("[*] Traced-dlsym Call function: %p, handle=%p, symbol=%s, ret=%p", (void *)lr, handle, symbol, ret);
	return ret;
}

static void* new_mmap(void* start,size_t length,int prot,int flags,int fd,off_t offset) {
	unsigned lr;
	void* base = NULL;
	
	GETLR(lr);
	base = old_mmap(start, length, prot, flags, fd, offset);
	if((flags & MAP_ANONYMOUS) == 0){	//文件映射
		char file_name[256];
		char buf[256];
		memset(buf, 0, 256);
		sprintf(file_name, "/proc/self/fd/%d", fd);
		if(readlink(file_name, buf, 256) < 0){
			LOGD("[E] Traced-mmap --> readlink %s error", file_name);
			goto _done;
		}
		LOGD("[*] Traced-mmap --> [file] start = %p, length = %d, filename = %s, offset = %u",
			start, length, buf, (unsigned) offset);
	} else {	//内存映射
		LOGD("[*] Traced-mmap --> [mem] start = %p, length = %d",
			start, length);
	}
	LOGD("[*] Traced-mmap Call function: %p, Ret address: %p", (void *)lr, base);
_done:
	return base;
}

static FILE* new_fopen(const char *path, const char * mode) {
	unsigned lr;
	GETLR(lr);

	// return old_fopen(path, mode);

	/*if (strstr(path, "status") != NULL) {
		if (strstr(path, "task") != NULL) {
			return mapFile(path, mode, "[*] Traced-anti-task/status: %s with mode %s", false);
		} else {
			return mapFile(path, mode, "[*] Traced-anti-status: %s with mode %s", false);
		}
	} else if (strstr(path, "wchan") != NULL) {
		return mapFile(path, mode, "[*] Traced-anti-wchan: %s with mode %s", false);
	} else if (strstr(path, "stat") != NULL) {
		return mapFile(path, mode, "[*] Traced-anti-stat: %s with mode %s", false);
	} else if (strstr(path, "maps") != NULL) {
		return mapFile(path, mode, "[*] Traced-anti-maps: %s with mode %s", false);
	} else {
		LOGD("[*] Traced-fopen Call function: %p, %s", (void *)lr, path);
	}*/
	FILE *ret = old_fopen(path, mode);
	LOGD("[*] Traced-fopen Call function: %p, %s, %p", (void *)lr, path, ret);
	return ret;
}

static unsigned int new_strlen(char *s) {
    unsigned lr;
    GETLR(lr);

    pthread_mutex_lock(&lock_strlen);
    unsigned int len = old_strlen(s);
    pthread_mutex_unlock(&lock_strlen);
    LOGD("[*] Traced-strlen Call function: %p, %s, %d", (void *)lr, s, len);
    return len;
}

static char *new_strncpy(char *dest,char *src,size_t n) {
    unsigned lr;
    GETLR(lr);

    pthread_mutex_lock(&lock_strncpy);
    char *ret = old_strncpy(dest, src, n);
    pthread_mutex_unlock(&lock_strncpy);
    LOGD("[*] Traced-strncpy Call function: %p, dest=%s, src=%s, ret=%s, n=%d", (void *)lr, dest, src, ret, n);
    return ret;
}

static int new_strncmp(const char * str1, const char * str2, size_t n) {
    unsigned lr;
    GETLR(lr);

    pthread_mutex_lock(&lock_strncmp);
    int ret = old_strncmp(str1, str2, n);
    pthread_mutex_unlock(&lock_strncmp);
    LOGD("[*] Traced-strncmp Call function: %p, str1=%s, str2=%s, ret=%d, n=%d", (void *)lr, str1, str2, ret, n);
    return ret;
}

typedef struct {
	jboolean anti_thread_create;
	jboolean trace_file;
	jboolean trace_sys_call;
	jboolean trace_trace;
    jboolean trace_string;
} patch_settings;

static void patch_lib(patch_settings settings){
	HookStruct libc;
	strcpy(libc.SOName, "libc.so");

	HookStruct libdl;
	strcpy(libdl.SOName, "libdl.so");

    if (settings.trace_string) {
        // hook(&libc, (void *) new_strlen, "strlen", (void **)&old_strlen);
        // hook(&libc, (void *) new_strncpy, "strncpy", (void **)&old_strncpy);
        // hook(&libc, (void *) new_strncmp, "strncmp", (void **)&old_strncmp);

		// inline_hook((void *)dexFileParse, (void *)new_dexFileParse, "dexFileParse", (void **)&old_dexFileParse);
		// inline_mshook((void *)dexFileParse, (void *)new_dexFileParse, "dexFileParse", (void **)&old_dexFileParse);
    }

	if(settings.trace_file) {
		// hook(&libc, (void*)new_fopen, "fopen", (void **)&old_fopen);
		mshook("libc.so", "fopen", (void*)new_fopen, (void **)&old_fopen);
		// inline_hook((void *)fopen, (void*)new_fopen, "fopen", (void **)&old_fopen);

		hook(&libc, (void*)new_stat, "stat", (void **)&old_stat);
		hook(&libc, (void*)new_fstat, "fstat", (void **)&old_fstat);
		hook(&libc, (void*)new_lstat, "lstat", (void **)&old_lstat);

		hook(&libc, (void*)new_inotify_add_watch, "inotify_add_watch", (void **)&old_inotify_add_watch);
	}

	if(settings.trace_sys_call) {
		hook(&libc, (void*)new_exit, "exit", (void **)&old_exit);
		hook(&libc, (void*)new_kill, "kill", (void **)&old_kill);
		hook(&libc, (void*)new_abort, "abort", (void **)&old_abort);
		hook(&libc, (void*)new_raise, "raise", (void **)&old_raise);
		// hook(&libc, (void*)new_sigaction, "sigaction", (void **)&old_sigaction);
		// hook(&libc, (void*)new_signal, "bsd_signal", (void **)&old_signal);

		inline_hook((void *)_exit, (void*)new__exit, "_exit", (void **)&old__exit);

		void *lib = dlopen("libc.so", RTLD_NOW | RTLD_GLOBAL);
		void *__fork = lib == NULL ? NULL : dlsym(lib, "__fork");
		if(__fork == NULL) {
			hook(&libc, (void*)new_fork, "fork", (void **)&old_fork);
		} else {
			inline_hook(__fork, (void*)new__fork, "__fork", (void **)&old__fork);
			// hook(&libc, (void*)new__fork, "__fork", (void **)&old__fork);
		}

		inline_hook((void*)execve, (void*)new_execve, "execve", (void **)&old_execve);
		// hook(&libc, (void*)new_open, "open", (void **)&old_open);
	}

	if(settings.anti_thread_create) {
		hook(&libc, (void *)new_pthread_create, "pthread_create", (void **)&old_pthread_create);
	}

	if(settings.trace_trace) {
		if(!settings.trace_sys_call) {
			hook(&libc, (void*)new_fork, "fork", (void **)&old_fork);
		}

		inline_hook((void *)dlopen, (void *)new_dlopen, "dlopen", (void **)&old_dlopen);
		LOGD("[*] Hooked-dlopen new_dlopen=%p", new_dlopen);

		hook(&libc, (void *)new_mprotect, "mprotect", (void **)&old_mprotect);
		// hook(&libc, (void*)new_mmap, "mmap", (void **)&old_mmap);
		// inline_hook((void*) munmap, (void*)new_munmap, "munmap", (void **)&old_munmap);
		// hook(&libc, (void*)new__mmap2, "__mmap2", (void **)&old__mmap2);
		// inline_hook((void*)__mmap2, (void*)new__mmap2, "__mmap2", (void **)&old__mmap2);

		//# hook(&libdl, (void*)new_dlsym, "dlsym", (void **)&old_dlsym);
		inline_hook((void*)dlsym, (void*)new_dlsym, "dlsym", (void **)&old_dlsym);
		// inline_hook((void*)ptrace, (void*)new_ptrace, "ptrace", (void **)&old_ptrace);
	} else if(settings_patch_ssl) {
		inline_mshook((void *)dlopen, (void *)ssl_dlopen, "dlopen", (void **)&old_dlopen);
		LOGD("[*] Hooked-dlopen: ssl_dlopen=%p", ssl_dlopen);
	}
}

/*extern "C" int hook_entry(char *sopath){
	soinfo *handle;
	LOGD("Start");
	handle = (soinfo*)dlopen(sopath, RTLD_NOW);
	handle->refcount ++;
	dlclose(handle);
	patch_lib();
	LOGD("Finish");
	return 0;
}*/

JNIEXPORT void JNICALL Java_com_fuzhu8_inspector_jni_TraceAnti__1traceAnti
  (JNIEnv *env, jobject obj, jstring dataDir, jboolean anti_thread_create,
		  jboolean trace_file, jboolean trace_sys_call, jboolean trace_trace,
		  jint patch_ssl) {
	myDataDir = env->GetStringUTFChars(dataDir, NULL);
	LOGD("Start dataDir=%s, anti_thread_create=%d, trace_file=%d, trace_sys_call=%d, trace_trace=%d, patch_ssl=0x%x", myDataDir, anti_thread_create, trace_file, trace_sys_call, trace_trace, patch_ssl);

	patch_settings settings = patch_settings();
	settings.anti_thread_create = anti_thread_create;
	settings.trace_file = trace_file;
	settings.trace_sys_call = trace_sys_call;
	settings.trace_trace = trace_trace;
    settings.trace_string = JNI_TRUE;
	settings_patch_ssl = patch_ssl;

	patch_lib(settings);

	LOGD("Finish traceAnti pid=%u", getpid());
}

typedef int (*proto_native)(int r0, int r1, int r2, int r3, int sp1, int sp2, int sp3, int sp4, int sp5, int sp6);

static proto_native old_native;

static int new_native(int r0, int r1, int r2, int r3, int sp1, int sp2, int sp3, int sp4, int sp5, int sp6) {
	inspect("nativeHook: r0=%p, r1=%p, r2=%p, r3=%p, sp1=%p, sp2=%p, sp3=%p, sp4=%p, sp5=%p, sp6=%p",
			r0, r1, r2, r3,
			sp1, sp2, sp3, sp4, sp5, sp6);
	return old_native(r0, r1, r2, r3, sp1, sp2, sp3, sp4, sp5, sp6);
}

JNIEXPORT jboolean JNICALL Java_com_fuzhu8_inspector_jni_TraceAnti__1nativeHook
  (JNIEnv *env, jobject obj, jint addr) {
	if(TK_InlineHookFunction((void *)addr, (void *)new_native, (void **)&old_native) == HOOK_FAILED) {
		return (jboolean) JNI_FALSE;
	} else {
		return (jboolean) JNI_TRUE;
	}
}

JNIEXPORT void JNICALL Java_com_fuzhu8_inspector_jni_TraceAnti__1testAntiHook
  (JNIEnv *env, jobject obj, jint r0, jint r1, jint r2, jint r3) {
	anti_hook(r0, r1, r2, r3);
}

JNIEXPORT void JNICALL Java_com_fuzhu8_inspector_jni_TraceAnti__1enableCollectBytecode
  (JNIEnv *env, jobject obj, jstring filter) {
	enable_collect_bytecode = JNI_TRUE;

	if(filter != NULL) {
		const char *temp = env->GetStringUTFChars(filter, NULL);
		if(temp != NULL) {
			size_t len = strlen(temp);
			void *mem = malloc(len + 1);
			memcpy(mem, temp, len + 1);
			if(bytecodeFilter != NULL) {
				free((void *) bytecodeFilter);
				bytecodeFilter = NULL;
			}
			bytecodeFilter = (const char *) mem;
		}
	} else if (bytecodeFilter != NULL) {
		free((void *) bytecodeFilter);
		bytecodeFilter = NULL;
	}

	if(old_dvmReportInvoke == NULL) {
		inline_hook((void *)dvmReportInvoke, (void *)new_dvmReportInvoke, "dvmReportInvoke", (void **)&old_dvmReportInvoke);
	}
	if(old_dvmReportExceptionThrow == NULL) {
		inline_hook((void *)dvmReportExceptionThrow, (void *)new_dvmReportExceptionThrow, "dvmReportExceptionThrow", (void **)&old_dvmReportExceptionThrow);
	}
	if(old_dvmReportPreNativeInvoke != NULL) {
		inline_hook((void *)dvmReportPreNativeInvoke, (void *)new_dvmReportPreNativeInvoke, "dvmReportPreNativeInvoke", (void **)&old_dvmReportPreNativeInvoke);
	}
	if(old_dvmReportPostNativeInvoke != NULL) {
		inline_hook((void *)dvmReportPostNativeInvoke, (void *)new_dvmReportPostNativeInvoke, "dvmReportPostNativeInvoke", (void **)&old_dvmReportPostNativeInvoke);
	}
	if(old_dvmReportReturn != NULL) {
		inline_hook((void *)dvmReportReturn, (void *)new_dvmReportReturn, "dvmReportReturn", (void **)&old_dvmReportReturn);
	}
}
