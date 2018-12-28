package com.gallantrealm.android;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;

public class IconButton extends Button {

	private static Typeface typeface;

	public IconButton(Context context) {
		super(context);
		init();
	}

	public IconButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public IconButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public void init() {
		try {
			if (typeface == null) {
				typeface = Typeface.createFromAsset(getContext().getAssets(), "fontawesome-webfont.ttf");
			}
			setTypeface(typeface);
		} catch (Exception e) {
			// ignore
		}
	}

}
