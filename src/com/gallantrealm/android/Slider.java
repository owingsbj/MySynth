package com.gallantrealm.android;

import com.gallantrealm.mysynth.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class Slider extends SeekBar {

	private final Bitmap thumbBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.slider_normal);
	private final Bitmap thumbBitmapPressed = BitmapFactory.decodeResource(getResources(), R.drawable.slider_pressed);
	private int thumbWidth;
	private int thumbHeight;
	private Paint blackPaint = new Paint();
	private Paint whitePaint = new Paint();
	private Paint greyPaint = new Paint();
	private Paint greenPaint = new Paint();
	private Paint textPaint = new Paint();
	private RectF rect;
	private int seekbar_height;
	private float dp;

	public Slider(Context context) {
		super(context);
		init();
	}

	public Slider(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		dp = getContext().getResources().getDisplayMetrics().density;
		rect = new RectF();
		seekbar_height = 6;
		blackPaint.setColor(0xFF000000);
		blackPaint.setStyle(Style.FILL_AND_STROKE);
		blackPaint.setAntiAlias(true);
		whitePaint.setColor(0xFFD0D0D0);
		whitePaint.setStyle(Style.FILL_AND_STROKE);
		whitePaint.setAntiAlias(true);
		greyPaint.setColor(0x80808080);
		greyPaint.setStyle(Style.FILL_AND_STROKE);
		greyPaint.setAntiAlias(true);
		greenPaint.setColor(0xFF40FF40);
		greenPaint.setStyle(Style.FILL_AND_STROKE);
		greenPaint.setAntiAlias(true);
		textPaint.setTextSize(12 * dp);
		textPaint.setAntiAlias(true);
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		if (thumbWidth == 0 && thumbBitmap != null) {
			thumbWidth = thumbBitmap.getWidth();
			thumbHeight = thumbBitmap.getHeight();
		}

		// draw "ticks"
		if (getMax() <= 20) {
			for (int i = 0; i <= getMax(); i++) {
				float x = thumbWidth / 2 + i * (getWidth() - thumbWidth) / getMax();
				rect.set(x - dp, getHeight() / 2 - 4 * dp, x + 2 * dp, getHeight() / 2 + 4 * dp);
				canvas.drawRect(rect, greyPaint);
				rect.set(x, getHeight() / 2 - 4 * dp, x + 1 * dp, getHeight() / 2 + 4 * dp);
				canvas.drawRect(rect, whitePaint);
			}
		} else {
			for (int i = 0; i <= getMax(); i += 10) {
				float x = thumbWidth / 2 + i * (getWidth() - thumbWidth) / getMax();
				rect.set(x - dp, getHeight() / 2 - 4 * dp, x + 2 * dp, getHeight() / 2 + 4 * dp);
				canvas.drawRect(rect, greyPaint);
				rect.set(x, getHeight() / 2 - 4 * dp, x + dp, getHeight() / 2 + 4 * dp);
				canvas.drawRect(rect, whitePaint);
			}
		}

		// draw the background
		rect.set( //
				thumbWidth / 2, //
				getHeight() / 2 - 3 * dp, //
				getWidth() - thumbWidth / 2 + 2 * dp, //
				getHeight() / 2 + 3 * dp);
		canvas.drawRect(rect, greyPaint);
		rect.set( //
				thumbWidth / 2, //
				getHeight() / 2 - 2 * dp, //
				getWidth() - thumbWidth / 2 + 1 * dp, //
				getHeight() / 2 + 2 * dp);
		canvas.drawRect(rect, blackPaint);

		// draw the green level indicator
		rect.set( //
				thumbWidth / 2, //
				getHeight() / 2 - 2 * dp, //
				thumbWidth / 2 + (getWidth() - thumbWidth) * getProgress() / getMax(), //
				getHeight() / 2 + 2 * dp);
		canvas.drawRect(rect, greenPaint);

		// draw the thumb
		if (thumbBitmap != null) {
			if (isPressed()) {
				canvas.drawBitmap(thumbBitmapPressed, getProgress() * (getWidth() - thumbWidth) / getMax(), (getHeight() - thumbHeight) / 2, blackPaint);
			} else {
				canvas.drawBitmap(thumbBitmap, getProgress() * (getWidth() - thumbWidth) / getMax(), (getHeight() - thumbHeight) / 2, blackPaint);
			}
		}

		// draw the number text
		String value = "" + getProgress();
		float textWidth = textPaint.measureText(value);
		float textHeight = -textPaint.getFontMetrics().top;
		textPaint.setColor(0xC0000000);
		canvas.drawText(value, getWidth() - textWidth, getHeight() / 2 + textHeight / 2 - 2 * dp, textPaint);
		textPaint.setColor(0xC0FFFFFF);
		canvas.drawText(value, getWidth() - textWidth - dp, getHeight() / 2 + textHeight / 2 - 3 * dp, textPaint);
	}

}
