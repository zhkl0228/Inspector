LOCAL_PATH		:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE	:= netguard
LOCAL_CFLAGS := -std=gnu99
LOCAL_SRC_FILES	:= dhcp.c dns.c icmp.c ip.c netguard.c pcap.c session.c tcp.c udp.c util.c
LOCAL_LDLIBS	:= -llog
include $(BUILD_SHARED_LIBRARY)
