/*
 * arm.S
 *
 *  Created on: 2015年10月12日
 *      Author: zhkl0228
 */

.global my_add
my_add:
	add r0, r0, r1
	bx lr
