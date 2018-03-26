#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <dlfcn.h>
#include <unistd.h>
#include "util.h"
#include "log.h"

#include "Hooker.h"
#include "hook.h"

int elfHook(const char *soname, const char *symbol, void *replace_func,
		void **old_func) {
	void *addr = NULL;
	if (find_name(getpid(), symbol, soname, (unsigned long *) &addr) < 0) {
		LOGW("Not find: %s in %s\n", symbol, soname);
		return -1;
	}
	Cydia::MSHookFunction(addr, replace_func, old_func);
	return 0;
}

int elfHookDirect(unsigned int addr, void *replace_func, void **old_func) {
	if (addr == 0) {
		LOGW("hook direct addr: %p error!", (void* )addr);
		return -1;
	}
	Cydia::MSHookFunction((void*) addr, replace_func, old_func);
	return 0;
}
