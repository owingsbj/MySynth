package jp.kshoji.driver.midi.activity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.os.Bundle;
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

/**
 * base Activity for using USB MIDI interface. In this implement, the only one device (connected at first) will be detected. launchMode must be "singleTask" or "singleInstance".
 * 
 * @author K.Shoji
 * @author BJ Owings
 */
@SuppressLint("NewApi")
public abstract class AbstractSingleMidiActivity extends Activity implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {

	/**
	 * Implementation for single device connections.
	 * 
	 * @author K.Shoji
	 */
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

			List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(getApplicationContext());

			Set<MidiInputDevice> foundInputDevices = UsbMidiDeviceUtils.findMidiInputDevices(attachedDevice, deviceConnection, deviceFilters, AbstractSingleMidiActivity.this);
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

			AbstractSingleMidiActivity.this.onDeviceAttached(attachedDevice);
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

	@Override
	protected void onStart() {
		super.onStart();
		if (!getPackageManager().hasSystemFeature("android.software.midi")) {
			System.out.println("No Android MIDI on this device");
		}
		if (getPackageManager().hasSystemFeature("android.software.midi")) {
			System.out.println("Using Android MIDI");
			final MidiManager midiManager = (MidiManager) getSystemService(Context.MIDI_SERVICE);
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
				UsbManager usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
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
				deviceConnectionWatcher = new MidiDeviceConnectionWatcher(getApplicationContext(), usbManager, deviceAttachedListener, deviceDetachedListener);
			}
		}
	}

	MidiDeviceInfo outputDevice;
	MidiReceiver midiReceiver;
	MidiOutputPort outputPort;

	private void androidMidiDeviceAdded(MidiDeviceInfo device) {
		final MidiManager midiManager = (MidiManager) getSystemService(Context.MIDI_SERVICE);
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

	@Override
	protected void onStop() {
		super.onStop();

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.stop();
		}
		deviceConnectionWatcher = null;

		if (midiInputDevice != null) {
			midiInputDevice.stop();
			midiInputDevice = null;
		}

		if (midiOutputDevice != null) {
			midiOutputDevice.stop();
			midiOutputDevice = null;
		}

		deviceConnection = null;
	}

	/**
	 * Get MIDI output device, if available.
	 * 
	 * @param usbDevice
	 * @return MidiOutputDevice, null if not available
	 */
	public final MidiOutputDevice getMidiOutputDevice() {
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.checkConnectedDevicesImmediately();
		}

		return midiOutputDevice;
	}

	/**
	 * RPN message This method is just the utility method, do not need to be implemented necessarily by subclass.
	 * 
	 * @param sender
	 * @param cable
	 * @param channel
	 * @param function
	 *            14bits
	 * @param valueMSB
	 *            higher 7bits
	 * @param valueLSB
	 *            lower 7bits. -1 if value has no LSB. If you know the function's parameter value have LSB, you must ignore when valueLSB < 0.
	 */
	public void onMidiRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
		// do nothing in this implementation
	}

	/**
	 * NRPN message This method is just the utility method, do not need to be implemented necessarily by subclass.
	 * 
	 * @param sender
	 * @param cable
	 * @param channel
	 * @param function
	 *            14bits
	 * @param valueMSB
	 *            higher 7bits
	 * @param valueLSB
	 *            lower 7bits. -1 if value has no LSB. If you know the function's parameter value have LSB, you must ignore when valueLSB < 0.
	 */
	public void onMidiNRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
		// do nothing in this implementation
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

//	public void sendMidiNoteOn(int channel, int note, int velocity) {
//		if (midiInputPort != null) {
//			byte[] data = new byte[3];
//			data[0] = (byte)0x90;
//			data[1] = (byte)note;
//			data[2] = (byte)velocity;
//			try {
//				midiInputPort.send(data,  0,  3);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}

//	public void sendMidiNoteOff(int channel, int note, int velocity) {
//		if (midiInputPort != null) {
//			byte[] data = new byte[3];
//			data[0] = (byte)0x80;
//			data[1] = (byte)note;
//			data[2] = (byte)velocity;
//			try {
//				midiInputPort.send(data,  0,  3);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}

	// TODO the logic below assumes that all messages that come in are who (no split messages)
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
	
	public void onMidiSingleByte(MidiInputDevice sender, int cable, int command) {
	}

	public void onMidiClock() {
	}

	public void onMidiSPP(int position) {
	}

}
