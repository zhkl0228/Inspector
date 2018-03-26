LOCAL_PATH		:= $(call my-dir)

SOFTMMU_SRC_FILES := qemu/translate-all.c qemu/exec.c qemu/cpu-exec.c qemu/tcg/tcg.c \
                     qemu/tcg/optimize.c qemu/fpu/softfloat.c qemu/cpus.c qemu/ioport.c \
                     qemu/memory.c qemu/cputlb.c qemu/memory_mapping.c qemu/hw/arm/tosa.c \
                     qemu/hw/arm/virt.c qemu/target-arm/translate.c qemu/target-arm/op_helper.c \
                     qemu/target-arm/helper.c qemu/target-arm/cpu.c qemu/target-arm/neon_helper.c \
                     qemu/target-arm/iwmmxt_helper.c qemu/target-arm/psci.c qemu/target-arm/unicorn_arm.c \
                     qemu/target-arm/crypto_helper.c

QEMU_CFLAGS := -D_GNU_SOURCE -D_FILE_OFFSET_BITS=64 -fPIC -D_LARGEFILE_SOURCE \
               -I$(LOCAL_PATH)/include -I$(LOCAL_PATH)/qemu \
               -I$(LOCAL_PATH)/qemu/include -I$(LOCAL_PATH)/qemu/tcg

SOFTMMU_CFLAGS := $(QEMU_CFLAGS) -I$(LOCAL_PATH)/qemu/tcg/arm \
                  -I$(LOCAL_PATH)/qemu/target-arm -DNEED_CPU_H -fvisibility=hidden

ifeq ($(TARGET_ARCH),arm)
    include $(CLEAR_VARS)
    LOCAL_MODULE    := arm-softmmu
    LOCAL_CFLAGS := $(SOFTMMU_CFLAGS) -I$(LOCAL_PATH)/qemu/arm-softmmu -include $(LOCAL_PATH)/qemu/arm.h
    LOCAL_SRC_FILES := $(SOFTMMU_SRC_FILES)
    include $(BUILD_STATIC_LIBRARY)

    include $(CLEAR_VARS)
    LOCAL_MODULE    := armeb-softmmu
    LOCAL_CFLAGS := $(SOFTMMU_CFLAGS) -I$(LOCAL_PATH)/qemu/armeb-softmmu -include $(LOCAL_PATH)/qemu/armeb.h
    LOCAL_SRC_FILES := $(SOFTMMU_SRC_FILES)
    include $(BUILD_STATIC_LIBRARY)

    include $(CLEAR_VARS)
    LOCAL_MODULE    := unicorn
    LOCAL_CFLAGS := $(QEMU_CFLAGS) -DUNICORN_HAS_ARM -DUNICORN_HAS_ARMEB
    LOCAL_STATIC_LIBRARIES += libarm-softmmu libarmeb-softmmu
    LOCAL_SRC_FILES := uc.c list.c unicorn_Unicorn.c \
                       qemu/glib_compat.c qemu/accel.c qemu/vl.c qemu/qemu-timer.c qemu/qemu-log.c \
                       qemu/tcg-runtime.c qemu/hw/core/qdev.c qemu/hw/core/machine.c qemu/qom/object.c \
                       qemu/qom/container.c qemu/qom/qom-qobject.c qemu/qom/cpu.c qemu/qapi-types.c qemu/qapi-visit.c \
                       qemu/qapi/qapi-visit-core.c qemu/qapi/qapi-dealloc-visitor.c qemu/qapi/qmp-input-visitor.c \
                       qemu/qapi/qmp-output-visitor.c qemu/qapi/string-input-visitor.c \
                       qemu/qobject/qint.c qemu/qobject/qstring.c qemu/qobject/qdict.c qemu/qobject/qlist.c \
                       qemu/qobject/qfloat.c qemu/qobject/qbool.c qemu/qobject/qerror.c \
                       qemu/util/cutils.c qemu/util/qemu-timer-common.c qemu/util/qemu-thread-posix.c \
                       qemu/util/oslib-posix.c qemu/util/module.c qemu/util/bitmap.c qemu/util/bitops.c \
                       qemu/util/error.c qemu/util/aes.c qemu/util/crc32c.c qemu/util/host-utils.c qemu/util/getauxval.c
    include $(BUILD_SHARED_LIBRARY)
endif
