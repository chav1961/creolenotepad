package chav1961.creolenotepad;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;

import org.mozilla.universalchardet.UniversalDetector;

import com.j256.simplemagic.ContentType;

import chav1961.creolenotepad.dialogs.Find;
import chav1961.creolenotepad.dialogs.FindReplace;
import chav1961.creolenotepad.interfaces.PreviewMode;
import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.Utils;
import chav1961.purelib.basic.interfaces.InputStreamGetter;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.basic.interfaces.LoggerFacadeOwner;
import chav1961.purelib.basic.interfaces.OutputStreamGetter;
import chav1961.purelib.enumerations.MarkupOutputFormat;
import chav1961.purelib.i18n.interfaces.Localizer;
import chav1961.purelib.i18n.interfaces.SupportedLanguages;
import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.streams.char2char.CreoleWriter;
import chav1961.purelib.streams.interfaces.PrologueEpilogueMaster;
import chav1961.purelib.ui.swing.JToolBarWithMeta;
import chav1961.purelib.ui.swing.SwingUtils;
import chav1961.purelib.ui.swing.interfaces.FunctionalDocumentListener;
import chav1961.purelib.ui.swing.useful.JCloseableTab;
import chav1961.purelib.ui.swing.useful.JCreoleEditor;
import chav1961.purelib.ui.swing.useful.JEnableMaskManipulator;
import chav1961.purelib.ui.swing.useful.JLocalizedOptionPane;
import chav1961.purelib.ui.swing.useful.LocalizedFormatter;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

class CreoleTab extends JPanel implements LoggerFacadeOwner, InputStreamGetter, OutputStreamGetter {
	private static final long serialVersionUID = -6709758784195051007L;

	private static final String		PROLOGUE_TEMPLATE = "<html><head><link rel=\"stylesheet\" href=\"%1$s\"></head><body>";
	private static final Icon		SAVE_ICON = new ImageIcon(CreoleTab.class.getResource("icon_save_16.png"));
	private static final Icon		GRAY_SAVE_ICON = new ImageIcon(GrayFilter.createDisabledImage(((ImageIcon)SAVE_ICON).getImage()));
	private static final String		KEY_ASK_SAVE_TITLE = "chav1961.creolenotepad.CreoleTab.save.title";
	private static final String		KEY_ASK_SAVE_MESSAGE = "chav1961.creolenotepad.CreoleTab.save.message";	
	public static final String		KEY_MESSAGE_START_OCR = "chav1961.creolenotepad.CreoleTab.message.startOCR";
	public static final String		KEY_MESSAGE_END_OCR = "chav1961.creolenotepad.CreoleTab.message.endOCR";
	public static final String		KEY_MESSAGE_OCR_FAILED = "chav1961.creolenotepad.CreoleTab.message.OCRfailed";
	public static final String		KEY_MESSAGE_LOAD_FAILED = "chav1961.creolenotepad.CreoleTab.message.loadFailed";
	
	private final Application				app;
	private final int						fileSupportId;
	private final CardLayout				cardLayout = new CardLayout();
	private final JPanel					card = new JPanel(cardLayout);
	private final UndoManager 				manager = new UndoManager();
	private final JCreoleEditor				editor = new JCreoleEditor();
	private final JEditorPane				viewer = new JEditorPane("text/html", "");
	private final JToolBar					toolbar;
	private final JEnableMaskManipulator	emm;
	private final Find						find;
	private final FindReplace				findReplace;
	private final JCloseableTab				tab;
	
	private PreviewMode						previewMode = PreviewMode.EDIT;
	private boolean 						isMicrophoneEnabled = false;
	private boolean 						isModified = false;
	private String							lastInserted = "";
	
	CreoleTab(final Application app, final ContentMetadataInterface mdi, final JMenuBar parentMenu, final int fileSupportId) {
		setLayout(new BorderLayout());
		
		this.app = app;
		this.fileSupportId = fileSupportId;
		this.toolbar = SwingUtils.toJComponent(mdi.byUIPath(URI.create("ui:/model/navigation.top.toolbarmenu")), JToolBar.class);
		this.emm = new JEnableMaskManipulator(app.getEnableMaskManipulator(), Application.EDIT_MICROPHONE, toolbar);
		this.find = new Find(app.getLogger(), editor);
		this.findReplace = new FindReplace(app.getLogger(), editor);
		this.tab = new JCloseableCreoleTab(app.getLocalizer());
		
        SwingUtils.assignActionListeners(toolbar, app);
        ((JToolBarWithMeta)toolbar).assignAccelerators(editor);
        ((JToolBarWithMeta)toolbar).assignAccelerators(viewer);
		editor.getDocument().addUndoableEditListener((e)->processUndoable(e));
		editor.getDocument().addDocumentListener((FunctionalDocumentListener)(ct, e)->setModified(true));
		
		new DropTarget(editor,new DropTargetListener() {
				@Override public void dragEnter(final DropTargetDragEvent dtde) {}
				@Override public void dragOver(final DropTargetDragEvent dtde) {}
				@Override public void dropActionChanged(final DropTargetDragEvent dtde) {}
				@Override public void dragExit(final DropTargetEvent dte) {}
				
				@Override
				public void drop(final DropTargetDropEvent dtde) {
					if (dtde.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
						boolean	anyLoaded = false;
						
						try {
							for (File f : (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor)) {
								if (f.isFile() && f.getName().contains(".")) {
									final ContentType 	type = ContentType.fromFileExtension(f.getName().substring(f.getName().lastIndexOf('.')+1));
									
									if (type.getMimeType().startsWith("text")) {
										loadTextContent(f);
										anyLoaded = true;
									}
									else if (type.getMimeType().startsWith("image")) {
										loadImageContent(f);
										anyLoaded = true;
									}
								}
							}
						} catch (UnsupportedFlavorException | IOException e) {
							getLogger().message(Severity.warning, e.getLocalizedMessage());
						}
						if (anyLoaded) {
		                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
						}
					}
				}
	        });		
		
		editor.addKeyListener(new KeyListener() {
			boolean pausePressed = false;
			
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_PAUSE && isMicrophoneEnabled()) {
					if (app.getProperties().getProperty(Application.PROP_TOGGLE_PAUSE, boolean.class, "false")) {
						if (!pausePressed) {
							pausePressed = true;
							if (app.getVoiceParser().isSuspended()) {
								app.getVoiceParser().setPreferredLang(SupportedLanguages.of(editor.getInputContext().getLocale()));
								app.getVoiceParser().resume();
							}
							else {
								app.getVoiceParser().suspend();
							}
						}
					}
					else if (app.getVoiceParser().isSuspended()) {
						app.getVoiceParser().setPreferredLang(SupportedLanguages.of(editor.getInputContext().getLocale()));
						app.getVoiceParser().resume();
					}
				}
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_PAUSE && isMicrophoneEnabled()) {
					if (app.getProperties().getProperty(Application.PROP_TOGGLE_PAUSE, boolean.class, "false")) {
						pausePressed = false;
					}
					else if (!app.getVoiceParser().isSuspended()) {
						app.getVoiceParser().suspend();
					}
				}
			}
			
			@Override public void keyTyped(final KeyEvent e) {}

		});
		
		manager.discardAllEdits();
		viewer.setBackground(Color.WHITE);
		
		card.add(new JScrollPane(editor), PreviewMode.EDIT.getCardName());
		card.add(new JScrollPane(viewer), PreviewMode.VIEW.getCardName());
		cardLayout.show(card, previewMode.getCardName());

		toolbar.setFloatable(false);
		add(toolbar, BorderLayout.NORTH);
		add(card, BorderLayout.CENTER);
		
        editor.setEditable(false);
        viewer.setEditable(false);
        editor.addCaretListener((e)->refreshSelectionMenu());
		tab.setIcon(GRAY_SAVE_ICON);
		
		addComponentListener(new ComponentListener() {
			@Override public void componentResized(ComponentEvent e) {}
			@Override public void componentMoved(ComponentEvent e) {}
			@Override public void componentHidden(ComponentEvent e) {}
			
			@Override
			public void componentShown(ComponentEvent e) {
				emm.refresh();
			}
		});
		
		emm.setEnableMaskTo(Application.EDIT_MICROPHONE, app.hasMicrophone());
		emm.setCheckMaskOff(Application.EDIT_MICROPHONE);
	}

	@Override
	public LoggerFacade getLogger() {
		return SwingUtils.getNearestLogger(getParent());
	}
	
	public int getFileSupportId() {
		return fileSupportId;
	}
	
	public JCloseableTab getTabLabel() {
		return tab;
	}
	
	public PreviewMode getPreviewMode() {
		return previewMode;
	}
	
	public void setPreviewMode(final PreviewMode newMode) {
		if (newMode != getPreviewMode()) {
			previewMode = newMode;
			emm.setCheckMaskTo(Application.TOOLS_PREVIEW, previewMode == PreviewMode.VIEW);
			if (previewMode == PreviewMode.VIEW) {
				try{final boolean	cssFound = app.getProperties().containsKey(Application.PROP_CSS_FILE); 
				
					if (cssFound && app.getProperties().getProperty(Application.PROP_CSS_FILE,File.class,"").isFile() && app.getProperties().getProperty(Application.PROP_CSS_FILE,File.class,"").canRead()) {
						final PrologueEpilogueMaster pm = (wr,inst)->{((Writer)wr).write(String.format(PROLOGUE_TEMPLATE, app.getProperties().getProperty(Application.PROP_CSS_FILE, URI.class))); return false;}; 
						final PrologueEpilogueMaster em = (wr,inst)->{((Writer)wr).write("</body>\n</html>\n"); return false;};
						
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
							getLogger().message(Severity.warning, Application.KEY_APPLICATION_MESSAGE_CSS_NOT_EXISTS, app.getProperties().getProperty(Application.PROP_CSS_FILE,File.class,"").getAbsolutePath());
						}
						else {
							getLogger().message(Severity.warning, Application.KEY_APPLICATION_MESSAGE_NO_CSS_FOUND);
						}
						final PrologueEpilogueMaster pm = (wr,inst)->{((Writer)wr).write(String.format(PROLOGUE_TEMPLATE, getClass().getResource("cre.css"))); return false;}; 
						final PrologueEpilogueMaster em = (wr,inst)->{((Writer)wr).write("</body>\n</html>\n"); return false;};
						
						try(final StringWriter	wr = new StringWriter();
							final CreoleWriter	cw = new CreoleWriter(wr, MarkupOutputFormat.XML2HTML, pm, em)) {
			
							cw.write(editor.getText());
							cw.write("\n\n");
							cw.flush();
							viewer.setText(wr.toString());
						}
					}
				} catch (IOException e) {
					getLogger().message(Severity.error, e, e.getLocalizedMessage());
				} finally {
					emm.setEnableMaskOff(Application.TOTAL_EDIT);
					cardLayout.show(card, previewMode.getCardName());
					SwingUtilities.invokeLater(()->viewer.requestFocusInWindow());
				}
			}
			else {
				emm.setEnableMaskOn(Application.TOTAL_EDIT);
				cardLayout.show(card, previewMode.getCardName());
				SwingUtilities.invokeLater(()->editor.requestFocusInWindow());
			}
		}
	}

	public UndoManager getUndoManager() {
		return manager;
	}

	public JCreoleEditor getEditor() {
		return editor;
	}
	
	public Find getFind() {
		return find;
	}

	public FindReplace getFindReplace() {
		return findReplace;
	}
	
	public void refreshUndoMenu() {
		emm.setEnableMaskTo(Application.EDIT_UNDO, manager.canUndo());
		emm.setEnableMaskTo(Application.EDIT_REDO, manager.canRedo());
	}
	
	public void clipboardChanged() {
		try{if (Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.stringFlavor)) {
				emm.setEnableMaskOn(Application.EDIT_PASTE);
			}
			else {
				emm.setEnableMaskOff(Application.EDIT_PASTE);
			}
			if (Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.imageFlavor)) {
				emm.setEnableMaskOn(Application.EDIT_OCR_CLIP);
			}
			else {
				emm.setEnableMaskOff(Application.EDIT_OCR_CLIP);
			}
		} catch (IllegalStateException exc) {
			emm.setEnableMaskOff(Application.EDIT_PASTE);
			emm.setEnableMaskOff(Application.EDIT_OCR_CLIP);
		}
	}

	public void refreshSelectionMenu() {
		emm.setEnableMaskTo(Application.TOTAL_EDIT_SELECTION, editor.getCaret().getDot() != editor.getCaret().getMark());
	}

	@Override
	public OutputStream getOutputContent() throws IOException {
		return new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException{
				super.close();
				editor.setText(this.toString(PureLibSettings.DEFAULT_CONTENT_ENCODING));
			}
		};
	}

	@Override
	public InputStream getInputContent() throws IOException {
		return new ByteArrayInputStream(editor.getText().getBytes(PureLibSettings.DEFAULT_CONTENT_ENCODING));
	}
	
	public boolean isMicrophoneEnabled() {
		return isMicrophoneEnabled;
	}

	public void setMicrophoneEnabled(final boolean isMicrophoneEnabled) {
		this.isMicrophoneEnabled = isMicrophoneEnabled;
		emm.setCheckMaskTo(Application.EDIT_MICROPHONE, isMicrophoneEnabled);
	}
	
	void insertOCR(final BufferedImage image, final SupportedLanguages lang) throws IOException {
		final Cursor		oldCursor = editor.getCursor();
		final Tesseract		tesseract = new Tesseract();
		
		try{
			tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
			switch (lang) {
				case en	:
					tesseract.setLanguage("eng");
					break;
				case ru	:
					tesseract.setLanguage("rus");
					break;
				default	:
					throw new UnsupportedOperationException("Language ["+lang+"] is not supported yet");
			
			}
			tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO_OSD);
			tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
			
			getLogger().message(Severity.info, KEY_MESSAGE_START_OCR);
			editor.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			editor.replaceSelection(tesseract.doOCR(image));
			getLogger().message(Severity.info, KEY_MESSAGE_END_OCR);
		} catch (TesseractException e) {
			getLogger().message(Severity.warning, KEY_MESSAGE_OCR_FAILED, "", e.getLocalizedMessage());
			throw new IOException(e);
		} finally {
			editor.setCursor(oldCursor);
		}
	}

	void insertVoice(final String text) {
		if (!Utils.checkEmptyOrNullString(text) && !lastInserted.equalsIgnoreCase(text)) {
			final int		from = editor.getSelectionStart();
			final int		to = editor.getSelectionEnd();
			final String	selected = editor.getSelectedText() + ' ' + text;
			
			lastInserted = text;
			editor.setSelectionStart(from);
			editor.setSelectionEnd(to);
			editor.replaceSelection(selected);
			editor.setSelectionStart(from);
			editor.setSelectionEnd(from + selected.length());
		}
	}

	boolean saveContent(final boolean saveAs) {
		try{
			if (saveAs ? app.getFileContentManipulator().saveFileAs() : app.getFileContentManipulator().saveFile()) {
				setModified(false);
				return true;
			}
		} catch (IOException e) {
			app.getLogger().message(Severity.error, e, e.getLocalizedMessage());
		}
		return false;
	}
	
	private void processUndoable(final UndoableEditEvent e) {
		if (!editor.isHighlightingLocked()) {
			manager.addEdit(e.getEdit());
			refreshUndoMenu();
		}
	}
	
	private boolean isModified() {
		return app.getFileContentManipulator().wasChanged();
	}
	
	private void setModified(final boolean modified) {
		if (isModified != modified) {
			isModified = modified;
			if (isModified) {
				app.getFileContentManipulator().setModificationFlag();
				tab.setIcon(SAVE_ICON);
			}
			else {
				app.getFileContentManipulator().clearModificationFlag();
				tab.setIcon(GRAY_SAVE_ICON);
			}
		}
	}

	private void loadTextContent(final File textFile) {
		try{
			final String 		encoding = UniversalDetector.detectCharset(textFile);
		
			try(final Reader	rdr = new FileReader(textFile, Utils.checkEmptyOrNullString(encoding) ? Charset.defaultCharset() : Charset.forName(encoding))) {
				editor.replaceSelection(Utils.fromResource(rdr));
			}
		} catch (IOException exc) {
			getLogger().message(Severity.warning, KEY_MESSAGE_OCR_FAILED, textFile.getAbsolutePath(), exc.getLocalizedMessage());
		}
	}
	
	private void loadImageContent(final File imageFile) {
		try {
			insertOCR(ImageIO.read(imageFile), SupportedLanguages.of(editor.getLocale()));
		} catch (IOException exc) {
			getLogger().message(Severity.warning, KEY_MESSAGE_OCR_FAILED, imageFile.getAbsolutePath(), exc.getLocalizedMessage());
		}
	}
	
	private class JCloseableCreoleTab extends JCloseableTab {
		private static final long serialVersionUID = 7933685864528960962L;

		private final Localizer	localizer;
		
		private JCloseableCreoleTab(Localizer localizer) {
			super(localizer);
			this.localizer = localizer;
		}
		
		@Override
		public boolean closeTab() {
			if (isModified()) {
				switch (new JLocalizedOptionPane(localizer).confirm(CreoleTab.this, new LocalizedFormatter(KEY_ASK_SAVE_MESSAGE, tab.getToolTipText()), KEY_ASK_SAVE_TITLE, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION)) {
					case JOptionPane.YES_OPTION		:
						if (!saveContent(false)) {
							return false;
						}
					case JOptionPane.NO_OPTION		:
						app.removeTab(getFileSupportId());
						return super.closeTab();
					case JOptionPane.CANCEL_OPTION	:
						return false;
					default :
						throw new UnsupportedOperationException("Unknown option returned from JLocalizedOptionPane.confirm(...)");
				}
			}
			else {
				app.removeTab(getFileSupportId());
				return super.closeTab();
			}
		}
			
		@Override
		protected void onClickIcon() {
			if (isModified()) {
				saveContent(false);
			}
			else {
				super.onClickIcon();
			}
		}
	}
}
