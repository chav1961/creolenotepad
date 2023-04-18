package chav1961.creolenotepad;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.DefaultCaret;
import javax.swing.undo.UndoManager;

import chav1961.creolenotepad.dialogs.Find;
import chav1961.creolenotepad.dialogs.FindReplace;
import chav1961.creolenotepad.dialogs.InsertLink;
import chav1961.creolenotepad.dialogs.Settings;
import chav1961.purelib.basic.ArgParser;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.CommandLineParametersException;
import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.basic.interfaces.LoggerFacadeOwner;
import chav1961.purelib.basic.interfaces.ModuleAccessor;
import chav1961.purelib.enumerations.MarkupOutputFormat;
import chav1961.purelib.fsys.FileSystemFactory;
import chav1961.purelib.fsys.interfaces.FileSystemInterface;
import chav1961.purelib.i18n.LocalizerFactory;
import chav1961.purelib.i18n.interfaces.Localizer;
import chav1961.purelib.i18n.interfaces.Localizer.LocaleChangeListener;
import chav1961.purelib.i18n.interfaces.SupportedLanguages;
import chav1961.purelib.model.ContentModelFactory;
import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.model.interfaces.ContentMetadataInterface.ContentNodeMetadata;
import chav1961.purelib.model.interfaces.NodeMetadataOwner;
import chav1961.purelib.streams.char2char.CreoleOutputWriter;
import chav1961.purelib.streams.char2char.CreoleWriter;
import chav1961.purelib.streams.interfaces.PrologueEpilogueMaster;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.LRUPersistence;
import chav1961.purelib.ui.swing.AutoBuiltForm;
import chav1961.purelib.ui.swing.JToolBarWithMeta;
import chav1961.purelib.ui.swing.SwingUtils;
import chav1961.purelib.ui.swing.interfaces.OnAction;
import chav1961.purelib.ui.swing.useful.JCreoleEditor;
import chav1961.purelib.ui.swing.useful.JEnableMaskManipulator;
import chav1961.purelib.ui.swing.useful.JFileContentManipulator;
import chav1961.purelib.ui.swing.useful.JFileSelectionDialog;
import chav1961.purelib.ui.swing.useful.JFileSelectionDialog.FilterCallback;
import chav1961.purelib.ui.swing.useful.JSimpleSplash;
import chav1961.purelib.ui.swing.useful.JStateString;
import chav1961.purelib.ui.swing.useful.interfaces.FileContentChangedEvent;

public class Application extends JFrame implements AutoCloseable, NodeMetadataOwner, LocaleChangeListener, LoggerFacadeOwner {
	private static final long 	serialVersionUID = 1L;
	
	public static final String			ARG_PROPFILE_LOCATION = "prop";
	public static final String			LRU_PREFIX = "lru.";
	public static final String			PROP_CSS_FILE = "cssFile";
	
	public static final String			KEY_APPLICATION_TITLE = "chav1961.bt.creolenotepad.Application.title";
	public static final String			KEY_APPLICATION_MESSAGE_READY = "chav1961.bt.creolenotepad.Application.message.ready";
	public static final String			KEY_APPLICATION_MESSAGE_FILE_NOT_EXISTS = "chav1961.bt.creolenotepad.Application.message.file.not.exists";
	public static final String			KEY_APPLICATION_MESSAGE_NOT_FOUND = "chav1961.bt.creolenotepad.Application.message.notfound";
	public static final String			KEY_APPLICATION_MESSAGE_NO_CSS_FOUND = "chav1961.bt.creolenotepad.Application.message.nocssfound";
	public static final String			KEY_APPLICATION_MESSAGE_CSS_NOT_EXISTS = "chav1961.bt.creolenotepad.Application.message.cssnotexists";
	public static final String			KEY_APPLICATION_MESSAGE_REPLACED = "chav1961.bt.creolenotepad.Application.message.replaced";
	public static final String			KEY_APPLICATION_HELP_TITLE = "chav1961.bt.creolenotepad.Application.help.title";
	public static final String			KEY_APPLICATION_HELP_CONTENT = "chav1961.bt.creolenotepad.Application.help.content";

	private static final String			PROLOGUE_TEMPLATE = "<html><head><link rel=\"stylesheet\" href=\"%1$s\">></head><body>";
	
	private static final String			CARD_EDITOR = "editor";
	private static final String			CARD_VIEWER = "viewer";
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

	private static final String[]		MENUS = {
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
	
	private static final long 			FILE_LRU = 1L << 0;
	private static final long 			FILE_SAVE = 1L << 1;
	private static final long 			FILE_SAVE_AS = 1L << 2;
	private static final long 			EDIT = 1L << 3;
	private static final long 			EDIT_UNDO = 1L << 4;
	private static final long 			EDIT_REDO = 1L << 5;
	private static final long 			EDIT_CUT = 1L << 6;
	private static final long 			EDIT_COPY = 1L << 7;
	private static final long 			EDIT_PASTE = 1L << 8;
	private static final long 			EDIT_PASTE_LINK = 1L << 9;
	private static final long 			EDIT_PASTE_IMAGE = 1L << 10;
	private static final long 			EDIT_FIND = 1L << 11;
	private static final long 			EDIT_FIND_REPLACE = 1L << 12;
	private static final long 			EDIT_CAPTION_UP = 1L << 13;
	private static final long 			EDIT_CAPTION_DOWN = 1L << 14;
	private static final long 			EDIT_LIST_UP = 1L << 15;
	private static final long 			EDIT_LIST_DOWN = 1L << 16;
	private static final long 			EDIT_ORDERED_LIST_UP = 1L << 17;
	private static final long 			EDIT_ORDERED_LIST_DOWN = 1L << 18;
	private static final long 			EDIT_ORDERED_BOLD = 1L << 19;
	private static final long 			EDIT_ORDERED_ITALIC = 1L << 20;	
	private static final long 			TOOLS_PREVIEW = 1L << 21;
	private static final long 			TOTAL_EDIT = EDIT | EDIT_CUT | EDIT_COPY| EDIT_PASTE_LINK | EDIT_PASTE_IMAGE | EDIT_FIND | EDIT_FIND_REPLACE;
	private static final long 			TOTAL_EDIT_SELECTION = EDIT_CAPTION_UP | EDIT_CAPTION_DOWN | EDIT_LIST_UP | EDIT_LIST_DOWN | EDIT_ORDERED_LIST_UP | EDIT_ORDERED_LIST_DOWN | EDIT_ORDERED_BOLD | EDIT_ORDERED_ITALIC;	
	
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
	private final JToolBar					toolbar;
	private final Localizer					localizer;
	private final JStateString				state;
	private final FileSystemInterface		fsi = FileSystemFactory.createFileSystem(URI.create("fsys:file:/"));
	private final LRUPersistence			persistence;
	private final JFileContentManipulator	fcm;
	private final CardLayout				cardLayout = new CardLayout();
	private final JPanel					card = new JPanel(cardLayout);
	private final UndoManager 				manager = new UndoManager();
	private final JCreoleEditor				editor = new JCreoleEditor();
	private final JEditorPane				viewer = new JEditorPane("text/html", "");
	private final JEnableMaskManipulator	emm;
	private final Find						find;
	private final FindReplace				findReplace;
	
	private boolean 						anyOpened = false;
	private boolean 						contentModified = false;
	private boolean							inPreview = false;
	private FileFormat						fileFormat = FileFormat.CREOLE;
	
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
			this.toolbar = SwingUtils.toJComponent(mdi.byUIPath(URI.create("ui:/model/navigation.top.toolbarmenu")), JToolBar.class);
			this.emm = new JEnableMaskManipulator(MENUS, menuBar, toolbar);
			
			PureLibSettings.PURELIB_LOCALIZER.push(localizer);
			PureLibSettings.PURELIB_LOCALIZER.addLocaleChangeListener(this);

			editor.getDocument().addUndoableEditListener((e)->processUndoable(e));
			manager.discardAllEdits();
			
			viewer.setBackground(Color.LIGHT_GRAY);
			
			this.state = new JStateString(localizer);
			this.find = new Find(state, editor);
			this.findReplace = new FindReplace(state, editor);
			this.persistence = LRUPersistence.of(props, LRU_PREFIX);
			this.fcm = new JFileContentManipulator(fsi, localizer, editor, persistence);
			this.fcm.setFilters(FileFormat.CREOLE.getFilter());
			this.fcm.addFileContentChangeListener((e)->processLRU(e));
			
			setJMenuBar(menuBar);
			
			card.add(new JScrollPane(editor), CARD_EDITOR);
			card.add(new JScrollPane(viewer), CARD_VIEWER);
			cardLayout.show(card, CARD_EDITOR);
			toolbar.setFloatable(false);
			getContentPane().add(toolbar, BorderLayout.NORTH);
			getContentPane().add(card, BorderLayout.CENTER);
			getContentPane().add(state, BorderLayout.SOUTH);
			
			state.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

	        SwingUtils.assignActionListeners(menuBar, this);
	        SwingUtils.assignActionListeners(toolbar, this);
	        ((JToolBarWithMeta)toolbar).assignAccelerators(editor);
	        ((JToolBarWithMeta)toolbar).assignAccelerators(viewer);
			SwingUtils.assignExitMethod4MainWindow(this, ()->exit());
			SwingUtils.centerMainWindow(this, 0.85f);
	        emm.setEnableMaskOff(FILE_SAVE | FILE_SAVE_AS | TOTAL_EDIT | TOOLS_PREVIEW);
	        clipboardChanged();
	        fillLRU(fcm.getLastUsed());

	        editor.setEditable(false);
	        viewer.setEditable(false);
	        editor.addCaretListener((e)->refreshSelectionMenu());
	        
	        Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener((e)->clipboardChanged());	        
			
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
	
	public FileSystemInterface getFileSystem() {
		return fsi;
	}
	
	@Override
	public void localeChanged(final Locale oldLocale, final Locale newLocale) throws LocalizationException {
		SwingUtils.refreshLocale(menuBar, oldLocale, newLocale);
		SwingUtils.refreshLocale(toolbar, oldLocale, newLocale);
		SwingUtils.refreshLocale(state, oldLocale, newLocale);
		fillLocalizedStrings();
	}
	
	@Override
	public void close() throws IOException {
		fsi.close();
		PureLibSettings.PURELIB_LOCALIZER.pop(localizer);
		PureLibSettings.PURELIB_LOCALIZER.removeLocaleChangeListener(this);
	}

	@OnAction("action:/newCreoleProject")
	public void newCreoleProject() {
		try{fcm.newFile();
			fileFormat = FileFormat.CREOLE;
		} catch (IOException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}

	@OnAction("action:/openProject")
	public void openProject() {
		try{fcm.openFile();
			fileFormat = FileFormat.byFile(new File(fcm.getCurrentNameOfTheFile()));
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
			fileFormat = FileFormat.byFile(new File(fcm.getCurrentNameOfTheFile()));
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
		if (manager.canUndo()) {
			manager.undo();
			refreshUndoMenu();
		}
	}
	
	@OnAction("action:/redo")
	public void redo() {
		if (manager.canRedo()) {
			manager.redo();
			refreshUndoMenu();
		}
	}

	
	@OnAction("action:/cut")
	public void cut() {
		editor.cut();
	}
	
	@OnAction("action:/copy")
	public void copy() {
		editor.copy();
	}

	@OnAction("action:/paste")
	public void paste() {
		editor.paste();
	}

	@OnAction("action:/pasteLink")
	public void pasteLink() {
		final InsertLink	il = new InsertLink(getLogger(), editor);
		
		try{if (ask(il, localizer, 300, 100)) {
				editor.replaceSelection(" [["+il.link+"|"+il.title+"]] ");
			}
		} catch (ContentException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}
	
	@OnAction("action:/pasteImage")
	public void pasteImage() {
		try{for(String item : JFileSelectionDialog.select(this, localizer, fsi, JFileSelectionDialog.OPTIONS_FOR_OPEN | JFileSelectionDialog.OPTIONS_CAN_SELECT_FILE | JFileSelectionDialog.OPTIONS_FILE_MUST_EXISTS, IMAGE_FILTER)) {
				final String	lastComponent = item.substring(item.lastIndexOf('/')+1);
				
				editor.replaceSelection(" {{file:"+item+"|"+lastComponent+"}} ");
			}
		} catch (IOException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}		
	}
	
	@OnAction("action:/find")
	public void find() {
		try{
			showModeless(find, localizer, 400, 150);
		} catch (ContentException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}
	
	@OnAction("action:/findreplace")
	public void findReplace() {
		try{
			showModeless(findReplace, localizer, 400, 180);
		} catch (ContentException e) {
			getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
	}

	@OnAction("action:/paragraphCaptionUp")
	public void paragraphCaptionUp() {
		final int	pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		String		text = editor.getSelectedText();
		
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
		final int	pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		String		text = editor.getSelectedText();
		
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
		final int		pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		final String	text = editor.getSelectedText();
		
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
		final int		pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		final String	text = editor.getSelectedText();
		int				mark;
		
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
		final int		pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		final String	text = editor.getSelectedText();
		int				mark;
		
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
		final int		pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		final String	text = editor.getSelectedText();
		int				mark;
		
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
		final int	pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		int			mark;
		String		text = editor.getSelectedText();
		
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
		final int	pos = Math.min(editor.getCaret().getDot(),editor.getCaret().getMark());
		int			mark;
		String		text = editor.getSelectedText();

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
		inPreview = !inPreview;
		emm.setCheckMaskTo(TOOLS_PREVIEW, inPreview);
		if (inPreview) {
			final boolean		propsFound = props.isFile() && props.canRead();
			
			try{final SubstitutableProperties	subst = propsFound ? SubstitutableProperties.of(props) : new SubstitutableProperties();
				final boolean	cssFound = subst.containsKey(PROP_CSS_FILE); 
			
				if (cssFound && subst.getProperty(PROP_CSS_FILE,File.class,"").isFile() && subst.getProperty(PROP_CSS_FILE,File.class,"").canRead()) {
					PrologueEpilogueMaster<Writer,CreoleOutputWriter> pm = (wr,inst)->{wr.write(String.format(PROLOGUE_TEMPLATE, subst.getProperty(PROP_CSS_FILE))); return false;}; 
					PrologueEpilogueMaster<Writer,CreoleOutputWriter> em = (wr,inst)->{wr.write("</body>\n</html>\n"); return false;};
					
					try(final StringWriter	wr = new StringWriter();
						final CreoleWriter	cw = new CreoleWriter(wr, MarkupOutputFormat.XML2HTML, pm, em)) {
		
						cw.write(editor.getText());
						cw.write("\n\n");
						cw.flush();
						viewer.setText(wr.toString());
					}
				}
				else {
					if (cssFound) {
						getLogger().message(Severity.warning, KEY_APPLICATION_MESSAGE_CSS_NOT_EXISTS, subst.getProperty(PROP_CSS_FILE,File.class,"").getAbsolutePath());
					}
					else {
						getLogger().message(Severity.warning, KEY_APPLICATION_MESSAGE_NO_CSS_FOUND);
					}
					
					try(final StringWriter	wr = new StringWriter();
						final CreoleWriter	cw = new CreoleWriter(wr, MarkupOutputFormat.XML2HTML)) {
		
						cw.write(editor.getText());
						cw.write("\n\n");
						cw.flush();
						viewer.setText(wr.toString());
					}
				}
			} catch (IOException e) {
				getLogger().message(Severity.error, e, e.getLocalizedMessage());
			} finally {
				emm.setEnableMaskOff(TOTAL_EDIT);
				cardLayout.show(card, CARD_VIEWER);
				SwingUtilities.invokeLater(()->viewer.requestFocusInWindow());
			}
		}
		else {
			emm.setEnableMaskOn(TOTAL_EDIT);
			cardLayout.show(card, CARD_EDITOR);
			SwingUtilities.invokeLater(()->editor.requestFocusInWindow());
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
			try{fcm.openFile(path);
				fileFormat = FileFormat.byFile(new File(fcm.getCurrentNameOfTheFile()));
			} catch (IOException e) {
				getLogger().message(Severity.error, e, e.getLocalizedMessage());
			}
		}
		else {
			fcm.removeFileNameFromLRU(path);
			getLogger().message(Severity.warning, KEY_APPLICATION_MESSAGE_FILE_NOT_EXISTS, path);
		}
	}

	private void processLRU(final FileContentChangedEvent<?> event) {
		switch (event.getChangeType()) {
			case LRU_LIST_REFRESHED			:
				fillLRU(fcm.getLastUsed());
				break;
			case FILE_LOADED 				:
		        editor.setEditable(true);
		        editor.setCaret(new DefaultCaret());
		        editor.setCaretPosition(0);
				anyOpened = true;
				contentModified = false;
				emm.setEnableMaskOn(FILE_SAVE_AS | TOTAL_EDIT | TOOLS_PREVIEW);
				clipboardChanged();
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
		        editor.setEditable(true);
		        editor.setCaret(new DefaultCaret());
		        editor.setCaretPosition(0);
				anyOpened = true;
				contentModified = false;
				emm.setEnableMaskOn(FILE_SAVE_AS | TOTAL_EDIT | TOOLS_PREVIEW);
				clipboardChanged();
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

	private void clipboardChanged() {
		try{if (Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.plainTextFlavor) || Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.plainTextFlavor)) {
				emm.setEnableMaskTo(EDIT_PASTE, anyOpened);
			}
			else {
				emm.setEnableMaskOff(EDIT_PASTE);
			}
		} catch (IllegalStateException exc) {
			emm.setEnableMaskOff(EDIT_PASTE);
		}
	}

	private void refreshSelectionMenu() {
		if (editor.getCaret().getDot() != editor.getCaret().getMark()) {
			emm.setEnableMaskTo(TOTAL_EDIT_SELECTION, anyOpened);
		}
		else {
			emm.setEnableMaskOff(TOTAL_EDIT_SELECTION);
		}
	}

	private void processUndoable(final UndoableEditEvent e) {
		if (!editor.isHighlightingLocked()) {
			manager.addEdit(e.getEdit());
			refreshUndoMenu();
		}
	}

	
	private void refreshUndoMenu() {
		if (manager.canUndo()) {
			emm.setEnableMaskTo(EDIT_UNDO, anyOpened);
		}
		else {
			emm.setEnableMaskOff(EDIT_UNDO);
		}
		if (manager.canRedo()) {
			emm.setEnableMaskTo(EDIT_REDO, anyOpened);
		}
		else {
			emm.setEnableMaskOff(EDIT_REDO);
		}
	}
	
	private void fillTitle() {
		setTitle(localizer.getValue(KEY_APPLICATION_TITLE, (contentModified ? "* " : "") + fcm.getCurrentPathOfTheFile()));
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
