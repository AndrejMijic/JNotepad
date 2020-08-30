package xyz.croplayer.java.jnotepad.local;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract localization provider. Able to add, remove and notify listeners.
 * 
 * @author Andrej
 *
 */
public abstract class AbstractLocalizationProvider implements ILocalizationProvider {

	/**
	 * List of listeners.
	 */
	List<ILocalizationListener> listeners = new ArrayList<>();

	@Override
	public void addLocalizationListener(ILocalizationListener listener) {
		if(!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeLocalizationListener(ILocalizationListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Notifies the listeners.
	 */
	public void fire() {
		listeners.forEach(listener -> listener.localizationChanged());
	}
	
}
