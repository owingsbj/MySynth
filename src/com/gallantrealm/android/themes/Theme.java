package com.gallantrealm.android.themes;

import com.gallantrealm.mysynth.R;
import android.content.Context;
import android.graphics.Typeface;

/**
 * Theme and it's subclasses are used to parameterize features of dialogs
 * and widgets to match a common look.
 */
public class Theme {
	
	public static Theme theme;
	
	public static Theme getTheme() {
		if (theme == null) {
			theme = new DefaultTheme();
		}
		return theme;
	}
	
	public String font;
	public int themeSongId;
	public int themeBackgroundId;
	public int buttonStyleId;
	
	public Theme() {
		font = "ThemeFont.ttf";
		themeSongId = R.raw.theme_song;
		themeBackgroundId = R.raw.theme_background;
		buttonStyleId = 0;
	}
	
	private Typeface typeface;

	public Typeface getTypeface(Context context) {
		if (typeface == null) {
			try {
				typeface = Typeface.createFromAsset(context.getAssets(), font);
			} catch (Throwable e) {
				System.err.println("Could not create typeface for app.");
			}
		}
		return typeface;
	}


}
