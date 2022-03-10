/*
 * ptrace.c
 *
 *  Created on: Jun 4, 2011
 *      Author: d
 */

#include <stdio.h>
#include <sys/ptrace.h>

#include <stdlib.h>
#include <sys/wait.h>
#include <string.h>
#include <errno.h>

#include <sys/types.h>
#include <sys/wait.h>
#include <stdarg.h>

#include "utils.h"
#include "linker.h"

static regs_t oldregs;

static int debug = 0;

dl_fl_t ldl;

void ptrace_dump_regs(regs_t *regs, char *msg) {
	if (debug) {
		int i = 0;
		printf("------regs %s-----\n", msg);
		for (i = 0; i < 18; i++) {
			printf("r[%02d]=%lx\n", i, regs->uregs[i]);
		}
	}
}

void ptrace_attach(int pid) {
    regs_t regs;
    int status = 0;
    if (ptrace(PTRACE_ATTACH, pid, NULL, NULL ) < 0) {
        perror("ptrace_attach");
        exit(-1);
    }
    status = ptrace_wait_for_signal(pid, SIGSTOP);
    printf("ptrace_wait_for_signal: %d %d\n", __LINE__, status);
    //waitpid(pid, NULL, WUNTRACED);

    ptrace_readreg(pid, &regs);
    memcpy(&oldregs, &regs, sizeof(regs));

    ptrace_dump_regs(&oldregs, "old regs");
#ifdef ANDROID
#ifdef THUMB
    regs.ARM_pc = 0x11;
    regs.ARM_cpsr |=0x30;
#else
    regs.ARM_pc= 0;
#endif
#else
    regs.rip = 0;
#endif
    ptrace_writereg(pid, &regs);

    ptrace_cont(pid);

    printf("waiting.. sigal...\n");

    status = ptrace_wait_for_signal(pid, SIGSEGV);
    printf("ptrace_wait_for_signal2: %d %d\n", __LINE__, status);

}

void ptrace_cont(int pid) {
    //int stat;

    if (ptrace(PTRACE_CONT, pid, NULL, NULL ) < 0) {
        perror("ptrace_cont");
        exit(-1);
    }

    //while (!WIFSTOPPED(stat))
    //    waitpid(pid, &stat, WNOHANG);
}

void ptrace_detach(int pid) {
    ptrace_writereg(pid, &oldregs);

    if (ptrace(PTRACE_DETACH, pid, NULL, NULL ) < 0) {
        perror("ptrace_detach");
        exit(-1);
    }
}

void ptrace_write(int pid, unsigned long addr, void *vptr, int len) {
    int count;
    long word;
    void *src = (long*) vptr;
    count = 0;

    while (count < len) {
        memcpy(&word, src + count, sizeof(word));
        word = ptrace(PTRACE_POKETEXT, pid, (void*) (addr + count), (void*) word);
        count += 4;

        if (errno != 0)
            printf("ptrace_write failed\t %ld\n", addr + count);
    }
}

void ptrace_read(int pid, unsigned long addr, void *vptr, int len) {
    int i, count;
    long word;
    unsigned long *ptr = (unsigned long *) vptr;

    i = count = 0;

    while (count < len) {
        word = ptrace(PTRACE_PEEKTEXT, pid, (void*) (addr + count), NULL );
        count += 4;
        ptr[i++] = word;
    }
}

char * ptrace_readstr(int pid, unsigned long addr) {
    char *str = (char *) malloc(64);
    int i, count;
    long word;
    char *pa;

    i = count = 0;
    pa = (char *) &word;

    while (i <= 60) {
        word = ptrace(PTRACE_PEEKTEXT, pid, (void*) (addr + count), NULL );
        count += 4;

        if (pa[0] == '\0') {
            str[i] = '\0';
            break;
        } else
            str[i++] = pa[0];

        if (pa[1] == '\0') {
            str[i] = '\0';
            break;
        } else
            str[i++] = pa[1];

        if (pa[2] == '\0') {
            str[i] = '\0';
            break;
        } else
            str[i++] = pa[2];

        if (pa[3] == '\0') {
            str[i] = '\0';
            break;
        } else
            str[i++] = pa[3];
    }
    return str;
}

void ptrace_readreg(int pid, regs_t *regs) {
    if (ptrace(PTRACE_GETREGS, pid, NULL, regs))
        printf("*** ptrace_readreg error ***\n");

}

void ptrace_writereg(int pid, regs_t *regs) {
    if (ptrace(PTRACE_SETREGS, pid, NULL, regs))
        printf("*** ptrace_writereg error ***\n");
}

unsigned long ptrace_push(int pid, regs_t *regs, void *paddr, int size) {
#ifdef ANDROID
    unsigned long arm_sp;
    arm_sp = regs->ARM_sp;
    arm_sp -= size;
    arm_sp = arm_sp - arm_sp % 4;
    regs->ARM_sp= arm_sp;
    ptrace_write(pid, arm_sp, paddr, size);
    return arm_sp;
#else
    unsigned long esp;
    regs_t regs;
    ptrace_readreg(pid, &regs);
    esp = regs.esp;
    esp -= size;
    esp = esp - esp % 4;
    regs.esp = esp;
    ptrace_writereg(pid, &regs);
    ptrace_write(pid, esp, paddr, size);
    return esp;
#endif
}

long ptrace_stack_alloc(pid_t pid, regs_t *regs, int size) {
    unsigned long arm_sp;
    arm_sp = regs->ARM_sp;
    arm_sp -= size;
    arm_sp = arm_sp - arm_sp % 4;
    regs->ARM_sp= arm_sp;
    return arm_sp;
}

void *ptrace_dlopen(pid_t pid, const char *filename, int flag) {
#ifdef ANDROID
    regs_t regs;
    //int stat;
    ptrace_readreg(pid, &regs);

    ptrace_dump_regs(&regs, "before call to ptrace_dlopen\n");

#ifdef THUMB
    regs.ARM_lr = 1;
#else
    regs.ARM_lr= 0;
#endif

    regs.ARM_r0= (long)ptrace_push(pid,&regs, (void*)filename,strlen(filename)+1);
    regs.ARM_r1= flag;
    regs.ARM_pc= ldl.l_dlopen;
    ptrace_writereg(pid, &regs);
    ptrace_cont(pid);
    printf("done %d\n", ptrace_wait_for_signal(pid, SIGSEGV));
    ptrace_readreg(pid, &regs);
    ptrace_dump_regs(&regs, "before return ptrace_call\n");
    return (void*) regs.ARM_r0;
#endif
}

void *ptrace_dlsym(pid_t pid, void *handle, const char *symbol) {

#ifdef ANDROID
    regs_t regs;
    //int stat;
    ptrace_readreg(pid, &regs);
    ptrace_dump_regs(&regs, "before call to ptrace_dlsym\n");

#ifdef THUMB

    regs.ARM_lr = 1;
#else
    regs.ARM_lr= 0;
#endif

    regs.ARM_r0= (long)handle;
    regs.ARM_r1= (long)ptrace_push(pid,&regs, (void*)symbol,strlen(symbol)+1);

    regs.ARM_pc= ldl.l_dlsym;
    ptrace_writereg(pid, &regs);
    ptrace_cont(pid);
    printf("done %d\n", ptrace_wait_for_signal(pid, SIGSEGV));
    ptrace_readreg(pid, &regs);
    ptrace_dump_regs(&regs, "before return ptrace_dlsym\n");
    return (void*) regs.ARM_r0;
#endif
}

int ptrace_mymath_add(pid_t pid, long mymath_add_addr, int a, int b) {
#ifdef ANDROID
    regs_t regs;
    //int stat;
    ptrace_readreg(pid, &regs);
    ptrace_dump_regs(&regs, "before call to ptrace_mymath_add\n");

#ifdef THUMB
    regs.ARM_lr = 1;
#else
    regs.ARM_lr= 0;
#endif

    regs.ARM_r0= a;
    regs.ARM_r1= b;

    regs.ARM_pc= mymath_add_addr;
    ptrace_writereg(pid, &regs);
    ptrace_cont(pid);
    printf("done %d\n", ptrace_wait_for_signal(pid, SIGSEGV));
    ptrace_readreg(pid, &regs);
    ptrace_dump_regs(&regs, "before return ptrace_mymath_add\n");
    return regs.ARM_r0;
#endif
}

int ptrace_call(int pid, long proc, int argc, ptrace_arg *argv) {
    int i = 0;
#define ARGS_MAX 64
    regs_t regs;
    ptrace_readreg(pid, &regs);
    ptrace_dump_regs(&regs, "before ptrace_call\n");

    /*prepare stacks*/
    for (i = 0; i < argc; i++) {
        ptrace_arg *arg = &argv[i];
        if (arg->type == PAT_STR) {
            arg->_stackid = ptrace_push(pid, &regs, arg->s, strlen(arg->s) + 1);
        } else if (arg->type == PAT_MEM) {
            //printf("push data %p to stack[%d] :%d \n", arg->mem.addr, stackcnt, *((int*)arg->mem.addr));
            arg->_stackid = ptrace_push(pid, &regs, arg->mem.addr, arg->mem.size);
        }
    }
    for (i = 0; (i < 4) && (i < argc); i++) {
        ptrace_arg *arg = &argv[i];
        if (arg->type == PAT_INT) {
            regs.uregs[i] = arg->i;
        } else if (arg->type == PAT_STR) {
            regs.uregs[i] = arg->_stackid;
        } else if (arg->type == PAT_MEM) {
            regs.uregs[i] = arg->_stackid;
        } else {
            printf("unkonwn arg type\n");
        }
    }

    for (i = argc - 1; i >= 4; i--) {
        ptrace_arg *arg = &argv[i];
        if (arg->type == PAT_INT) {
            ptrace_push(pid, &regs, &arg->i, sizeof(int));
        } else if (arg->type == PAT_STR) {
            ptrace_push(pid, &regs, &arg->_stackid, sizeof(unsigned long));
        } else if (arg->type == PAT_MEM) {
            ptrace_push(pid, &regs, &arg->_stackid, sizeof(unsigned long));
        } else {
            printf("unkonwn arg type\n");
        }
    }
#ifdef THUMB
    regs.ARM_lr = 1;
#else
    regs.ARM_lr= 0;
#endif
    regs.ARM_pc= proc;
    ptrace_writereg(pid, &regs);
    ptrace_cont(pid);
    printf("done %d\n", ptrace_wait_for_signal(pid, SIGSEGV));
    ptrace_readreg(pid, &regs);
    ptrace_dump_regs(&regs, "before return ptrace_call\n");

    //sync memory
    for (i = 0; i < argc; i++) {
        ptrace_arg *arg = &argv[i];
        if (arg->type == PAT_STR) {
        } else if (arg->type == PAT_MEM) {
            ptrace_read(pid, arg->_stackid, arg->mem.addr, arg->mem.size);
        }
    }

    return regs.ARM_r0;
}

int ptrace_wait_for_signal(int pid, int signal) {
    int status;
    pid_t res;
    res = waitpid(pid, &status, 0);
    if (res != pid || !WIFSTOPPED (status))
        return 0;
    return WSTOPSIG (status) == signal;
}

static Elf32_Addr get_linker_base(int pid, Elf32_Addr *base_start, Elf32_Addr *base_end) {
    unsigned long base = 0;
    char mapname[FILENAME_MAX];
    memset(mapname, 0, FILENAME_MAX);
    snprintf(mapname, FILENAME_MAX, "/proc/%d/maps", pid);
    FILE *file = fopen(mapname, "r");
    *base_start = *base_end = 0;
    if (file) {
        //400a4000-400b9000 r-xp 00000000 103:00 139       /system/bin/linker
        while (1) {
            unsigned int atleast = 32;
            int xpos = 20;
            char startbuf[9];
            char endbuf[9];
            char line[FILENAME_MAX];
            memset(line, 0, FILENAME_MAX);
            char *linestr = fgets(line, FILENAME_MAX, file);
            if (!linestr) {
                break;
            }
            /*printf("........%s <--\n", line);*/
            if (strlen(line) > atleast && strstr(line, "/system/bin/linker")) {
                memset(startbuf, 0, sizeof(startbuf));
                memset(endbuf, 0, sizeof(endbuf));

                memcpy(startbuf, line, 8);
                memcpy(endbuf, &line[8 + 1], 8);
                if (*base_start == 0) {
                    *base_start = strtoul(startbuf, NULL, 16);
                    *base_end = strtoul(endbuf, NULL, 16);
                    base = *base_start;
                } else {
                    *base_end = strtoul(endbuf, NULL, 16);
                }
            }
        }
        fclose(file);

    }
    return base;

}
dl_fl_t *ptrace_find_dlinfo(int pid) {
    Elf32_Sym sym;
    Elf32_Addr addr;
    struct soinfo lsi;
#define LIBDLSO "libdl.so"
    Elf32_Addr base_start = 0;
    Elf32_Addr base_end = 0;
    Elf32_Addr base = get_linker_base(pid, &base_start, &base_end);

    if (base == 0) {
        printf("no linker found\n");
        return NULL ;
    } else {
        printf("search libdl.so from %08u to %08u\n", base_start, base_end);
    }

    for (addr = base_start; addr < base_end; addr += 4) {
        char soname[strlen(LIBDLSO)];
        Elf32_Addr off = 0;

        ptrace_read(pid, addr, soname, strlen(LIBDLSO));
        if (strncmp(LIBDLSO, soname, strlen(LIBDLSO))) {
            continue;
        }

        printf("soinfo found at %08u\n", addr);
        printf("symtab: %p\n", lsi.symtab);
        ptrace_read(pid, addr, &lsi, sizeof(lsi));

        off = (Elf32_Addr)lsi.symtab;

        ptrace_read(pid, off, &sym, sizeof(sym));
        //just skip
        off += sizeof(sym);

        ptrace_read(pid, off, &sym, sizeof(sym));
        ldl.l_dlopen = sym.st_value;
        off += sizeof(sym);

        ptrace_read(pid, off, &sym, sizeof(sym));
        ldl.l_dlclose = sym.st_value;
        off += sizeof(sym);

        ptrace_read(pid, off, &sym, sizeof(sym));
        ldl.l_dlsym = sym.st_value;
        off += sizeof(sym);

        printf("dlopen addr %p\n", (void*) ldl.l_dlopen);
        printf("dlclose addr %p\n", (void*) ldl.l_dlclose);
        printf("dlsym addr %p\n", (void*) ldl.l_dlsym);
        return &ldl;

    }
    printf("%s not found!\n", LIBDLSO);
    return NULL ;
}

