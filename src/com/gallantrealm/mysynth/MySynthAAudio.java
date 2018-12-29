package com.gallantrealm.mysynth;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;

public final class MySynthAAudio implements MySynth {

	static final int K64 = 65536;
	static final int K32 = 32768;
	static final int K16 = 16384;
	static final int K8 = 8192;

	public final int RATE_DIVISOR;
	public final int SAMPLE_RATE;

	Callbacks callbacks;

	boolean isStarted = false;
	boolean isRunning = false;

	short[] buffer;
	final int framesPerBuffer;
	int desiredBuffsize;

	Instrument instrument;

	public boolean scopeShowing;

	boolean recording;
	boolean replaying;
	int recordingIndex;
	int maxRecordingIndex;
	int RECORDING_BUFFER_SIZE;
	short[] recordBuffer;

	@SuppressLint("NewApi")
	public MySynthAAudio(int sampleRateReducer, int nbuffers) {
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
		System.out.println("Loading ModSynthAAudio.so..");
		try {
			System.loadLibrary("ModSynthAAudio");
			System.out.println("ModSynthAAudio.so has been loaded");
		} catch (Throwable t) {
			System.out.println("ModSynthAAudio.so failed to load");
		}
		int latency;

		// Determine optimal buffsize
		Context context = ClientModel.getClientModel().getContext();
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		String nativeSampleRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
		framesPerBuffer = Integer.parseInt(am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
		// for AAudio, the framesPerBuffer is a minimum, okay to exceed it
		
		// Note: a frame is 4 bytes long (2 bytes for each channel)
		System.out.println("AudioManager says.. NativeSampleRate: " + nativeSampleRate + "  FramesPerBuffer: " + framesPerBuffer);
		desiredBuffsize = framesPerBuffer * 4 * nbuffers; // used to also divide by rate divisor ( / RATE_DIVISOR; )

		latency = desiredBuffsize / 4 * 1000 / SAMPLE_RATE;
		System.out.println("ModSynth samplerate = " + SAMPLE_RATE + "  stereo pcm16 desiredBuffSize = " + desiredBuffsize + "  for a latency of " + latency + "ms");

		// Allocate the buffer
		buffer = new short[framesPerBuffer * 4 * 25];

	}

	@Override
	public void setInstrument(Instrument instrument) {
		System.out.println(">>ModSynthAAudio.setInstrument");

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
		System.out.println("<<ModSynthAAudio.setInstrument");
	}

	@Override
	public void destroy() {
		if (instrument != null) {
			instrument.terminate();
			instrument = null;
		}
	}

	@Override
	public Instrument getInstrument() {
		return instrument;
	}

	@Override
	public void setScopeShowing(boolean scopeShowing) {
		this.scopeShowing = scopeShowing;
	}

	@Override
	public void start() throws Exception {
		if (!isStarted) {
			firstTime = true;
			// Start up things on the native side
			System.out.println("Calling nativeStart("+SAMPLE_RATE+",buffer,"+desiredBuffsize+","+(buffer.length/2)+")");
			int rc = nativeStart(SAMPLE_RATE, buffer, desiredBuffsize, buffer.length / 2);
			if (rc == 0) {
				System.out.println("nativeStart completed successfully");
				isRunning = true;
				isStarted = true;
			} else {
				System.out.println("nativeStart failed");
				throw new Exception("Synthesis failed to start (rc=" + rc + ").  There is likely a problem supporting your device.  Email support@gallantrealm.com for help. ");
			}
		}
		resume();
	}

	@Override
	public void stop() {
		pause();
		if (isStarted) {
			System.out.println("Calling nativeDetachCallback");
			nativeDetachCallback();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			System.out.println("Calling nativeStop..");
			nativeStop();
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
	public void notePress(int note, float velocity) {
		if (instrument != null) {
			instrument.notePress(note, velocity);
		}
	}

	@Override
	public void noteRelease(int note) {
		if (instrument != null) {
			instrument.noteRelease(note);
		}
	}

	@Override
	public void pitchBend(float bend) {
		if (instrument != null) {
			instrument.pitchBend(bend);
		}
	}

	@Override
	public void expression(float amount) {
		// TODO
	}

	@Override
	public void pressure(int voice, float amount) {
		if (instrument != null) {
			instrument.pressure(voice, amount);
		}
	}

	@Override
	public void pressure(float amount) {
		if (instrument != null) {
			instrument.pressure(amount);
		}
	}

	@Override
	public boolean getDamper() {
		if (instrument != null) {
			return instrument.isSustaining();
		}
		return false;
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
	public void saveRecording(String filename) throws IOException {
		write_wav(filename, maxRecordingIndex, recordBuffer);
	}

	void write_wav(String filename, int num_samples, short[] data) throws IOException {
		OutputStream wav_file;
		int num_channels;
		int bytes_per_sample;
		int byte_rate;

		num_channels = 2; /* stereo */
		bytes_per_sample = 2;

		byte_rate = SAMPLE_RATE * num_channels * bytes_per_sample;

		wav_file = new BufferedOutputStream(new FileOutputStream(filename));

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

	@Override
	public void setDamper(boolean damper) {
		if (instrument != null) {
			instrument.setSustaining(damper);
		}
	}

	@Override
	public void updateCC(int control, double value) {
		if (instrument != null && !instrument.isEditing()) {
			instrument.updateCC(control, value);
		}
	}
	
	@Override
	public void midiclock() {
		if (instrument != null && !instrument.isEditing()) {
			instrument.midiclock();
		}
	}

	@Override
	public void setCallbacks(Callbacks callbacks) {
		this.callbacks = callbacks;
	}

	public native int nativeStart(int sampleRate, short[] buffer, int desiredBufferSize, int maxBufferSize);

	public native void nativeSetAffinity(int cpu);

	public native void nativeDetachCallback();

	public native void nativeStop();

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
				nativeSetAffinity(Runtime.getRuntime().availableProcessors() -1);
			}
			firstTime = false;
		}
		if (instrument != null && !instrument.isEditing() && instrument.isSounding()) {
			try {
				for (int i = 0; i < numFrames; i++) {
					instrument.generate(output);
					double left = output[0];
					double right = output[1];

					if (replaying && recordingIndex < maxRecordingIndex) {
						left += recordBuffer[recordingIndex] / (float) K32;
						right += recordBuffer[recordingIndex + 1] / (float) K32;
					}

					left = left > 1.0 ? 1.0 : left;
					left = left < -1.0 ? -1.0 : left;
					right = right > 1.0 ? 1.0 : right;
					right = right < -1.0 ? -1.0 : right;

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

// TODO -- make scope available in mysynth
//					if (scopeShowing && outputModule.viewer != null) {
//						double scopeLevel = 0.0;
//						if (outputModule.mod1 == null) {
//							scopeLevel = left + right;
//							scopeLevel /= 4.0;
//						} else {
//							for (int voice = 0; voice < voiceCount; voice++) {
//								scopeLevel += outputModule.mod1.value[voice];
//							}
//							scopeLevel /= voiceCount * 2;
//						}
//						OutputViewer outputViewer = (OutputViewer) outputModule.viewer;
//						if (outputViewer.scope != null) {
//							outputViewer.scope.scope((float) scopeLevel);
//						}
//					}
				}
			} catch (Throwable e) { // can happen due to instrument changes
				e.printStackTrace();
			}

		} else { // no instrument or no module sounding.. saving some cpu
			int bufsize = buffer.length;
			for (int i = 0; i < bufsize; i++) {
				buffer[i] = (short) 0;
			}
		}
	}

}