package xyz.croplayer.java.jnotepad.local;

/**
 * Models a localization provider. 
 * 
 * @author Andrej
 *
 */
public interface ILocalizationProvider {

	/**
	 * Adds a localization listener, if not already added.
	 * 
	 * @param listener The listener.
	 */
	void addLocalizationListener(ILocalizationListener listener);
	
	/**
	 * Removes a localization listener.
	 * 
	 * @param listener The listener.
	 */
	void removeLocalizationListener(ILocalizationListener listener);
	
	/**
	 * Gets a localized string for the given key.
	 * 
	 * @param string The key.
	 * @return The localized string.
	 */
	String getString(String string);
}
