package com.gallantrealm.mysynth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import com.gallantrealm.android.Scope;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceInfo.PortInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiManager.DeviceCallback;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.thread.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;
import jp.kshoji.driver.usb.util.DeviceFilter;

public abstract class MySynth implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {
	
	public static MySynth create(Context context) {
		return MySynth.create(context, 0, 0);
	}

	public static MySynth create(Context context, int sampleRateReducer, int nbuffers) {
		MySynth synth;
		synchronized (MySynth.class) {
			// Determine if AAudio is available and stable. If so, use ModSynthAAudio. Else use ModSynthOpenSL
			if (Build.VERSION.SDK_INT >= 27) {
				synth = new MySynthAAudio(sampleRateReducer, nbuffers);
			} else {
				synth = new MySynthOpenSL(sampleRateReducer, nbuffers);
			}
		}
		synth.context = context;
		synth.midiInitialize();
		return synth;
	}
	
	public interface Callbacks {
		public void updateLevels();
	}
	
	private Context context;

	public abstract void setCallbacks(Callbacks callbacks);

	public void destroy() {
		midiTerminate();
	}

	public abstract void setInstrument(AbstractInstrument instrument);

	public abstract AbstractInstrument getInstrument();

	public abstract void setScopeShowing(boolean scopeShowing);
	
	public abstract void setScope(Scope scope);

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

	// --- MIDI ----


	final class OnMidiDeviceAttachedListenerImpl implements OnMidiDeviceAttachedListener {
		private final UsbManager usbManager;

		/**
		 * constructor
		 * 
		 * @param usbManager
		 */
		public OnMidiDeviceAttachedListenerImpl(UsbManager usbManager) {
			this.usbManager = usbManager;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener#onDeviceAttached(android.hardware.usb.UsbDevice, android.hardware.usb.UsbInterface)
		 */
		@Override
		public synchronized void onDeviceAttached(final UsbDevice attachedDevice) {

			System.out.println("USB Device attached");

			if (device != null) {
				System.out.println("  Skipping, there's already a device attached.");
				// already one device has been connected
				return;
			}

			try {
				deviceConnection = usbManager.openDevice(attachedDevice);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (deviceConnection == null) {
				System.out.println("  Failed to open the device!");
				return;
			}

			List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(context);

			Set<MidiInputDevice> foundInputDevices = UsbMidiDeviceUtils.findMidiInputDevices(attachedDevice, deviceConnection, deviceFilters, MySynth.this);
			if (foundInputDevices.size() == 0) {
				System.out.println("  No MIDI input interface was found on the device.");
				return;
			}

			midiInputDevice = (MidiInputDevice) foundInputDevices.toArray()[0];

// bjo -- skip the midi outputs since only want midi controller (for now)
//			Set<MidiOutputDevice> foundOutputDevices = UsbMidiDeviceUtils.findMidiOutputDevices(attachedDevice, deviceConnection, deviceFilters);
//			if (foundOutputDevices.size() > 0) {
//				midiOutputDevice = (MidiOutputDevice) foundOutputDevices.toArray()[0];
//			}

			Log.d(Constants.TAG, "Device " + attachedDevice.getDeviceName() + " has been attached.");

			MySynth.this.onDeviceAttached(attachedDevice);
		}
	}

	/**
	 * Implementation for single device connections.
	 * 
	 * @author K.Shoji
	 */
	final class OnMidiDeviceDetachedListenerImpl implements OnMidiDeviceDetachedListener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
		 */
		@Override
		public synchronized void onDeviceDetached(final UsbDevice detachedDevice) {

			System.out.println("USB Device detached");

			AsyncTask<UsbDevice, Void, Void> task = new AsyncTask<UsbDevice, Void, Void>() {

				@Override
				protected Void doInBackground(UsbDevice... params) {
					if (params == null || params.length < 1) {
						return null;
					}

					UsbDevice usbDevice = params[0];

					System.out.println("Handling the USB Device detached.");

					if (midiInputDevice != null) {
						midiInputDevice.stop();
						midiInputDevice = null;
					}

					if (midiOutputDevice != null) {
						midiOutputDevice.stop();
						midiOutputDevice = null;
					}

					if (deviceConnection != null) {
						deviceConnection.close();
						deviceConnection = null;
					}
					device = null;

					Log.d(Constants.TAG, "Device " + usbDevice.getDeviceName() + " has been detached.");

					Message message = Message.obtain(deviceDetachedHandler);
					message.obj = usbDevice;
					deviceDetachedHandler.sendMessage(message);
					return null;
				}
			};
			task.execute(detachedDevice);
		}
	}


	
	UsbDevice device = null;
	UsbDeviceConnection deviceConnection = null;
	MidiInputDevice midiInputDevice = null;
	MidiOutputDevice midiOutputDevice = null;
	OnMidiDeviceAttachedListener deviceAttachedListener = null;
	OnMidiDeviceDetachedListener deviceDetachedListener = null;
	Handler deviceDetachedHandler = null;
	private MidiDeviceConnectionWatcher deviceConnectionWatcher = null;

	private MidiInputPort midiInputPort;

	public boolean midiDeviceAttached;
	int myMidiChannel;
	
	MidiDeviceInfo outputDevice;
	MidiReceiver midiReceiver;
	MidiOutputPort outputPort;

	private void midiInitialize() {
		if (!context.getPackageManager().hasSystemFeature("android.software.midi")) {
			System.out.println("No Android MIDI on this device");
		}
		if (context.getPackageManager().hasSystemFeature("android.software.midi")) {
			System.out.println("Using Android MIDI");
			final MidiManager midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
			MidiDeviceInfo[] devices = midiManager.getDevices();
			if (devices != null && devices.length > 0) {
				System.out.println("MIDI Device(s) already exist");
				for (int i = 0; i < devices.length; i++) {
					androidMidiDeviceAdded(devices[i]);
				}
			}
			midiManager.registerDeviceCallback(new DeviceCallback() {
				@Override
				public void onDeviceAdded(MidiDeviceInfo device) {
					androidMidiDeviceAdded(device);
				}
				@Override
				public void onDeviceRemoved(MidiDeviceInfo device) {
					androidMidiDeviceRemoved(device);
				}
			}, new Handler(Looper.getMainLooper()));
		} else {
			if (android.os.Build.VERSION.SDK_INT >= 12) {
				UsbManager usbManager = (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE);
				deviceAttachedListener = new OnMidiDeviceAttachedListenerImpl(usbManager);
				deviceDetachedListener = new OnMidiDeviceDetachedListenerImpl();
				deviceDetachedHandler = new Handler(new Callback() {
					@Override
					public boolean handleMessage(Message msg) {
						UsbDevice usbDevice = (UsbDevice) msg.obj;
						onDeviceDetached(usbDevice);
						return true;
					}
				});

				System.out.println("Starting MidiDeviceConnectionWatcher");
				deviceConnectionWatcher = new MidiDeviceConnectionWatcher(context.getApplicationContext(), usbManager, deviceAttachedListener, deviceDetachedListener);
			}
		}
	}
	
	private void midiTerminate() {
		if (deviceConnectionWatcher != null) {
			System.out.println("Stopping MidiDeviceConnectionWatcher");
			deviceConnectionWatcher.stop();
		}
		deviceConnectionWatcher = null;

		if (outputPort != null && midiReceiver != null) {
			System.out.println("Removing MIDI receiver from input device port");
			outputPort.disconnect(midiReceiver);
			outputPort = null;
			midiReceiver = null;
		}

		if (midiInputDevice != null) {
			System.out.println("Stopping midi input device");
			midiInputDevice.stop();
			midiInputDevice = null;
		}

		if (midiOutputDevice != null) {
			System.out.println("Stopping midi output device");
			midiOutputDevice.stop();
			midiOutputDevice = null;
		}

		deviceConnection = null;
	}

	private void androidMidiDeviceAdded(MidiDeviceInfo device) {
		final MidiManager midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
		System.out.println("Establishing device connection for " + device);
		PortInfo outputPortToUse = null;
		PortInfo inputPortToUse = null;
		for (PortInfo portInfo : device.getPorts()) {
			System.out.println("  Device port " + portInfo.getPortNumber() + " type " + portInfo.getType() + " " + portInfo.getName());
			if (portInfo.getType() == PortInfo.TYPE_OUTPUT) {
				outputPortToUse = portInfo;
			} else if (portInfo.getType() == PortInfo.TYPE_INPUT) {
				inputPortToUse = portInfo;
			}
		}

		if (outputPortToUse != null || inputPortToUse != null) {
			System.out.println("Found some interesting ports, opening device..");

			final int outputPortNum = (outputPortToUse != null) ? outputPortToUse.getPortNumber() : -1;
			final int inputPortNum = (inputPortToUse != null) ? inputPortToUse.getPortNumber() : -1;

			midiManager.openDevice(device, new MidiManager.OnDeviceOpenedListener() {
				public void onDeviceOpened(MidiDevice device) {
					try {
						if (device == null) {
							System.out.println("Could not open device " + device);
						} else {
							System.out.println("Device opened.");
							if (outputPortNum >= 0) {
								outputPort = device.openOutputPort(outputPortNum);
								if (outputPort == null) {
									System.out.println("Output port could not be opened on the device");
								} else {
									midiReceiver = new MidiReceiver() {
										public void onSend(byte[] data, int offset, int count, long timestamp) throws IOException {
											processMidi(data, offset, count);
										}
									};
									outputPort.connect(midiReceiver);
									System.out.println("Midi receiver connected!");
								}
							}
							if (inputPortNum >= 0) {
								midiInputPort = device.openInputPort(inputPortNum);
								if (midiInputPort == null) {
									System.out.println("Input port could not be opened on the device");
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, new Handler(Looper.getMainLooper()));
		}
	}

	private void androidMidiDeviceRemoved(MidiDeviceInfo device) {
		System.out.println("MIDI device connection removed for " + device);
		if (device == outputDevice && outputPort != null && midiReceiver != null) {
			outputPort.disconnect(midiReceiver);
			outputPort = null;
			midiReceiver = null;
		}
	}

	ByteArrayOutputStream systemExclusive = null;

	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	private byte[] data;
	private int offset;
	private int endOffset;

	private byte getByte() {
		byte b = 0;
		if (offset < endOffset) {
			b = (byte) (data[offset] & 0xff);
			offset++;
		}
		return b;
	}

	// TODO the logic below assumes that all messages that come in are whole (no split messages)
	// it can handle multiple messages in one process call.
	private void processMidi(byte[] data_arg, int offset_arg, int length_arg) {
		if (length_arg == 0) {
			System.out.println("USELESS LOOPING");
		} else {
			data = data_arg;
			offset = offset_arg;
			endOffset = offset_arg + length_arg;

//			System.out.print("MIDI data received: ");
//			for (int i = 0; i < length_arg; i++) {
//				System.out.print(HEX_DIGITS[(0xF0 & data[offset + i]) >>> 4]);
//				System.out.print(HEX_DIGITS[0x0F & data[offset + i]]);
//			}
//			System.out.println();

			while (offset < endOffset) {
				int code = data[offset] & 0xff;
				int codeIndexNumber = (code >> 4) & 0xf;
				int channel = (code & 0xf) + 1;
				offset++;
				switch (codeIndexNumber) {
				case 0:
					onMidiMiscellaneousFunctionCodes(null, channel, getByte(), getByte(), getByte());
					break;
				case 1:
					onMidiCableEvents(null, channel, getByte(), getByte(), getByte());
					break;
				case 2:
				// system common message with 2 bytes
				{
					byte[] bytes = new byte[] { getByte(), getByte() };
					onMidiSystemCommonMessage(null, channel, bytes);
				}
					break;
				case 3:
				// system common message with 3 bytes
				{
					byte[] bytes = new byte[] { getByte(), getByte(), getByte() };
					onMidiSystemCommonMessage(null, channel, bytes);
				}
					break;
				case 4:
					// sysex starts, and has next
					synchronized (this) {
						if (systemExclusive == null) {
							systemExclusive = new ByteArrayOutputStream();
						}
						systemExclusive.write(getByte());
						systemExclusive.write(getByte());
						systemExclusive.write(getByte());
					}
					break;
				case 5:
					// system common message with 1byte
					// sysex end with 1 byte
					synchronized (this) {
						if (systemExclusive == null) {
							byte[] bytes = new byte[] { getByte() };
							onMidiSystemCommonMessage(null, channel, bytes);
						} else {
							systemExclusive.write(getByte());
							onMidiSystemExclusive(null, channel, systemExclusive.toByteArray());
							systemExclusive = null;
						}
					}
					break;
				case 6:
					// sysex end with 2 bytes
					synchronized (this) {
						if (systemExclusive != null) {
							systemExclusive.write(getByte());
							systemExclusive.write(getByte());
							onMidiSystemExclusive(null, channel, systemExclusive.toByteArray());
							systemExclusive = null;
						}
					}
					break;
				case 7:
					// sysex end with 3 bytes
					synchronized (this) {
						if (systemExclusive != null) {
							systemExclusive.write(getByte());
							systemExclusive.write(getByte());
							systemExclusive.write(getByte());
							onMidiSystemExclusive(null, channel, systemExclusive.toByteArray());
							systemExclusive = null;
						}
					}
					break;
				case 8:
					onMidiNoteOff(null, 0, channel, getByte() & 0x7F, getByte() & 0x7F);
					break;
				case 9: {
					int note = getByte() & 0x7F;
					int velocity = getByte() & 0x7F;
					if (velocity == 0) {
						onMidiNoteOff(null, 0, channel, note, velocity);
					} else {
						onMidiNoteOn(null, 0, channel, note, velocity);
					}
				}
					break;
				case 10:
					// poly key press
					onMidiPolyphonicAftertouch(null, 0, channel, getByte() & 0x7F, getByte() & 0x7F);
					break;
				case 11:
					int c = getByte();
					int v = getByte();
					if (c < 120) {
						// control change
						onMidiControlChange(null, 0, channel, c, v);
					} else {
						// channel mode messages
						if (c == 120) { // all sound off
							System.out.println("onMidi Channel mode message: all sound off -- unsupported.");
						} else if (c == 121) { // reset all controllers
							System.out.println("onMidi Channel mode message: reset all controllers -- unsupported.");
						} else if (c == 122) { // local control
							System.out.println("onMidi Channel mode message: local control -- unsupported.");
						} else if (c == 123) { // all notes off
							System.out.println("onMidi Channel mode message: all notes off -- unsupported.");
						} else if (c == 124) { // omni mode off
							System.out.println("onMidi Channel mode message: omni mode off -- unsupported.");
						} else if (c == 125) { // omni mode on
							System.out.println("onMidi Channel mode message: omni mode on -- unsupported.");
						} else if (c == 126) { // mono mode on
							System.out.println("onMidi Channel mode message: mono mode on -- unsupported.");
						} else if (c == 127) { // poly mode on
							System.out.println("onMidi Channel mode message: poly mode on -- unsupported.");
						}
					}
					break;
				case 12:
					// program change
					onMidiProgramChange(null, 0, channel, getByte());
					break;
				case 13:
					// channel pressure
					onMidiChannelAftertouch(null, 0, channel, getByte());
					break;
				case 14:
					// pitch bend
					onMidiPitchWheel(null, 0, channel, getByte() | (getByte() << 7));
					break;
				case 15:
					onMidiSingleByte(null, 0, code);
					if (code == 0xf0) { // system exclusive
						System.out.println("onMidiSysEx -- unsupported");
					} else if (code == 0xf1) { // midi time code quarter frame
						System.out.println("onMidiMTC: " + getByte() + " -- unsupported");
					} else if (code == 0xf2) { // song position pointer
						onMidiSPP(getByte() + (getByte() << 7));
					} else if (code == 0xf3) { // song select
						int song = getByte();
						System.out.println("onMidiSongSelect " + song + " -- unsupported");
					} else if (code == 0xf4) { // undefined
					} else if (code == 0xf5) { // undefined
					} else if (code == 0xf6) { // tune request
						System.out.println("onMidiTuneRequest -- unsupported");
					} else if (code == 0xf7) { // end of system exclusive
						System.out.println("onMidiEndSysEx -- unsupported");
					} else if (code == 0xf8) { // timing clock
						onMidiClock();
					} else if (code == 0xf9) { // undefined
					} else if (code == 0xfa) { // start sequence playing
						System.out.println("onMidiStartSequence -- unsupported");
					} else if (code == 0xfb) { // continue sequence playing
						System.out.println("onMidiContinueSequence -- unsupported");
					} else if (code == 0xfc) { // stop sequence playing
						System.out.println("onMidiStopSequence -- unsupported");
					} else if (code == 0xfd) { // undefined
					} else if (code == 0xfe) { // active sensing
						System.out.println("onMidiActiveSensing -- unsupported");
					} else if (code == 0xff) { // reset
						System.out.println("onMidiReset -- unsupported");
					}
					break;
				default:
					// do nothing.
					break;
				}
			}
		}
	}
	


	public void setMidiChannel(int midiChannel) {
		this.myMidiChannel = midiChannel;
	}

	public boolean isMidiDeviceAttached() {
		return midiDeviceAttached;
	}

	@Override
	public void onDeviceAttached(UsbDevice usbDevice) {
		System.out.println("USB Device Attached: Vendor:" + usbDevice.getVendorId() + " Device: " + usbDevice.getDeviceId());
		// TODO RE-ENABLE
//		keyboardPane.setVisibility(View.GONE);
		midiDeviceAttached = true;
	}

	@Override
	public void onDeviceDetached(UsbDevice usbDevice) {
		System.out.println("USB Device Detached: Vendor:" + usbDevice.getVendorId() + " Device: " + usbDevice.getDeviceId());
		// TODO RE-ENABLE
//		keyboardPane.setVisibility(View.VISIBLE);
		midiDeviceAttached = false;
	}

	@Override
	public void onMidiMiscellaneousFunctionCodes(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
	}

	@Override
	public void onMidiCableEvents(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
	}

	@Override
	public void onMidiSystemCommonMessage(MidiInputDevice sender, int cable, byte[] bytes) {
	}

	@Override
	public void onMidiSystemExclusive(MidiInputDevice sender, int cable, byte[] systemExclusive) {
	}

	@Override
	public void onMidiNoteOn(MidiInputDevice sender, int cable, int channel, int midinote, int midivelocity) {
		if (midivelocity == 0) {
			onMidiNoteOff(sender, cable, channel, midinote, midivelocity);
			return;
		}
		if (myMidiChannel != 0 && channel != myMidiChannel) {
			return;
		}
		// System.out.println("MIDI Note On: " + midinote + " velocity " + midivelocity);
		float velocity = FastMath.min(1.0f, (midivelocity + 1) / 128.0f); // TODO add a sensitivity option
		notePress(midinote, velocity);
	}

	@Override
	public void onMidiNoteOff(MidiInputDevice sender, int cable, int channel, int midinote, int midivelocity) {
		if (myMidiChannel != 0 && channel != myMidiChannel) {
			return;
		}
		// System.out.println("MIDI Note Off: " + midinote + " velocity " + midivelocity);
		noteRelease(midinote);
	}

	@Override
	public void onMidiPolyphonicAftertouch(MidiInputDevice sender, int cable, int channel, int midinote, int pressure) {
		if (myMidiChannel != 0 && channel != myMidiChannel) {
			return;
		}
		// System.out.println("MIDI Poly Aftertouch: " + midinote + " pressure " + pressure);
	}

	@Override
	public void onMidiControlChange(MidiInputDevice sender, int cable, int channel, int function, int value) {
		if (myMidiChannel != 0 && channel != myMidiChannel) {
			return;
		}
		// System.out.println("MIDI CC " + function + " " + value);
		if (function != 0) {
			updateCC(function, value / 127.0f);
		}
// TODO RE-ENABLE
//		MidiControlDialog.controlChanged(this, function);

//		if (function == 0) { // ?
//		} else if (function == 1) { // modulation amount
//			// float currentVibratoAmount = progress * progress / 25000.0f;
//			synth.expression(value / 128.0f);
//		} else if (function == 2) { // breath controller
//			synth.pressure(0, value); // assumed monophonic
//		} else if (function == 3) { // chorus (pulse) width
//			// ((Instrument)synth.getInstrument()).chorusWidth = value / 128.0f;
//			// TODO determine what to do with pulse width
//			updateControls();
//		} else if (function == 7) { // overall volume
//			AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//			am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) ((value / 128.0f) * am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);
//		} else if (function == 10) { // pan
//			// doesn't exist
//		} else if (function == 14) { // amp level
//			AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//			am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) ((value / 128.0f) * am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);
//		} else if (function == 16) { // vibrato rate
//			// synth.vibratoRate = value;
//			updateControls();
//		} else if (function == 17) { // tremelo rate (actually lfo2)
//			// doesn't exist
//		} else if (function == 18) { // vibrato amount
//			// synth.vibratoAmount = value / 128.0f;
//			updateControls();
//		} else if (function == 22) { // tremelo amount (actually lfo2)
//			// doesn't exist
//		} else if (function == 28) { // filter sustain
//			// synth.filterEnvSustain = value / 128.0f;
//			updateControls();
//		} else if (function == 29) { // filter release
//			// synth.filterEnvRelease = value;
//			updateControls();
//		} else if (function == 31) { // amp sustain
//			// synth.ampSustain = value / 128.0f;
//			updateControls();
//		} else if (function == 64) { // damper pedal
//			if (value > 0) {
//				synth.damper(true);
//			} else {
//				synth.damper(false);
//			}
//		} else if (function == 70) {
//
//		} else if (function == 71) { // filter resonance
//			// synth.filterResonance = value / 128.0f;
//			updateControls();
//		} else if (function == 72) { // amp release
//			// synth.ampRelease = value;
//			updateControls();
//		} else if (function == 73) { // amp attack
//			// synth.ampAttack = value;
//			updateControls();
//		} else if (function == 74) { // filter cutoff
//			// synth.filterLow = value / 128.0f;
//			updateControls();
//		} else if (function == 75) { // amp decay
//			// synth.ampDecay = value;
//			updateControls();
//		} else if (function == 76) { // vibrato rate
//			// synth.vibratoRate = value / 128.0f;
//			updateControls();
//		} else if (function == 77) { // vibrato amount
//			// synth.vibratoAmount = value / 128.0f;
//			updateControls();
//		} else if (function == 78) { // vibrato attack
//			// synth.vibratoAttack = value;
//			updateControls();
//		} else if (function == 81) { // filter depth
//			// synth.filterHigh = value / 128.0f;
//			updateControls();
//		} else if (function == 82) { // filter attack
//			// synth.filterEnvAttack = value;
//			updateControls();
//		} else if (function == 83) { // filter decay
//			// synth.filterEnvDecay = value;
//			updateControls();
//		} else if (function == 91) { // reverb amount
//			// synth.echoFeedback = value / 128.0f;
//			updateControls();
//		} else if (function == 93) { // chorus amount
//			// synth.echoAmount = value / 128.0f;
//			updateControls();
//		}
//
//		// Special commands
//		else if (function == 120) { // all sound off
//			synth.allSoundOff();
//		} else if (function == 121) { // reset all controllers
//			// not implemented
//		} else if (function == 122) { // local control
//			// not implemented
//		} else if (function == 123) { // all notes off
//			// todo
//		} else if (function == 124) { // omni mode off
//			// not supported
//		} else if (function == 125) { // omni mode on
//			// the default
//		} else if (function == 126) { // mono mode on
//			// todo
//		} else if (function == 127) { // poly mode on
//			// todo
//		}
	}

	@Override
	public void onMidiProgramChange(MidiInputDevice sender, int cable, int channel, int program) {
		if (myMidiChannel != 0 && channel != myMidiChannel) {
			return;
		}
		System.out.println("MIDI Program Change: " + program);
		// TODO RE-ENABLE
//		loadProgram(program + 1);
	}

	@Override
	public void onMidiChannelAftertouch(MidiInputDevice sender, int cable, int channel, int midipressure) {
		if (myMidiChannel != 0 && channel != myMidiChannel) {
			return;
		}
		// System.out.println("MIDI Channel Aftertouch: " + channel + " " + midipressure);
		float pressure = FastMath.min(1.0f, (midipressure + 1) / 128.0f); // TODO add a sensitivity option
		pressure(pressure);
	}

	@Override
	public void onMidiPitchWheel(MidiInputDevice sender, int cable, int channel, int amount) {
		if (myMidiChannel != 0 && channel != myMidiChannel) {
			return;
		}
		pitchBend((amount - 8192) / 8192.0f);
	}

	@Override
	public void onMidiRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
		// do nothing in this implementation
	}

	@Override
	public void onMidiNRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
		// do nothing in this implementation
	}

	public void onMidiSingleByte(MidiInputDevice sender, int cable, int command) {
	}

	public void onMidiClock() {
	}

	public void onMidiSPP(int position) {
	}


}