#include <sys/mman.h>
#include <pthread.h>
#include <stdio.h>
#include <stdint.h>
#include <unistd.h>
#include <signal.h>
#include <sys/stat.h>
#include <sys/ptrace.h>
#include <sys/inotify.h>
#include <errno.h>
#include "TKHooklib.h"
#include "dvm.h"
#include "hook.h"

#ifndef PAGESIZE
#define PAGESIZE (size_t)(sysconf(_SC_PAGESIZE))
#endif

#define TK_LOG_TAG "TraceAnti"
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TK_LOG_TAG, fmt, ##args)

#define GETR0(store_lr)	\
	__asm__ __volatile__(	\
		"mov %0, r0\n\t"	\
		:	"=r"(store_lr)	\
	)

#define GETLR(store_lr)	\
	__asm__ __volatile__(	\
		"mov %0, lr\n\t"	\
		:	"=r"(store_lr)	\
	)

#define GETPC(store_lr)	\
	__asm__ __volatile__(	\
		"mov %0, pc\n\t"	\
		:	"=r"(store_lr)	\
	)

typedef void* (*proto_mmap)(void* start,size_t length,int prot,int flags,int fd,off_t offset);
typedef FILE* (*proto_fopen)(const char * path,const char * mode);
typedef int (*proto_ptrace)(int request, int pid, int addr, int data);
typedef int (*proto_open)(const char *pathname, int oflag, mode_t mode);
typedef int (*proto_fstat)(int fildes,struct stat *buf);
typedef int (*proto_munmap)(void *start, size_t length);

typedef void* (*proto_dlopen)(const char * pathname, int mode);
typedef void* (*proto_dlsym)(void* handle, const char* symbol);
typedef int (*proto_inotify_add_watch)(int fd, const char *pathname, uint32_t mask);
typedef ssize_t (*proto_write)(int fd, const void *buf, size_t count);
typedef int (*proto_close)(int fd);
typedef void* (*proto_memmove)( void* dest, const void* src, size_t count );
typedef int (*proto_memcmp)(const void *buf1, const void *buf2, unsigned int count);

typedef void (*proto_exit)(int status);
typedef int (*proto_kill)(pid_t pid, int sig);
typedef void (*proto_abort)();
typedef int (*proto_raise)(int sig);
typedef void (*proto__exit)(int status);
typedef void* (*proto__mmap2)(void*, size_t, int, int, int, size_t);
typedef pid_t (*proto_fork)();
typedef int (*proto_stat)(const char *path, struct stat *buf);
typedef int (*proto_lstat)(const char *path, struct stat *buf);
typedef pid_t (*proto__fork)();
typedef int (*proto_sigaction)(int signum, const struct sigaction *act, struct sigaction *oldact);
typedef void* (*proto_signal)(int sig_num, void (*handler)(int));
typedef int (*proto_mprotect)(const void *addr, size_t len, int prot);
typedef int (*proto_pthread_create)(pthread_t *tidp, const pthread_attr_t *attr, void* (*start_rtn)(void *), void *arg);
typedef DexFile* (*proto_dexFileParse)(const u1 *data, size_t length, int flags);
typedef Object* (*proto_dvmInvokeMethod)(Object* obj, const Method* method, ArrayObject* argList, ArrayObject* params, ClassObject* returnType, bool noAccessCheck);
typedef void (*proto_dvmInterpret)(Thread* self, const Method* method, JValue* pResult);
typedef void (*proto_dvmReportInvoke)(Thread* self, const Method* methodToCall);
typedef void (*proto_dvmReportExceptionThrow)(Thread* self, Object* exception);
typedef void (*proto_dvmReportPreNativeInvoke)(const Method* methodToCall, Thread* self, u4* fp);
typedef void (*proto_dvmReportPostNativeInvoke)(const Method* methodToCall, Thread* self, u4* fp);
typedef void (*proto_dvmReportReturn)(Thread* self);

typedef unsigned int (*proto_strlen)(char *s);
typedef char *(*proto_strncpy)(char *dest,char *src,size_t n);
typedef int (*proto_strncmp)( const char * str1, const char * str2, size_t n );

static proto_mmap old_mmap = NULL;
static proto_fopen old_fopen = fopen;
static proto_ptrace old_ptrace = NULL;
static proto_open old_open = NULL;
static proto_fstat old_fstat = NULL;
static proto_munmap old_munmap = NULL;

static proto_dlopen old_dlopen = NULL;
static proto_dlsym old_dlsym = NULL;
static proto_inotify_add_watch old_inotify_add_watch = NULL;
static proto_write old_write = NULL;
static proto_close old_close = NULL;
static proto_memcmp old_memcmp = NULL;
static proto_memmove old_memmove = NULL;

static proto_exit old_exit = NULL;
static proto_kill old_kill = NULL;
static proto_abort old_abort = NULL;
static proto_raise old_raise = NULL;
static proto__exit old__exit = NULL;
static proto__mmap2 old__mmap2 = NULL;
static proto_fork old_fork = NULL;
static proto_stat old_stat = NULL;
static proto_lstat old_lstat = NULL;
static proto__fork old__fork = NULL;
static proto_sigaction old_sigaction = NULL;
static proto_signal old_signal = NULL;
static proto_mprotect old_mprotect = NULL;
static proto_pthread_create old_pthread_create = NULL;
static proto_dexFileParse old_dexFileParse = NULL;
static proto_dvmInvokeMethod old_dvmInvokeMethod = NULL;
static proto_dvmInterpret old_dvmInterpret = NULL;
static proto_dvmReportInvoke old_dvmReportInvoke = NULL;
static proto_dvmReportExceptionThrow old_dvmReportExceptionThrow = NULL;
static proto_dvmReportPreNativeInvoke old_dvmReportPreNativeInvoke = NULL;
static proto_dvmReportPostNativeInvoke old_dvmReportPostNativeInvoke = NULL;
static proto_dvmReportReturn old_dvmReportReturn = NULL;

typedef int (*proto_execve)(const char *path, char *const argv[], char *const envp[]);
static proto_execve old_execve = NULL;
static pthread_mutex_t lock_execve;

static proto_strlen old_strlen = NULL;
static pthread_mutex_t lock_strlen;
static proto_strncpy old_strncpy = NULL;
static pthread_mutex_t lock_strncpy;
static proto_strncmp old_strncmp = NULL;
static pthread_mutex_t lock_strncmp;

#define BUFFERSIZE 512

static void copyFile(FILE *src, FILE *dest) {
	int c;
	while(true) {
		c = fgetc(src);
		if(feof(src)) {
			break;
		}
		fputc(c, dest);
	};
	/*char buffer[BUFFERSIZE];
	int read;
	while((read = fread(buffer, 1, BUFFERSIZE, src)) != EOF) {
		fwrite(buffer, 1, BUFFERSIZE, dest);
	}*/
	/*while (fgets(buffer, BUFFERSIZE, src) != NULL) {
		fputs(buffer, dest);
	}*/
}

extern "C" int mkdir(const char*, mode_t);

const char *myDataDir = NULL;

static FILE *createFile(const char *path) {
	int len = strlen(path);
	char dir[len + 1];
	dir[len] = '\0';
	strncpy(dir, path, len);

	LOGD("[*] Try mkdir: %s", path);

	for(int i = 1; i < len; i++) {
		if(dir[i] != '/') {
			continue;
		}

		dir[i] = '\0';
		if(access(dir, F_OK) < 0) {
			if(mkdir(dir, 0755) < 0) {
				LOGD("[E] mkdir failed: %s, msg=%s", dir, strerror(errno));
				return NULL;
			}
		}
		dir[i] = '/';
	}

	return old_fopen(path, "w");
}

static void saveMemory(const char *path, void *mem, size_t size) {
	char re_path[256];
	if(myDataDir != NULL) {
		sprintf(re_path, "%s%s", myDataDir, path);
	} else {
		sprintf(re_path, "/data/local/tmp%s", path);
	}

	FILE *dest = createFile(re_path);
	if(dest == NULL) {
		LOGD("[E] Save memory [%s]failed for create dest file", re_path);
		return;
	}

	char *copy = (char *) mem;
	for(int i = 0; i < size; i++) {
		fputc(copy[i], dest);
	}

	LOGD("[*] Save memory %u count from %p to %s", size, mem, re_path);
	fclose(dest);
}

static FILE* mapFile(const char *path, const char *mode, const char *label, bool mustCopy) {
	char re_path[256];
	if(myDataDir != NULL) {
		sprintf(re_path, "%s%s", myDataDir, path);
	} else {
		sprintf(re_path, "/data/local/tmp%s", path);
	}

	FILE *dest = mustCopy ? createFile(re_path) : fopen(re_path, mode);
	if(dest != NULL && !mustCopy) {
		// LOGD("[E] re-path [%s]failed create dest file.", re_path);
		return dest;
	}

	LOGD(label, path, mode);

	FILE *src = old_fopen(path, "r");
	if(src == NULL) {
		LOGD("[E] re-path [%s]failed for src file", path);
		return old_fopen(path, mode);
	}
	if(dest == NULL) {
		dest = createFile(re_path);
	}
	if(dest == NULL) {
		LOGD("[E] re-path [%s]failed for create dest file", path);
		return old_fopen(path, mode);
	}

	LOGD("[*] re-path %s to %s", path, re_path);

	copyFile(src, dest);
	fclose(src);
	fclose(dest);

	return old_fopen(re_path, mode);
}

static int new_execve(const char *path, char * argv[], char *const envp[]) {
	unsigned lr;
	GETLR(lr);

	pthread_mutex_lock(&lock_execve);

	LOGD("[*] Traced-execve Call function: %p, %s", (void *)lr, path);
	FILE *file = mapFile(path, "r", "[*] Saved execve file: %s with mode %s", true);
	if(file != NULL) {
		fclose(file);
	}

	int ret = old_execve(path, argv, envp);
	pthread_mutex_unlock(&lock_execve);
	return ret;
}

static pthread_mutex_t lock_mprotect;
static int new_mprotect(void *addr, size_t len, int prot) {
	unsigned lr;
	GETLR(lr);
	pthread_mutex_lock(&lock_mprotect);
	int ret = old_mprotect(addr, len, prot);
	pthread_mutex_unlock(&lock_mprotect);
	LOGD("[*] Traced-mprotect Call function: %p, addr=%p, length=%d, prot=0x%X, ret=%d", (void *)lr, addr, len, prot, ret);
	return ret;
}

static void new_exit(int status) {
	unsigned lr;
	GETLR(lr);

	LOGD("[*] Traced-exit Call function: %p, status=%d", (void *)lr, status);
	old_exit(status);
}

static int new_kill(pid_t pid, int sig) {
	unsigned lr;
	GETLR(lr);

	char re_path[256];
	memset(re_path, 0, 256);
	sprintf(re_path, "/proc/%u/maps", getpid());
	mapFile(re_path, "r", "[*] Saved maps file: %s with mode %s", true);

	memset(re_path, 0, 256);
	sprintf(re_path, "/kill_from_%u", getpid());
	saveMemory(re_path, (void *) (lr & ~(PAGESIZE - 1)), PAGESIZE * 72);

	LOGD("[*] Traced-kill Call function: %p, pid=%d, sig=%d", (void *)lr, pid, sig);

	return old_kill(pid, sig);
}

static void new_abort() {
	unsigned lr;
	GETLR(lr);

	LOGD("[*] Traced-abort Call function: %p", (void *)lr);
	return old_abort();
}

static pthread_mutex_t lock_ptrace;

static int new_ptrace(int request, int pid, int addr, int data) {
	if (request == PTRACE_TRACEME) {
		LOGD("[*] Traced-anti-PTRACE_TRACEME!");
	} else if (request == PTRACE_ATTACH) {
		if (pid == getppid()) {
			LOGD("[*] Detect Traced-anti-ptrace attach parent!");
		}
	}

	/*if(request == PTRACE_ATTACH ||
			request == PTRACE_CONT) {
		LOGD("[*] Patched-anti-PTRACE_ATTACH: pid=%d, request=%d", pid, request);
		return 0;
	}*/

	unsigned lr;
	GETLR(lr);
	pthread_mutex_lock(&lock_ptrace);
	int ret = old_ptrace(request, pid, addr, data);
	pthread_mutex_unlock(&lock_ptrace);
	LOGD("[*] Traced-ptrace Call function: %p, request=%d, pid=%d, addr=%p, data=%p, ret=%d", (void *)lr, request, pid, (void *)addr, (void *)data, ret);
	return ret;
}

static pthread_mutex_t lock_signal;
static void* new_signal(int signum, void (*handler)(int)) {
	unsigned lr;
	GETLR(lr);
	pthread_mutex_lock(&lock_signal);
	void *ret = old_signal(signum, handler);
	LOGD("[*] Traced-signal Call function: %p, signum=%d, handler=%p, old=%p", (void *)lr, signum, handler, ret);
	pthread_mutex_unlock(&lock_signal);
	return ret;
}

static void inline_hook(void *fun, void *new_fun, const char *name, void **old) {
	if(TK_InlineHookFunction(fun, new_fun, old) == HOOK_FAILED) {
		LOGD("InlineHook %s failed", name);
	} else {
		LOGD("InlineHook %s OK: new_fun=%p, old=%p, original=%p", name, new_fun, fun, *old);
	}
}

static void inline_mshook(void *fun, void *new_fun, const char *name, void **old) {
	if(elfHookDirect((unsigned int) fun, new_fun, old) != 0) {
		LOGD("InlineMSHook %s failed", name);
	} else {
		LOGD("InlineMSHook %s OK: new_fun=%p, old=%p, original=%p", name, new_fun, fun, *old);
	}
}

static void* do_patch(HookStruct *entity, void* new_func, const char* FunctionName){
	strcpy(entity->FunctionName, FunctionName);
	entity->NewFunc = (void*)new_func;
	if(TK_HookExportFunction(entity)){
		return (void*)-1;
	}
	return (void*)0;
}

static void hook(HookStruct *entity, void* new_fun, const char* name, void** old) {
	if (do_patch(entity, new_fun, name) == (void*) -1) {
		LOGD("Hook %s failed", name);
		return;
	}
	*old = entity->OldFunc;
	LOGD("Hook %s OK: new_fun=%p, old=%p", name, new_fun, *old);
}

static void mshook(const char *soname, const char *symbol, void* new_fun, void** old) {
	if (elfHook(soname, symbol, new_fun, old) != 0) {
		LOGD("MSHook %s failed", symbol);
	} else {
		LOGD("MSHook %s OK: new_fun=%p, old=%p", symbol, new_fun, *old);
	}
}
