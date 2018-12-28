package com.gallantrealm.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class VerticalSlider extends SeekBar {

	public VerticalSlider(Context context) {
		super(context);
	}

	public VerticalSlider(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public VerticalSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
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
	protected void onDraw(Canvas c) {
		c.rotate(-90);
		c.translate(-getHeight(), 0);
		super.onDraw(c);
		c.translate(getHeight(), 0);
		c.rotate(90);

		Paint paint = new Paint();
		final float scale = getContext().getResources().getDisplayMetrics().density;
		paint.setTextSize(12 * scale);
		paint.setAntiAlias(true);
		String value = "" + getProgress();
		float textWidth = paint.measureText(value);
		float textHeight = -paint.getFontMetrics().top;
		paint.setColor(0xC0000000);
		c.drawText(value, getWidth() / 2 - textWidth / 2, textHeight, paint);
		paint.setColor(0xC0FFFFFF);
		c.drawText(value, getWidth() / 2 - textWidth / 2 - 1, textHeight - 1, paint);

//		Thread.currentThread().dumpStack();
	}

	private float initialX, initialY;
	private boolean alreadyMoving;
	private boolean scrollingMove;

	@SuppressLint("ClickableViewAccessibility")
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
				float delX = Math.abs(initialX - event.getX());
				float delY =  Math.abs(initialY - event.getY());
				if (delX < 5 && delY < 5) {
					return true;
				}
				if (delX > delY) {
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
