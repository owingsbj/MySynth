package com.gallantrealm.mysynth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceInfo.PortInfo;
import android.media.midi.MidiManager;
import android.media.midi.MidiManager.DeviceCallback;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Handler;
import android.os.Looper;

/**
 * This subclass of MySynthMidi uses Android MIDI support for connecting to MIDI controllers and receiving MIDI
 * messages.
 */
public class MySynthMidiAndroid extends MySynthMidi {

	MidiDevice outputDevice;
	MidiReceiver midiReceiver;
	MidiOutputPort outputPort;
	DeviceCallback midiDeviceCallback;

	public MySynthMidiAndroid(Context context, MySynth synth, Callbacks callbacks) {
		super(context, synth, callbacks);

		System.out.println("MySynthMidiAndroid: Using Android MIDI");
		final MidiManager midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
		MidiDeviceInfo[] devices = midiManager.getDevices();
		if (devices != null && devices.length > 0) {
			System.out.println("MySynthMidiAndroid: MIDI Device(s) already exist");
			for (int i = 0; i < devices.length; i++) {
				onMidiDeviceAdded(devices[i]);
			}
		}
		System.out.println("MySynthMidiAndroid: Registering MidiManager.DeviceCallback");
		midiDeviceCallback = new DeviceCallback() {
			public void onDeviceAdded(MidiDeviceInfo device) {
				onMidiDeviceAdded(device);
			}
			public void onDeviceRemoved(MidiDeviceInfo device) {
				onMidiDeviceRemoved(device);
			}
		};
		midiManager.registerDeviceCallback(midiDeviceCallback, new Handler(Looper.getMainLooper()));
	}

	@Override
	public void terminate() {
		if (midiDeviceCallback != null) {
			System.out.println("MySynthMidiAndroid: Unregistering MidiManager.DeviceCallback");
			MidiManager midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
			midiManager.unregisterDeviceCallback(midiDeviceCallback);
			midiDeviceCallback = null;
		}

		if (outputPort != null && midiReceiver != null) {
			System.out.println("MySynthMidiAndroid: Removing MIDI receiver from input device port");
			outputPort.disconnect(midiReceiver);
			outputPort = null;
			midiReceiver = null;
		}
		super.terminate();
	}

	private void onMidiDeviceAdded(MidiDeviceInfo device) {
		final MidiManager midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
		System.out.println("MySynthMidiAndroid: MIDI device added: "
				+ device.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME));
		if (device.getType() == MidiDeviceInfo.TYPE_VIRTUAL) {
			System.out.println("MySynthMidiAndroid: Ignoring device as it is virtual.");
			return;
		}
		PortInfo outputPortToUse = null;
		for (PortInfo portInfo : device.getPorts()) {
			System.out.println("MySynthMidiAndroid: Device port " + portInfo.getPortNumber() + " type "
					+ ((portInfo.getType() == 1) ? "INPUT" : "OUTPUT") + " " + portInfo.getName());
			if (portInfo.getType() == PortInfo.TYPE_OUTPUT  && outputPortToUse == null) {
				// Note: output port means output of the device. It is thus input to other devices.
				// Use the first output port found, assuming it is the keyboard.
				outputPortToUse = portInfo;
			}
		}

		if (outputPortToUse != null) {
			System.out.println("MySynthMidiAndroid: Opening port " + outputPortToUse.getPortNumber());

			final int outputPortNum = (outputPortToUse != null) ? outputPortToUse.getPortNumber() : -1;

			try {
				midiManager.openDevice(device, new MidiManager.OnDeviceOpenedListener() {
					public void onDeviceOpened(MidiDevice device) {
						try {
							if (device == null) {
								System.out.println("MySynthMidiAndroid: Could not open device " + device);
							} else {
								System.out.println("MySynthMidiAndroid: Device opened.");
								outputDevice = device;
								if (outputPortNum >= 0) {
									outputPort = device.openOutputPort(outputPortNum);
									if (outputPort == null) {
										System.out.println(
												"MySynthMidiAndroid: Output port could not be opened on the device");
									} else {
										if (logMidi) {
											File file = new File(context.getExternalFilesDir(null), "midilog.txt");
											if (file.exists()) {
												file.delete();
											}
											try {
												midiLogStream = new PrintStream(file);
											} catch (FileNotFoundException e) {
												e.printStackTrace();
											}
										}
										midiReceiver = new MySynthMidiReceiver(MySynthMidiAndroid.this);
										outputPort.connect(midiReceiver);
										System.out.println("MySynthMidiAndroid: Midi receiver connected!");
										midiDeviceAttached = true;
										if (callbacks != null) {
											callbacks.onDeviceAttached(device.getInfo().getProperties()
													.getString(MidiDeviceInfo.PROPERTY_NAME));
										}
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}, new Handler(Looper.getMainLooper()));
			} catch (RuntimeException e) {
				// exceptions can happen when opening the device if the device has already been removed
				e.printStackTrace();
			}
		}
	}

	private void onMidiDeviceRemoved(MidiDeviceInfo device) {
		System.out.println("MySynthMidiAndroid: MIDI device removed: "
				+ device.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME));
		if (outputDevice != null && device.getId() == outputDevice.getInfo().getId() && outputPort != null
				&& midiReceiver != null) {
			outputPort.disconnect(midiReceiver);
			outputPort = null;
			midiReceiver = null;
			midiDeviceAttached = false;
			if (callbacks != null) {
				callbacks.onDeviceDetached(device.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME));
			}

		}
	}

}
