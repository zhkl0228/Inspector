LOCAL_PATH		:= $(call my-dir)

ifeq ($(TARGET_ARCH),arm)
	include $(CLEAR_VARS)
	LOCAL_MODULE	:= xAnSo
	LOCAL_CPPFLAGS	:= -std=c++11 -fexceptions
	xAnSo_SOURCES	:= log.cpp fix/section_fix.cpp util/util.cpp Android/unpacker.cpp \
	                   Core/dyn_item.cpp Core/dyn_section.cpp Core/elf_header.cpp Core/elf_section.cpp Core/elf_segment.cpp
	LOCAL_SRC_FILES	:= $(xAnSo_SOURCES)
	include $(BUILD_SHARED_LIBRARY)
endif