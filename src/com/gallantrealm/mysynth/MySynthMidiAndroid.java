package com.gallantrealm.mysynth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import android.annotation.SuppressLint;
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
 * This subclass of MySynthMidi uses Android MIDI support for connecting to MIDI controllers and receiving MIDI messages. Note that API level 23 is required for Android MIDI support.
 */
@SuppressLint("NewApi")
public class MySynthMidiAndroid extends MySynthMidi {

	MidiDevice outputDevice;
	MidiReceiver midiReceiver;
	MidiOutputPort outputPort;
	MidiManager.DeviceCallback midiDeviceCallback;

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

	private void onMidiDeviceAdded(final MidiDeviceInfo deviceInfo) {
		System.out.println("MySynthMidiAndroid: MIDI device added: " + deviceInfo.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME));
		if (deviceInfo.getOutputPortCount() > 0) {
			final MidiManager midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
			midiManager.openDevice(deviceInfo, new MidiManager.OnDeviceOpenedListener() {
				public void onDeviceOpened(MidiDevice device) {
					if (device == null) {
						System.out.println("MySynthMidiAndroid: Could not open device " + device);
						return;
					}
					for (int i = 0; i < deviceInfo.getOutputPortCount(); i++) {
						openOutputPort(device, i);
					}
				}
			}, new Handler(Looper.getMainLooper()));
		}
	}

	private void openOutputPort(MidiDevice device, int outputPortIndex) {
		System.out.println("MySynthMidiAndroid: Opening output port " + outputPortIndex);
		try {
			System.out.println("MySynthMidiAndroid: Device opened.");
			outputDevice = device;
			outputPort = device.openOutputPort(outputPortIndex);
			if (outputPort == null) {
				System.out.println("MySynthMidiAndroid: Output port could not be opened on the device");
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
				midiReceiver = new MidiReceiver() {
					byte[] messageBytes = new byte[3];
					int messageByteIndex = 0;
					int messageLength = 0;

					public void onSend(byte[] data, int offset, int count, long timestamp) throws IOException {
						for (int i = 0; i < count; i++) {
							byte b = data[offset + i];
							messageBytes[messageByteIndex] = b;
							if (messageByteIndex == 0) {
								messageLength = getMidiMessageLength(b);
							}
							messageByteIndex += 1;
							if (messageByteIndex >= messageLength) {
								if (midiLogStream != null) {
									if ((messageBytes[0] & 0xff) != 0xf8 && (messageBytes[0] & 0xff) != 0xfe) { // don't log timing clocks and active sensing
										midiLogStream.format("%02x%02x%02x\n", messageBytes[0], messageBytes[1], messageBytes[2]);
									}
								}
								processMidi(messageBytes[0], messageBytes[1], messageBytes[2]);
								messageByteIndex = 0;
								messageBytes[0] = 0;
								messageBytes[1] = 0;
								messageBytes[2] = 0;
							}
						}
					}
				};
				outputPort.connect(midiReceiver);
				System.out.println("MySynthMidiAndroid: Midi receiver connected!");
				midiDeviceAttached = true;
				if (callbacks != null) {
					callbacks.onDeviceAttached(device.getInfo().getProperties().getString(MidiDeviceInfo.PROPERTY_NAME));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void onMidiDeviceRemoved(MidiDeviceInfo device) {
		System.out.println("MySynthMidiAndroid: MIDI device removed: " + device.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME));
		if (outputDevice != null && device.getId() == outputDevice.getInfo().getId() && outputPort != null && midiReceiver != null) {
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
