package com.gallantrealm.mysynth;

import java.io.Serializable;

/**
 * Implement this abstract class to provide an implementation of your synthesizer.
 * Note: Any non-transient fields will be serialized to store the instrument.
 */
public abstract class AbstractInstrument implements Serializable {
	private static final long serialVersionUID = 1L;

	public abstract boolean isDirty();

	public abstract void clearDirty();
	
	public abstract boolean isEditing();

	public abstract void setEditing(boolean edit);

	public abstract void initialize(int sampleRate);

	public abstract void terminate();

	public abstract boolean isSounding();
	
	public abstract  void setSustaining(boolean sustaining);
	
	public abstract boolean isSustaining();
	
	public abstract void notePress(int note, float velocity);

	public abstract void noteRelease(int note);

	public abstract void pitchBend(float bend);
		
	public abstract void expression(float amount);

	public abstract void pressure(int voice, float amount);

	public abstract void pressure(float amount);

	public abstract void updateCC(int control, double value);
	
	public abstract void midiclock();

	public abstract void generate(float[] output);
}
