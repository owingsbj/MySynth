package com.gallantrealm.mysynth;

import java.util.Timer;
import java.util.TimerTask;
import com.gallantrealm.android.Translator;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class GallantSplashActivity extends GallantActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		clientModel.loadPreferences(this); // to prepare model for use later

		setContentView(R.layout.gallant_splash);
		Typeface typeface = clientModel.getTypeface(this);
		((TextView) findViewById(R.id.goggleDog)).setTypeface(typeface);
		((TextView) findViewById(R.id.appTagline)).setTypeface(typeface);

		clientModel.setGoggleDogPass(false);
		View goggleDog = findViewById(R.id.goggleDog);
		if (goggleDog != null) {
			goggleDog.setClickable(true);
			goggleDog.setOnLongClickListener(new View.OnLongClickListener() {
				public boolean onLongClick(View v) {
					clientModel.setGoggleDogPass(true);
					return false;
				}
			});
		}

// HeyzapLib.setFlags(1 << 23); // turn off Heyzap notification

		Translator.getTranslator().translate(this.getWindow().getDecorView());
	}

	public void showMainMenu() {
		try {
			Intent intent = new Intent(GallantSplashActivity.this, GallantSplashActivity.this.getClassLoader().loadClass(getString(R.string.mainMenuClassName)));
			intent.setData(getIntent().getData()); // pass along invokation params
			startActivity(intent);
			GallantSplashActivity.this.finish();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	Timer t;

	@Override
	protected void onStart() {
		super.onStart();
		t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				clientModel.updatePlayCount(GallantSplashActivity.this);
				showMainMenu();
			}
		}, 2000l);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (t != null) {
			t.cancel();
			t = null;
		}
	}

}
