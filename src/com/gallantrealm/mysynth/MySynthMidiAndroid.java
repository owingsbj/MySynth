package com.gallantrealm.mysynth;

import java.io.IOException;
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
 * This subclass of MySynthMidi uses Android MIDI support for connecting to MIDI controllers and receiving MIDI messages.
 */
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
	}

	private void onMidiDeviceAdded(MidiDeviceInfo device) {
		final MidiManager midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
		System.out.println("MySynthMidiAndroid: MIDI device added: " + device.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME));
		PortInfo outputPortToUse = null;
		for (PortInfo portInfo : device.getPorts()) {
			System.out.println("MySynthMidiAndroid:   Device port " + portInfo.getPortNumber() + " type " + portInfo.getType() + " " + portInfo.getName());
			if (portInfo.getType() == PortInfo.TYPE_OUTPUT) {
				// Note: output port means output of the MIDI device. It is thus input to MySynth.
				outputPortToUse = portInfo;
			}
		}

		if (outputPortToUse != null) {
			System.out.println("MySynthMidiAndroid: Found some interesting ports, opening device..");

			final int outputPortNum = (outputPortToUse != null) ? outputPortToUse.getPortNumber() : -1;

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
									System.out.println("MySynthMidiAndroid: Output port could not be opened on the device");
								} else {
									midiReceiver = new MidiReceiver(3) {
										byte[] messageBytes = new byte[3];
										int messageByteIndex = 0;
										int messageLength = 0;
										public void onSend(byte[] data, int offset, int count, long timestamp) throws IOException {
											for (int i = 0; i < count; i++) {
												byte b = data[offset+i];
												messageBytes[messageByteIndex] = b;
												if (messageByteIndex == 0) {
													messageLength = getMidiMessageLength(b);
												}
												messageByteIndex += 1;
												if (messageByteIndex >= messageLength)  {
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
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, new Handler(Looper.getMainLooper()));
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
