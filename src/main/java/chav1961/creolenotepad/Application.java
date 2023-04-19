package chav1961.creolenotepad;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import javax.swing.text.DefaultCaret;

import chav1961.creolenotepad.dialogs.InsertLink;
import chav1961.creolenotepad.dialogs.Settings;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.InputStreamGetter;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.basic.interfaces.LoggerFacadeOwner;
import chav1961.purelib.basic.interfaces.ModuleAccessor;
import chav1961.purelib.basic.interfaces.OutputStreamGetter;
import chav1961.purelib.fsys.FileSystemFactory;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;
import chav1961.purelib.i18n.LocalizerFactory;
import chav1961.purelib.i18n.interfaces.Localizer;
import chav1961.purelib.i18n.interfaces.LocalizerOwner;
import chav1961.purelib.i18n.interfaces.Localizer.LocaleChangeListener;
import chav1961.purelib.i18n.interfaces.SupportedLanguages;
import chav1961.purelib.model.ContentModelFactory;
import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.model.interfaces.ContentMetadataInterface.ContentNodeMetadata;
import chav1961.purelib.model.interfaces.NodeMetadataOwner;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.LRUPersistence;
import chav1961.purelib.ui.swing.AutoBuiltForm;
import chav1961.purelib.ui.swing.SwingUtils;
import chav1961.purelib.ui.swing.interfaces.OnAction;
import chav1961.purelib.ui.swing.useful.JCloseableTab;
import chav1961.purelib.ui.swing.useful.JCreoleEditor;
import chav1961.purelib.ui.swing.useful.JEnableMaskManipulator;
import chav1961.purelib.ui.swing.useful.JFileContentManipulator;
import chav1961.purelib.ui.swing.useful.JFileSelectionDialog;
import chav1961.purelib.ui.swing.useful.JFileSelectionDialog.FilterCallback;
import chav1961.purelib.ui.swing.useful.JSimpleSplash;
import chav1961.purelib.ui.swing.useful.JStateString;
import chav1961.purelib.ui.swing.useful.interfaces.FileContentChangedEvent;

public class Application extends JFrame implements AutoCloseable, NodeMetadataOwner, LocaleChangeListener, LocalizerOwner, LoggerFacadeOwner, InputStreamGetter, OutputStreamGetter {
	private static final long 	serialVersionUID = 1L;
	
	public static final String			ARG_PROPFILE_LOCATION = "prop";
	public static final String			LRU_PREFIX = "lru.";
	public static final String			PROP_CSS_FILE = "cssFile";
	public static final String			PROP_APP_RECTANGLE = "appRectangle";
	
	public static final String			KEY_APPLICATION_TITLE = "chav1961.bt.creolenotepad.Application.title";
	public static final String			KEY_APPLICATION_MESSAGE_READY = "chav1961.bt.creolenotepad.Application.message.ready";
	public static final String			KEY_APPLICATION_MESSAGE_FILE_NOT_EXISTS = "chav1961.bt.creolenotepad.Application.message.file.not.exists";
	public static final String			KEY_APPLICATION_MESSAGE_NOT_FOUND = "chav1961.bt.creolenotepad.Application.message.notfound";
	public static final String			KEY_APPLICATION_MESSAGE_NO_CSS_FOUND = "chav1961.bt.creolenotepad.Application.message.nocssfound";
	public static final String			KEY_APPLICATION_MESSAGE_CSS_NOT_EXISTS = "chav1961.bt.creolenotepad.Application.message.cssnotexists";
	public static final String			KEY_APPLICATION_MESSAGE_REPLACED = "chav1961.bt.creolenotepad.Application.message.replaced";
	public static final String			KEY_APPLICATION_HELP_TITLE = "chav1961.bt.creolenotepad.Application.help.title";
	public static final String			KEY_APPLICATION_HELP_CONTENT = "chav1961.bt.creolenotepad.Application.help.content";

	private static final FilterCallback	CREOLE_FILTER = FilterCallback.of("Creole files", "*.cre");
	private static final FilterCallback	IMAGE_FILTER = FilterCallback.of("Image files", "*.png", "*.jpg");

	private static final String			MENU_FILE_LRU = "menu.main.file.lru";
	private static final String			MENU_FILE_SAVE = "menu.main.file.save";
	private static final String			MENU_FILE_SAVE_AS = "menu.main.file.saveAs";
	private static final String			MENU_EDIT = "menu.main.edit";
	private static final String			MENU_EDIT_UNDO = "menu.main.edit.undo";
	private static final String			MENU_EDIT_REDO = "menu.main.edit.redo";
	private static final String			MENU_EDIT_CUT = "menu.main.edit.cut";
	private static final String			MENU_EDIT_COPY = "menu.main.edit.copy";
	private static final String			MENU_EDIT_PASTE = "menu.main.edit.paste";
	private static final String			MENU_EDIT_PASTE_LINK = "menu.main.edit.pasteLink";
	private static final String			MENU_EDIT_PASTE_IMAGE = "menu.main.edit.pasteImage";
	private static final String			MENU_EDIT_FIND = "menu.main.edit.find";
	private static final String			MENU_EDIT_FIND_REPLACE = "menu.main.edit.findreplace";
	private static final String			MENU_EDIT_CAPTION_UP = "menu.main.edit.captionUp";
	private static final String			MENU_EDIT_CAPTION_DOWN = "menu.main.edit.captionDown";
	private static final String			MENU_EDIT_LIST_UP = "menu.main.edit.listUp";
	private static final String			MENU_EDIT_LIST_DOWN = "menu.main.edit.listDown";
	private static final String			MENU_EDIT_ORDERED_LIST_UP = "menu.main.edit.orderedListUp";
	private static final String			MENU_EDIT_ORDERED_LIST_DOWN = "menu.main.edit.orderedListDown";
	private static final String			MENU_EDIT_ORDERED_BOLD = "menu.main.edit.bold";
	private static final String			MENU_EDIT_ORDERED_ITALIC = "menu.main.edit.italic";
	private static final String			MENU_TOOLS_PREVIEW = "menu.main.tools.preview";

	static final String[]				MENUS = {
											MENU_FILE_LRU,
											MENU_FILE_SAVE,
											MENU_FILE_SAVE_AS,
											MENU_EDIT,
											MENU_EDIT_UNDO,
											MENU_EDIT_REDO,
											MENU_EDIT_CUT,
											MENU_EDIT_COPY,
											MENU_EDIT_PASTE,
											MENU_EDIT_PASTE_LINK,
											MENU_EDIT_PASTE_IMAGE,
											MENU_EDIT_FIND,
											MENU_EDIT_FIND_REPLACE,
											MENU_EDIT_CAPTION_UP,
											MENU_EDIT_CAPTION_DOWN,
											MENU_EDIT_LIST_UP,
											MENU_EDIT_LIST_DOWN,
											MENU_EDIT_ORDERED_LIST_UP,
											MENU_EDIT_ORDERED_LIST_DOWN,
											MENU_EDIT_ORDERED_BOLD,
											MENU_EDIT_ORDERED_ITALIC,
											MENU_TOOLS_PREVIEW
										};
	
	static final long 					FILE_LRU = 1L << 0;
	static final long 					FILE_SAVE = 1L << 1;
	static final long 					FILE_SAVE_AS = 1L << 2;
	static final long 					EDIT = 1L << 3;
	static final long 					EDIT_UNDO = 1L << 4;
	static final long 					EDIT_REDO = 1L << 5;
	static final long 					EDIT_CUT = 1L << 6;
	static final long 					EDIT_COPY = 1L << 7;
	static final long 					EDIT_PASTE = 1L << 8;
	static final long 					EDIT_PASTE_LINK = 1L << 9;
	static final long 					EDIT_PASTE_IMAGE = 1L << 10;
	static final long 					EDIT_FIND = 1L << 11;
	static final long 					EDIT_FIND_REPLACE = 1L << 12;
	static final long 					EDIT_CAPTION_UP = 1L << 13;
	static final long 					EDIT_CAPTION_DOWN = 1L << 14;
	static final long 					EDIT_LIST_UP = 1L << 15;
	static final long 					EDIT_LIST_DOWN = 1L << 16;
	static final long 					EDIT_ORDERED_LIST_UP = 1L << 17;
	static final long 					EDIT_ORDERED_LIST_DOWN = 1L << 18;
	static final long 					EDIT_ORDERED_BOLD = 1L << 19;
	static final long 					EDIT_ORDERED_ITALIC = 1L << 20;	
	static final long 					TOOLS_PREVIEW = 1L << 21;
	static final long 					TOTAL_EDIT = EDIT | EDIT_CUT | EDIT_COPY| EDIT_PASTE_LINK | EDIT_PASTE_IMAGE | EDIT_FIND | EDIT_FIND_REPLACE;
	static final long 					TOTAL_EDIT_SELECTION = EDIT_CAPTION_UP | EDIT_CAPTION_DOWN | EDIT_LIST_UP | EDIT_LIST_DOWN | EDIT_ORDERED_LIST_UP | EDIT_ORDERED_LIST_DOWN | EDIT_ORDERED_BOLD | EDIT_ORDERED_ITALIC;	
	
	private static enum FileFormat {
		CREOLE(CREOLE_FILTER);
		
		private final FilterCallback	filter;
		
		private FileFormat(final FilterCallback filter) {
			this.filter = filter;
		}
		
		public FilterCallback getFilter() {
			return filter;
		}
		
		private static FileFormat byFile(final File file) {
			if (file == null) {
				throw new NullPointerException("File to define format can't be null");
			}
			else {
				try{for(FileFormat item : values()) {
					if (item.getFilter().accept(file)) {
						return item;
					}
				}
				throw new IllegalArgumentException("File ["+file.getAbsolutePath()+"]: unknown file format");
			} catch (IOException exc) {
				throw new IllegalArgumentException(exc);
			}
			}
		}
	}
	
	private final ContentMetadataInterface	mdi;
	private final CountDownLatch			latch;
	private final File						props;
	private final SubstitutableProperties	properties;
	private final JMenuBar					menuBar;
	private final Localizer					localizer;
	private final JStateString				state;
	private final FileSystemInterface		fsi = FileSystemFactory.createFileSystem(URI.create("fsys:file:/"));
	private final LRUPersistence			persistence;
	private final JFileContentManipulator	fcm;
	private final JTabbedPane				tabs = new JTabbedPane();
	private final JEnableMaskManipulator	emm;
	private final List<String>				lruFiles = new ArrayList<>();
	
	private boolean 						contentModified = false;
	
	public Application(final ContentMetadataInterface mdi, final CountDownLatch latch, final File props) throws IOException {
		if (mdi == null) {
			throw new NullPointerException("Metadata can't be null");
		}
		else if (latch == null) {
			throw new NullPointerException("Countdown latch can't be null");
		}
		else if (props == null) {
			throw new NullPointerException("Properties file can't be null");
		}
		else {
			this.mdi = mdi;
			this.latch = latch;
			this.props = props;
			this.properties = props.isFile() && props.canRead() ? SubstitutableProperties.of(props) : new SubstitutableProperties();
			this.localizer = LocalizerFactory.getLocalizer(mdi.getRoot().getLocalizerAssociated());
			this.menuBar = SwingUtils.toJComponent(mdi.byUIPath(URI.create("ui:/model/navigation.top.mainmenu")), JMenuBar.class);
			this.emm = new JEnableMaskManipulator(MENUS, menuBar);
			
			PureLibSettings.PURELIB_LOCALIZER.push(localizer);
			PureLibSettings.PURELIB_LOCALIZER.addLocaleChangeListener(this);

			this.state = new JStateString(localizer);
			this.persistence = LRUPersistence.of(props, LRU_PREFIX);
			this.fcm = new JFileContentManipulator("system", fsi, localizer, this, this, persistence, lruFiles);
			this.fcm.setFilters(FileFormat.CREOLE.getFilter());
			this.fcm.addFileContentChangeListener((e)->processLRU(e));
			
			setJMenuBar(menuBar);
			
			getContentPane().add(tabs, BorderLayout.CENTER);
			getContentPane().add(state, BorderLayout.SOUTH);
			
			state.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

	        SwingUtils.assignActionListeners(menuBar, this);
			SwingUtils.assignExitMethod4MainWindow(this, ()->exit());
			
			if (properties.containsKey(PROP_APP_RECTANGLE)) {
				final String[]	content = properties.getProperty(PROP_APP_RECTANGLE).split("\\s*,\\s*");
				
				setBounds(Integer.valueOf(content[0]), Integer.valueOf(content[1]), Integer.valueOf(content[2]), Integer.valueOf(content[3]));
				setPreferredSize(new Dimension(Integer.valueOf(content[2]), Integer.valueOf(content[3])));
			}
			else {
				SwingUtils.centerMainWindow(this, 0.85f);
			}
	        emm.setEnableMaskOff(FILE_SAVE | FILE_SAVE_AS | TOTAL_EDIT | TOOLS_PREVIEW);
	        fillLRU(fcm.getLastUsed());

			fillLocalizedStrings();
		}
	}

	@Override
	public ContentNodeMetadata getNodeMetadata() {
		return mdi.getRoot();
	}

	@Override
	public LoggerFacade getLogger() {
		return state;
	}
	
	@Override
	public Localizer getLocalizer() {
		return localizer;
	}
	
	@Override
	public OutputStream getOutputContent() throws IOException {
		return getCurrentTab().getOutputContent();
	}

	@Override
	public InputStream getInputContent() throws IOException {
		return getCurrentTab().getInputContent();
	}
	
	@Override
	public void localeChanged(final Locale oldLocale, final Locale newLocale) throws LocalizationException {
		SwingUtils.refreshLocale(menuBar, oldLocale, newLocale);
		SwingUtils.refreshLocale(tabs, oldLocale, newLocale);
		SwingUtils.refreshLocale(state, oldLocale, newLocale);
		fillLocalizedStrings();
	}
	
	@Override
	public void close() throws IOException {
		final Rectangle	rect = getBounds();
		
		properties.setProperty(PROP_APP_RECTANGLE, String.format("%1$d,%2$d,%3$d,%4$d", rect.x, rect.y, rect.width, rect.height));
		properties.store(props);
		
		fsi.close();
		PureLibSettings.PURELIB_LOCALIZER.pop(localizer);
		PureLibSettings.PURELIB_LOCALIZER.removeLocaleChangeListener(this);
	}

	public FileSystemInterface getFileSystem() {
		return fsi;
	}
	
	public SubstitutableProperties getProperties() {
		return properties;
	}

	@OnAction("action:/newCreoleProject")
	public void newCreoleProject() {
		try{newTab();
			fcm.newFile();
		} catch (IOException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}

	@OnAction("action:/openProject")
	public void openProject() {
		try{newTab();
			fcm.openFile();
		} catch (IOException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}
	
	@OnAction("action:/saveProject")
	public void saveProject() {
		try{fcm.saveFile();
		} catch (IOException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}
	
	@OnAction("action:/saveProjectAs")
	public void saveProjectAs() {
		try{fcm.saveFileAs();
		} catch (IOException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}
	
	@OnAction("action:/exit")
	public void exit() {
		try{if (fcm.commit()) {
				latch.countDown();
			}
		} catch (IOException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}
	
	@OnAction("action:/undo")
	public void undo() {
		if (getCurrentTab().getUndoManager().canUndo()) {
			getCurrentTab().getUndoManager().undo();
			getCurrentTab().refreshUndoMenu();
		}
	}
	
	@OnAction("action:/redo")
	public void redo() {
		if (getCurrentTab().getUndoManager().canRedo()) {
			getCurrentTab().getUndoManager().redo();
			getCurrentTab().refreshUndoMenu();
		}
	}

	
	@OnAction("action:/cut")
	public void cut() {
		getCurrentTab().getEditor().cut();
	}
	
	@OnAction("action:/copy")
	public void copy() {
		getCurrentTab().getEditor().copy();
	}

	@OnAction("action:/paste")
	public void paste() {
		getCurrentTab().getEditor().paste();
	}

	@OnAction("action:/pasteLink")
	public void pasteLink() {
		final InsertLink	il = new InsertLink(getLogger(), getCurrentTab().getEditor());
		
		try{if (ask(il, localizer, 300, 100)) {
				getCurrentTab().getEditor().replaceSelection(" [["+il.link+"|"+il.title+"]] ");
			}
		} catch (ContentException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}
	
	@OnAction("action:/pasteImage")
	public void pasteImage() {
		try{for(String item : JFileSelectionDialog.select(this, localizer, fsi, JFileSelectionDialog.OPTIONS_FOR_OPEN | JFileSelectionDialog.OPTIONS_CAN_SELECT_FILE | JFileSelectionDialog.OPTIONS_FILE_MUST_EXISTS, IMAGE_FILTER)) {
				final String	lastComponent = item.substring(item.lastIndexOf('/')+1);
				
				getCurrentTab().getEditor().replaceSelection(" {{file:"+item+"|"+lastComponent+"}} ");
			}
		} catch (IOException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}		
	}
	
	@OnAction("action:/find")
	public void find() {
		try{
			showModeless(getCurrentTab().getFind(), localizer, 400, 150);
		} catch (ContentException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}
	
	@OnAction("action:/findreplace")
	public void findReplace() {
		try{
			showModeless(getCurrentTab().getFindReplace(), localizer, 400, 180);
		} catch (ContentException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}

	@OnAction("action:/paragraphCaptionUp")
	public void paragraphCaptionUp() {
		final JCreoleEditor	editor = getCurrentTab().getEditor();
		final int			pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		String				text = editor.getSelectedText();
		
		if (text.length() > 0) {
			if (text.startsWith("=")) {
				text = text.substring(1);
			}
			else {
				text = "======"+text;
			}
			editor.replaceSelection(text);
			editor.setSelectionStart(pos);
			editor.setSelectionEnd(pos + text.length());
		}
	}
	
	@OnAction("action:/paragraphCaptionDown")
	public void paragraphCaptionDown() {
		final JCreoleEditor	editor = getCurrentTab().getEditor();
		final int			pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		String				text = editor.getSelectedText();
		
		if (text.length() > 0) {
			if (text.startsWith("======")) {
				text = text.substring(6);
			}
			else {
				text = '=' + text;
			}
			editor.replaceSelection(text);
			editor.setSelectionStart(pos);
			editor.setSelectionEnd(pos + text.length());
		}
	}
	
	@OnAction("action:/paragraphListUp")
	public void paragraphListUp() {
		final JCreoleEditor	editor = getCurrentTab().getEditor();
		final int			pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		final String		text = editor.getSelectedText();
		
		if (text.length() > 0) {
			final StringBuilder	sb = new StringBuilder();
			
			for (String line : text.split("\n")) {
				if (line.startsWith("*")) {
					sb.append(line.substring(1)).append('\n');
				}
				else {
					sb.append(line).append('\n');
				}
			}
			editor.replaceSelection(sb.toString());
			editor.setCaretPosition(pos);
			editor.setSelectionStart(pos);
			editor.setSelectionEnd(pos + sb.length());
		}
	}
	
	@OnAction("action:/paragraphListDown")
	public void paragraphListDown() {
		final JCreoleEditor	editor = getCurrentTab().getEditor();
		final int			pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		final String		text = editor.getSelectedText();
		int					mark;
		
		if (text.length() > 0) {
			final StringBuilder	sb = new StringBuilder("\n");
			
			for (String line : text.split("\n")) {
				sb.append('*').append(line).append('\n');
			}
			editor.replaceSelection(sb.toString());
			mark = pos + sb.length();
			editor.setSelectionStart(pos);
			editor.setSelectionEnd(mark);
		}
	}
	
	@OnAction("action:/paragraphOrderedListUp")
	public void paragraphOrderedListUp() {
		final JCreoleEditor	editor = getCurrentTab().getEditor();
		final int			pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		final String		text = editor.getSelectedText();
		int					mark;
		
		if (text.length() > 0) {
			final StringBuilder	sb = new StringBuilder();
			
			for (String line : text.split("\n")) {
				if (line.startsWith("#")) {
					sb.append(line.substring(1)).append('\n');
				}
				else {
					sb.append(line).append('\n');
				}
			}
			editor.replaceSelection(sb.toString());
			mark = pos + sb.length();
			editor.setSelectionStart(pos);
			editor.setSelectionEnd(mark);
		}
	}
	
	@OnAction("action:/paragraphOrderedListDown")
	public void paragraphOrderedListDown() {
		final JCreoleEditor	editor = getCurrentTab().getEditor();
		final int			pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		final String		text = editor.getSelectedText();
		int					mark;
		
		if (text.length() > 0) {
			final StringBuilder	sb = new StringBuilder("\n");
			
			for (String line : text.split("\n")) {
				sb.append('#').append(line).append('\n');
			}
			editor.replaceSelection(sb.toString());
			mark = pos + sb.length();
			editor.setSelectionStart(pos);
			editor.setSelectionEnd(mark);
		}
	}
	
	@OnAction("action:/fontBold")
	public void fontBold() {
		final JCreoleEditor	editor = getCurrentTab().getEditor();
		final int			pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		int					mark;
		String				text = editor.getSelectedText();
		
		if (text.length() > 0) {
			if (text.startsWith("**") && text.endsWith("**")) {
				if (text.length() > 4) {
					editor.replaceSelection(text.substring(2, text.length()-2));
					mark = pos + text.length() - 4; 
				}
				else {
					editor.replaceSelection("");
					mark = 0;
				}
			}
			else {
				editor.replaceSelection("**"+text+"**");
				mark = pos + text.length() + 4;
			}
			editor.setSelectionStart(pos);
			editor.setSelectionEnd(mark);
		}
	}
	
	@OnAction("action:/fontItalic")
	public void fontItalic() {
		final JCreoleEditor	editor = getCurrentTab().getEditor();
		final int			pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		int					mark;
		String				text = editor.getSelectedText();

		if (text.length() > 0) {
			if (text.startsWith("//") && text.endsWith("//")) {
				if (text.length() > 4) {
					editor.replaceSelection(text.substring(2, text.length()-2));
					mark = pos + text.length() - 4; 
				}
				else {
					editor.replaceSelection("");
					mark = 0;
				}
			}
			else {
				editor.replaceSelection("//"+text+"//");
				mark = pos + text.length() + 4;
			}
			editor.setSelectionStart(pos);
			editor.setSelectionEnd(mark);
		}
	}
	
	@OnAction("action:/previewProject")
	public void previewProject(final Hashtable<String,String[]> modes) {
		switch (getCurrentTab().getPreviewMode()) {
			case EDIT	:
				getCurrentTab().setPreviewMode(PreviewMode.VIEW);
				break;
			case VIEW	:
				getCurrentTab().setPreviewMode(PreviewMode.EDIT);
				break;
			default:
				throw new UnsupportedOperationException("Preview mode ["+getCurrentTab().getPreviewMode()+"] is not implemented yet");
		}
	}
	
	@OnAction("action:/settings")
	public void settings() {
		final Settings	settings = new Settings(state, properties);
		
		try{if (ask(settings, localizer, 300, 50)) {
				settings.storeProperties(properties);
				properties.store(props);
			}
		} catch (ContentException | IOException e) {
			getLogger().message(Severity.warning, e, e.getLocalizedMessage());
		}
	}
	
	@OnAction("action:builtin:/builtin.languages")
    public void language(final Hashtable<String,String[]> langs) throws LocalizationException {
		PureLibSettings.PURELIB_LOCALIZER.setCurrentLocale(SupportedLanguages.valueOf(langs.get("lang")[0]).getLocale());
	}	
	
	@OnAction("action:/about")
	public void about() {
		SwingUtils.showAboutScreen(this, localizer, KEY_APPLICATION_HELP_TITLE, KEY_APPLICATION_HELP_CONTENT, URI.create("root://"+getClass().getCanonicalName()+"/chav1961/bt/creolenotepad/avatar.jpg"), new Dimension(640, 400));
	}
	
	void loadLRU(final String path) {
		final File	f = new File(path);
		
		if (f.exists() && f.isFile() && f.canRead()) {
			try{newTab();
				fcm.openFile(path);
			} catch (IOException e) {
				getLogger().message(Severity.error, e, e.getLocalizedMessage());
			}
		}
		else {
			fcm.removeFileNameFromLRU(path);
			getLogger().message(Severity.warning, KEY_APPLICATION_MESSAGE_FILE_NOT_EXISTS, path);
		}
	}
	
	private CreoleTab getCurrentTab() {
		return (CreoleTab)tabs.getSelectedComponent();
	}

	private CreoleTab newTab() {
		final CreoleTab		tab = new CreoleTab(this, mdi, menuBar);
		final JCloseableTab	label = tab.getTabLabel(); 
		
		label.associate(tabs, tab);
		tabs.addTab("", tab);
		tabs.setTabComponentAt(tabs.getTabCount()-1, label);
		tabs.setSelectedIndex(tabs.getTabCount()-1);
		return tab;
	}
	
	private void processLRU(final FileContentChangedEvent<?> event) {
		switch (event.getChangeType()) {
			case LRU_LIST_REFRESHED			:
				fillLRU(fcm.getLastUsed());
				break;
			case FILE_LOADED 				:
		        getCurrentTab().getEditor().setEditable(true);
		        getCurrentTab().getEditor().setCaret(new DefaultCaret());
		        getCurrentTab().getEditor().setCaretPosition(0);
				contentModified = false;
				emm.setEnableMaskOn(FILE_SAVE_AS | TOTAL_EDIT | TOOLS_PREVIEW);
				getCurrentTab().clipboardChanged();
				fillTitle();
				break;
			case FILE_STORED 				:
				fcm.clearModificationFlag();
				contentModified = false;
				break;
			case FILE_STORED_AS 			:
				fcm.clearModificationFlag();
				contentModified = false;
				fillTitle();
				break;
			case MODIFICATION_FLAG_CLEAR 	:
				emm.setEnableMaskOff(FILE_SAVE);
				contentModified = false;
				fillTitle();
				break;
			case MODIFICATION_FLAG_SET 		:
				emm.setEnableMaskOn(FILE_SAVE_AS | FILE_SAVE);
				contentModified = true;
				fillTitle();
				break;
			case NEW_FILE_CREATED 			:
				getCurrentTab().getEditor().setEditable(true);
				getCurrentTab().getEditor().setCaret(new DefaultCaret());
				getCurrentTab().getEditor().setCaretPosition(0);
				contentModified = false;
				emm.setEnableMaskOn(FILE_SAVE_AS | TOTAL_EDIT | TOOLS_PREVIEW);
				getCurrentTab().clipboardChanged();
				fillTitle();
				break;
			default :
				throw new UnsupportedOperationException("Change type ["+event.getChangeType()+"] is not supported yet");
		}
	}
	
	private void fillLRU(final List<String> lastUsed) {
		if (lastUsed.isEmpty()) {
			emm.setEnableMaskOff(FILE_LRU);
		}
		else {
			final JMenu	menu = (JMenu)SwingUtils.findComponentByName(menuBar, MENU_FILE_LRU);
			
			menu.removeAll();
			for (String file : lastUsed) {
				final JMenuItem	item = new JMenuItem(file);
				
				item.addActionListener((e)->loadLRU(item.getText()));
				menu.add(item);
			}
			emm.setEnableMaskOn(FILE_LRU);
		}
	}

	private void fillTitle() {
		setTitle(localizer.getValue(KEY_APPLICATION_TITLE, (contentModified ? "* " : "")));
	}
	
	private void fillLocalizedStrings() {
		fillTitle();
	}

	private <T> boolean ask(final T instance, final Localizer localizer, final int width, final int height) throws ContentException {
		final ContentMetadataInterface	mdi = ContentModelFactory.forAnnotatedClass(instance.getClass());
		
		try(final AutoBuiltForm<T,?>	abf = new AutoBuiltForm<>(mdi, localizer, PureLibSettings.INTERNAL_LOADER, instance, (FormManager<?,T>)instance)) {
			
			((ModuleAccessor)instance).allowUnnamedModuleAccess(abf.getUnnamedModules());
			abf.setPreferredSize(new Dimension(width,height));
			return AutoBuiltForm.ask((JFrame)null,localizer,abf);
		}
	}
	
	private <T> void showModeless(final T instance, final Localizer localizer, final int width, final int height) throws ContentException {
		final ContentMetadataInterface	mdi = ContentModelFactory.forAnnotatedClass(instance.getClass());

		final Thread	t = new Thread(()->{
							try(final AutoBuiltForm<T,?>	abf = new AutoBuiltForm<>(mdi, localizer, PureLibSettings.INTERNAL_LOADER, instance, (FormManager<?,T>)instance)) {
								
								((ModuleAccessor)instance).allowUnnamedModuleAccess(abf.getUnnamedModules());
								abf.setPreferredSize(new Dimension(width,height));
								AutoBuiltForm.ask((JFrame)null,localizer,abf);
							} catch (ContentException e) {
								getLogger().message(Severity.warning, e, e.getLocalizedMessage());
							}
						});
		t.setDaemon(true);
		t.start();
	}
	
	public static void main(String[] args) {
		final ArgParser	parser = new ApplicationArgParser();
		int				retcode = 0;
		
		try(final JSimpleSplash		jss = new JSimpleSplash()) {
			final ArgParser			parsed = parser.parse(args);
			final CountDownLatch	latch = new CountDownLatch(1);
			final ContentMetadataInterface	mdi = ContentModelFactory.forXmlDescription(Application.class.getResourceAsStream("application.xml"));
			
			jss.start("load", 2);
			try(final Application	app = new Application(mdi, latch, parsed.getValue(ARG_PROPFILE_LOCATION, File.class))) {
				
				app.setVisible(true);
				app.getLogger().message(Severity.info, KEY_APPLICATION_MESSAGE_READY);
				
				latch.await();
			} catch (InterruptedException e) {
			}
		} catch (CommandLineParametersException | IOException e) {
			System.err.println(e.getLocalizedMessage());
			System.err.println(parser.getUsage("creolenotepad"));
			retcode = 128;
		}
		System.exit(retcode);
	}

	private static class ApplicationArgParser extends ArgParser {
		private static final ArgParser.AbstractArg[]	KEYS = {
			new FileArg(ARG_PROPFILE_LOCATION, false, "Property file location", "./.bt.creolenotepad.properties")
		};
		
		private ApplicationArgParser() {
			super(KEYS);
		}
	}
}
