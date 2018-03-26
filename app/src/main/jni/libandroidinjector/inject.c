/*
 * inject.c
 *
 *  Created on: Jun 4, 2011
 *      Author: d
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <dlfcn.h>
#include "utils.h"
#include <signal.h>
#include <sys/types.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <jni.h>

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



    handle = ptrace_dlopen(pid, argv[2], RTLD_LAZY);
    printf("ptrace_dlopen handle %p\n",handle);
    ptrace_detach(pid);
    exit(0);

}










