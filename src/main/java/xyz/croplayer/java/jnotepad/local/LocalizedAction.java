package xyz.croplayer.java.jnotepad.local;

import javax.swing.AbstractAction;

/**
 * Represents a localized action.
 * 
 * @author Andrej
 *
 */
public abstract class LocalizedAction extends AbstractAction {

	private static final long serialVersionUID = 1L;

	/**
	 * The localization key of this action.
	 */
	private String key;
	/**
	 * The key of the description of this action.
	 */
	private String descKey;
	/**
	 * The localization provider for this action.
	 */
	private ILocalizationProvider provider;
	/**
	 * The internal listener. Changes the name and description of this action depending on the current localization.
	 */
	private ILocalizationListener listener = new ILocalizationListener() {
		
		@Override
		public void localizationChanged() {
			putValue(NAME, provider.getString(key));
			putValue(SHORT_DESCRIPTION, provider.getString(descKey));
		}
	};
	
	/**
	 * Constructor.
	 * 
	 * @param key The localizaton key of this action.
	 * @param provider The localization provider.
	 */
	public LocalizedAction(String key, ILocalizationProvider provider) {
		this.key = key;
		this.provider = provider;
		this.descKey = "desc" + key;
		provider.addLocalizationListener(listener);
		
		putValue(NAME, provider.getString(key));
		putValue(SHORT_DESCRIPTION, provider.getString(descKey));
	}
	
	
}
