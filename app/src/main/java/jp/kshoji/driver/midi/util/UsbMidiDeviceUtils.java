package jp.kshoji.driver.midi.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.annotation.SuppressLint;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import jp.kshoji.driver.usb.util.DeviceFilter;

/**
 * Utility for finding MIDI device
 * 
 * @author K.Shoji
 */
@SuppressLint("NewApi")
public final class UsbMidiDeviceUtils {
	
	/**
	 * Find {@link UsbInterface} from {@link UsbDevice} with the direction
	 * 
	 * @param usbDevice
	 * @param direction
	 *            {@link UsbConstants.USB_DIR_IN} or {@link UsbConstants.USB_DIR_OUT}
	 * @param deviceFilters
	 * @return {@link Set<UsbInterface>} always not null
	 */
	public static Set<UsbInterface> findMidiInterfaces(UsbDevice usbDevice, int direction, List<DeviceFilter> deviceFilters) {
		Set<UsbInterface> usbInterfaces = new HashSet<UsbInterface>();

		int count = usbDevice.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = usbDevice.getInterface(i);

			if (findMidiEndpoint(usbDevice, usbInterface, direction, deviceFilters) != null) {
				usbInterfaces.add(usbInterface);
			}
		}
		return Collections.unmodifiableSet(usbInterfaces);
	}

	/**
	 * Find all {@link UsbInterface} from {@link UsbDevice}
	 * 
	 * @param usbDevice
	 * @param deviceFilters
	 * @return {@link Set<UsbInterface>} always not null
	 */
	public static Set<UsbInterface> findAllMidiInterfaces(UsbDevice usbDevice, List<DeviceFilter> deviceFilters) {
		Set<UsbInterface> usbInterfaces = new HashSet<UsbInterface>();

		int count = usbDevice.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = usbDevice.getInterface(i);
			System.out.println("  Interface "+i+"  class: "+usbInterface.getInterfaceClass()+" subclass: "+usbInterface.getInterfaceSubclass()+" protocol: "+usbInterface.getInterfaceProtocol());

			if (findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_IN, deviceFilters) != null) {
				usbInterfaces.add(usbInterface);
			}
// bjo -- not using output direction
//			if (findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_OUT, deviceFilters) != null) {
//				usbInterfaces.add(usbInterface);
//			}
		}
		return Collections.unmodifiableSet(usbInterfaces);
	}

	/**
	 * Find {@link UsbEndpoint} from {@link findMidiEndpoint} with the direction
	 * 
	 * @param usbDevice
	 * @param usbInterface
	 * @param direction
	 * @param deviceFilters
	 * @return {@link UsbEndpoint}, null if not found
	 */
	@SuppressLint("NewApi")
	public static UsbEndpoint findMidiEndpoint(UsbDevice usbDevice, UsbInterface usbInterface, int direction, List<DeviceFilter> deviceFilters) {
		
		int endpointCount = usbInterface.getEndpointCount();

		// standard USB MIDI interface (1/3)
		if (usbInterface.getInterfaceClass() == 1  && usbInterface.getInterfaceSubclass() == 3) {
			System.out.println("  This is a standard MIDI device");
			UsbEndpoint chosenEndpoint = null;
			for (int endpointIndex = 0; endpointIndex < endpointCount; endpointIndex++) {
				UsbEndpoint endpoint = usbInterface.getEndpoint(endpointIndex);
				System.out.println("      Endpoint "+endpoint.getEndpointNumber()+" direction: "+endpoint.getDirection());
				if (endpoint.getDirection() == direction) {
					chosenEndpoint = endpoint;
				}
			}
			if (chosenEndpoint == null) {
				System.out.println("  No appropriate MIDI endpoint found");
			} else {
				System.out.println("  Chose endpoint "+chosenEndpoint.getEndpointNumber());
				return chosenEndpoint;
			}
		} else {
			// search for a match in the list of supported vendors for USB MIDI devices
			boolean filterMatched = false;
			for (DeviceFilter deviceFilter : deviceFilters) {
				if (deviceFilter.matches(usbDevice)) {
					filterMatched = true;
					break;
				}
			}

			if (filterMatched == false) {
				System.out.println("    The interface doesn't appear to have any MIDI endpoints");
				return null;
			}

			// non standard USB MIDI interface
			for (int endpointIndex = 0; endpointIndex < endpointCount; endpointIndex++) {
				UsbEndpoint endpoint = usbInterface.getEndpoint(endpointIndex);
				if ((endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK || endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT)) {
					if (endpoint.getDirection() == direction) {
						System.out.println("    Non-standard MIDI endpoint "+endpoint.getEndpointNumber()+" direction: "+endpoint.getDirection());
						return endpoint;
					}
				}
			}
		}
		return null;
	}
}
