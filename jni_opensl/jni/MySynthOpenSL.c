#undef __cplusplus

#include <assert.h>
#include <jni.h>
#include <string.h>
#include <math.h>
#include <stdio.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <sys/syscall.h>
#include <pthread.h>
#include <sched.h>

#include <android/log.h>
#define trace(text) __android_log_write(ANDROID_LOG_INFO, "OPENSL", text)
#define tracev(text, i) __android_log_print(ANDROID_LOG_INFO, "OPENSL", text, i)
#define tracev2(text, i, j) __android_log_print(ANDROID_LOG_INFO, "OPENSL", text, i, j)

#define boolean char
#define true 1
#define false 0

#define MAXBUFFERS 10
int nbuffers;

// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;

// buffer queue player interfaces
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;

#define MAX_SAMPLE_RATE 50000

static short* samples1;
static short* samples2;
static short* samples3;
static short* samples4;
static short* samples5;
static short* samples6;
static short* samples7;
static short* samples8;
static short* samples9;
static short* samples10;

static int androidLevel;
static int sampleRate;
static int buffSize;
static int samplesPerBuff;
static boolean hasLowLatencySupport;

/*****************************
 * The JNI entry points
 ******************************/

JNIEXPORT jint JNICALL Java_com_gallantrealm_mysynth_MySynthOpenSL_nativeStart(
		JNIEnv* env, jobject obj, int jsampleRate,
		jobject jbuffer1, jobject jbuffer2, jobject jbuffer3, jobject jbuffer4, jobject jbuffer5,
		jobject jbuffer6, jobject jbuffer7, jobject jbuffer8, jobject jbuffer9, jobject jbuffer10,
		int jnbuffers, int jbufferSize, jboolean jhasLowLatencySupport, int jandroidLevel) {
	SLresult result;
	int i;

	trace(">>nativeStart");

	sampleRate = jsampleRate;
	nbuffers = jnbuffers;
	buffSize = jbufferSize;
	hasLowLatencySupport = jhasLowLatencySupport;
	androidLevel = jandroidLevel;

	boolean isCopy = false;

	// get access to shared buffer
	samples1 = (*env)->GetDirectBufferAddress(env, jbuffer1);
	samples2 = (*env)->GetDirectBufferAddress(env, jbuffer2);
	samples3 = (*env)->GetDirectBufferAddress(env, jbuffer3);
	samples4 = (*env)->GetDirectBufferAddress(env, jbuffer4);
	samples5 = (*env)->GetDirectBufferAddress(env, jbuffer5);
	samples6 = (*env)->GetDirectBufferAddress(env, jbuffer6);
	samples7 = (*env)->GetDirectBufferAddress(env, jbuffer7);
	samples8 = (*env)->GetDirectBufferAddress(env, jbuffer8);
	samples9 = (*env)->GetDirectBufferAddress(env, jbuffer9);
	samples10 = (*env)->GetDirectBufferAddress(env, jbuffer10);

	samplesPerBuff = buffSize / 2;

	// create engine
	result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
	assert(SL_RESULT_SUCCESS == result);
	(void) result;

	// realize the engine
	result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
	assert(SL_RESULT_SUCCESS == result);
	(void) result;

	// get the engine interface, which is needed in order to create other objects
	result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
	assert(SL_RESULT_SUCCESS == result);
	(void) result;

	// create output mix, with no effects so fast path will be used
	result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
	assert(SL_RESULT_SUCCESS == result);
	(void) result;

	// realize the output mix
	result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
	assert(SL_RESULT_SUCCESS == result);
	(void) result;

	// configure audio source
	SLDataLocator_AndroidSimpleBufferQueue loc_bufq = { SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, MAXBUFFERS };
	SLDataFormat_PCM format_pcm = { SL_DATAFORMAT_PCM, //
			2, // two channels for stereo
			sampleRate * 1000, // sample rate (in millihertz)
			SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16, //
			SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, //
			SL_BYTEORDER_LITTLEENDIAN };
	SLDataSource audioSrc = { &loc_bufq, &format_pcm };

	// configure audio sink
	SLDataLocator_OutputMix loc_outmix = { SL_DATALOCATOR_OUTPUTMIX, outputMixObject };
	SLDataSink audioSnk = { &loc_outmix, NULL };

	// create audio player
	const SLInterfaceID ids1[2] = { SL_IID_BUFFERQUEUE };
	const SLboolean req1[2] = { SL_BOOLEAN_TRUE };
	result = (*engineEngine)->CreateAudioPlayer(engineEngine, &bqPlayerObject, &audioSrc, &audioSnk, 1, ids1, req1);
	assert(SL_RESULT_SUCCESS == result);
	(void) result;

	// realize the player
	result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
	assert(SL_RESULT_SUCCESS == result);
	(void) result;

	// get the play interface
	result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerPlay);
	assert(SL_RESULT_SUCCESS == result);
	(void) result;

	// get the buffer queue interface
	result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE, &bqPlayerBufferQueue);
	assert(SL_RESULT_SUCCESS == result);
	(void) result;

	// set the player's state to playing
	trace("Starting player");
	result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
	assert(SL_RESULT_SUCCESS == result);
	(void) result;

	trace("<<nativeStart");
	return 0;
}

JNIEXPORT void JNICALL Java_com_gallantrealm_mysynth_MySynthOpenSL_nativeSetAffinity(JNIEnv* env, jobject obj, int nCpu) {
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

JNIEXPORT void JNICALL Java_com_gallantrealm_mysynth_MySynthOpenSL_nativeEnqueue(JNIEnv* env, jobject obj, int nBuffer) {
//	trace(">>nativeEnqueue");
	SLresult result;
	if (bqPlayerBufferQueue != NULL) {
		SLAndroidBufferQueueState state;
		(*bqPlayerBufferQueue)->GetState(bqPlayerBufferQueue, &state);
		while (state.count >= nbuffers-1) {
			usleep(1000);
			if (bqPlayerBufferQueue == NULL) {
				state.count = 0;
			} else {
				(*bqPlayerBufferQueue)->GetState(bqPlayerBufferQueue, &state);
			}
		}
		if (bqPlayerBufferQueue != NULL) {
			if (nBuffer == 1) {
				result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples1, buffSize * 2);
			} else if (nBuffer == 2) {
				result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples2, buffSize * 2);
			} else if (nBuffer == 3) {
				result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples3, buffSize * 2);
			} else if (nBuffer == 4) {
				result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples4, buffSize * 2);
			} else if (nBuffer == 5) {
				result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples5, buffSize * 2);
			} else if (nBuffer == 6) {
				result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples6, buffSize * 2);
			} else if (nBuffer == 7) {
				result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples7, buffSize * 2);
			} else if (nBuffer == 8) {
				result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples8, buffSize * 2);
			} else if (nBuffer == 9) {
				result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples9, buffSize * 2);
			} else if (nBuffer == 10) {
				result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, samples10, buffSize * 2);
			}
		}
	}
//	trace("<<nativeEnqueue");
}

JNIEXPORT void JNICALL Java_com_gallantrealm_mysynth_MySynthOpenSL_nativeStop(JNIEnv* env, jobject obj) {
	trace(">>nativeStop");

	if (bqPlayerObject != NULL) {

		// stop playing
		trace("  stop playing");
		(*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_STOPPED);

		trace("  clear buffer queue");
		(*bqPlayerBufferQueue)->Clear(bqPlayerBufferQueue);

		// destroy buffer queue audio player object, and invalidate all associated interfaces
		trace("  destroy player object");
		(*bqPlayerObject)->Destroy(bqPlayerObject);
		bqPlayerObject = NULL;
		bqPlayerPlay = NULL;
		bqPlayerBufferQueue = NULL;
	}

	// destroy output mix object, and invalidate all associated interfaces
	if (outputMixObject != NULL) {
		trace("  destroy outputmix object");
		(*outputMixObject)->Destroy(outputMixObject);
		outputMixObject = NULL;
	}

	// destroy engine object, and invalidate all associated interfaces
	if (engineObject != NULL) {
		trace("  destroy engine");
		(*engineObject)->Destroy(engineObject);
		engineObject = NULL;
		engineEngine = NULL;
	}

	trace("<<nativeStop");
}
