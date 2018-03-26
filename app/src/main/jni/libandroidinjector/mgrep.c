/*
 * mgrep.c
 *
 *  Created on: Jun 27, 2011
 *      Author: d
 *        Mail: dalvikboss@gmail.com
 */

#include <stdio.h>

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <dlfcn.h>
#include "utils.h"
#include <signal.h>
#include <sys/types.h>
#ifdef ANDROID
#include <linker.h>
#endif
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <jni.h>


#define RBIT 0
#define WBIT 1
#define XBIT 2
#define PBIT 3
#define SBIT 3

#define ISR(_rgn) (_rgn->modes[RBIT]=='r')
#define ISW(_rgn) (_rgn->modes[WBIT]=='w')
#define ISX(_rgn) (_rgn->modes[XBIT]=='x')
#define ISP(_rgn) (_rgn->modes[PBIT]=='p')
#define ISS(_rgn) (_rgn->modes[SBIT]=='s')


//00008000-00009000 r-xp 00000000 b3:01 16396      /system/bin/app_process

typedef struct mregion {
    int start;
    int end;
    char modes[5];
    struct mregion *next;
}mregion;



static mregion *read_regions(char *maps);
static void clean_regions(mregion *regions);
static int mgrep(int pid, int start, int end, int vlen, int value);
/**
 * args: pid valuelen value
 */
int main(int argc, char *argv[]) {
    int pid, valuelen, value;
    char maps[PATH_MAX];
    void *handle = NULL;
    long proc = 0;
    long base = 0;
    mregion *regions = NULL;
    pid = atoi(argv[1]);
    valuelen = atoi(argv[2]);
    value = atoi(argv[3]);

    printf("finding %08x(%d) in %d\n", value, value, pid);
    memset(maps,0,PATH_MAX);
    sprintf(maps,"/proc/%d/maps",pid);
    printf("maps: %s\n",maps);
    ptrace_attach(pid);
    regions = read_regions(maps);


    ptrace_find_dlinfo(pid);
    handle = ptrace_dlopen(pid, "/system/lib/libdvm.so",1);
    proc = (long)ptrace_dlsym(pid,handle,"dvmHeapSourceGetBase");
    printf("dvmHeapSourceGetBase %p\n",proc);
    base = ptrace_call(pid,proc,0,NULL);
    printf("base = %lx\n",base);


    if(regions) {
        mregion *p = regions;
        do {
            printf("%08x~%08x %s\n",p->start, p->end, p->modes);
            if(ISW(p)) {
                //mgrep(pid, p->start, p->end, valuelen, value);
            }


            p = p->next;
        }while(p);

    }

    clean_regions(regions);
    ptrace_detach(pid);

    return 0;
}



static mregion *read_regions(char *maps) {
    char line[512];
    FILE *fd = fopen(maps, "r");
    char intbuf[9];

    mregion *first = NULL;
    while(1) {

        mregion *m = malloc(sizeof(mregion));

        memset(m,0,sizeof(mregion));

        memset(line,0,sizeof(line));
        if(!fgets(line, sizeof(line), fd)) {
            break;
        }
                //00008000-00009000 r-xp 00000000 b3:01 16396      /system/bin/app_process
        memset(intbuf,0,sizeof(intbuf));
        memcpy(intbuf,line,8);
        m->start = strtoul(intbuf,NULL,16);

        memset(intbuf,0,sizeof(intbuf));
        memcpy(intbuf,line + 8 + 1,8);
        m->end = strtoul(intbuf,NULL,16);

        memset(m->modes,0,sizeof(m->modes));
        memcpy(m->modes, line + 8 + 1 + 8 + 1, 4);


        //printf(">%08x %08x %s\n",start, end, line);
        //printf("modes %s\n", modes);

        if(!first) {
            first = m;
        } else {
            mregion *p = first;
            while(p->next) {
                p = p->next;
            }
            p->next = m;
        }

    }
    fclose(fd);
    return first;
}


static void clean_regions(mregion *regions) {
    while(regions) {
        mregion *next = regions->next;
        free(regions);
        regions = next;
    }
}


static int mgrep(int pid, int start, int end, int vlen, int value) {
    int addr = 0;
    int newdata = 9999;
    for(addr = start; addr < end; addr+=4) {
        int data = 0;
        ptrace_read(pid, addr, &data, 4);
        if(data==value) {
            fprintf(stderr, "\t\t\t#%08x\n", addr);
            //ptrace_write(pid,addr,&newdata,4);


        }
    }
    return 0;
}
