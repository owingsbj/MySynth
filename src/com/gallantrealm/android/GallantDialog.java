package com.gallantrealm.android;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;

public class GallantDialog extends Dialog {

	public GallantDialog(Context context) {
		super(context);
	}

	public GallantDialog(Context context, int theme) {
		super(context, theme);
	}

	public GallantDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	// ------------------------
	// Mapping controllers to keyboard
	// ------------------------

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		System.out.println("Keycode: " + keyCode);
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) {
			View v = getWindow().getCurrentFocus();
			if (v instanceof Button || v instanceof CheckBox || v instanceof RadioButton) {
				System.out.println("Performing click");
				v.performClick();
			} else {
				System.out.println("Performing okay");
				onOkay();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
			System.out.println("Performing cancel");
			onCancel();
			return true;
		} else {
			System.out.println("Some other key");
		}
		return false;
	}

	/**
	 * Override to give a behavior on okay pressed. Default is to close.
	 */
	public void onOkay() {
		dismiss();
		cancel();
	}

	/**
	 * Override to give a behavior on cancel pressed. Default is to close.
	 */
	public void onCancel() {
		dismiss();
		cancel();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return false;
	}

}
