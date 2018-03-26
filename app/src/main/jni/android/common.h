/*
 * common.h
 *
 *  Created on: 2016年1月1日
 *      Author: zhkl0228
 */

#ifndef COMMON_H_
#define COMMON_H_

#include <stdlib.h>
#include <stdint.h>

/*
 * These match the definitions in the VM specification.
 */
typedef uint8_t             u1;
typedef uint16_t            u2;
typedef uint32_t            u4;
typedef uint64_t            u8;
typedef int8_t              s1;
typedef int16_t             s2;
typedef int32_t             s4;
typedef int64_t             s8;

/*
 * Use this to keep track of mapped segments.
 */
struct MemMapping {
    void*   addr;           /* start of data */
    size_t  length;         /* length of data */

    void*   baseAddr;       /* page-aligned base address */
    size_t  baseLength;     /* length of mapping */
};

/*
 * Get the current time, in nanoseconds.  This is "relative" time, meaning
 * it could be wall-clock time or a monotonic counter, and is only suitable
 * for computing time deltas.
 */
extern "C++" u8 dvmGetRelativeTimeNsec(void);

/*
 * Get the current time, in microseconds.  This is "relative" time, meaning
 * it could be wall-clock time or a monotonic counter, and is only suitable
 * for computing time deltas.
 */
inline u8 dvmGetRelativeTimeUsec(void) {
    return dvmGetRelativeTimeNsec() / 1000;
}

/*
 * Get the current time, in milliseconds.  This is "relative" time,
 * meaning it could be wall-clock time or a monotonic counter, and is
 * only suitable for computing time deltas.  The value returned from
 * this function is a u4 and should only be used for debugging
 * messages.
 */
inline u4 dvmGetRelativeTimeMsec(void) {
    return (u4)(dvmGetRelativeTimeUsec() / 1000);
}

inline void dvmAbort(void) {
    exit(1);
}

#endif /* COMMON_H_ */
