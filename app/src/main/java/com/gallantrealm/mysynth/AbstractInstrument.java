package com.gallantrealm.mysynth;

import java.io.Serializable;

/**
 * Implement this abstract class to provide an implementation of your synthesizer. Most of the methods of this class are
 * used by the Keyboard control in MyAndroid or the MySynthMidi MIDI support to play the instrument. The initialize,
 * terminate, and generate methods are used by MySynth. The setEditing, isEditing, isDirty and clearDirty methods are
 * used for editing of an instrument.
 * <p>
 * <b>Important</b>: Any non-transient fields will be serialized to store the instrument.
 */
public abstract class AbstractInstrument implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Returns true if the instrument is considered dirty, or in need of saving.
	 */
	public abstract boolean isDirty();

	/**
	 * Clears the dirty flag.
	 */
	public abstract void clearDirty();

	/**
	 * Returns true if the instrument is being edited.
	 */
	public abstract boolean isEditing();

	/**
	 * Sets the editing flag. Editing is typically a rewiring or reprogramming of the instrument. An instrument that is
	 * being edited will not play while it is edited. After an instrument is edited it will be considered dirty and in
	 * need of saving.
	 */
	public abstract void setEditing(boolean edit);

	/**
	 * Initializes the instrument. Any java objects needed by the instrument should be created during this call (and not
	 * during generate or any other call).
	 * 
	 * @param sampleRate
	 *            the sample rate. This will usually be 48000 or 41000 but it can also be less if a division of the
	 *            natural sample rate was chosen when initializing MySynth.
	 */
	public abstract void initialize(int sampleRate);

	/**
	 * Terminates the instrument. Any large objects being held should be released during this call if possible.
	 */
	public abstract void terminate();

	/**
	 * Returns true if the instrument is making any sound.
	 */
	public abstract boolean isSounding();

	/**
	 * Sets the sustain (damper). This is also the same as the CC 64.
	 */
	public abstract void setSustaining(boolean sustaining);

	/**
	 * Returns true if sustain is on.
	 */
	public abstract boolean isSustaining();

	/**
	 * Presses a note.
	 * 
	 * @param note
	 *            the note number, as defined in MIDI.
	 * @param velocity
	 *            The velocity or initial pressure of the note. This can be from 0.0 to 1.0.
	 */
	public abstract void notePress(int note, float velocity);

	/**
	 * Releases a note.
	 * 
	 * @param note
	 *            the note number, as defined in MIDI.
	 */
	public abstract void noteRelease(int note);

	/**
	 * Modifies the pitch of all playing notes.
	 * 
	 * @param bend
	 *            the amound to bend, from -1.0 to 1.0. This typically bends pitch from -2 to 2 semitones.
	 */
	public abstract void pitchBend(float bend);

	/**
	 * Applies expression to all playing notes. What expression is depends on the instrument but is usually vibrato.
	 * This is also the same as CC 11.
	 * 
	 * @param amount
	 *            the amount of expression to apply, from 0.0 (none) to 1.0 (maximum).
	 */
	public abstract void expression(float amount);

	/**
	 * Applies pressure to all playing notes. This is typically from a breath controller or an aftertouch sensitive
	 * keyboard. Some controllers also emit CC 2 for this.
	 * 
	 * @param amount
	 */
	public abstract void pressure(float amount);

	/**
	 * Called to handle a control change (CC) message. See
	 * https://www.midi.org/specifications-old/item/table-3-control-change-messages-data-bytes-2 for a list of control
	 * change messages and their typical definitions in synthesizers.
	 * 
	 * @param control
	 *            the control number
	 * @param value
	 *            the value from 0.0 to 1.0 (these map to 0 to 127)
	 */
	public abstract void controlChange(int control, double value);

	/**
	 * Called to handle a MIDI clock pulse. These pulses occur 64 times a beat, or 256 times in a 4/4 measure. Any LFO's
	 * that should be synchronized with an external clock should use the midi clock pulses to perform this
	 * synchronization. If no pulses occur than a default 120bpm should be assumed.
	 */
	public abstract void midiclock();

	/**
	 * Generate a single frame of sound. This method is called repeatedly (up to 48000 times a second) to generate the
	 * synthesized sound, so it should run fast. It also should avoid creating java objects as that will cause garbage
	 * collection pauses which will lead to glitches in the sound.
	 * 
	 * @param output
	 *            an array of two floating point numbers. Write the left and right channel signals into output[0] and
	 *            output[1] respectively.
	 */
	public abstract void generate(float[] output);
}
