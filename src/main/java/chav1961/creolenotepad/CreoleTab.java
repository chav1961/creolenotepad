package chav1961.creolenotepad;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.net.URI;

import javax.swing.JEditorPane;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;

import chav1961.purelib.model.interfaces.ContentMetadataInterface;
import chav1961.purelib.ui.swing.JToolBarWithMeta;
import chav1961.purelib.ui.swing.SwingUtils;
import chav1961.purelib.ui.swing.useful.JCreoleEditor;
import chav1961.purelib.ui.swing.useful.JEnableMaskManipulator;

class CreoleTab extends JPanel {
	private static final long serialVersionUID = -6709758784195051007L;
	
	private final CardLayout				cardLayout = new CardLayout();
	private final JPanel					card = new JPanel(cardLayout);
	private final UndoManager 				manager = new UndoManager();
	private final JCreoleEditor				editor = new JCreoleEditor();
	private final JEditorPane				viewer = new JEditorPane("text/html", "");
	private final JToolBar					toolbar;
	private final JEnableMaskManipulator	emm;
	
	private PreviewMode						previewMode = PreviewMode.EDIT;
	
	private boolean anyOpened = false;
	
	public CreoleTab(final Application app, final ContentMetadataInterface mdi, final JMenuBar parentMenu) {
		setLayout(new BorderLayout());
		
		this.toolbar = SwingUtils.toJComponent(mdi.byUIPath(URI.create("ui:/model/navigation.top.toolbarmenu")), JToolBar.class);
		this.emm = new JEnableMaskManipulator(Application.MENUS, parentMenu, toolbar);
		
        SwingUtils.assignActionListeners(toolbar, app);
        ((JToolBarWithMeta)toolbar).assignAccelerators(editor);
        ((JToolBarWithMeta)toolbar).assignAccelerators(viewer);
		editor.getDocument().addUndoableEditListener((e)->processUndoable(e));
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
	}
	
	public PreviewMode getPreviewMode() {
		return previewMode;
	}
	
	public void setPreviewMode(final PreviewMode newMode) {
		if (newMode != getPreviewMode()) {
			previewMode = newMode;
			cardLayout.show(card, previewMode.getCardName());
		}
	}
	
	public long getCurrentMenuEnableState() {
		return emm.getEnableMask();
	}

	public long getCurrentMenuCheckState() {
		return emm.getCheckMask();
	}
	
	public void setCurrentMenuState(final long enableMask, final long checkMask) {
		emm.setEnableMask(enableMask);
		emm.setCheckMask(checkMask);
	}
	
	private void processUndoable(final UndoableEditEvent e) {
		if (!editor.isHighlightingLocked()) {
			manager.addEdit(e.getEdit());
			refreshUndoMenu();
		}
	}

	private void refreshUndoMenu() {
		if (manager.canUndo()) {
			emm.setEnableMaskTo(Application.EDIT_UNDO, anyOpened);
		}
		else {
			emm.setEnableMaskOff(Application.EDIT_UNDO);
		}
		if (manager.canRedo()) {
			emm.setEnableMaskTo(Application.EDIT_REDO, anyOpened);
		}
		else {
			emm.setEnableMaskOff(Application.EDIT_REDO);
		}
	}

	private void refreshSelectionMenu() {
		if (editor.getCaret().getDot() != editor.getCaret().getMark()) {
			emm.setEnableMaskTo(Application.TOTAL_EDIT_SELECTION, anyOpened);
		}
		else {
			emm.setEnableMaskOff(Application.TOTAL_EDIT_SELECTION);
		}
	}
}
