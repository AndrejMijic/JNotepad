package xyz.croplayer.java.jnotepad.local;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

/**
 * An extension of the localization provider bridge, enabling automatic
 * connection and disconnection of the bridge when a window is opened or closed.
 * 
 * @author Andrej
 *
 */
public class FormLocalizationProvider extends LocalizationProviderBridge {

	/**
	 * Constructor.
	 * 
	 * @param provider The provider to connect to.
	 */
	public FormLocalizationProvider(ILocalizationProvider provider) {
		super(provider);
	}

	/**
	 * Constructor.
	 * 
	 * @param provider
	 *            The provider to connect to.
	 * @param frame
	 *            The frame on whose opening or closing the bridge will be
	 *            connected or disconnected.
	 */
	public FormLocalizationProvider(ILocalizationProvider provider, JFrame frame) {
		super(provider);
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowOpened(WindowEvent e) {
				connect();
			}

			@Override
			public void windowClosed(WindowEvent e) {
				disconnect();
			}
		});
	}

}
