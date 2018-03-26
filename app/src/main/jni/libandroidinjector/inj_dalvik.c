/*
 * inj_dalvik.c
 *
 *  Created on: 2013-1-18
 *      Author: d
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <dlfcn.h>
#include "utils.h"
#include <signal.h>
#include <sys/types.h>
#ifdef ANDROID
//#include <linker.h>
#endif
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <jni.h>





char *sos[] = {
        "linker"
        "libdvm.so",
        "libnativehelper.so",
        "libandroid_runtime.so",
        "libmath.so",
        "libc.so",
        NULL
};



int main(int argc, char *argv[]) {
    int pid;
    struct link_map *map;
    struct elf_info einfo;

    extern dl_fl_t ldl;

    void *handle = NULL;
    long proc = 0;
    long hooker_fopen = 0;
    (void)argc;
    pid = atoi(argv[1]);
    ptrace_attach(pid);
    ptrace_find_dlinfo(pid);
    handle = ptrace_dlopen(pid, "/system/lib/libmynet.so",1);
    printf("ptrace_dlopen handle %p\n",handle);
    proc = (long)ptrace_dlsym(pid, handle, "my_connect");
    printf("my_connect = %lx\n",proc);
    replace_all_rels(pid, "connect", proc, sos);
    ptrace_detach(pid);
    exit(0);

}










