package com.gallantrealm.mysynth;

import java.io.IOException;

public interface MySynth {
	
	public interface Callbacks {
		public void updateLevels();
	}
	
	void setCallbacks(Callbacks callbacks);

	void destroy();

	void setInstrument(Instrument instrument);

	Instrument getInstrument();
	
	void setScopeShowing(boolean scopeShowing);

	void start() throws Exception;

	void stop();
	
	void pause();
	
	void resume();

	void notePress(int note, float velocity);

	void noteRelease(int note);

	void pitchBend(float bend);

	void expression(float amount);

	/**
	 * This comes either from the on-screen keyboard (by moving finger up/down on key, or from a breath controller (the pressure of blowing).
	 */
	void pressure(int voice, float amount);

	/**
	 * Monophonic pressure, from breath controller.
	 */
	void pressure(float amount);

	void setDamper(boolean damper);

	boolean getDamper();

	void allSoundOff();

	boolean startRecording();

	void stopRecording();

	void playbackRecording();

	void saveRecording(String filename) throws IOException;

	int getRecordTime();

	void updateCC(int control, double value);
	
	void midiclock();

}