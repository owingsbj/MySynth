package com.gallantrealm.android;

import com.gallantrealm.mysynth.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class RangeSlider extends View {

	static public abstract class RangeChangeListener {
		public abstract void rangeChanged(int thumb1Value, int thumb2Value);
	}

	private String TAG = this.getClass().getSimpleName();
	private final Bitmap thumbBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.slider_normal);
	private final Bitmap thumbBitmapPressed = BitmapFactory.decodeResource(getResources(), R.drawable.slider_pressed);
	private int thumbWidth;
	private int thumbHeight;
	NinePatchDrawable scaleDrawable;

	private int minValue = 0;
	private int maxValue = 100;
	private int thumb1Value, thumb2Value = 100;
	private Paint blackPaint = new Paint();
	private Paint whitePaint = new Paint();
	private Paint greyPaint = new Paint();
	private Paint greenPaint = new Paint();
	private RangeChangeListener scl;
	private float dp;

	public RangeSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeSlider);
		minValue = a.getInt(R.styleable.RangeSlider_minValue, 0);
		maxValue = a.getInt(R.styleable.RangeSlider_maxValue, 100);
		setThumb1Value(a.getInt(R.styleable.RangeSlider_thumb1Value, minValue));
		setThumb2Value(a.getInt(R.styleable.RangeSlider_thumb2Value, maxValue));
		a.recycle();
	}

	public RangeSlider(Context context) {
		super(context);
		init();
	}

	private void init() {
		whitePaint.setColor(0xFFD0D0D0);
		greyPaint.setColor(0x80808080);
		greenPaint.setColor(0xFF40FF40);
		dp = getContext().getResources().getDisplayMetrics().density;
	}

	public int getMinValue() {
		return minValue;
	}

	public void setMinValue(int minValue) {
		this.minValue = minValue;
	}

	public int getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

	public int getThumb1Value() {
		return thumb1Value * (maxValue - minValue) / 100 + minValue;
	}

	public void setThumb1Value(int thumb1Value) {
		if (maxValue == minValue) {
			this.thumb1Value = (thumb1Value - minValue) * 100;   // protect from div by zero

		} else {
			this.thumb1Value = (thumb1Value - minValue) * 100 / (maxValue - minValue);
		}
	}

	public int getThumb2Value() {
		return thumb2Value * (maxValue - minValue) / 100 + minValue;
	}

	public void setThumb2Value(int thumb2Value) {
		if (maxValue == minValue) {
			this.thumb2Value = (thumb2Value - minValue) * 100;   // protect from div by zero
		} else {
			this.thumb2Value = (thumb2Value - minValue) * 100 / (maxValue - minValue);
		}
	}

	public void setOnRangeChangeListener(RangeChangeListener rangeChangeListener) {
		this.scl = rangeChangeListener;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (thumbWidth == 0 && thumbBitmap != null) {
			thumbWidth = thumbBitmap.getWidth();
			thumbHeight = thumbBitmap.getHeight();
		}

		// draw "ticks"
//		for (int i = 0; i <= 100; i += 10) {
//			float x = thumbWidth / 2 + i * (getWidth() - thumbWidth) / 100.0f;
//			canvas.drawRect(new RectF(x, getHeight() / 2 - 4, x + 1, getHeight() / 2 + 4), whitePaint);
//			canvas.drawRect(new RectF(x + 1, getHeight() / 2 - 3, x + 2, getHeight() / 2 + 5), greyPaint);
//		}

		// draw the bar
		canvas.drawRect(new Rect(thumbWidth / 2, getHeight() / 2 - 4, getWidth() - thumbWidth / 2 + 1, getHeight() / 2 + 4), greyPaint);
		canvas.drawRect(new Rect(thumbWidth / 2, getHeight() / 2 - 3, getWidth() - thumbWidth / 2 + 1, getHeight() / 2 + 3), blackPaint);

		// draw the green active region
		canvas.drawRect(new RectF( //
				thumb1Value * (getWidth() - thumbWidth) / 100.0f + thumbWidth / 2, //
				getHeight() / 2 - 3, //
				thumb2Value * (getWidth() - thumbWidth) / 100.0f + thumbWidth / 2, //
				getHeight() / 2 + 3), //
				greenPaint);

		// draw the thumbs
		if (thumbBitmap != null) {
			if (thumb1Press) {
				canvas.drawBitmap(thumbBitmapPressed, thumb1Value * (getWidth() - thumbWidth) / 100.0f, (getHeight() - thumbHeight) / 2, blackPaint);
			} else {
				canvas.drawBitmap(thumbBitmap, thumb1Value * (getWidth() - thumbWidth) / 100.0f, (getHeight() - thumbHeight) / 2, blackPaint);
			}
			if (thumb2Press) {
				canvas.drawBitmap(thumbBitmapPressed, thumb2Value * (getWidth() - thumbWidth) / 100.0f, (getHeight() - thumbHeight) / 2, blackPaint);
			} else {
				canvas.drawBitmap(thumbBitmap, thumb2Value * (getWidth() - thumbWidth) / 100.0f, (getHeight() - thumbHeight) / 2, blackPaint);
			}
		}

		// draw the numbers
		Paint paint = new Paint();
		paint.setTextSize(12 * dp);
		paint.setAntiAlias(true);
		String value = "" + getThumb1Value() + " - " + getThumb2Value();
		float textWidth = paint.measureText(value);
		float textHeight = -paint.getFontMetrics().top;
		paint.setColor(0xC0000000);
		canvas.drawText(value, getWidth() - textWidth, getHeight() / 2 + textHeight / 2 - 2 * dp, paint);
		paint.setColor(0xC0FFFFFF);
		canvas.drawText(value, getWidth() - textWidth - dp, getHeight() / 2 + textHeight / 2 - 3 * dp, paint);

	}

	boolean thumb1Press;
	boolean thumb2Press;
	int startX;
	int thumb1PressValue;
	int thumb2PressValue;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int) event.getX();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (x >= thumb1Value * (getWidth() - thumbWidth) / 100.0f && x <= thumb1Value * (getWidth() - thumbWidth) / 100.0f + thumbWidth) {
				thumb1Press = true;
				thumb1PressValue = thumb1Value;
			}
			if (x >= thumb2Value * (getWidth() - thumbWidth) / 100.0f && x <= thumb2Value * (getWidth() - thumbWidth) / 100.0f + thumbWidth) {
				thumb2Press = true;
				thumb2PressValue = thumb2Value;
			}
			startX = x;
			break;
		case MotionEvent.ACTION_MOVE:
			if (thumb1Press) {
				thumb1Value = thumb1PressValue + (x - startX) * 100 / (getWidth() - thumbWidth);
				thumb1Value = Math.max(0, Math.min(90, thumb1Value));
				thumb2Value = Math.max(thumb1Value + 5, thumb2Value);
			}
			if (thumb2Press) {
				thumb2Value = thumb2PressValue + (x - startX) * 100 / (getWidth() - thumbWidth);
				thumb2Value = Math.max(5, Math.min(100, thumb2Value));
				thumb1Value = Math.min(thumb1Value, thumb2Value - 5);
			}
			break;
		case MotionEvent.ACTION_UP:
			thumb1Press = false;
			thumb2Press = false;
			break;
		}
		invalidate();
		if (scl != null) {
			scl.rangeChanged(getThumb1Value(), getThumb2Value());
		}
		return true;
	}

}