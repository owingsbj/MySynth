LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := MySynthOpenSL
LOCAL_SRC_FILES := MySynthOpenSL.c
# for OpenSLES audio
LOCAL_LDLIBS    += -lOpenSLES
# for logging
LOCAL_LDLIBS    += -llog
# for native asset manager
LOCAL_LDLIBS    += -landroid
include $(BUILD_SHARED_LIBRARY)

