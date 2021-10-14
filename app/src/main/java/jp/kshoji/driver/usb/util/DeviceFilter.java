package jp.kshoji.driver.usb.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;

/**
 * Utility methods from com.android.server.usb.UsbSettingsManager.DeviceFilter
 * 
 * @see http ://ics-custom-services.googlecode.com/git-history/1899c9df4b68885df4f351fa9feee603a08ee8ec/java/com/android/ server /usb/UsbSettingsManager.java
 * @author K.Shoji
 */
public final class DeviceFilter {
	// USB Vendor ID (or -1 for unspecified)
	private final int usbVendorId;
	// USB Product ID (or -1 for unspecified)
	private final int usbProductId;
	// USB device or interface class (or -1 for unspecified)
	private final int usbClass;
	// USB device subclass (or -1 for unspecified)
	private final int usbSubclass;
	// USB device protocol (or -1 for unspecified)
	private final int usbProtocol;

	/**
	 * constructor
	 * 
	 * @param vendorId
	 * @param productId
	 * @param clasz
	 * @param subclass
	 * @param protocol
	 */
	public DeviceFilter(int vendorId, int productId, int clasz, int subclass, int protocol) {
		usbVendorId = vendorId;
		usbProductId = productId;
		usbClass = clasz;
		usbSubclass = subclass;
		usbProtocol = protocol;
	}

	/**
	 * Load DeviceFilter from resources(res/xml/device_filter.xml).
	 * 
	 * @param context
	 * @return
	 */
	public static List<DeviceFilter> getDeviceFilters(Context context) {
		// create device filter
		XmlPullParser parser = context.getResources().getXml(com.gallantrealm.mysynth.R.xml.device_filter);
		List<DeviceFilter> deviceFilters = new ArrayList<DeviceFilter>();
		try {
			int hasNext = XmlPullParser.START_DOCUMENT;
			while (hasNext != XmlPullParser.END_DOCUMENT) {
				hasNext = parser.next();
				DeviceFilter deviceFilter = parseXml(parser);
				if (deviceFilter != null) {
					deviceFilters.add(deviceFilter);
				}
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Collections.unmodifiableList(deviceFilters);
	}

	/**
	 * convert {@link XmlPullParser} into {@link DeviceFilter}
	 * 
	 * @param parser
	 * @return parsed {@link DeviceFilter}
	 */
	public static DeviceFilter parseXml(XmlPullParser parser) {
		int vendorId = -1;
		int productId = -1;
		int deviceClass = -1;
		int deviceSubclass = -1;
		int deviceProtocol = -1;

		int count = parser.getAttributeCount();
		for (int i = 0; i < count; i++) {
			String name = parser.getAttributeName(i);
			// All attribute values are ints
			int value = Integer.parseInt(parser.getAttributeValue(i));

			if ("vendor-id".equals(name)) {
				vendorId = value;
			} else if ("product-id".equals(name)) {
				productId = value;
			} else if ("class".equals(name)) {
				deviceClass = value;
			} else if ("subclass".equals(name)) {
				deviceSubclass = value;
			} else if ("protocol".equals(name)) {
				deviceProtocol = value;
			}
		}

		// all blank(may be not proper tags)
		if (vendorId == -1 && productId == -1 && deviceClass == -1 && deviceSubclass == -1 && deviceProtocol == -1) {
			return null;
		}

		return new DeviceFilter(vendorId, productId, deviceClass, deviceSubclass, deviceProtocol);
	}

	/**
	 * check equals
	 * 
	 * @param clasz
	 * @param subclass
	 * @param protocol
	 * @return
	 */
	private boolean matches(int clasz, int subclass, int protocol) {
		return ((usbClass == -1 || clasz == usbClass) && (usbSubclass == -1 || subclass == usbSubclass) && (usbProtocol == -1 || protocol == usbProtocol));
	}

	/**
	 * check equals
	 * 
	 * @param device
	 * @return
	 */
	@SuppressLint("NewApi")
	public boolean matches(UsbDevice device) {
		if (usbVendorId != -1 && device.getVendorId() != usbVendorId) {
			return false;
		}
		if (usbProductId != -1 && device.getProductId() != usbProductId) {
			return false;
		}

		// check device class/subclass/protocol
		if (matches(device.getDeviceClass(), device.getDeviceSubclass(), device.getDeviceProtocol())) {
			return true;
		}

		// if device doesn't match, check the interfaces
		int count = device.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface intf = device.getInterface(i);
			if (matches(intf.getInterfaceClass(), intf.getInterfaceSubclass(), intf.getInterfaceProtocol())) {
				return true;
			}
		}

		return false;
	}
}
