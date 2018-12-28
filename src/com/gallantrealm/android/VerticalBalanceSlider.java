package com.gallantrealm.android;

import com.gallantrealm.mysynth.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class VerticalBalanceSlider extends SeekBar {

	private final Bitmap thumbBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.slider_normal);
	private final Bitmap thumbBitmapPressed = BitmapFactory.decodeResource(getResources(), R.drawable.slider_pressed);
	private final Bitmap thumbBitmapActivated = BitmapFactory.decodeResource(getResources(), R.drawable.slider_activated);
	private int thumbWidth;
	private Paint blackPaint = new Paint();
	private Paint whitePaint = new Paint();
	private Paint greyPaint = new Paint();
	private Paint greenPaint = new Paint();
	private RectF rect;
	private Paint textPaint = new Paint();
	private float dp;

	public VerticalBalanceSlider(Context context) {
		super(context);
		init();
	}

	public VerticalBalanceSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		dp = getContext().getResources().getDisplayMetrics().density;
		rect = new RectF();
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
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(h, w, oldh, oldw);
	}

	@Override
	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.rotate(-90);
		canvas.translate(-getHeight(), 0);

		if (thumbWidth == 0 && thumbBitmap != null) {
			thumbWidth = thumbBitmap.getWidth();
		}

		// draw "ticks"
		if (getMax() <= 24) {
			for (int i = 0; i <= getMax(); i++) {
				float x = thumbWidth / 2 + i * (getHeight() - thumbWidth) / getMax();
				rect.set(x - dp, getWidth() / 2 - 4 * dp, x + 2 * dp, getWidth() / 2 + 4 * dp);
				canvas.drawRect(rect, greyPaint);
				rect.set(x, getWidth() / 2 - 4 * dp, x + 1 * dp, getWidth() / 2 + 4 * dp);
				canvas.drawRect(rect, whitePaint);
			}
		} else {
			for (int i = 0; i <= getMax(); i += 10) {
				float x = thumbWidth / 2 + i * (getHeight() - thumbWidth) / getMax();
				rect.set(x - dp, getWidth() / 2 - 4 * dp, x + 2 * dp, getWidth() / 2 + 4 * dp);
				canvas.drawRect(rect, greyPaint);
				rect.set(x, getWidth() / 2 - 4 * dp, x + dp, getWidth() / 2 + 4 * dp);
				canvas.drawRect(rect, whitePaint);
			}
		}

		// draw the background
		rect.set( //
				thumbWidth / 2, //
				getWidth() / 2 - 3 * dp, //
				getHeight() - thumbWidth / 2 + 1 * dp, //
				getWidth() / 2 + 3 * dp);
		canvas.drawRect(rect, greyPaint);
		rect.set( //
				thumbWidth / 2, //
				getWidth() / 2 - 2 * dp, //
				getHeight() - thumbWidth / 2, //
				getWidth() / 2 + 2 * dp);
		canvas.drawRect(rect, blackPaint);

		// draw the green level indicator
		if (this.getProgress() > getMax() / 2) {
			rect.set( //
					getHeight() / 2, //
					getWidth() / 2 - 2 * dp, //
					getHeight() / 2 + (getHeight() - thumbWidth) * (getProgress() - getMax() / 2) / getMax(), //
					getWidth() / 2 + 2 * dp);
			canvas.drawRect(rect, greenPaint);
		}
		if (this.getProgress() < getMax() / 2) {
			rect.set( //
					getHeight() / 2 - (getHeight() - thumbWidth) * (getMax() / 2 - getProgress()) / getMax(), //
					getWidth() / 2 - 2 * dp, //
					getHeight() / 2, //
					getWidth() / 2 + 2 * dp);
			canvas.drawRect(rect, greenPaint);
		}

		// draw the thumb
		if (thumbBitmap != null) {
			if (isPressed()) {
				canvas.drawBitmap(thumbBitmapPressed, getProgress() * (getHeight() - thumbWidth) / getMax(), 0, blackPaint);
			} else if (isActivated()) {
				canvas.drawBitmap(thumbBitmapActivated, getProgress() * (getHeight() - thumbWidth) / getMax(), 0, blackPaint);
			} else {
				canvas.drawBitmap(thumbBitmap, getProgress() * (getHeight() - thumbWidth) / getMax(), 0, blackPaint);
			}
		}

		canvas.translate(getHeight(), 0);
		canvas.rotate(90);

		Paint paint = new Paint();
		final float scale = getContext().getResources().getDisplayMetrics().density;
		paint.setTextSize(12 * scale);
		paint.setAntiAlias(true);
		String value = "" + (getProgress() - getMax() / 2);
		float textWidth = paint.measureText(value);
		float textHeight = -paint.getFontMetrics().top;
		paint.setColor(0xC0000000);
		canvas.drawText(value, getWidth() / 2 - textWidth / 2, textHeight, paint);
		paint.setColor(0xC0FFFFFF);
		canvas.drawText(value, getWidth() / 2 - textWidth / 2 - 1, textHeight - 1, paint);
	}

	private float initialX, initialY;
	private boolean alreadyMoving;
	private boolean scrollingMove;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}

		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:
			initialX = event.getX();
			initialY = event.getY();
			alreadyMoving = false;
			break;

		case MotionEvent.ACTION_MOVE:
			if (!alreadyMoving) {
				if (Math.abs(initialX - event.getX()) > Math.abs(initialY - event.getY())) {
					scrollingMove = true;
				} else {
					scrollingMove = false;
				}
				alreadyMoving = true;
			}
			if (scrollingMove) {
				return false;
			}
			setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
			onSizeChanged(getWidth(), getHeight(), 0, 0);
			return false;

		case MotionEvent.ACTION_UP:
			alreadyMoving = false;
			setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
			onSizeChanged(getWidth(), getHeight(), 0, 0);
			break;

		case MotionEvent.ACTION_CANCEL:
			break;

		}

		return true;

	}

	@Override
	public synchronized void setProgress(int progress) {
		super.setProgress(progress);
		onSizeChanged(getWidth(), getHeight(), 0, 0);
//		invalidate();
	}

	@SuppressLint("NewApi")
	public void setThumbLight(final boolean light) {
		post(new Runnable() {
			public void run() {
				if (light) {
					setActivated(true);
				} else {
					setActivated(false);
				}
			}
		});
	}

}
