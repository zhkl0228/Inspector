package com.fuzhu8.inspector.unicorn.arm;

import unicorn.Unicorn;

/**
 * android os
 * Created by zhkl0228 on 2017/5/9.
 */

interface Os {

    int open(String path, int flags, int mode);
    int close(int fd);

    int writev(Unicorn unicorn, int fd, int iov, int iovcnt);

    int munmap(int start, int length);
    long mmap(int start, int length, int prot, int flags, int fd, int offset);
    int madvise(int addr, int len, int advise);

    int gettimeofday(Unicorn unicorn, int tv, int tz);

}
