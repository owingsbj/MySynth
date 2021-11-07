#include <stdlib.h>
#include <sys/syscall.h>
#include <pthread.h>
#include <sched.h>
#include <assert.h>
#include <jni.h>
#include <string.h>
#include <math.h>
#include <stdio.h>
#include <aaudio/AAudio.h>

#include <android/log.h>
#include <unistd.h>

#define trace(text) __android_log_write(ANDROID_LOG_INFO, "MySynth", text)
#define tracev(text, i) __android_log_print(ANDROID_LOG_INFO, "MySynth", text, i)
#define tracev2(text, i, j) __android_log_print(ANDROID_LOG_INFO, "MySynth", text, i, j)

#define boolean char
#define true 1
#define false 0

#define MAX_SAMPLE_RATE 50000

struct JNIData {

	AAudioStreamBuilder *builder;
	AAudioStream *stream;

	short* buffer;
	jobject g_buffer;
	int maxBufferSize;

	boolean started;
	boolean detachCallback;

	JavaVM * g_vm;
	jobject g_obj;
	jmethodID g_mid;
	JNIEnv * thread_env;
	boolean thread_attached;

};

/**
 * dataCallback: this callback is called when more data is needed
 */
aaudio_data_callback_result_t callback(AAudioStream *stream, void *userData, void *audioData, int32_t numFrames) {
	struct JNIData *data = userData;
	if (data == NULL) {
		trace("dataCallback ERROR: data is NULL");
	}

	if (data->g_vm == NULL) {   // not initialized
		return AAUDIO_CALLBACK_RESULT_CONTINUE;
	}

	if (data->detachCallback) {
		trace("  dataCallback:  detaching thread from jvm");
		(*data->g_vm)->DetachCurrentThread(data->g_vm);
		data->started = false;
		data->thread_attached = false;
		data->detachCallback = false;
		return AAUDIO_CALLBACK_RESULT_CONTINUE;
	}

	if (!data->started) {
		return AAUDIO_CALLBACK_RESULT_CONTINUE;
	}

	if (!data->thread_attached) {
		trace("  dataCallback:  attaching thread to jvm");
		(*data->g_vm)->AttachCurrentThread(data->g_vm, &(data->thread_env), NULL); // need to call every time as new threads sometimes happen on old android 404
		data->thread_attached = true;
	}

	// Call Java.  Guard against a request too large.
	int frames = numFrames;
	if (numFrames * 4 > data->maxBufferSize) {
		frames = data->maxBufferSize / 4;
	}
	(*data->thread_env)->CallVoidMethod(data->thread_env, data->g_obj, data->g_mid, frames);

	// Write java buffer directly into the audioData array.
	(*data->thread_env)->GetShortArrayRegion(data->thread_env, data->g_buffer, 0, frames * 2, audioData);

	return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

/*****************************
 * The JNI entry points
 ******************************/

JNIEXPORT void* JNICALL Java_com_gallantrealm_mysynth_MySynthAAudio_nativeStart(JNIEnv* env, jobject obj, int jsampleRate,
		jobject jbuffer, int jdesiredBufferSize, int jmaxBufferSize) {
	aaudio_result_t result;
	int i;
	trace(">>nativeStart");

	struct JNIData *data = calloc(1, sizeof (struct JNIData));

	// get access to shared buffer
	data->maxBufferSize = jmaxBufferSize;

	// save info for java callback
	(*env)->GetJavaVM(env, &(data->g_vm));
	data->g_obj = (*env)->NewGlobalRef(env, obj);
	data->g_buffer = (*env)->NewGlobalRef(env, jbuffer);
	jclass g_clazz = (*env)->GetObjectClass(env, data->g_obj);
	data->g_mid = (*env)->GetMethodID(env, g_clazz, "playerCallback", "(I)V");

	// create audio stream builder and set parameters
	if (data->builder == NULL) {
		result = AAudio_createStreamBuilder(&(data->builder));
		//AAudioStreamBuilder_setDeviceId(builder, deviceId);   use default device
		AAudioStreamBuilder_setDirection(data->builder, AAUDIO_DIRECTION_OUTPUT);
		AAudioStreamBuilder_setSharingMode(data->builder,
										   AAUDIO_SHARING_MODE_SHARED); // EXCLUSIVE isn't supported yet
		AAudioStreamBuilder_setPerformanceMode(data->builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
		AAudioStreamBuilder_setSampleRate(data->builder, jsampleRate);
		AAudioStreamBuilder_setChannelCount(data->builder, 2);
		AAudioStreamBuilder_setFormat(data->builder, AAUDIO_FORMAT_PCM_I16);
		AAudioStreamBuilder_setBufferCapacityInFrames(data->builder, jdesiredBufferSize / 4);
		AAudioStreamBuilder_setDataCallback(data->builder, callback, data);
	}

	// create an audio stream
	result = AAudioStreamBuilder_openStream(data->builder, &(data->stream));

	// Start the streaming
	result = AAudioStream_requestStart(data->stream);

	data->started = true;

	trace("<<nativeStart");
	return data;
}

JNIEXPORT void JNICALL Java_com_gallantrealm_mysynth_MySynthAAudio_nativeSetAffinity(JNIEnv* env, jobject obj, int nCpu) {
    trace(">>nativeSetAffinity ");
    int mask = 1 << nCpu;
    tracev("  nativeSetAffinity: Using mask %i", mask);
    int syscallres = syscall(__NR_sched_setaffinity, gettid(), sizeof(mask), &mask);
    if (syscallres)
    {
        trace("  nativeSetAffinity: Error in the syscall setaffinity");
    }
    trace("<<nativeSetAffinity ");
}

JNIEXPORT void JNICALL Java_com_gallantrealm_mysynth_MySynthAAudio_nativeStop(JNIEnv* env, jobject obj, struct JNIData* data) {
	aaudio_result_t result;
	trace(">>nativeStop");

	data->started = false;

	// signal callback thread to detach
	data->detachCallback = true;

	// wait till callback thread is detached or timeout..
	trace("  nativestop: waiting for callback thread to detach");
	int t = 0;
	while (data->thread_attached && t < 20) {
		usleep(100000); // 10th of a second
		t += 1;
	}
	trace("  nativestop: callback thread detached");

	// Stop and close the stream
	if (data->stream != NULL) {
		trace("  nativestop: requesting audio stream stop");
		int64_t timeoutNanos = 1000 * 1000000;
		aaudio_stream_state_t nextState = AAUDIO_STREAM_STATE_UNINITIALIZED;
		result = AAudioStream_requestStop(data->stream);
		result = AAudioStream_waitForStateChange(data->stream, AAUDIO_STREAM_STATE_STOPPING, &nextState, timeoutNanos);
		trace("  nativestop: closing audio stream");
		result = AAudioStream_close(data->stream);
		data->stream = NULL;
	}

	// Done with the audio stream builder, so delete it
	if (data->builder != NULL) {
		trace("  nativestop: deleating audio stream builder");
		AAudioStreamBuilder_delete(data->builder);
		data->builder = NULL;
	}

	// Cleanup JVM
	trace("  nativestop: deleting global refs");
	(*env)->DeleteGlobalRef(env, data->g_obj);
	(*env)->DeleteGlobalRef(env, data->g_buffer);

	trace("  nativestop: freeing jnidata");
	free(data);

	trace("<<nativeStop");
}
