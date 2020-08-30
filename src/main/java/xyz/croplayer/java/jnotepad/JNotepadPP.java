package xyz.croplayer.java.jnotepad;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

import xyz.croplayer.java.jnotepad.local.FormLocalizationProvider;
import xyz.croplayer.java.jnotepad.local.ILocalizationProvider;
import xyz.croplayer.java.jnotepad.local.LocalizationProvider;
import xyz.croplayer.java.jnotepad.local.LocalizedAction;
import xyz.croplayer.java.jnotepad.local.LocalizedJMenu;

/**
 * <p>
 * A simple text editor. Supports tabbed editing of multiple files at once. Can
 * count the total number of characters, number of non-black characters and the
 * number of lines using the Statistics command.
 * </p>
 * The other supported functions are: turning charaters into upper and lower
 * case, inverting charater case, sorting lines lexicographically depending on
 * the locale, and removing duplicate lines from the text.
 * 
 * @author Andrej
 *
 */
/**
 * @author Aco
 *
 */
public class JNotepadPP extends JFrame {

	/**
	 * Serial version ID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The tabbed pane which holds currently open texts.
	 */
	private JTabbedPane pane = new JTabbedPane();

	/**
	 * The currently active text area, representing the data of a single file.
	 */
	private JTextArea currentlyActive;
	
	/**
	 * Map of tabs to the paths of their files.
	 */
	private Map<Component, Path> pathMap = new HashMap<>();
	/**
	 * Maps texts to the value which says if their contents were changed since
	 * the file has been opened.
	 */
	private Map<JTextArea, Boolean> changedMap = new HashMap<>();
		
	
	/**
	 * Map of tabs to their undo managers.
	 */
	private Map<Component, UndoManager> undoerMap = new HashMap<>();
	
	/**
	 * Green diskette icon, visible in the tab whose file was not modified.
	 */
	private ImageIcon unmodifiedIcon;
	/**
	 * Red diskette icon, visible in the tab whose file was modified.
	 */
	private ImageIcon modifiedIcon;

	/**
	 * Label holding a number, the length of the file.
	 */
	private JLabel lengthLabel = new JLabel();
	/**
	 * Label holding a number, the line of the caret.
	 */
	private JLabel lineLabel = new JLabel();
	/**
	 * Label holding a number, the column of the caret.
	 */
	private JLabel columnLabel = new JLabel();
	/**
	 * Label holding a number, the length of the current selection.
	 */
	private JLabel selectionLabel = new JLabel();
	/**
	 * Label holding the current time and date.
	 */
	private JLabel timeLabel = new JLabel();

	/**
	 * The list of buttons enables only when a selection exists.
	 */
	private List<AbstractButton> selectionDependentButtons = new ArrayList<>();

	/**
	 * True if the program is closing, used to end the timer thread.
	 */
	private volatile boolean endTimer = false;

	/**
	 * The current locale, used for sorting.
	 */
	private Locale currentLocale = new Locale("en");

	/**
	 * The localization provider.
	 */
	private ILocalizationProvider flp = new FormLocalizationProvider(LocalizationProvider.getInstance(), this);

	/**
	 * Constructor.
	 */
	public JNotepadPP() {
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setLocation(100, 100);
		setSize(1000, 800);

		initGUI();
	}

	/**
	 * Initializes the GUI and the timer.
	 */
	private void initGUI() {

		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(pane, BorderLayout.CENTER);
		pane.addChangeListener((e) -> {
			setActive();
		});
		
		pane.setName("tabs");

		createActions();
		createMenus();
		createToolbars();

		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				closing();
			}
		});

		try {
			unmodifiedIcon = readIcon("icons/disketteGreen.png");
			modifiedIcon = readIcon("icons/disketteRed.png");
		} catch (IOException ignorable) {
		}

		addNewTab();
		setActive();

		new Thread(() -> {

			while (true) {
				SwingUtilities.invokeLater(() -> {
					timeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
				});

				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignorable) {
				}
				if (endTimer) {
					return;
				}
			}
		}).start();
	}

	/**
	 * Reads an icon from the path represented by a string.
	 * 
	 * @param pathToIcon
	 *            The path to the icon.
	 * @return The icon.
	 * @throws IOException
	 *             If an icon could not be read.
	 */
	private ImageIcon readIcon(String pathToIcon) throws IOException {
		InputStream is = this.getClass().getResourceAsStream(pathToIcon);
		if (is == null)
			return null;

		List<Byte> list = new ArrayList<>();
		byte[] bytes = new byte[4096];
		while (true) {
			int r = is.read(bytes);
			if (r < 1)
				break;
			for (int i = 0; i < r; i++) {
				list.add(bytes[i]);
			}
		}

		is.close();

		bytes = new byte[list.size()];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = list.get(i);
		}

		return new ImageIcon(bytes);
	}

	/**
	 * Adds a new empty tab to the editor. Called when the program is started and when
	 * all open tabs are closed.
	 */
	private void addNewTab() {
		addNewTab("", null);
	}

	/**
	 * Refreshed the reference to the currently active tab.
	 */
	private void setActive() {
		if (pane.getComponentCount() == 0) {
			addNewTab();
		}

		JScrollPane scrollPane = (JScrollPane) pane.getSelectedComponent();

		if (scrollPane == null) {
			return;
		}

		JViewport viewport = scrollPane.getViewport();
		currentlyActive = (JTextArea) viewport.getView();
		
		Path currentFilePath = pathMap.get(pane.getSelectedComponent());
		if (currentFilePath != null) {
			pane.setTitleAt(pane.getSelectedIndex(), pathMap.get(pane.getSelectedComponent()).getFileName().toString());
			pane.setToolTipTextAt(pane.getSelectedIndex(), pathMap.get(pane.getSelectedComponent()).toString());
			setTitle(currentFilePath.toString() + " - JNotepad++");
		} else {
			pane.setTitleAt(pane.getSelectedIndex(), "new");
			setTitle("New" + " - JNotepad++");
		}
		setTitle("JNotepad");

		updateStatus(currentlyActive);
	}

	/**
	 * Sets the accelerator and mnemonic keys for the actions.
	 */
	private void createActions() {
		newAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control N"));
		newAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
		openAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control O"));
		openAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
		saveAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control S"));
		saveAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		saveAsAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control D"));
		saveAsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		
		undoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control Z"));
		redoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control Y"));
		
		closeAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control W"));
		closeAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_W);
		exitAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control Q"));
		exitAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
		
		statisticsAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control I"));
		statisticsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
		
		copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
		copyAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
		cutAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
		pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
		pasteAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
		
		upperCaseAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
		lowerCaseAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);
		invertCaseAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
		uniqueAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
		sortAscAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		sortDescAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
	}

	/**
	 * Creates the menus.
	 */
	private void createMenus() {
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new LocalizedJMenu("File", flp);
		menuBar.add(fileMenu);

		fileMenu.add(new JMenuItem(newAction));
		fileMenu.add(new JMenuItem(openAction));
		fileMenu.add(new JMenuItem(saveAction));
		fileMenu.add(new JMenuItem(saveAsAction));
		fileMenu.add(new JMenuItem(closeAction));
		fileMenu.addSeparator();
		fileMenu.add(new JMenuItem(statisticsAction));
		fileMenu.addSeparator();
		fileMenu.add(new JMenuItem(exitAction));

		JMenu editMenu = new LocalizedJMenu("Edit", flp);
		menuBar.add(editMenu);
		editMenu.add(new JMenuItem(undoAction));
		editMenu.add(new JMenuItem(redoAction));
		editMenu.addSeparator();
		
		JMenuItem copyItem = new JMenuItem(copyAction);
		editMenu.add(copyItem);
		JMenuItem cutItem = new JMenuItem(cutAction);
		editMenu.add(cutItem);
		
		selectionDependentButtons.add(copyItem);
		selectionDependentButtons.add(cutItem);
		
		editMenu.add(new JMenuItem(pasteAction));

		JMenu toolsMenu = new LocalizedJMenu("Tools", flp);
		menuBar.add(toolsMenu);

		JMenu caseMenu = new LocalizedJMenu("Case", flp);
		toolsMenu.add(caseMenu);
		JMenuItem upperCase = new JMenuItem(upperCaseAction);
		caseMenu.add(upperCase);
		selectionDependentButtons.add(upperCase);
		JMenuItem lowerCase = new JMenuItem(lowerCaseAction);
		caseMenu.add(lowerCase);
		selectionDependentButtons.add(lowerCase);
		JMenuItem invertCase = new JMenuItem(invertCaseAction);
		caseMenu.add(invertCase);
		selectionDependentButtons.add(invertCase);

		JMenu sortMenu = new LocalizedJMenu("Sort", flp);
		toolsMenu.add(sortMenu);
		JMenuItem ascending = new JMenuItem(sortAscAction);
		sortMenu.add(ascending);
		selectionDependentButtons.add(ascending);
		JMenuItem descending = new JMenuItem(sortDescAction);
		sortMenu.add(descending);
		selectionDependentButtons.add(descending);

		JMenuItem unique = new JMenuItem(uniqueAction);
		toolsMenu.add(unique);
		selectionDependentButtons.add(unique);

		JMenu langMenu = new LocalizedJMenu("Languages", flp);
		langMenu.add(new JMenuItem(new LocalizedAction("English", flp) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				changeLanguage("en");
			}
		}));
		langMenu.add(new JMenuItem(new LocalizedAction("Croatian", flp) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				changeLanguage("hr");
			}
		}));
		langMenu.add(new JMenuItem(new LocalizedAction("Japanese", flp) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				changeLanguage("ja");
			}
		}));
		menuBar.add(langMenu);

		setJMenuBar(menuBar);
	}

	/**
	 * Changes the language depeding on the language tag.
	 * 
	 * @param languageTag
	 *            The language tag
	 */
	private void changeLanguage(String languageTag) {
		LocalizationProvider.getInstance().setLanguage(languageTag);
		currentLocale = new Locale(languageTag);
	}

	/**
	 * Creates the toolbar and the status bar.
	 */
	private void createToolbars() {
		JToolBar toolBar = new JToolBar("ToolBar");

		toolBar.add(new JButton(newAction));
		toolBar.add(new JButton(openAction));
		toolBar.add(new JButton(saveAction));
		toolBar.add(new JButton(saveAsAction));
		toolBar.add(new JButton(closeAction));
		toolBar.addSeparator();
		
		toolBar.add(new JButton(undoAction));
		toolBar.add(new JButton(redoAction));
		toolBar.addSeparator();
		
		toolBar.add(new JButton(copyAction));
		toolBar.add(new JButton(cutAction));
		toolBar.add(new JButton(pasteAction));
		toolBar.addSeparator();
		
		toolBar.add(new JButton(statisticsAction));
		toolBar.addSeparator();
		
		toolBar.add(new JButton(exitAction));
		getContentPane().add(toolBar, BorderLayout.PAGE_START);

		JToolBar statusBar = new JToolBar();
		statusBar.setBorder(BorderFactory.createLineBorder(Color.black));
		statusBar.setLayout(new BorderLayout());
		JPanel leftPanel = new JPanel();
		statusBar.add(leftPanel, BorderLayout.LINE_START);
		leftPanel.add(new JLabel("Length: "));
		leftPanel.add(lengthLabel);
		leftPanel.add(new JLabel("Ln: "));
		leftPanel.add(lineLabel);
		leftPanel.add(new JLabel("Col: "));
		leftPanel.add(columnLabel);
		leftPanel.add(new JLabel("Sel: "));
		leftPanel.add(selectionLabel);

		JPanel rightPanel = new JPanel();
		statusBar.add(rightPanel, BorderLayout.LINE_END);
		rightPanel.add(timeLabel);
		statusBar.add(new JPanel(), BorderLayout.CENTER);

		getContentPane().add(statusBar, BorderLayout.PAGE_END);
	}

	/**
	 * The creation of a new file (adding of a new tab).
	 */
	private final Action newAction = new LocalizedAction("New", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			addNewTab();
		}
	};

	/**
	 * Opening an existing file.
	 */
	private final Action openAction = new LocalizedAction("Open", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser();
			fc.setDialogTitle("Open file");
			if (fc.showOpenDialog(JNotepadPP.this) != JFileChooser.APPROVE_OPTION) {
				return;
			}

			Path filePath = fc.getSelectedFile().toPath();

			if (!Files.isReadable(filePath)) {
				JOptionPane.showMessageDialog(JNotepadPP.this, "File " + filePath + " cannot be read.", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			byte[] data = null;
			try {
				data = Files.readAllBytes(filePath);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(JNotepadPP.this,
						"Error while reading file: " + filePath + ".", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			String text = new String(data, StandardCharsets.UTF_8);
			addNewTab(text, filePath);
		}
	};
	
	
	/**
	 * Adds a new tab to the editor with the given starting text and the name of the file being edited.
	 * 
	 * If the given path is null, the name will be "new"
	 * 
	 * @param startingText The initial text of the tab.
	 * @param filePath The path to the file which was opened.
	 */
	private void addNewTab(String startingText, Path filePath) {
		JTextArea newArea = new JTextArea();
		
		newArea.setText(startingText);
		newArea.setMargin(new Insets(2, 2, 2, 2));
		changedMap.put(newArea, false);
		JScrollPane newPane = new JScrollPane(newArea);
		
		if(filePath == null) {
			pane.addTab("new", newPane);
		} else {
			pane.addTab(filePath.getFileName().toString(), newPane);
		}
		
		
		pane.setSelectedComponent(newPane);
		pane.setIconAt(pane.getSelectedIndex(), unmodifiedIcon);
		
		if(filePath != null) {
			pathMap.put(newPane, filePath);
		}
		
		setActive();
		newArea.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				changedMap.replace(newArea, true);
				pane.setIconAt(pane.getSelectedIndex(), modifiedIcon);
				lengthLabel.setText(String.valueOf(newArea.getText().length()));
			};
		});
		lengthLabel.setText(String.valueOf(newArea.getText().length()));

		newArea.addCaretListener((e) -> {
			updateStatus(newArea);
		});
		newArea.getCaret().addChangeListener((e) -> {
			int selLength = Math.abs(newArea.getCaret().getDot() - newArea.getCaret().getMark());
			selectionLabel.setText(String.valueOf(selLength));

			if (selLength == 0) {
				selectionDependentButtons.forEach(button -> button.setEnabled(false));
			} else {
				selectionDependentButtons.forEach(button -> button.setEnabled(true));
			}
		});
		
		UndoManager undoManager = new UndoManager();
		newArea.getDocument().addUndoableEditListener(undoManager);
		undoerMap.put(newArea, undoManager);
	}

	/**
	 * Updates the status bar.
	 * 
	 * @param currentlyActive2
	 */
	private void updateStatus(JTextArea currentlyActive) {
		lengthLabel.setText(String.valueOf(currentlyActive.getText().length()));
		
		int caretPosition = currentlyActive.getCaretPosition();
		try {
			int linenum = currentlyActive.getLineOfOffset(caretPosition);
			int columnnum = caretPosition - currentlyActive.getLineStartOffset(linenum);

			lineLabel.setText(String.valueOf(linenum + 1));
			columnLabel.setText(String.valueOf(columnnum));

		} catch (BadLocationException ignorable) {
		}
	}

	/**
	 * Saving a file.
	 */
	private final Action saveAction = new LocalizedAction("Save", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			setActive();
			Path currentFilePath = pathMap.get(pane.getSelectedComponent());

			if (currentFilePath == null) {
				currentFilePath = getFileSelection();

				if (currentFilePath == null) {
					return;
				}
			}

			saveFile(currentFilePath);
		}
	};

	/**
	 * Opens a file chooser dialog to select the file name to save to.
	 * 
	 * @return The selected path.
	 */
	private Path getFileSelection() {
		Path currentFilePath;
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Save file");
		if (fc.showSaveDialog(JNotepadPP.this) != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		currentFilePath = fc.getSelectedFile().toPath();

		if (Files.exists(currentFilePath)) {
			if (JOptionPane.showConfirmDialog(JNotepadPP.this, "File already exists. Overwirite?", "Confirm overwrite",
					JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
				return null;
			}
		}
		return currentFilePath;
	}

	/**
	 * Represents the "Save As..." action (always asks the user for the file
	 * path).
	 */
	private final Action saveAsAction = new LocalizedAction("SaveAs", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			setActive();
			Path currentFilePath = getFileSelection();

			if (currentFilePath == null) {
				return;
			}

			if (Files.exists(currentFilePath)) {
				if (JOptionPane.showConfirmDialog(JNotepadPP.this, "File already exists. Overwirite?",
						"Confirm overwrite", JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
					return;
				}
			}
			saveFile(currentFilePath);
		}

	};

	/**
	 * Saves a file to the given path.
	 * 
	 * @param pathToSave
	 *            The path.
	 */
	private void saveFile(Path pathToSave) {
		try {
			Files.write(pathToSave, currentlyActive.getText().getBytes(StandardCharsets.UTF_8));
			pathMap.put(pane.getSelectedComponent(), pathToSave);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(JNotepadPP.this, "Doslo je do pogreske pri snimanju datoteke.", "Greska",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		changedMap.replace(currentlyActive, false);
		pane.setIconAt(pane.getSelectedIndex(), unmodifiedIcon);
		setActive();

	}

	/**
	 * Closing a tab.
	 */
	private final Action closeAction = new LocalizedAction("Close", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			closeTab((JScrollPane) pane.getSelectedComponent());
		}

	};
	
	
	private final Action undoAction = new LocalizedAction("Undo", flp) {
		
		
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			UndoManager undo = undoerMap.get(((JScrollPane)pane.getSelectedComponent()).getViewport().getView());
			if(undo.canUndo()) {
				undo.undo();
			}
		}
	};
	
	private final Action redoAction = new LocalizedAction("Redo", flp) {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			UndoManager undo = undoerMap.get(((JScrollPane)pane.getSelectedComponent()).getViewport().getView());
			if(undo.canRedo()) {
				undo.redo();
			}
		}
	};

	/**
	 * Exits the program.
	 */
	private final Action exitAction = new LocalizedAction("Exit", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			closing();
		}
	};

	/**
	 * The statistics function. Counts the number of characters, non-blank
	 * characters and lines.
	 */
	private final Action statisticsAction = new LocalizedAction("Statistics", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			setActive();
			int charLength;
			int nonBlankLength;
			int lines;

			String text = currentlyActive.getText();
			charLength = text.length();
			nonBlankLength = text.replaceAll("\\p{Space}", "").length();
			Matcher m = Pattern.compile("\r\n|\n").matcher(text);
			lines = 1;
			while (m.find()) {
				lines++;
			}

			JOptionPane.showMessageDialog(JNotepadPP.this,
					String.format("You have %d characters, %d non-blank characters, and %d lines.", charLength,
							nonBlankLength, lines),
					"Statistics", JOptionPane.INFORMATION_MESSAGE);

		}
	};

	/**
	 * Copying a selection to the clipboard.
	 */
	private final Action copyAction = new LocalizedAction("Copy", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			currentlyActive.copy();
		}
	};

	/**
	 * Cutting a selection to the clipboard.
	 */
	private final Action cutAction = new LocalizedAction("Cut", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			currentlyActive.cut();
		}
	};

	/**
	 * Pasting a selection from the clipboard.
	 */
	private final Action pasteAction = new LocalizedAction("Paste", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			currentlyActive.paste();
		}
	};

	/**
	 * Inverting the case of the selected text.
	 */
	private final Action invertCaseAction = new LocalizedAction("Invert", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			setActive();
			changeCase(CaseChange.INVERT);
		}

	};

	/**
	 * Setting the selected text to uppercase.
	 */
	private final Action upperCaseAction = new LocalizedAction("Uppercase", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			setActive();
			changeCase(CaseChange.UPPER);
		}
	};

	/**
	 * Setting the selected text to lowercase.
	 */
	private final Action lowerCaseAction = new LocalizedAction("Lowercase", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			setActive();
			changeCase(CaseChange.LOWER);
		}
	};

	/**
	 * Changes the case of the selected text depending on the parameter.
	 * 
	 * @param change
	 *            If LOWER, the text will be changed to lowercase, if UPPER, the
	 *            text will be changed to uppercase, if INVERT, the case will be
	 *            inverted.
	 */
	private void changeCase(CaseChange change) {
		Document doc = currentlyActive.getDocument();

		int offset = 0;
		int len = Math.abs(currentlyActive.getCaret().getDot() - currentlyActive.getCaret().getMark());
		if (len == 0) {
			len = doc.getLength();
		} else {
			offset = Math.min(currentlyActive.getCaret().getDot(), currentlyActive.getCaret().getMark());
		}

		try {
			String text = doc.getText(offset, len);
			switch (change) {
			case UPPER:
				text = text.toUpperCase();
				break;
			case LOWER:
				text = text.toLowerCase();
				break;
			case INVERT:
				text = invertText(text);
				break;
			}
			doc.remove(offset, len);
			doc.insertString(offset, text, null);
		} catch (BadLocationException ignorable) {
		}
	}

	/**
	 * Inverts the case of the given text.
	 * 
	 * @param text
	 *            The text.
	 * @return The inverted text.
	 */
	private static String invertText(String text) {
		StringBuilder sb = new StringBuilder(text.length());
		for (char c : text.toCharArray()) {
			if (Character.isUpperCase(c)) {
				sb.append(Character.toLowerCase(c));
			} else if (Character.isLowerCase(c)) {
				sb.append(Character.toUpperCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Removing duplicate lines from the selected text.
	 */
	private final Action uniqueAction = new LocalizedAction("Unique", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			setActive();

			Document doc = currentlyActive.getDocument();

			int len = Math.abs(currentlyActive.getCaret().getDot() - currentlyActive.getCaret().getMark());
			int start = Math.min(currentlyActive.getCaret().getDot(), currentlyActive.getCaret().getMark());
			int end = start + len - 1;

			try {
				if (start > currentlyActive.getLineOfOffset(start)) {
					start = currentlyActive.getLineOfOffset(start);
				}
				if (end < currentlyActive.getLineEndOffset(currentlyActive.getLineOfOffset(end))) {
					end = currentlyActive.getLineEndOffset(currentlyActive.getLineOfOffset(end)) - 1;
				}

				len = end - start + 1;
				String text = doc.getText(start, len);
				doc.remove(start, len);

				String[] lines = text.split("\n");

				Set<String> set = new LinkedHashSet<>();
				for (String line : lines) {
					set.add(line);
				}

				for (String line : set) {
					doc.insertString(start, line + "\n", null);
					start += line.length() + 1;
				}

			} catch (BadLocationException ignorable) {
			}

			changedMap.replace(currentlyActive, true);
			pane.setIconAt(pane.getSelectedIndex(), modifiedIcon);
		}
	};

	/**
	 * Sorting the selected lines in ascending order.
	 */
	private final Action sortAscAction = new LocalizedAction("Ascending", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			sortLines(false);

		}
	};


	/**
	 * Sorts the lines of the selected text in order depending on the parameter.
	 * 
	 * @param isDescending
	 *            If true, the text will be sorted in descending order, if
	 *            false, it will be sorted in ascending order.
	 */
	private void sortLines(boolean isDescending) {
		setActive();

		Document doc = currentlyActive.getDocument();

		int len = Math.abs(currentlyActive.getCaret().getDot() - currentlyActive.getCaret().getMark());
		int start = Math.min(currentlyActive.getCaret().getDot(), currentlyActive.getCaret().getMark());
		int end = start + len - 1;

		try {
			if (start > currentlyActive.getLineOfOffset(start)) {
				start = currentlyActive.getLineOfOffset(start);
			}
			if (end < currentlyActive.getLineEndOffset(currentlyActive.getLineOfOffset(end))) {
				end = currentlyActive.getLineEndOffset(currentlyActive.getLineOfOffset(end)) - 1;
			}

			len = end - start + 1;
			String text = doc.getText(start, len);
			doc.remove(start, len);

			String[] lines = text.split("\n");

			List<String> list = new ArrayList<>();
			for (String line : lines) {
				list.add(line);
			}
			
			Collator collator = Collator.getInstance(currentLocale);

			if (isDescending) {
				Collections.sort(list, collator.reversed());
			} else {
				Collections.sort(list, collator);
			}

			for (String line : list) {
				doc.insertString(start, line + "\n", null);
				start += line.length() + 1;
			}

		} catch (BadLocationException ignorable) {
		}
		changedMap.replace(currentlyActive, true);
		pane.setIconAt(pane.getSelectedIndex(), modifiedIcon);
	}

	/**
	 * Sorting the selected lines in descending order.
	 */
	private final Action sortDescAction = new LocalizedAction("Descending", flp) {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			sortLines(true);
		}
	};

	/**
	 * Closes all tabs and exits disposes the frame, unless the user cancels the
	 * closing.
	 */
	private void closing() {
		for (Component tab : pane.getComponents()) {
			boolean answer = closeTab((JScrollPane) tab);
			if (!answer) {
				return;
			}
		}
		endTimer = true;
		dispose();
	}

	/**
	 * Closes the given tab. Asks the user to save changes, if they exist.
	 * 
	 * @param tab
	 *            The tab to close.
	 * @return True if the tab was closed.
	 */
	private boolean closeTab(JScrollPane tab) {
		pane.setSelectedComponent(tab);
		setActive();
		if (changedMap.get(currentlyActive)) {
			int answer = JOptionPane.showConfirmDialog(JNotepadPP.this, "Save changes?", "Closing",
					JOptionPane.YES_NO_CANCEL_OPTION);

			if (answer == JOptionPane.CANCEL_OPTION) {
				return false;
			} else if (answer == JOptionPane.YES_OPTION) {
				Path pathToSave = pathMap.get(tab);
				if (pathToSave == null) {
					pathToSave = getFileSelection();
				}
				saveFile(pathToSave);
			}
		}

		changedMap.remove(currentlyActive);
		pathMap.remove(tab);
		undoerMap.remove(tab);
		pane.remove(tab);

		if (pane.getComponentCount() == 0) {
			addNewTab();
		}
		return true;
	}

	/**
	 * Main method, starts when the program launches.
	 * 
	 * @param args
	 *            Command-line arguments, unused.
	 */
	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {
			new JNotepadPP().setVisible(true);
		});

	}

	/**
	 * Constants for determining the type of case change.
	 * 
	 * @author Andrej
	 *
	 */
	private static enum CaseChange {
		/**
		 * Indicates the case should be changed to uppercase.
		 */
		UPPER,
		/**
		 * Indicates the case should be changed to lowercase.
		 */
		LOWER,
		/**
		 * Indicates the case should be inverted.
		 */
		INVERT
	}

}
