package com.gallantrealm.android;

import java.util.Locale;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * This utility helps in translations
 */
public class Translator {

	public static Translator translator = new Translator();

	public static void setTranslator(Translator aTranslator) {
		translator = aTranslator;
	}

	public static Translator getTranslator() {
		return translator;
	}

	public static ArrayAdapter<CharSequence> getArrayAdapter(Context context, int resource, String[] selections) {
		for (int i = 0; i < selections.length; i++) {
			selections[i] = translator.translate(selections[i]);
		}
		return new ArrayAdapter(context, resource, selections);
	}
	
	private int language = 0;
	
	public void setLanguage(int language) {
		this.language = language;
	}

	/**
	 * Translates the provided view into the current language.
	 * 
	 * @param view
	 *            a view
	 */
	public final void translate(View view) {
		// System.out.println(Locale.getDefault().getLanguage());
		if (view instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) view;
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View childView = viewGroup.getChildAt(i);
				translate(childView);
			}
		} else if (view instanceof ToggleButton) {
			ToggleButton button = (ToggleButton) view;
			String textOff = button.getTextOff().toString();
			button.setTextOff(translate(textOff));
			String textOn = button.getTextOn().toString();
			button.setTextOn(translate(textOn));
		} else if (view instanceof TextView) {
			TextView textView = (TextView) view;
			String text = textView.getText().toString();
			String translatedText = translate(text);
			if (!(text.equals(translatedText))) {
				textView.setText(translatedText);
			}
		}
	}
	public final String translate(String text) {
		if (text == null || text.equals("")) {
			return "";
		}
		if (text.startsWith(" ")) {
			return " " + translate(text.substring(1));
		}
		if (text.endsWith(" ")) {
			return translate(text.substring(0, text.length() - 1)) + " ";
		}
		try {
			if (Integer.parseInt(text) >= 0) {
				return text;
			}
		} catch (NumberFormatException e) {
		}
		String translation = text;
		try {
			String lang;
			if (language == 0) {
				lang = Locale.getDefault().getLanguage();
			} else if (language == 1) {
				lang = "en";
			} else if (language == 2) {
				lang = "fr";
			} else if (language == 3) {
				lang = "de";
			} else if (language == 4) {
				lang = "es";
			} else if (language == 5) {
				lang = "ru";
			} else {
				lang = "en";
			}
			if (lang.equals("en")) {
				translation = text;
			} else if (lang.equals("es")) { // spanish
				translation = translateSpanish(text);
			} else if (lang.equals("pt")) { // portuguese
				translation = text;
			} else if (lang.equals("fr")) { // french
				translation = translateFrench(text);
			} else if (lang.equals("it")) { // italian
				translation = text;
			} else if (lang.equals("de")) { // german
				translation = translateGerman(text);
			} else if (lang.equals("ru")) { // russian
				translation = translateRussian(text);
			} else if (lang.equals("ar")) { // arabic
				translation = text;
			} else if (lang.equals("ja")) { // japanese
				translation = text;
			}
		} catch (Exception e) {
			translation = text;
		}
		return translation;
	}

	public String translateSpanish(String text) {
		String translation;
		if (text.equals("Gallant Realm")) {
			translation = "Gallant Realm";
		} else if (text.equals("Language")) {
			translation = "Lengua";
		} else if (text.equals("OK")) {
			translation = "Aceptar";
		} else if (text.equals("Quit")) {
			translation = "Dejar";
		} else if (text.equals("Help")) {
			translation = "Ayuda";
		} else if (text.equals("Settings")) {
			translation = "Ajustes";
		} else if (text.equals("Yes")) {
			translation = "Sí";
		} else if (text.equals("No")) {
			translation = "No";
		} else if (text.equals("Title")) {
			translation = "Título";
		} else if (text.equals("by")) {
			translation = "por";
		} else if (text.equals("Cancel")) {
			translation = "Cancelar";
		} else if (text.equals("Option 1")) {
			translation = "Opción 1";
		} else if (text.equals("Option 2")) {
			translation = "Opción 2";
		} else if (text.equals("Option 3")) {
			translation = "Opción 3";
		} else if (text.equals("Share it on Heyzap >>")) {
			translation = "Comparte en Heyzap >>";
		} else {
			System.out.println("} else if (text.equals(\"" + text + "\")) {");
			System.out.println("  translation = \"\";");
			translation = text;
		}
		return translation;
	}

	public String translateFrench(String text) {
		String translation;
		if (text.equals("Gallant Realm")) {
			translation = "Gallant Realm";
		} else if (text.equals("Language")) {
			translation = "La langue";
		} else if (text.equals("OK")) {
			translation = "OK";
		} else if (text.equals("Quit")) {
			translation = "Quitter";
		} else if (text.equals("Help")) {
			translation = "Aidez";
		} else if (text.equals("Settings")) {
			translation = "Paramètres";
		} else if (text.equals("Yes")) {
			translation = "Oui";
		} else if (text.equals("No")) {
			translation = "Non";
		} else if (text.equals("Title")) {
			translation = "Titre";
		} else if (text.equals("by")) {
			translation = "par";
		} else if (text.equals("Cancel")) {
			translation = "Annuler";
		} else if (text.equals("Option 1")) {
			translation = "Option 1";
		} else if (text.equals("Option 2")) {
			translation = "Option 2";
		} else if (text.equals("Option 3")) {
			translation = "Option 3";
		} else if (text.equals("Share it on Heyzap >>")) {
			translation = "Partagez-le sur Heyzap >>";
		} else {
			System.out.println("} else if (text.equals(\"" + text + "\")) {");
			System.out.println("  translation = \"\";");
			translation = text;
		}
		return translation;
	}

	public String translateGerman(String text) {
		String translation;
		if (text.equals("Gallant Realm")) {
			translation = "Gallant Realm";
		} else if (text.equals("Language")) {
			translation = "Sprache";
		} else if (text.equals("OK")) {
			translation = "OK";
		} else if (text.equals("Quit")) {
			translation = "Beenden";
		} else if (text.equals("Help")) {
			translation = "Hilfe";
		} else if (text.equals("Settings")) {
			translation = "Einstellungen";
		} else if (text.equals("Yes")) {
			translation = "Ja";
		} else if (text.equals("No")) {
			translation = "Nein";
		} else if (text.equals("Title")) {
			translation = "Titel";
		} else if (text.equals("by")) {
			translation = "von";
		} else if (text.equals("Cancel")) {
			translation = "Abbrechen";
		} else if (text.equals("Option 1")) {
			translation = "Option 1";
		} else if (text.equals("Option 2")) {
			translation = "Option 2";
		} else if (text.equals("Option 3")) {
			translation = "Option 3";
		} else if (text.equals("Share it on Heyzap >>")) {
			translation = "Teilen Sie es auf Heyzap >>";
		} else {
			System.out.println("} else if (text.equals(\"" + text + "\")) {");
			System.out.println("  translation = \"\";");
			translation = text;
		}
		return translation;
	}

	public String translateRussian(String text) {
		String translation;
		if (text.equals("Gallant Realm")) {
			translation = "Gallant Realm";
		} else if (text.equals("Language")) {
			translation = "язык";
		} else if (text.equals("OK")) {
			translation = "Хорошо";
		} else if (text.equals("Quit")) {
			translation = "Уволиться";
		} else if (text.equals("Help")) {
			translation = "Помогите";
		} else if (text.equals("Settings")) {
			translation = "настройки";
		} else if (text.equals("Yes")) {
			translation = "Да";
		} else if (text.equals("No")) {
			translation = "Нет";
		} else if (text.equals("Title")) {
			translation = "заглавие";
		} else if (text.equals("by")) {
			translation = "от";
		} else if (text.equals("Cancel")) {
			translation = "Отмена";
		} else if (text.equals("Option 1")) {
			translation = "вариант 1";
		} else if (text.equals("Option 2")) {
			translation = "вариант 2";
		} else if (text.equals("Option 3")) {
			translation = "вариант 3";
		} else if (text.equals("Share it on Heyzap >>")) {
			translation = "Доля его на Heyzap >>";
		} else {
			System.out.println("} else if (text.equals(\"" + text + "\")) {");
			System.out.println("  translation = \"\";");
			translation = text;
		}
		return translation;
	}

}
