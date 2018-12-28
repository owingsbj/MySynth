package com.gallantrealm.mysynth;

import java.io.Serializable;

/**
 * Implement this abstract class to provide an implementation of your synthesizer.
 * Note: Any non-transient fields will be serialized to store the instrument.
 */
public abstract class Instrument implements Serializable {
	private static final long serialVersionUID = 1L;

	public abstract boolean isDirty();

	public abstract void clearDirty();
	
	public abstract boolean isEditing();

	public abstract void setEditing(boolean edit);

	public abstract void initialize(int sampleRate);

	public abstract void terminate();

	public abstract boolean isSounding();

	public abstract void generate();
}
