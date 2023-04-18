package chav1961.creolenotepad.dialogs;

import chav1961.creolenotepad.Application;
import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.ModuleAccessor;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.ui.interfaces.Action;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;
import chav1961.purelib.ui.swing.useful.JCreoleEditor;

@LocaleResourceLocation("i18n:xml:root://chav1961.bt.creolenotepad.dialogs.FindReplace/chav1961/bt/creolenotepad/i18n/localization.xml")
@LocaleResource(value="findreplace.title",tooltip="findreplace.title.tt",help="findreplace.title.help")
@Action(resource=@LocaleResource(value="findreplace.button.find",tooltip="findreplace.button.find.tt"),actionString="find")
@Action(resource=@LocaleResource(value="findreplace.button.replace",tooltip="findreplace.button.replace.tt"),actionString="replace")
@Action(resource=@LocaleResource(value="findreplace.button.replaceAll",tooltip="findreplace.button.replaceAll.tt"),actionString="replaceAll")
public class FindReplace implements FormManager<Object, FindReplace>, ModuleAccessor {
	private final LoggerFacade	facade;
	private final JCreoleEditor	editor;

	@LocaleResource(value="findreplace.string",tooltip="findreplace.string.tt")
	@Format("30ms")
	public String	toFind = "";

	@LocaleResource(value="findreplace.toReplace",tooltip="findreplace.toReplace.tt")
	@Format("30ms")
	public String	toReplace = "";
	
	@LocaleResource(value="findreplace.backward",tooltip="findreplace.backward.tt")
	@Format("1")
	public boolean	backward = false;

	@LocaleResource(value="findreplace.wholeWord",tooltip="findreplace.wholeWord.tt")
	@Format("1")
	public boolean	wholeWord = false;

	@LocaleResource(value="findreplace.useRegex",tooltip="findreplace.useRegex.tt")
	@Format("1")
	public boolean	useRegex = false;
	
	public FindReplace(final LoggerFacade facade, final JCreoleEditor editor) {
		if (facade == null) {
			throw new NullPointerException("Logger facade can't be null");
		}
		else if (editor == null) {
			throw new NullPointerException("Creole editor can't be null");
		}
		else {
			this.facade = facade;
			this.editor = editor;
		}
	}

	@Override
	public RefreshMode onField(FindReplace inst, Object id, String fieldName, Object oldValue, boolean beforeCommit) throws FlowException, LocalizationException {
		return RefreshMode.DEFAULT;
	}

	@Override
	public LoggerFacade getLogger() {
		return facade;
	}

	@Override
	public void allowUnnamedModuleAccess(final Module... unnamedModules) {
		for (Module item : unnamedModules) {
			this.getClass().getModule().addExports(this.getClass().getPackageName(),item);
		}
	}

	@Override
	public RefreshMode onAction(final FindReplace inst, final Object id, final String actionName, final Object... parameter) throws FlowException, LocalizationException {
		switch (actionName) {
			case "app:action:/FindReplace.find" 		:
				if (!InternalUtils.find(editor, toFind, backward, wholeWord, useRegex)) {
					getLogger().message(Severity.warning, Application.KEY_APPLICATION_MESSAGE_NOT_FOUND);
				}
				break;
			case "app:action:/FindReplace.replace" 		:
				if (InternalUtils.find(editor, toFind, backward, wholeWord, useRegex)) {
					editor.replaceSelection(toReplace);
				}
				else {
					getLogger().message(Severity.warning, Application.KEY_APPLICATION_MESSAGE_NOT_FOUND);
				}
				break;
			case "app:action:/FindReplace.replaceAll"	:
				int	count = 0;
				
				while (InternalUtils.find(editor, toFind, backward, wholeWord, useRegex)) {
					editor.replaceSelection(toReplace);
					count++;
				}
				if (count == 0) {
					getLogger().message(Severity.warning, Application.KEY_APPLICATION_MESSAGE_NOT_FOUND);
				}
				else {
					getLogger().message(Severity.warning, Application.KEY_APPLICATION_MESSAGE_REPLACED, count);
				}
				break;
			default :
				throw new UnsupportedOperationException("Action name ["+actionName+"] is not supported yet"); 
		}
		return RefreshMode.DEFAULT;
	}
}
