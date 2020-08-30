package xyz.croplayer.java.jnotepad.local;

import javax.swing.JMenu;

/**
 * Represents a localized JMenu.
 * 
 * @author Andrej
 *
 */
public class LocalizedJMenu extends JMenu {

	private static final long serialVersionUID = 1L;

	/**
	 * The localization key of this menu.
	 */
	private String key;
	/**
	 * The localization provider for this menu.
	 */
	private ILocalizationProvider provider;
	/**
	 * The internal listener. Changes the text of this menu depending on the current localization.
	 */
	private ILocalizationListener listener = new ILocalizationListener() {
		
		@Override
		public void localizationChanged() {
			setText(provider.getString(key));
		}
	};
	
	/**
	 * Constructor.
	 * 
	 * @param key The localization key of this action.
	 * @param provider The localization provider for this menu.
	 */
	public LocalizedJMenu(String key, ILocalizationProvider provider) {
		this.key = key;
		this.provider = provider;
		provider.addLocalizationListener(listener);
		
		setText(provider.getString(key));
	}
	
}
