package com.gallantrealm.mysynth;

import java.io.IOException;
import android.content.Context;
import android.os.Build;

public abstract class MySynth {

	/**
	 * Create the synthesizer. The natural sample rate of the device and default buffer count is used.
	 */
	public static MySynth create(Context context) {
		return MySynth.create(context, 0, 0);
	}

	/**
	 * Create the synthesizer. The sample rate can be controlled (as a division of the natural rate, and the number of buffers can be adjusted.
	 * 
	 * @param context
	 * @param sampleRateReducer
	 *            division of the sample rate. 0 is no division. 1 is division by 2. 2 is division by 4.
	 * @param nbuffers
	 *            the number of buffers. 0 means use the default number.
	 * @return
	 */
	public static MySynth create(Context context, int sampleRateReducer, int nbuffers) {
		synchronized (MySynth.class) {
			// AAudio is available in 26, but not very stable on Samsung, so enable only on >=27.
			// If it turns out to still be unstable for Samsung on 27, add a device test.
			if (Build.VERSION.SDK_INT >= 27) {
				return new MySynthAAudio(context, sampleRateReducer, nbuffers);
			} else {
				return new MySynthOpenSL(context, sampleRateReducer, nbuffers);
			}
		} 
	}

	/**
	 * Implement this interface and pass an instance of the class that implements it to MySynth.setMonitor to receive
	 * callbacks for monitoring (scope), CC changes
	 */
	public interface Callbacks {
		public void onUpdateScope(float left, float right);
	}

	Context context;
	Callbacks callbacks;

	MySynth(Context context) {
		this.context = context;
	}

	/**
	 * Sets the callbacks.  Callbacks are called frequently (once per frame) so they need to run FAST!
	 */
	public void setCallbacks(Callbacks callbacks) {
		this.callbacks = callbacks;
	}

	/**
	 * Terminates the synthesizer.  This releases any held system audio resources.
	 */
	public void terminate() {
	}

	/**
	 * Sets the instrument to play in the synthesizer.
	 * @param instrument
	 */
	public abstract void setInstrument(AbstractInstrument instrument);

	/**
	 * Gets the instrument playing in the synthesizer
	 * @return
	 */
	public abstract AbstractInstrument getInstrument();

	/**
	 * Starts the synthesizer playing from an initial state.
	 * @throws Exception
	 */
	public abstract void start() throws Exception;

	/**
	 * Stops playing the synthesizer
	 */
	public abstract void stop();

	/**
	 * Pauses playing.  Call this from the pause method of the activity controlling  the synthesizer.
	 */
	public abstract void pause();

	/**
	 * Resumes playing.  Call this from the resume method of the activity controlling the synthesizer
	 */
	public abstract void resume();

	/**
	 * Quiets all sound generation if possible.
	 */
	public abstract void allSoundOff();

	/**
	 * Starts recording the sound generated, to an internal buffer.  This clears any previous recorded sound.
	 * The maximum length of a recording is 5 minutes.
	 * @return
	 */
	public abstract boolean startRecording();

	/**
	 * Stops recording.  This will also stop playback.
	 */
	public abstract void stopRecording();

	/**
	 * Replays the recorded sound, adding it to the generated sound
	 */
	public abstract void playbackRecording();

	/**
	 * Saves the recorded sound to a file.
	 * @param filename the name of the file to save the sound.
	 * @throws IOException
	 */
	public abstract void saveRecording(String filename) throws IOException;

	/**
	 * Returns the length of the recorded sound in seconds.
	 * @return
	 */
	public abstract int getRecordTime();

}