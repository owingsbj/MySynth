package com.gallantrealm.mysynth;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;

public final class MySynthAAudio extends MySynth {

	static final int K64 = 65536;
	static final int K32 = 32768;
	static final int K16 = 16384;
	static final int K8 = 8192;

	public final int RATE_DIVISOR;
	public final int SAMPLE_RATE;

	boolean isStarted = false;
	boolean isRunning = false;
	int jniData;  // used by native code

	short[] buffer;
	final int framesPerBuffer;
	int desiredBuffsize;

	AbstractInstrument instrument;

	boolean recording;
	boolean replaying;
	int recordingIndex;
	int maxRecordingIndex;
	int RECORDING_BUFFER_SIZE;
	short[] recordBuffer;

	@SuppressLint("NewApi")
	public MySynthAAudio(Context context, int sampleRateReducer, int nbuffers) {
		super(context);
		if (sampleRateReducer == 0) {
			RATE_DIVISOR = 1;
		} else if (sampleRateReducer == 1) {
			RATE_DIVISOR = 2;
		} else if (sampleRateReducer == 2) {
			RATE_DIVISOR = 4;
		} else {
			RATE_DIVISOR = 1;
		}
		SAMPLE_RATE = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) / RATE_DIVISOR;
		System.out.println("SAMPLE RATE = " + SAMPLE_RATE);
		if (nbuffers <= 0) {
			nbuffers = 5; // a good default
		}
		System.out.println("Loading MySynthAAudio.so..");
		try {
			System.loadLibrary("MySynthAAudio");
			System.out.println("MySynthAAudio.so has been loaded");
		} catch (Throwable t) {
			System.out.println("MySynthAAudio.so failed to load");
		}
		int latency;

		// Determine optimal buffsize
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		String nativeSampleRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
		framesPerBuffer = Integer.parseInt(am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
		// for AAudio, the framesPerBuffer is a minimum, okay to exceed it

		// Note: a frame is 4 bytes long (2 bytes for each channel)
		System.out.println("AudioManager says.. NativeSampleRate: " + nativeSampleRate + "  FramesPerBuffer: " + framesPerBuffer);
		desiredBuffsize = framesPerBuffer * 4 * nbuffers; // used to also divide by rate divisor ( / RATE_DIVISOR; )

		latency = desiredBuffsize / 4 * 1000 / SAMPLE_RATE;
		System.out.println("MySynth samplerate = " + SAMPLE_RATE + "  stereo pcm16 desiredBuffSize = " + desiredBuffsize + "  for a latency of " + latency + "ms");

		// Allocate the buffer
		buffer = new short[framesPerBuffer * 4 * 25];

	}

	@Override
	public void setInstrument(AbstractInstrument instrument) {
		System.out.println(">>MySynthAAudio.setInstrument");

		if (this.instrument != null) {
			synchronized (this.instrument) {
				this.instrument.terminate();
				this.instrument = null;
			}
		}
		synchronized (instrument) {
			instrument.initialize(SAMPLE_RATE);
		}
		this.instrument = instrument;
		System.out.println("<<MySynthAAudio.setInstrument");
	}

	@Override
	public void terminate() {
		if (instrument != null) {
			instrument.terminate();
			instrument = null;
		}
		super.terminate();
	}

	@Override
	public AbstractInstrument getInstrument() {
		return instrument;
	}

	@Override
	public void start() throws Exception {
		if (!isStarted) {
			firstTime = true;
			// Start up things on the native side
			System.out.println("Calling nativeStart(" + SAMPLE_RATE + ",buffer," + desiredBuffsize + "," + (buffer.length / 2) + ")");
			jniData = nativeStart(SAMPLE_RATE, buffer, desiredBuffsize, buffer.length / 2);
		}
		resume();
	}

	@Override
	public void stop() {
		pause();
		if (isStarted) {
			System.out.println("Calling nativeStop..");
			nativeStop(jniData);
			System.out.println("nativeStop completed");
			isStarted = false;
		}
	}

	@Override
	public void pause() {
		isRunning = false;
	}

	@Override
	public void resume() {
		isRunning = true;
	}

	@Override
	public void allSoundOff() {
	}

	@Override
	public boolean startRecording() {
		if (recordBuffer == null) {
			// allocate a recording buffer
			try {
				RECORDING_BUFFER_SIZE = (SAMPLE_RATE * 2 * 60 * 5);
				recordBuffer = new short[RECORDING_BUFFER_SIZE];
			} catch (OutOfMemoryError e) {
				return false;
			}
		}
		recording = true;
		replaying = false;
		recordingIndex = 0;
		maxRecordingIndex = 0;
		return true;
	}

	@Override
	public void stopRecording() {
		recording = false;
		replaying = false;
	}

	@Override
	public void playbackRecording() {
		recording = false;
		replaying = true;
		recordingIndex = 0;
	}

	@Override
	public void saveRecording(OutputStream outputStream) throws IOException {
		write_wav(outputStream, maxRecordingIndex, recordBuffer);
	}

	void write_wav(OutputStream wav_file, int num_samples, short[] data) throws IOException {
		int num_channels;
		int bytes_per_sample;
		int byte_rate;

		num_channels = 2; /* stereo */
		bytes_per_sample = 2;

		byte_rate = SAMPLE_RATE * num_channels * bytes_per_sample;

		/* write RIFF header */
		wav_file.write("RIFF".getBytes());
		write_little_endian(36 + bytes_per_sample * num_samples, 4, wav_file);
		wav_file.write("WAVE".getBytes());

		/* write fmt subchunk */
		wav_file.write("fmt ".getBytes());
		write_little_endian(16, 4, wav_file); /* SubChunk1Size is 16 */
		write_little_endian(1, 2, wav_file); /* PCM is format 1 */
		write_little_endian(num_channels, 2, wav_file);
		write_little_endian(SAMPLE_RATE, 4, wav_file);
		write_little_endian(byte_rate, 4, wav_file);
		write_little_endian(num_channels * bytes_per_sample, 2, wav_file); /* block align */
		write_little_endian(8 * bytes_per_sample, 2, wav_file); /* bits/sample */

		/* write data subchunk */
		wav_file.write("data".getBytes());
		write_little_endian(bytes_per_sample * num_samples, 4, wav_file);
		for (int i = 0; i < num_samples; i++) {
			write_little_endian((int) (data[i]), bytes_per_sample, wav_file);
		}

		wav_file.close();
	}

	void write_little_endian(int word, int num_bytes, OutputStream wav_file) throws IOException {
		byte b;
		while (num_bytes > 0) {
			b = (byte) (word & 0xff);
			wav_file.write(b);
			num_bytes--;
			word >>= 8;
		}
	}

	@Override
	public int getRecordTime() {
		return recordingIndex / SAMPLE_RATE / 2;
	}

	public native int nativeStart(int sampleRate, short[] buffer, int desiredBufferSize, int maxBufferSize);

	public native void nativeSetAffinity(int cpu);

	public native void nativeStop(int jniData);

	boolean firstTime = true;
	float[] output = new float[2];

	public final void playerCallback(int numFrames) {
		if (firstTime) {
			Thread.currentThread().setName("AAudioCallbackThread");
			// shouldn't be needed as AAudio gives thread max priority, but it has been reported as running on slow cores
			// so hopefully this helps
			int myPriority = android.os.Process.getThreadPriority(android.os.Process.myTid());
			System.out.println("playerCallback thread priority is " + myPriority);
			if (myPriority > -19) {
				// Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
				try {
					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				myPriority = android.os.Process.getThreadPriority(android.os.Process.myTid());
				System.out.println("playerCallback adjusted thread priority is " + myPriority);
				nativeSetAffinity(Runtime.getRuntime().availableProcessors() - 1);
			}
			firstTime = false;
		}
		try {
			if (instrument != null && !instrument.isEditing() && instrument.isSounding()) {
				for (int i = 0; i < numFrames; i++) {
					instrument.generate(output);
					float left = output[0];
					float right = output[1];

					if (replaying && recordingIndex < maxRecordingIndex) {
						left += recordBuffer[recordingIndex] / (float) K32;
						right += recordBuffer[recordingIndex + 1] / (float) K32;
					}

					left = left > 1.0f ? 1.0f : left;
					left = left < -1.0f ? -1.0f : left;
					right = right > 1.0f ? 1.0f : right;
					right = right < -1.0f ? -1.0f : right;

					short sampleLeft = (short) (left * (K32 - 1));
					short sampleRight = (short) (right * (K32 - 1));

					if (recording) {
						recordBuffer[recordingIndex] = sampleLeft;
						recordBuffer[recordingIndex + 1] = sampleRight;
					}

					buffer[2 * i] = sampleLeft;
					buffer[2 * i + 1] = sampleRight;

					if (recording || replaying) {
						if (recording && recordingIndex < RECORDING_BUFFER_SIZE - 4) {
							recordingIndex += 2;
							if (recordingIndex > maxRecordingIndex) {
								maxRecordingIndex = recordingIndex;
							}
						} else if (replaying && recordingIndex < maxRecordingIndex) {
							recordingIndex += 2;
						}
					}

					if (callbacks != null) {
						callbacks.onUpdateScope(left, right);
					}

				}
			} else { // no instrument or no module sounding.. saving some cpu
				int bufsize = buffer.length;
				for (int i = 0; i < bufsize; i++) {
					buffer[i] = (short) 0;
				}
			}
		} catch (Throwable e) { // can happen due to instrument changes
			e.printStackTrace();
		}

	}

}
