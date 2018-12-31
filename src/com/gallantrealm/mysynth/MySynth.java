package com.gallantrealm.mysynth;

import java.io.IOException;
import android.os.Build;

public abstract class MySynth {

	public static MySynth create() {
		return MySynth.create(0, 0);
	}
	
	public static MySynth create(int sampleRateReducer, int nbuffers) {
		MySynth synth;
		synchronized (MySynth.class) {
			// Determine if AAudio is available and stable. If so, use ModSynthAAudio. Else use ModSynthOpenSL
			if (Build.VERSION.SDK_INT >= 27) {
				synth = new MySynthAAudio(sampleRateReducer, nbuffers);
			} else {
				synth = new MySynthOpenSL(sampleRateReducer, nbuffers);
			}
		}
		return synth;
	}

	public interface Callbacks {
		public void updateLevels();
	}

	public abstract void setCallbacks(Callbacks callbacks);

	public abstract void destroy();

	public abstract void setInstrument(AbstractInstrument instrument);

	public abstract AbstractInstrument getInstrument();

	public abstract void setScopeShowing(boolean scopeShowing);

	public abstract void start() throws Exception;

	public abstract void stop();

	public abstract void pause();

	public abstract void resume();

	public abstract void notePress(int note, float velocity);

	public abstract void noteRelease(int note);

	public abstract void pitchBend(float bend);

	public abstract void expression(float amount);

	/**
	 * This comes either from the on-screen keyboard (by moving finger up/down on key, or from a breath controller (the pressure of blowing).
	 */
	public abstract void pressure(int voice, float amount);

	/**
	 * Monophonic pressure, from breath controller.
	 */
	public abstract void pressure(float amount);

	public abstract void setDamper(boolean damper);

	public abstract boolean getDamper();

	public abstract void allSoundOff();

	public abstract boolean startRecording();

	public abstract void stopRecording();

	public abstract void playbackRecording();

	public abstract void saveRecording(String filename) throws IOException;

	public abstract int getRecordTime();

	public abstract void updateCC(int control, double value);

	public abstract void midiclock();

}