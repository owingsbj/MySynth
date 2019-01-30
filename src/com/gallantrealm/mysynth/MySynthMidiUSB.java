package com.gallantrealm.mysynth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;
import jp.kshoji.driver.usb.util.DeviceFilter;

/**
 * This subclass of MySynthMidi uses general Android USB support for connecting to MIDI controllers and receiving MIDI messages.
 */
public class MySynthMidiUSB extends MySynthMidi {

	private UsbDeviceConnectionThread deviceConnectThread;
	final HashMap<String, UsbDevice> grantedDeviceMap;
	final Queue<UsbDevice> deviceGrantQueue;
	volatile boolean isGranting;

	public MySynthMidiUSB(Context context, MySynth synth, Callbacks callbacks) {
		super(context, synth, callbacks);

		System.out.println("MySynthMidiUSB: Starting MidiDeviceConnectionWatcherThread");
		deviceGrantQueue = new LinkedList<UsbDevice>();
		isGranting = false;
		grantedDeviceMap = new HashMap<String, UsbDevice>();
		deviceConnectThread = new UsbDeviceConnectionThread(context.getApplicationContext(), (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE));
		deviceConnectThread.start();
	}

	@Override
	public void terminate() {

		// stop the device watcher thread
		if (deviceConnectThread != null) {
			deviceConnectThread.stopFlag = true;
			while (deviceConnectThread.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			deviceConnectThread = null;
		}

		if (usbMidiDeviceDispatchThread != null) {
			System.out.println("MySynthMidiUSB: Stopping midi input device");
			usbMidiDeviceDispatchThread.stopFlag = true;
			while (usbMidiDeviceDispatchThread.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			usbMidiDeviceDispatchThread = null;
		}

		usbDeviceConnection = null;
	}

	public void checkConnectedDevicesImmediately() {
		deviceConnectThread.checkConnectedDevices();
	}

	/**
	 * Broadcast receiver for MIDI device connection granted
	 */
	private final class UsbDevicePermissionGrantedReceiver extends BroadcastReceiver {
		private static final String USB_PERMISSION_GRANTED_ACTION = "jp.kshoji.driver.midi.USB_PERMISSION_GRANTED_ACTION";

		private final String deviceName;
		private final UsbDevice device;

		public UsbDevicePermissionGrantedReceiver(String deviceName, UsbDevice device) {
			this.deviceName = deviceName;
			this.device = device;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (USB_PERMISSION_GRANTED_ACTION.equals(action)) {
				boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
				if (granted) {
					if (device != null) {
						grantedDeviceMap.put(deviceName, device);
						MySynthMidiUSB.this.onUsbDeviceAttached(device);
					}
				} else {
					// reset the 'isGranting' to false
					notifyDeviceGranted();
				}
			}
		}
	}

	/**
	 * This thread is used to wait for USB device connections, then determine if they are MIDI and if so to start dispatching of MIDI messages.
	 */
	private final class UsbDeviceConnectionThread extends Thread {
		private Context context;
		private UsbManager usbManager;
		private Set<String> connectedDeviceNameSet;
		private Set<String> removedDeviceNames;
		boolean stopFlag;
		private List<DeviceFilter> deviceFilters;

		UsbDeviceConnectionThread(Context context, UsbManager usbManager) {
			super("MidiDeviceConnectionThread");
			this.context = context;
			this.usbManager = usbManager;
			connectedDeviceNameSet = new HashSet<String>();
			removedDeviceNames = new HashSet<String>();
			stopFlag = false;
			deviceFilters = DeviceFilter.getDeviceFilters(context);
		}

		@Override
		public void run() {
			try {

				while (stopFlag == false) {
					try {
						checkConnectedDevices();

						synchronized (deviceGrantQueue) {
							if (!deviceGrantQueue.isEmpty()) { // bjo && !isGranting) {
								System.out.println("Asking for granting for new device");
								isGranting = true;
								UsbDevice device = deviceGrantQueue.remove();

								PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(UsbDevicePermissionGrantedReceiver.USB_PERMISSION_GRANTED_ACTION), 0);
								context.registerReceiver(new UsbDevicePermissionGrantedReceiver(device.getDeviceName(), device), new IntentFilter(UsbDevicePermissionGrantedReceiver.USB_PERMISSION_GRANTED_ACTION));
								usbManager.requestPermission(device, permissionIntent);
							}
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}

					sleep(1000);
				}
			} catch (Throwable e) {
				System.out.println("MySunthMidiUSB: thread interrupted" + e);
			}
		}

		/**
		 * checks Attached/Detached devices
		 */
		synchronized void checkConnectedDevices() {

			HashMap<String, UsbDevice> deviceMap = usbManager.getDeviceList();

			// check attached device
			for (String deviceName : deviceMap.keySet()) {

				// check if already removed
				if (removedDeviceNames.contains(deviceName)) {
					continue;
				}

				if (!connectedDeviceNameSet.contains(deviceName)) {
					System.out.println("A new USB Device!!");
					connectedDeviceNameSet.add(deviceName);
					UsbDevice device = deviceMap.get(deviceName);
					System.out.println("Device class: " + device.getDeviceClass() + " subclass: " + device.getDeviceSubclass());

					Set<UsbInterface> midiInterfaces = UsbMidiDeviceUtils.findAllMidiInterfaces(device, deviceFilters);
					if (midiInterfaces.size() > 0) {
						System.out.println("Adding device to the grant queue");
						synchronized (deviceGrantQueue) {
							deviceGrantQueue.add(device);
						}
					}
				}
			}

			// check detached device
			for (String deviceName : connectedDeviceNameSet) {
				if (!deviceMap.containsKey(deviceName)) {
					System.out.println("  Removing device by adding to the removedDeviceNames list. ");
					removedDeviceNames.add(deviceName);
					UsbDevice device = grantedDeviceMap.remove(deviceName);

					System.out.println("deviceName:" + deviceName + ", device:" + device + " detached.");
					if (device != null) {
						System.out.println("Notifying onDeviceDetached listener");
						MySynthMidiUSB.this.onUsbDeviceDetached(device);
					}
				}
			}

			if (removedDeviceNames.size() > 0) {
				connectedDeviceNameSet.removeAll(removedDeviceNames);
				removedDeviceNames.clear();
			}
		}
	}

	/**
	 * notifies the 'current granting device' has successfully granted.
	 */
	public void notifyDeviceGranted() {
		isGranting = false;
	}

	UsbDevice usbMidiDevice = null;
	UsbDeviceConnection usbDeviceConnection = null;
	Handler deviceDetachedHandler = null;
	UsbMidiDeviceDispatchThread usbMidiDeviceDispatchThread;
	UsbEndpoint inputEndpoint = null;

	public synchronized void onUsbDeviceAttached(final UsbDevice usbDevice) {
		System.out.println("MySynthMidiUSB: USB Device attached");

		if (usbMidiDeviceDispatchThread != null) {
			System.out.println("MySynthMidiUSB:   Skipping, there's already a device attached.");
			// already one device has been connected
			return;
		}

		try {
			UsbManager usbManager = (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE);
			usbDeviceConnection = usbManager.openDevice(usbDevice);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (usbDeviceConnection == null) {
			System.out.println("MySynthMidiUSB:   Failed to open the device!");
			return;
		}

		UsbInterface usbInterface = null;
		int interfaceCount = usbDevice.getInterfaceCount();
		for (int i = 0; i < interfaceCount; i++) {
			usbInterface = usbDevice.getInterface(i);
			if (usbInterface.getInterfaceClass() == 1 && usbInterface.getInterfaceSubclass() == 3) {
				System.out.println("  This is a standard MIDI device");
				int endpointCount = usbInterface.getEndpointCount();
				for (int endpointIndex = 0; endpointIndex < endpointCount; endpointIndex++) {
					UsbEndpoint endpoint = usbInterface.getEndpoint(endpointIndex);
					System.out.println("      Endpoint " + endpoint.getEndpointNumber() + " direction: " + endpoint.getDirection());
					if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
						inputEndpoint = endpoint;
						break;
					}
				}
				if (inputEndpoint != null) {
					break;
				}
			}
		}
		if (inputEndpoint == null) {
			System.out.println("MySynthMidiUSB:   No MIDI input interface was found on the device.");
			return;
		}
		usbDeviceConnection.claimInterface(usbInterface, true);

		usbMidiDeviceDispatchThread = new UsbMidiDeviceDispatchThread();
		usbMidiDeviceDispatchThread.start();

		midiDeviceAttached = true;
		if (callbacks != null) {
			callbacks.onDeviceAttached(usbDevice.getDeviceName());
		}

		System.out.println("MySynthMidiUSB: Finished setting up handling of USB MIDI device");
	}

	public synchronized void onUsbDeviceDetached(final UsbDevice detachedDevice) {
		System.out.println("MySynthMidiUSB: USB Device detached");

		AsyncTask<UsbDevice, Void, Void> task = new AsyncTask<UsbDevice, Void, Void>() {

			@Override
			protected Void doInBackground(UsbDevice... params) {
				if (params == null || params.length < 1) {
					return null;
				}

				UsbDevice usbDevice = params[0];

				System.out.println("MySynthMidiUSB: Handling the USB Device detached.");

				if (usbMidiDeviceDispatchThread != null) {
					usbMidiDeviceDispatchThread.stopFlag = true;
					usbMidiDeviceDispatchThread = null;
				}

				if (usbDeviceConnection != null) {
					usbDeviceConnection.close();
					usbDeviceConnection = null;
				}
				usbMidiDevice = null;

//				Message message = Message.obtain(deviceDetachedHandler);
//				message.obj = usbDevice;
//				deviceDetachedHandler.sendMessage(message);

				System.out.println("MySynthMidiUSB: USB MIDI Device detached");

				midiDeviceAttached = false;
				if (callbacks != null) {
					callbacks.onDeviceDetached(usbDevice.getDeviceName());
				}

				return null;
			}
		};
		task.execute(detachedDevice);
	}

	private class UsbMidiDeviceDispatchThread extends Thread {

		boolean stopFlag;

		public void run() {
			int BUFFER_LENGTH = inputEndpoint.getMaxPacketSize();
			System.out.println("MySynthMidiUSB: UsbMidiDeviceThread is running, using buffer length " + BUFFER_LENGTH);

			final byte[] data = new byte[BUFFER_LENGTH];
			while (!stopFlag) {
				int length = usbDeviceConnection.bulkTransfer(inputEndpoint, data, BUFFER_LENGTH, 0);
				if (length < 0) {
					System.out.println("MySynthMidiUSB: Error length " + length + " received, pausing");
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
					}
				} else {
					for (int i = 0; i < length; i += 4) {
						processMidi(data[i + 1], data[i + 2], data[i + 3]);
					}
				}
			}

			System.out.println("MySynthMidiUSB: UsbMidiDeviceThread is stopping");
		}
	}

}
