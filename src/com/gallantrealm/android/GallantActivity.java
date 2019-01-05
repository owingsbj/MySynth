package com.gallantrealm.android;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;

/**
 * This activity can be subclassed for menus within a game. It provides controller support within the menus.
 */
public class GallantActivity extends Activity {

	public int songId = 0;

	View currentFocusView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
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
				v.performClick();
			} else {
				onOkay();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
			onCancel();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
			onPrevious();
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_R1 || keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
			onNext();
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
//		System.out.println("Keycode: " + keyCode);
		return false;
	}

	private void sendKey(final int keyCode) {
		new Thread() {
			@Override
			public void run() {
				try {
					Instrumentation inst = new Instrumentation();
					inst.sendKeyDownUpSync(keyCode);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * Override to provide behavior to show next item to select from in a list
	 */
	public void onNext() {

	}

	/**
	 * Override to provide behavior to show previous item to select from in a list
	 */
	public void onPrevious() {

	}

	/**
	 * Override to give a behavior on okay pressed. Default is to finish.
	 */
	public void onOkay() {
		finish();
	}

	/**
	 * Override to give a behavior on cancel pressed. Default is to finish.
	 */
	public void onCancel() {
		finish();
	}

}
