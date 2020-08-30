package xyz.croplayer.java.jnotepad.local;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A singleton localization provider
 * 
 * @author Andrej
 *
 */
public class LocalizationProvider extends AbstractLocalizationProvider {

	/**
	 * The current language.
	 */
	private String language;
	/**
	 * The resource bundle for the currently active localization.
	 */
	private ResourceBundle bundle;
	/**
	 * The singleton instance.
	 */
	private static LocalizationProvider instance = new LocalizationProvider();
	
	/**
	 * Private constructor.
	 */
	private LocalizationProvider() {
		setLanguage("en");
	}
	
	/**
	 * Returns the singleton instance.
	 * 
	 * @return The singleton instance.
	 */
	public static LocalizationProvider getInstance() {
		return instance;
	}
	
	/**
	 * Sets a new localization.
	 * 
	 * @param language The language to set to.
	 */
	public void setLanguage(String language) {
		this.language = language;
		bundle = ResourceBundle.getBundle("hr.fer.zemris.java.hw11.notepadpp.local.language", Locale.forLanguageTag(this.language));
		fire();
	}

	@Override
	public String getString(String string) {
		return bundle.getString(string);
	}

}
