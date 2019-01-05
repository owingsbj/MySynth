package com.gallantrealm.android;

import com.gallantrealm.android.themes.Theme;
import com.gallantrealm.mysynth.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class SelectItemDialog extends GallantDialog {

	TextView messageText;
	GridView itemsView;
	Button option1Button;
	Button option2Button;
	Button option3Button;
	int buttonPressed;
	String title;
	String message;
	String[] options;
	String checkinMessage;
	String leaderboardId;
	float score;
	String scoreMsg;
	Activity activity;
	Class[] availableItems;
	Class selectedItem;

	public SelectItemDialog(Context context, String message, Class[] availableItems, String[] options) {
		super(context, R.style.Theme_Dialog);
		activity = (Activity) context;
		this.availableItems = availableItems;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.title = null;
		this.message = message;
		this.checkinMessage = null;
		this.leaderboardId = null;
		this.options = options;
		setContentView(R.layout.select_item_dialog);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		messageText = (TextView) findViewById(R.id.messageText);
		itemsView = (GridView) findViewById(R.id.itemsView);
		option1Button = (Button) findViewById(R.id.option1Button);
		option2Button = (Button) findViewById(R.id.option2Button);
		option3Button = (Button) findViewById(R.id.option3Button);

		Typeface typeface = Theme.getTheme().getTypeface(getContext());
		if (typeface != null) {
			messageText.setTypeface(typeface);
			option1Button.setTypeface(typeface);
			option2Button.setTypeface(typeface);
			option3Button.setTypeface(typeface);
		}

		int styleId = Theme.getTheme().buttonStyleId;
		if (styleId != 0) {
			option1Button.setBackgroundResource(styleId);
			option2Button.setBackgroundResource(styleId);
			option3Button.setBackgroundResource(styleId);
		}

		
		if (message != null) {
			messageText.setText(message);
		}
		itemsView.setAdapter(new ArrayAdapter<Class>(activity, R.layout.select_item_row, availableItems) {
			@SuppressLint("NewApi")
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View row;
				if (convertView != null) {
					row = convertView;
				} else {
					row = LayoutInflater.from(getContext()).inflate(R.layout.select_item_row, parent, false);
				}
//				if (itemsView.isItemChecked(position)) {
//					row.setBackgroundColor(0xff80c0ff);
//				} else {
//					row.setBackgroundColor(0x00ffffff);
//				}
				TextView label = (TextView) row.findViewById(R.id.item_row_label);
				Class itemClass = availableItems[position];
				try {
					String name = (String) itemClass.getMethod("getStaticName").invoke(null);
					label.setText(Translator.getTranslator().translate(name));
				} catch (Exception e) {
					label.setText(Translator.getTranslator().translate(itemClass.getSimpleName()));
				}
				ImageView image = (ImageView) row.findViewById(R.id.item_row_image);
				try {
					int imageResource = (Integer) itemClass.getMethod("getStaticImageResource").invoke(null);
					if (imageResource == 0) {
						image.setVisibility(View.INVISIBLE);
					}
					image.setImageResource(imageResource);
				} catch (Exception e) {
					int resid = getContext().getApplicationContext().getResources().getIdentifier(itemClass.getSimpleName().toLowerCase(), "drawable", getContext().getApplicationContext().getPackageName());
					image.setImageResource(resid);
				}
				TextView description = (TextView) row.findViewById(R.id.item_row_description);
				try {
					String text = (String) itemClass.getMethod("getStaticDescription").invoke(null);
					description.setText(Translator.getTranslator().translate(text));
				} catch (Exception e) {
					description.setText("");
				}
				return row;
			}
		});
		itemsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
				selectedItem = availableItems[position];
				if (options == null) {
					buttonPressed = 0;
					SelectItemDialog.this.dismiss();
					SelectItemDialog.this.cancel();
				}
			}
		});

		option1Button.setVisibility(View.GONE);
		option2Button.setVisibility(View.GONE);
		option3Button.setVisibility(View.GONE);
		if (options == null) {
//			option1Button.setText("OK");
//			option1Button.setVisibility(View.VISIBLE);
		} else {
			if (options.length > 0) {
				option1Button.setText(options[0]);
				option1Button.setVisibility(View.VISIBLE);
				if (options.length > 1) {
					option2Button.setText(options[1]);
					option2Button.setVisibility(View.VISIBLE);
					if (options.length > 2) {
						option3Button.setText(options[2]);
						option3Button.setVisibility(View.VISIBLE);
					}
				}
			}
		}
		option1Button.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				buttonPressed = 0;
				SelectItemDialog.this.dismiss();
				SelectItemDialog.this.cancel();
				return true;
			}

		});
		option2Button.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				buttonPressed = 1;
				SelectItemDialog.this.dismiss();
				SelectItemDialog.this.cancel();
				return true;
			}

		});
		option3Button.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				buttonPressed = 2;
				SelectItemDialog.this.dismiss();
				SelectItemDialog.this.cancel();
				return true;
			}

		});

		Translator.getTranslator().translate(this.getWindow().getDecorView());

	}

	@Override
	public void show() {
		super.show();
	}

	@Override
	public void dismiss() {
		super.dismiss();
	}

	public int getButtonPressed() {
		return buttonPressed;
	}

	public Class getItemSelected() {
		return selectedItem;
	}

}
