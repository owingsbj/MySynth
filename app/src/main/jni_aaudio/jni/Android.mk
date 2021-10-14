LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := MySynthAAudio
LOCAL_SRC_FILES := MySynthAAudio.c
# for android audio
LOCAL_LDLIBS    += -laaudio
# for logging
LOCAL_LDLIBS    += -llog
# for native asset manager
LOCAL_LDLIBS    += -landroid
include $(BUILD_SHARED_LIBRARY)
