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
#define trace(text) __android_log_write(ANDROID_LOG_INFO, "MySynth", text)
#define tracev(text, i) __android_log_print(ANDROID_LOG_INFO, "MySynth", text, i)
#define tracev2(text, i, j) __android_log_print(ANDROID_LOG_INFO, "MySynth", text, i, j)

#define boolean char
#define true 1
#define false 0

#define MAXBUFFERS 10
int nbuffers;

// audio stream
AAudioStreamBuilder *builder;
AAudioStream *stream;

#define MAX_SAMPLE_RATE 50000

static short* buffer;
static jobject g_buffer;
static int maxBufferSize;

static boolean started;
static boolean detachCallback;

static JavaVM * g_vm;
static jobject g_obj;
static jmethodID g_mid;
static JNIEnv * g_env;
static boolean thread_attached;

/**
 * dataCallback: this callback is called when more data is needed
 */
aaudio_data_callback_result_t dataCallback(AAudioStream *stream, void *userData, void *audioData, int32_t numFrames) {
	if (!started || g_vm == NULL) {
		trace("  dataCallback:  not started");
		return AAUDIO_CALLBACK_RESULT_CONTINUE;
	}
	if (detachCallback) {
		trace("  dataCallback:  detaching thread from jvm");
		(*g_vm)->DetachCurrentThread(g_vm);
		started = false;
		thread_attached = false;
		return AAUDIO_CALLBACK_RESULT_CONTINUE;
	}
	if (g_env == NULL) {
		trace("  dataCallback:  getting g_env instance");
		(*g_vm)->GetEnv(g_vm, &g_env, JNI_VERSION_1_6);
	}
	if (!thread_attached) {
		trace("  dataCallback:  attaching thread to jvm");
		(*g_vm)->AttachCurrentThread(g_vm, &g_env, NULL); // need to call every time as new threads sometimes happen on old android 404
		thread_attached = true;
	}

	// Call Java.  Guard against a request too large.
	int frames = numFrames;
	if (numFrames * 4 > maxBufferSize) {
		frames = maxBufferSize / 4;
	}
	(*g_env)->CallVoidMethod(g_env, g_obj, g_mid, frames);

	// Write java buffer directly into the audioData array.
	(*g_env)->GetShortArrayRegion(g_env, g_buffer, 0, frames * 2, audioData);

	return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

/*****************************
 * The JNI entry points
 ******************************/

JNIEXPORT jint JNICALL Java_com_gallantrealm_mysynth_MySynthAAudio_nativeStart(JNIEnv* env, jobject obj, int jsampleRate,
		jobject jbuffer, int jdesiredBufferSize, int jmaxBufferSize) {
	aaudio_result_t result;
	int i;
	trace(">>nativeStart");

	// get access to shared buffer
	maxBufferSize = jmaxBufferSize;

	// save info for java callback
	(*env)->GetJavaVM(env, &g_vm);
	g_obj = (*env)->NewGlobalRef(env, obj);
	g_buffer = (*env)->NewGlobalRef(env, jbuffer);
	jclass g_clazz = (*env)->GetObjectClass(env, g_obj);
	g_mid = (*env)->GetMethodID(env, g_clazz, "playerCallback", "(I)V");

	// create audio stream builder and set parameters
	result = AAudio_createStreamBuilder(&builder);
	//AAudioStreamBuilder_setDeviceId(builder, deviceId);   use default device
	AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
	AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED); // EXCLUSIVE isn't supported yet
	AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
	AAudioStreamBuilder_setSampleRate(builder, jsampleRate);
	AAudioStreamBuilder_setChannelCount(builder, 2);
	AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_I16);
	AAudioStreamBuilder_setBufferCapacityInFrames(builder, jdesiredBufferSize / 4);
	AAudioStreamBuilder_setDataCallback(builder, dataCallback, NULL);

	// create an audio stream
	result = AAudioStreamBuilder_openStream(builder, &stream);

	// Start the streaming
	result = AAudioStream_requestStart(stream);

	detachCallback = false;
	started = true;

	trace("<<nativeStart");
	return 0;
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

JNIEXPORT void JNICALL Java_com_gallantrealm_mysynth_MySynthAAudio_nativeDetachCallback(JNIEnv* env, jobject obj) {
	trace(">>nativeDetachCallback");
	detachCallback = true;
	// wait till callback thread is detached
	while (thread_attached) {
		usleep(100000); // 10th of a second
	}
	trace("<<nativeDetachCallback");
}

JNIEXPORT void JNICALL Java_com_gallantrealm_mysynth_MySynthAAudio_nativeStop(JNIEnv* env, jobject obj) {
	aaudio_result_t result;
	trace(">>nativeStop");

	// Stop and close the stream
	if (stream != NULL) {
		int64_t timeoutNanos = 1000 * 1000000;
		aaudio_stream_state_t nextState = AAUDIO_STREAM_STATE_UNINITIALIZED;
		result = AAudioStream_requestStop(stream);
		result = AAudioStream_waitForStateChange(stream, AAUDIO_STREAM_STATE_STOPPING, &nextState, timeoutNanos);
		result = AAudioStream_close(stream);
		stream = NULL;
	}

	// Done with the audio stream builder, so delete it
	if (builder != NULL) {
		AAudioStreamBuilder_delete(builder);
		builder = NULL;
	}

	// Cleanup JVM
	(*env)->DeleteGlobalRef(env, g_obj);
	(*env)->DeleteGlobalRef(env, g_buffer);

	trace("<<nativeStop");
}
