package com.gallantrealm.mysynth;

import com.gallantrealm.android.Translator;
import com.zeemote.zc.event.ButtonEvent;
import com.zeemote.zc.event.IButtonListener;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class MessageDialog extends GallantDialog implements IButtonListener {
	ClientModel clientModel = ClientModel.getClientModel();

	public TextView titleText;
	public TextView messageText;
	Button option1Button;
	Button option2Button;
	Button option3Button;
	int buttonPressed = -1;
	String title;
	String message;
	String[] options;
	String checkinMessage;
	String leaderboardId;
	float score;
	String scoreMsg;
	Context context;

	public MessageDialog(Context context, String title, String message, String[] options) {
		this(context, title, message, options, null);
	}

	public MessageDialog(Context context, String title, String message, String[] options, String checkinMessage) {
		super(context, R.style.Theme_Dialog);
		this.context = context;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.title = title;
		this.message = message;
		this.checkinMessage = checkinMessage;
		this.leaderboardId = null;
		this.options = options;
		setContentView(R.layout.message_dialog);
		setCancelable(false);
		setCanceledOnTouchOutside(false);
	}

	public MessageDialog(Context context, String title, String message, String[] options, String leaderboardId, float score, String scoreMsg) {
		super(context, R.style.Theme_Dialog);
		this.context = context;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.title = title;
		this.message = message;
		this.leaderboardId = leaderboardId;
		this.checkinMessage = null;
		this.score = score;
		this.scoreMsg = scoreMsg;
		this.options = options;
		setContentView(R.layout.message_dialog);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		titleText = (TextView) findViewById(R.id.titleText);
		messageText = (TextView) findViewById(R.id.messageText);
		option1Button = (Button) findViewById(R.id.option1Button);
		option2Button = (Button) findViewById(R.id.option2Button);
		option3Button = (Button) findViewById(R.id.option3Button);

		Typeface typeface = clientModel.getTypeface(getContext());
		if (typeface != null) {
			titleText.setTypeface(typeface);
			messageText.setTypeface(typeface);
			option1Button.setTypeface(typeface);
			option2Button.setTypeface(typeface);
			option3Button.setTypeface(typeface);
		}

		int styleId = clientModel.getTheme().buttonStyleId;
		if (styleId != 0) {
			option1Button.setBackgroundResource(styleId);
			option2Button.setBackgroundResource(styleId);
			option3Button.setBackgroundResource(styleId);
		}

		if (title != null) {
			titleText.setText(title);
			titleText.setVisibility(View.VISIBLE);
		} else {
			titleText.setVisibility(View.GONE);
		}

		messageText.setText(message);
		
		option1Button.setVisibility(View.GONE);
		option2Button.setVisibility(View.GONE);
		option3Button.setVisibility(View.GONE);
		if (options == null) {
			option1Button.setText("OK");
			option1Button.setVisibility(View.VISIBLE);
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
		option1Button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonPressed = 0;
				MessageDialog.this.dismiss();
				MessageDialog.this.cancel();
			}
		});
		option2Button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonPressed = 1;
				MessageDialog.this.dismiss();
				MessageDialog.this.cancel();
			}
		});
		option3Button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buttonPressed = 2;
				MessageDialog.this.dismiss();
				MessageDialog.this.cancel();
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

	boolean controllerWasPressed;

	@Override
	public void buttonPressed(ButtonEvent buttonEvent) {
		controllerWasPressed = true;
	}

	@Override
	public void buttonReleased(ButtonEvent buttonEvent) {
		if (controllerWasPressed) {
			controllerWasPressed = false;
			if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_A) {
				buttonPressed = 0;
				MessageDialog.this.dismiss();
				MessageDialog.this.cancel();
			} else if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_B) {
				buttonPressed = options.length - 1;
				MessageDialog.this.dismiss();
				MessageDialog.this.cancel();
			}
		}
	}

	@Override
	public void onOkay() {
		buttonPressed = 0;
		super.onOkay();
	}

	@Override
	public void onCancel() {
		if (options == null) {
			buttonPressed = 0;
		} else {
			buttonPressed = options.length - 1;
		}
		super.onCancel();
	}

}
