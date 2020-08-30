package xyz.croplayer.java.jnotepad.local;

/**
 * The localization provider bridge. Listeners of this class may indirectly
 * listen to a localization provider without being directly registered. Useful
 * for avoiding registering a lot of GUI objects to a provider, which can make
 * them hard to remove if their references are lost, thus interfering with
 * garbage collection.
 * 
 * @author Andrej
 *
 */
public class LocalizationProviderBridge extends AbstractLocalizationProvider {

	/**
	 * The provider this is a bridge to.
	 */
	private ILocalizationProvider provider;
	/**
	 * This internal listener notifies all listeners of this class, as if they
	 * were listening to the bridged provider.
	 */
	private ILocalizationListener listener = new ILocalizationListener() {

		@Override
		public void localizationChanged() {
			LocalizationProviderBridge.this.fire();
		}
	};

	/**
	 * Constructor.
	 * 
	 * @param provider The provider this is a bridge to.
	 */
	public LocalizationProviderBridge(ILocalizationProvider provider) {
		this.provider = provider;
	}

	@Override
	public String getString(String string) {
		return provider.getString(string);
	}

	/**
	 * Connects the internal listener to the provider.
	 */
	public void connect() {
		provider.addLocalizationListener(listener);
	}

	/**
	 * Disconnects the internal listener from the provider.
	 */
	public void disconnect() {
		provider.removeLocalizationListener(listener);
	}
}
