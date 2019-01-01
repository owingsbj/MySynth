package com.gallantrealm.android;

import com.gallantrealm.mysynth.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * A control for a piano keyboard.  The size of the keyboard can be from 13 to 32 notes
 * @author owingsbj
 *
 */
public class KeyboardControl extends LinearLayout implements OnTouchListener {

	public interface Listener {
		public void onNotePressed(int note, float velocity);
		public void onNoteReleased(int note);
		public void onNoteAftertouch(int note, float pressure);
	}

	private Listener listener;

	View keyboard;
	int keyboardLocation[];
	Button key1;
	Button key2;
	Button key3;
	Button key4;
	Button key5;
	Button key6;
	Button key7;
	Button key8;
	Button key9;
	Button key10;
	Button key11;
	Button key12;
	Button key13;
	Button key14;
	Button key15;
	Button key16;
	Button key17;
	Button key18;
	Button key19;
	Button key20;
	Button key21;
	Button key22;
	Button key23;
	Button key24;
	Button key25;
	Button key26;
	Button key27;
	Button key28;
	Button key29;
	Button key30;
	Button key31;
	Button key32;

	int[] keyvoice = new int[25];
	private static final int PRESS = 1;
	private static final int RELEASE = 2;
	private static final int SLIDE = 0;

	private float lastTouchDownX;
	private float lastTouchDownY;

	private float initialX; // for pitch bend
	private float initialY; // for expression

	private final boolean[] keyPressed = new boolean[32];
	int[] lastNote = new int[20]; // 20 fingers max (do you have more?)

	public KeyboardControl(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public KeyboardControl(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public KeyboardControl(Context context) {
		super(context);
		init();
	}

	public KeyboardControl(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	private void init() {
		inflate(getContext(), R.layout.screenkeyboard, this);

		keyboard = this.findViewById(R.id.keyboard);
		key1 = (Button) this.findViewById(R.id.key1);
		key2 = (Button) this.findViewById(R.id.key2);
		key3 = (Button) this.findViewById(R.id.key3);
		key4 = (Button) this.findViewById(R.id.key4);
		key5 = (Button) this.findViewById(R.id.key5);
		key6 = (Button) this.findViewById(R.id.key6);
		key7 = (Button) this.findViewById(R.id.key7);
		key8 = (Button) this.findViewById(R.id.key8);
		key9 = (Button) this.findViewById(R.id.key9);
		key10 = (Button) this.findViewById(R.id.key10);
		key11 = (Button) this.findViewById(R.id.key11);
		key12 = (Button) this.findViewById(R.id.key12);
		key13 = (Button) this.findViewById(R.id.key13);
		key14 = (Button) this.findViewById(R.id.key14);
		key15 = (Button) this.findViewById(R.id.key15);
		key16 = (Button) this.findViewById(R.id.key16);
		key17 = (Button) this.findViewById(R.id.key17);
		key18 = (Button) this.findViewById(R.id.key18);
		key19 = (Button) this.findViewById(R.id.key19);
		key20 = (Button) this.findViewById(R.id.key20);
		key21 = (Button) this.findViewById(R.id.key21);
		key22 = (Button) this.findViewById(R.id.key22);
		key23 = (Button) this.findViewById(R.id.key23);
		key24 = (Button) this.findViewById(R.id.key24);
		key25 = (Button) this.findViewById(R.id.key25);
		key26 = (Button) this.findViewById(R.id.key26);
		key27 = (Button) this.findViewById(R.id.key27);
		key28 = (Button) this.findViewById(R.id.key28);
		key29 = (Button) this.findViewById(R.id.key29);
		key30 = (Button) this.findViewById(R.id.key30);
		key31 = (Button) this.findViewById(R.id.key31);
		key32 = (Button) this.findViewById(R.id.key32);

		keyboard.setOnTouchListener(this);
	}

	public Listener getListener() {
		return listener;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public void setKeyboardSize(int keysSelection) {
		if (keysSelection == 0) { // 13
			key14.setVisibility(View.GONE);
			key15.setVisibility(View.GONE);
			key16.setVisibility(View.GONE);
			this.findViewById(R.id.key16spacer).setVisibility(View.GONE);
			key17.setVisibility(View.GONE);
			key18.setVisibility(View.GONE);
			key19.setVisibility(View.GONE);
			key20.setVisibility(View.GONE);
			key21.setVisibility(View.GONE);
			key22.setVisibility(View.GONE);
			key23.setVisibility(View.GONE);
			this.findViewById(R.id.key23spacer).setVisibility(View.GONE);
			key24.setVisibility(View.GONE);
			key25.setVisibility(View.GONE);
			key26.setVisibility(View.GONE);
			key27.setVisibility(View.GONE);
			key28.setVisibility(View.GONE);
			this.findViewById(R.id.key28spacer).setVisibility(View.GONE);
			key29.setVisibility(View.GONE);
			key30.setVisibility(View.GONE);
			key31.setVisibility(View.GONE);
			key32.setVisibility(View.GONE);
		} else if (keysSelection == 1) { // 20
			key14.setVisibility(View.VISIBLE);
			key15.setVisibility(View.VISIBLE);
			key16.setVisibility(View.VISIBLE);
			this.findViewById(R.id.key16spacer).setVisibility(View.VISIBLE);
			key17.setVisibility(View.VISIBLE);
			key18.setVisibility(View.VISIBLE);
			key19.setVisibility(View.VISIBLE);
			key20.setVisibility(View.VISIBLE);
			key21.setVisibility(View.GONE);
			key22.setVisibility(View.GONE);
			key23.setVisibility(View.GONE);
			this.findViewById(R.id.key23spacer).setVisibility(View.GONE);
			key24.setVisibility(View.GONE);
			key25.setVisibility(View.GONE);
			key26.setVisibility(View.GONE);
			key27.setVisibility(View.GONE);
			key28.setVisibility(View.GONE);
			this.findViewById(R.id.key28spacer).setVisibility(View.GONE);
			key29.setVisibility(View.GONE);
			key30.setVisibility(View.GONE);
			key31.setVisibility(View.GONE);
			key32.setVisibility(View.GONE);
		} else if (keysSelection == 2) { // 25
			key14.setVisibility(View.VISIBLE);
			key15.setVisibility(View.VISIBLE);
			key16.setVisibility(View.VISIBLE);
			this.findViewById(R.id.key16spacer).setVisibility(View.VISIBLE);
			key17.setVisibility(View.VISIBLE);
			key18.setVisibility(View.VISIBLE);
			key19.setVisibility(View.VISIBLE);
			key20.setVisibility(View.VISIBLE);
			key21.setVisibility(View.VISIBLE);
			key22.setVisibility(View.VISIBLE);
			key23.setVisibility(View.VISIBLE);
			this.findViewById(R.id.key23spacer).setVisibility(View.VISIBLE);
			key24.setVisibility(View.VISIBLE);
			key25.setVisibility(View.VISIBLE);
			key26.setVisibility(View.GONE);
			key27.setVisibility(View.GONE);
			key28.setVisibility(View.GONE);
			this.findViewById(R.id.key28spacer).setVisibility(View.GONE);
			key29.setVisibility(View.GONE);
			key30.setVisibility(View.GONE);
			key31.setVisibility(View.GONE);
			key32.setVisibility(View.GONE);
		} else if (keysSelection == 3) { // 32
			key14.setVisibility(View.VISIBLE);
			key15.setVisibility(View.VISIBLE);
			key16.setVisibility(View.VISIBLE);
			this.findViewById(R.id.key16spacer).setVisibility(View.VISIBLE);
			key17.setVisibility(View.VISIBLE);
			key18.setVisibility(View.VISIBLE);
			key19.setVisibility(View.VISIBLE);
			key20.setVisibility(View.VISIBLE);
			key21.setVisibility(View.VISIBLE);
			key22.setVisibility(View.VISIBLE);
			key23.setVisibility(View.VISIBLE);
			this.findViewById(R.id.key23spacer).setVisibility(View.VISIBLE);
			key24.setVisibility(View.VISIBLE);
			key25.setVisibility(View.VISIBLE);
			key26.setVisibility(View.VISIBLE);
			key27.setVisibility(View.VISIBLE);
			key28.setVisibility(View.VISIBLE);
			this.findViewById(R.id.key28spacer).setVisibility(View.VISIBLE);
			key29.setVisibility(View.VISIBLE);
			key30.setVisibility(View.VISIBLE);
			key31.setVisibility(View.VISIBLE);
			key32.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int index = event.getActionIndex();
		int pointerCount = event.getPointerCount();

		float x = event.getX(index);
		float y = event.getY(index);
		if (keyboardLocation == null) {
			keyboardLocation = new int[2];
			keyboard.getLocationOnScreen(keyboardLocation);
		}
		x += keyboardLocation[0];
		y += keyboardLocation[1];
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
			doKey(event.getPointerId(index) + 1, x, y, PRESS, pointerCount);
			lastTouchDownX = x;
			lastTouchDownY = y;
		} else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
			for (int i = 0; i < pointerCount; i++) {
				int historySize = event.getHistorySize();
				for (int h = 0; h < historySize; h++) {
					doKey(event.getPointerId(i) + 1, event.getHistoricalX(i, h) + keyboardLocation[0], Math.max(0, event.getHistoricalY(i, h) + keyboardLocation[1]), SLIDE, pointerCount);
				}
				doKey(event.getPointerId(i) + 1, event.getX(i) + keyboardLocation[0], Math.max(0, event.getY(i) + keyboardLocation[1]), SLIDE, pointerCount);
			}
		} else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
			doKey(event.getPointerId(index) + 1, x, y, RELEASE, pointerCount);
		}
		return true;
	}

	@SuppressLint("NewApi")
	private void doKey(int finger, float x, float y, int type, int fingers) {

		if (!isPointInsideView(x, y, keyboard)) {
			int[] location = new int[2];
			keyboard.getLocationInWindow(location);
			y = location[1] + 4;
		}

		// test black keys first, then white
		int key = -1;
		while (key == -1) {
			if (isPointInsideView(x, y, key2)) {
				key = 1;
			} else if (isPointInsideView(x, y, key4)) {
				key = 3;
			} else if (isPointInsideView(x, y, key7)) {
				key = 6;
			} else if (isPointInsideView(x, y, key9)) {
				key = 8;
			} else if (isPointInsideView(x, y, key11)) {
				key = 10;
			} else if (isPointInsideView(x, y, key14)) {
				key = 13;
			} else if (isPointInsideView(x, y, key16)) {
				key = 15;
			} else if (isPointInsideView(x, y, key19)) {
				key = 18;
			} else if (isPointInsideView(x, y, key21)) {
				key = 20;
			} else if (isPointInsideView(x, y, key23)) {
				key = 22;
			} else if (x <= 0 || isPointInsideView(x, y, key1)) {
				key = 0;
			} else if (isPointInsideView(x, y, key3)) {
				key = 2;
			} else if (isPointInsideView(x, y, key5)) {
				key = 4;
			} else if (isPointInsideView(x, y, key6)) {
				key = 5;
			} else if (isPointInsideView(x, y, key8)) {
				key = 7;
			} else if (isPointInsideView(x, y, key10)) {
				key = 9;
			} else if (isPointInsideView(x, y, key12)) {
				key = 11;
			} else if (isPointInsideView(x, y, key13)) {
				key = 12;
			} else if (isPointInsideView(x, y, key15)) {
				key = 14;
			} else if (isPointInsideView(x, y, key17)) {
				key = 16;
			} else if (isPointInsideView(x, y, key18)) {
				key = 17;
			} else if (isPointInsideView(x, y, key20)) {
				key = 19;
			} else if (isPointInsideView(x, y, key21)) {
				key = 20;
			} else if (isPointInsideView(x, y, key22)) {
				key = 21;
			} else if (isPointInsideView(x, y, key24)) {
				key = 23;
			} else if (isPointInsideView(x, y, key25)) {
				key = 24;
			} else if (isPointInsideView(x, y, key26)) {
				key = 25;
			} else if (isPointInsideView(x, y, key27)) {
				key = 26;
			} else if (isPointInsideView(x, y, key28)) {
				key = 27;
			} else if (isPointInsideView(x, y, key29)) {
				key = 28;
			} else if (isPointInsideView(x, y, key30)) {
				key = 29;
			} else if (isPointInsideView(x, y, key31)) {
				key = 30;
			} else if (isPointInsideView(x, y, key32)) {
				key = 31;
			} else {
				// fudge x a bit and try again
				x = x - 4;
			}
		}
		int note = key + 60 - 12;

		int[] coords = new int[2];
		keyboard.getLocationOnScreen(coords);
		float velocity = Math.min(1.0f, 4.0f - 4.0f * (y - coords[1]) / keyboard.getHeight());

		if (listener != null) {
			if (type == PRESS) {
				listener.onNotePressed(note, velocity);
			} else if (type == RELEASE) {
				listener.onNoteReleased(note);
			} else if (type == SLIDE) {
				if (lastNote[finger] != note) {
					listener.onNoteReleased(lastNote[finger]);
					listener.onNotePressed(note, velocity);
				} else {
					listener.onNoteAftertouch(note, velocity);
				}
			}
		}
		lastNote[finger] = note;

		// if the action is up, remove the upped finger from the lastnote array
		if (type == RELEASE) {
			for (int i = finger; i < lastNote.length - 1; i++) {
				lastNote[finger] = lastNote[finger + 1];
			}
		}

	}

	public void setKeyPressed(int note, boolean pressed) {
		if (note < keyPressed.length) {
			getKeyForNote(note).setPressed(pressed);
			keyPressed[note] = pressed;
		}
	}

	public boolean isKeyPressed(int note) {
		return keyPressed[note];
	}

	private View getKeyForNote(int note) {
		if (note <= 0) {
			return key1;
		} else if (note == 1) {
			return key2;
		} else if (note == 2) {
			return key3;
		} else if (note == 3) {
			return key4;
		} else if (note == 4) {
			return key5;
		} else if (note == 5) {
			return key6;
		} else if (note == 6) {
			return key7;
		} else if (note == 7) {
			return key8;
		} else if (note == 8) {
			return key9;
		} else if (note == 9) {
			return key10;
		} else if (note == 10) {
			return key11;
		} else if (note == 11) {
			return key12;
		} else if (note == 12) {
			return key13;
		} else if (note == 13) {
			return key14;
		} else if (note == 14) {
			return key15;
		} else if (note == 15) {
			return key16;
		} else if (note == 16) {
			return key17;
		} else if (note == 17) {
			return key18;
		} else if (note == 18) {
			return key19;
		} else if (note == 19) {
			return key20;
		} else if (note == 20) {
			return key21;
		} else if (note == 21) {
			return key22;
		} else if (note == 22) {
			return key23;
		} else if (note == 23) {
			return key24;
		} else if (note == 24) {
			return key25;
		} else if (note == 25) {
			return key26;
		} else if (note == 26) {
			return key27;
		} else if (note == 27) {
			return key28;
		} else if (note == 28) {
			return key29;
		} else if (note == 29) {
			return key30;
		} else if (note == 30) {
			return key31;
		} else if (note == 31) {
			return key32;
		} else {
			return key32;
		}
	}

	private boolean isPointInsideView(float x, float y, View view) {
		if (view.getVisibility() == View.GONE) {
			return false;
		}
		int location[] = new int[2];
		view.getLocationOnScreen(location);
		int viewX = location[0];
		int viewY = location[1];

		// point is inside view bounds
		if ((x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight()))) {
			return true;
		} else {
			return false;
		}
	}

}
