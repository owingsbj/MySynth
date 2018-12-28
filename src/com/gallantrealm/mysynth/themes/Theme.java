package com.gallantrealm.mysynth.themes;

import com.gallantrealm.mysynth.R;

public class Theme {
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
}
