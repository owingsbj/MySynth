package com.gallantrealm.mysynth;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

public final class MySynthOpenSL extends MySynth {

	static final int K64 = 65536;
	static final int K32 = 32768;
	static final int K16 = 16384;
	static final int K8 = 8192;
	
	public final boolean hasLowLatencySupport;

	public final int RATE_DIVISOR;
	public final int SAMPLE_RATE;
	boolean isRunning = false;

	int currentBuffer = 1;

	final ShortBuffer nativeBuffer1;
	final ShortBuffer nativeBuffer2;
	final ShortBuffer nativeBuffer3;
	final ShortBuffer nativeBuffer4;
	final ShortBuffer nativeBuffer5;
	final ShortBuffer nativeBuffer6;
	final ShortBuffer nativeBuffer7;
	final ShortBuffer nativeBuffer8;
	final ShortBuffer nativeBuffer9;
	final ShortBuffer nativeBuffer10;
	short[] buffer1;
	short[] buffer2;
	short[] buffer3;
	short[] buffer4;
	short[] buffer5;
	short[] buffer6;
	short[] buffer7;
	short[] buffer8;
	short[] buffer9;
	short[] buffer10;
	final int samplesPerBuff;
	final int nbuffers;

	AbstractInstrument instrument;

	boolean recording;
	boolean replaying;
	int recordingIndex;
	int maxRecordingIndex;
	int RECORDING_BUFFER_SIZE;
	short[] recordBuffer;

	public class SynthThread extends Thread {

		public SynthThread() {
			super("SynthThread");
		}

		@Override
		public void run() {
			System.out.println(">>Synthread.run");
			int myPriority = android.os.Process.getThreadPriority(android.os.Process.myTid());
			System.out.println("SynthThread thread priority is " + myPriority);
			if (myPriority > -19) {
				// Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				myPriority = android.os.Process.getThreadPriority(android.os.Process.myTid());
				System.out.println("SynthThread adjusted thread priority is " + myPriority);
			}
			// not seeing performance change with setting affinity for opensl
			nativeSetAffinity(Runtime.getRuntime().availableProcessors() - 1);
			try {
				while (isRunning) { // nativeEnqueue throttles this loop
					play();
				}
			} catch (Exception e) {
				System.out.println("SynthThread interrupted");
			}
			System.out.println("<<SynthThread.run");
		}

	}

	@SuppressLint("NewApi")
	public MySynthOpenSL(Context context, int sampleRateReducer, int nbuffers) {
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
		this.nbuffers = Math.max(2, Math.min(10, nbuffers)); // minimum buffers for opensl is 2, max is 10
		SAMPLE_RATE = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) / RATE_DIVISOR;
		if (Build.VERSION.SDK_INT >= 17) {
			System.out.println("SDK is >= 17");
			PackageManager pm = context.getPackageManager();
			hasLowLatencySupport = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);
			System.out.println("Has low latency support: " + hasLowLatencySupport);
		} else {
			System.out.println("SDK is < 17, no low latency possible");
			hasLowLatencySupport = false;
		}
		System.out.println("Loading MySynthOpenSL.so..");
		try {
			System.loadLibrary("MySynthOpenSL");
			System.out.println("MySynthOpenSL.so has been loaded");
		} catch (Throwable t) {
			System.out.println("MySynthOpenSL.so failed to load");
		}
		int latency;

		// Determine optimal buffsize
		int buffsize;
		if (Build.VERSION.SDK_INT >= 17) {
			AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			String nativeSampleRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
			int framesPerBuffer = Integer.parseInt(am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
			// Note: above does not include stereo, so just * 2 for buffsize
			System.out.println("AudioManager says.. NativeSampleRate: " + nativeSampleRate + "  FramesPerBuffer: " + framesPerBuffer);
			while (framesPerBuffer < 256) {
				framesPerBuffer *= 2;
				System.out.println("FramesPerBuffer is really small.. increasing to " + framesPerBuffer);
			}
			buffsize = framesPerBuffer * 2 / RATE_DIVISOR;
		} else {
			buffsize = 1024 * 2 / RATE_DIVISOR;
		}

		latency = buffsize / 4 * 1000 / SAMPLE_RATE;
		System.out.println("MySynth samplerate = " + SAMPLE_RATE + "  stereo pcm16 buffSize = " + buffsize + "  for a latency of " + latency + "ms");
		System.out.println("Using " + nbuffers + " buffers.");

		// Allocate the buffers
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(buffsize);
		byteBuffer.order(ByteOrder.nativeOrder());
		nativeBuffer1 = byteBuffer.asShortBuffer();
		buffer1 = new short[buffsize / 2]; // nativeBuffer.capacity()];

		byteBuffer = ByteBuffer.allocateDirect(buffsize);
		byteBuffer.order(ByteOrder.nativeOrder());
		nativeBuffer2 = byteBuffer.asShortBuffer();
		buffer2 = new short[buffsize / 2]; // nativeBuffer.capacity()];

		byteBuffer = ByteBuffer.allocateDirect(buffsize);
		byteBuffer.order(ByteOrder.nativeOrder());
		nativeBuffer3 = byteBuffer.asShortBuffer();
		buffer3 = new short[buffsize / 2];

		byteBuffer = ByteBuffer.allocateDirect(buffsize);
		byteBuffer.order(ByteOrder.nativeOrder());
		nativeBuffer4 = byteBuffer.asShortBuffer();
		buffer4 = new short[buffsize / 2];

		byteBuffer = ByteBuffer.allocateDirect(buffsize);
		byteBuffer.order(ByteOrder.nativeOrder());
		nativeBuffer5 = byteBuffer.asShortBuffer();
		buffer5 = new short[buffsize / 2];

		byteBuffer = ByteBuffer.allocateDirect(buffsize);
		byteBuffer.order(ByteOrder.nativeOrder());
		nativeBuffer6 = byteBuffer.asShortBuffer();
		buffer6 = new short[buffsize / 2];

		byteBuffer = ByteBuffer.allocateDirect(buffsize);
		byteBuffer.order(ByteOrder.nativeOrder());
		nativeBuffer7 = byteBuffer.asShortBuffer();
		buffer7 = new short[buffsize / 2];

		byteBuffer = ByteBuffer.allocateDirect(buffsize);
		byteBuffer.order(ByteOrder.nativeOrder());
		nativeBuffer8 = byteBuffer.asShortBuffer();
		buffer8 = new short[buffsize / 2];

		byteBuffer = ByteBuffer.allocateDirect(buffsize);
		byteBuffer.order(ByteOrder.nativeOrder());
		nativeBuffer9 = byteBuffer.asShortBuffer();
		buffer9 = new short[buffsize / 2];

		byteBuffer = ByteBuffer.allocateDirect(buffsize);
		byteBuffer.order(ByteOrder.nativeOrder());
		nativeBuffer10 = byteBuffer.asShortBuffer();
		buffer10 = new short[buffsize / 2];

		samplesPerBuff = buffsize / 4; // nativeBuffer.capacity() / 2;
	}

	@Override
	public void setInstrument(AbstractInstrument instrument) {
		System.out.println(">>MySynthOpenSL.setInstrument");

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
		System.out.println("<<MySynthOpenSL.setInstrument");
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

	SynthThread synthThread;

	@Override
	public void start() throws Exception {
		System.out.println(">>MySynthOpenSL.start");
		if (!isRunning) {
			synthThread = new SynthThread();
			// Start up things on the native side
			System.out.println("Calling nativeStart..");
			int rc = nativeStart(SAMPLE_RATE, nativeBuffer1, nativeBuffer2, nativeBuffer3, nativeBuffer4, nativeBuffer5, nativeBuffer6, nativeBuffer7, nativeBuffer8, nativeBuffer9, nativeBuffer10, nbuffers, buffer1.length,
					hasLowLatencySupport, Build.VERSION.SDK_INT);
			if (rc == 0) {
				System.out.println("nativeStart completed successfully");
				isRunning = true;
				synthThread.start();
				System.out.println("SynthThread started");
			} else {
				System.out.println("nativeStart failed");
				throw new Exception("Synthesis failed to start (rc=" + rc + ").  There is likely a problem supporting your device.  Email support@gallantrealm.com for help. ");
			}
		}
		System.out.println("<<MySynthOpenSL.start");
	}

	@Override
	public void stop() {
		System.out.println(">>MySynthOpenSL.stop");
		if (isRunning) {
			isRunning = false;
			try {
				Thread.sleep(100); // give time for threads to stop running
			} catch (InterruptedException e) {
			}
			System.out.println("Calling nativeStop..");
			nativeStop();
			System.out.println("nativeStop completed");
			synthThread.interrupt();
			System.out.println("synthThread interrupted");
		}
		System.out.println("<<MySynthOpenSL.stop");
	}

	@Override
	public void pause() {
		System.out.println(">>MySynthOpenSL.pause");
		if (isRunning) {
			isRunning = false;
			synthThread.interrupt();
			System.out.println("synthThread interrupted");
		}
		System.out.println("<<MySynthOpenSL.pause");
	}

	@Override
	public void resume() {
		System.out.println(">>MySynthOpenSL.resume");
		if (!isRunning) {
			synthThread = new SynthThread();
			isRunning = true;
			synthThread.start();
			System.out.println("SynthThread started");
		}
		System.out.println("<<MySynthOpenSL.resume");
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
				try {
					RECORDING_BUFFER_SIZE = (SAMPLE_RATE * 2 * 60 * 1);
					recordBuffer = new short[RECORDING_BUFFER_SIZE];
				} catch (OutOfMemoryError e2) {
					return false;
				}
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

	public native int nativeStart(int sampleRate, ShortBuffer buffer1, ShortBuffer buffer2, ShortBuffer buffer3, ShortBuffer buffer4, ShortBuffer buffer5, ShortBuffer buffer6, ShortBuffer buffer7, ShortBuffer buffer8, ShortBuffer buffer9,
			ShortBuffer buffer10, int nbuffers, int bufferSize, boolean hasLowLatencySupport, int androidLevel);

	public native void nativeSetAffinity(int cpu);

	public native void nativeEnqueue(int nBuffer);

	public native void nativeStop();

	float[] output = new float[2];

	public final void play() {
		try {
			if (instrument != null && !instrument.isEditing() && instrument.isSounding()) {
				for (int i = 0; i < samplesPerBuff; i++) {
					if (!isRunning) {
						return;
					}

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

					int i2 = i * 2;
					if (currentBuffer == 1) {
						buffer1[i2] = sampleLeft;
						buffer1[i2 + 1] = sampleRight;
					} else if (currentBuffer == 2) {
						buffer2[i2] = sampleLeft;
						buffer2[i2 + 1] = sampleRight;
					} else if (currentBuffer == 3) {
						buffer3[i2] = sampleLeft;
						buffer3[i2 + 1] = sampleRight;
					} else if (currentBuffer == 4) {
						buffer4[i2] = sampleLeft;
						buffer4[i2 + 1] = sampleRight;
					} else if (currentBuffer == 5) {
						buffer5[i2] = sampleLeft;
						buffer5[i2 + 1] = sampleRight;
					} else if (currentBuffer == 6) {
						buffer6[i2] = sampleLeft;
						buffer6[i2 + 1] = sampleRight;
					} else if (currentBuffer == 7) {
						buffer7[i2] = sampleLeft;
						buffer7[i2 + 1] = sampleRight;
					} else if (currentBuffer == 8) {
						buffer8[i2] = sampleLeft;
						buffer8[i2 + 1] = sampleRight;
					} else if (currentBuffer == 9) {
						buffer9[i2] = sampleLeft;
						buffer9[i2 + 1] = sampleRight;
					} else if (currentBuffer == 10) {
						buffer10[i2] = sampleLeft;
						buffer10[i2 + 1] = sampleRight;
					}

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

				// copy to native buffer and enqueue
				if (currentBuffer == 1) {
					nativeBuffer1.rewind();
					nativeBuffer1.put(buffer1);
				} else if (currentBuffer == 2) {
					nativeBuffer2.rewind();
					nativeBuffer2.put(buffer2);
				} else if (currentBuffer == 3) {
					nativeBuffer3.rewind();
					nativeBuffer3.put(buffer3);
				} else if (currentBuffer == 4) {
					nativeBuffer4.rewind();
					nativeBuffer4.put(buffer4);
				} else if (currentBuffer == 5) {
					nativeBuffer5.rewind();
					nativeBuffer5.put(buffer5);
				} else if (currentBuffer == 6) {
					nativeBuffer6.rewind();
					nativeBuffer6.put(buffer6);
				} else if (currentBuffer == 7) {
					nativeBuffer7.rewind();
					nativeBuffer7.put(buffer7);
				} else if (currentBuffer == 8) {
					nativeBuffer8.rewind();
					nativeBuffer8.put(buffer8);
				} else if (currentBuffer == 9) {
					nativeBuffer9.rewind();
					nativeBuffer9.put(buffer9);
				} else if (currentBuffer == 10) {
					nativeBuffer10.rewind();
					nativeBuffer10.put(buffer10);
				}

				if (isRunning) {
					nativeEnqueue(currentBuffer);
				}

				if (currentBuffer == nbuffers) {
					currentBuffer = 1;
				} else {
					currentBuffer += 1;
				}

			} else { // no instrument or quiet
				currentBuffer = 1;
				try {
					Thread.currentThread().sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
//			int bufsize = buffer1.length;
//			for (int i = 0; i < bufsize; i++) {
//				buffer1[i] = (short) 0;
//			}
			}
		} catch (Throwable e) { // can happen due to instrument changes
			e.printStackTrace();
		}

	}

}
