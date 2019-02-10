package com.gallantrealm.mysynth;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import android.content.Context;

/**
 * MIDI support for MySynth. This is an abstract class. There are two concrete subclasses: one to use the Android MIDI
 * classes (available in Android 6.0+) and the other using USB device API directly.
 */
public abstract class MySynthMidi {

	/**
	 * Callbacks for MIDI events not handled by calling the MySynth synthesizer directly.
	 */
	public interface Callbacks {
		public void onDeviceAttached(String deviceName);
		public void onDeviceDetached(String deviceName);
		public void onProgramChange(int programNum);
		public void onControlChange(int control, int value);
		public void onTimingClock();
		public void onSysex(byte[] data);
	}

	/**
	 * Creates an instance of MySynthMidi based on the availabilty of MIDI support.
	 * 
	 * @param context
	 *            the application context
	 * @param synth
	 *            the synthesizer to be controlled by MIDI
	 * @param callbacks
	 *            a callbacks class
	 */
	public static MySynthMidi create(Context context, MySynth synth, Callbacks callbacks) {
		if (context.getPackageManager().hasSystemFeature("android.software.midi")) {
			return new MySynthMidiAndroid(context, synth, callbacks);
		} else {
			return new MySynthMidiUSB(context, synth, callbacks);
		}
	}

	Context context;
	MySynth synth;
	boolean midiDeviceAttached;
	int midiChannel;
	Callbacks callbacks;
	boolean logMidi;
	PrintStream midiLogStream;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            the application context
	 * @param synth
	 *            the synthesizer to be controlled by MIDI
	 * @param callbacks
	 *            a callbacks class
	 */
	public MySynthMidi(Context context, MySynth synth, Callbacks callbacks) {
		this.context = context;
		this.synth = synth;
		this.callbacks = callbacks;
	}

	/**
	 * Terminates the MIDI support. This should be called when the app is stopping or closing.
	 */
	public void terminate() {
		if (midiLogStream != null) {
			midiLogStream.close();
		}
	}

	/**
	 * Returns the MIDI channel being listened to.
	 */
	public int getMidiChannel() {
		return midiChannel;
	}

	/**
	 * Sets the MIDI channel to listen to, or zero for any channel.
	 */
	public void setMidiChannel(int midiChannel) {
		this.midiChannel = midiChannel;
	}
	
	/**
	 * Returns true if midi messages are being logged to midilog.txt.
	 */
	public boolean isLogMidi() {
		return logMidi;
	}

	/**
	 * If set to true, midi messages are logged in a file called midilog.txt.  This can
	 * be useful for debugging issues with support of  midi controllers.
	 */
	public void setLogMidi(boolean logMidi) {
		this.logMidi = logMidi;
	}

	/**
	 * Returns true if a MIDI controller is currently attached to the app.
	 */
	public boolean isMidiDeviceAttached() {
		return midiDeviceAttached;
	}

	// System exclusive message data spans multiple midi messages. It is collected in the following stream.
	ByteArrayOutputStream systemExclusive = null;

	/**
	 * Process a midi message. The message will be one, two or three bytes long. Subclasses call this method. They
	 * determine the message length when parsing midi and pad extra bytes (with zeros)
	 */
	void processMidi(byte byte1, byte byte2, byte byte3) {
		int codeIndexNumber = ((int) byte1 >> 4) & 0x0f;
		int channel = ((int) byte1 & 0x0f) + 1;
		switch (codeIndexNumber) {
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7: {
			if (systemExclusive != null) {
				systemExclusive.write(byte1);
			} else {
				System.out.println("Unexpected data byte: " + Integer.toString(byte1 & 0xff, 16));
			}
			break;
		}
		case 8: // note off
		{
			if (midiChannel != 0 && channel != midiChannel) {
				return;
			}
			int midiNote = byte2 & 0x7f;
			AbstractInstrument instrument = synth.getInstrument();
			if (instrument != null) {
				instrument.noteRelease(midiNote);
			}
			break;
		}
		case 9: // note on
		{
			if (midiChannel != 0 && channel != midiChannel) {
				return;
			}
			int midiNote = byte2 & 0x7F;
			int midiVelocity = byte3 & 0x7F;
			AbstractInstrument instrument = synth.getInstrument();
			if (instrument != null) {
				if (midiVelocity == 0) {
					instrument.noteRelease(midiNote);
				} else {
					float velocity = Math.min(1.0f, midiVelocity / 127.0f);
					instrument.notePress(midiNote, velocity);
				}
			}
			break;
		}
		case 10: // polyphonic key pressure (aftertouch)
		{
			if (midiChannel != 0 && channel != midiChannel) {
				return;
			}
			int midiNote = byte2 & 0x7F;
			int midiPressure = byte3 & 0x7F;
			float pressure = Math.min(1.0f, midiPressure / 127.0f);
			System.out.println("MySynthMidi: Polyphonic key pressure (aftertouch) -- unsupported.");
			break;
		}
		case 11: // control change
		{
			if (midiChannel != 0 && channel != midiChannel) {
				return;
			}
			int c = byte2 & 0x7f;
			int v = byte3 & 0x7f;
			if (c == 0 || c == 32) {
				// bank select, ignored
			} else if  (c < 120) {
				AbstractInstrument instrument = synth.getInstrument();
				if (instrument != null) {
					if (c == 64) {
						instrument.setSustaining(v >= 64);
					}
					if (c == 11) {
						instrument.expression(v / 127.0f);
					}
				}
				if (callbacks != null) {
					callbacks.onControlChange(c, v);
				}
			} else {
				// channel mode messages
				if (c == 120) { // all sound off
					System.out.println("MySynthMidi: Channel mode message: all sound off -- unsupported.");
				} else if (c == 121) { // reset all controllers
					System.out.println("MySynthMidi: Channel mode message: reset all controllers -- unsupported.");
				} else if (c == 122) { // local control
					System.out.println("MySynthMidi: Channel mode message: local control -- unsupported.");
				} else if (c == 123) { // all notes off
					synth.allSoundOff();
				} else if (c == 124) { // omni mode off
					System.out.println("MySynthMidi: Channel mode message: omni mode off -- unsupported.");
				} else if (c == 125) { // omni mode on
					System.out.println("MySynthMidi: Channel mode message: omni mode on -- unsupported.");
				} else if (c == 126) { // mono mode on
					System.out.println("MySynthMidi: Channel mode message: mono mode on -- unsupported.");
				} else if (c == 127) { // poly mode on
					System.out.println("MySynthMidi: Channel mode message: poly mode on -- unsupported.");
				}
			}
			break;
		}
		case 12: // program change
		{
			if (midiChannel != 0 && channel != midiChannel) {
				return;
			}
			int program = byte2 + 1;
			System.out.println("MySynthMidi: Program Change: " + (program));
			if (callbacks != null) {
				callbacks.onProgramChange(program);
			}
			break;
		}
		case 13: // channel pressure
		{
			if (midiChannel != 0 && channel != midiChannel) {
				return;
			}
			float pressure = Math.min(1.0f, byte2 / 127.0f);
			AbstractInstrument instrument = synth.getInstrument();
			if (instrument != null) {
				instrument.pressure(pressure);
			}
			break;
		}
		case 14: // pitch bend
		{
			if (midiChannel != 0 && channel != midiChannel) {
				return;
			}
			int l = byte2 & 0x7f;
			int m = byte3 & 0x7f;
			int midiAmount = l | (m << 7);
			float amount = (midiAmount - 8192) / 8192.0f;
			AbstractInstrument instrument = synth.getInstrument();
			if (instrument != null) {
				instrument.pitchBend(amount);
			}
			break;
		}
		case 15: // system common and system realtime messages
		{
			int code = ((int) byte1) & 0xff;
			if (code == 0xf0) { // start of system exclusive
				systemExclusive = new ByteArrayOutputStream();
			} else if (code == 0xf1) { // midi time code quarter frame
				System.out.println("MySynthMidi: onMidiMTC: " + byte1 + " -- unsupported");
			} else if (code == 0xf2) { // song position pointer
			} else if (code == 0xf3) { // song select
				int song = byte2;
				System.out.println("MySynthMidi: onMidiSongSelect " + song + " -- unsupported");
			} else if (code == 0xf4) { // undefined
			} else if (code == 0xf5) { // undefined
			} else if (code == 0xf6) { // tune request
				System.out.println("MySynthMidi: onMidiTuneRequest -- unsupported");
			} else if (code == 0xf7) { // end of system exclusive
				if (callbacks != null) {
					callbacks.onSysex(systemExclusive.toByteArray());
				}
			} else if (code == 0xf8) { // timing clock
				if (callbacks != null) {
					callbacks.onTimingClock();
				}
			} else if (code == 0xf9) { // undefined
			} else if (code == 0xfa) { // start sequence playing
				System.out.println("MySynthMidi: onMidiStartSequence -- unsupported");
			} else if (code == 0xfb) { // continue sequence playing
				System.out.println("MySynthMidi: onMidiContinueSequence -- unsupported");
			} else if (code == 0xfc) { // stop sequence playing
				System.out.println("MySynthMidi: onMidiStopSequence -- unsupported");
			} else if (code == 0xfd) { // undefined
			} else if (code == 0xfe) { // active sensing
				System.out.println("MySynthMidi: onMidiActiveSensing -- unsupported");
			} else if (code == 0xff) { // reset
				System.out.println("MySynthMidi: onMidiReset -- unsupported");
			}
			break;
		}
		default: // do nothing.
			break;
		}
	}

	/**
	 * Returns the required length of a midi message based on the first byte
	 */
	int getMidiMessageLength(byte byte1) {
		int codeIndexNumber = ((int) byte1 >> 4) & 0x0f;
		int codeSubNumber = ((int) byte1) & 0x0f;
		switch (codeIndexNumber) {
		case 8: // note off
		case 9: // note on
		case 10: // polyphonic key pressure
		case 11: // control change
			return 3;
		case 12: // program change
		case 13: // channel pressure
			return 2;
		case 14: // pitch bend
			return 3;
		case 15: // 15
			switch (codeSubNumber) {
			case 0: // system exclusive
				return 2; // minimally
			case 1:
				return 2;
			case 2:
				return 3;
			case 3:
				return 2;
			}
		}
		return 1; // all else
	}

}
