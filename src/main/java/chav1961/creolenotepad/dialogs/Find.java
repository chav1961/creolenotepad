package chav1961.creolenotepad.dialogs;

import chav1961.creolenotepad.Application;
import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.LoggerFacade.Severity;
import chav1961.purelib.basic.interfaces.ModuleAccessor;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.ui.interfaces.Action;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;
import chav1961.purelib.ui.swing.useful.JCreoleEditor;

@LocaleResourceLocation("i18n:xml:root://chav1961.bt.creolenotepad.dialogs.Find/chav1961/bt/creolenotepad/i18n/localization.xml")
@LocaleResource(value="find.title",tooltip="find.title.tt",help="find.title.help")
@Action(resource=@LocaleResource(value="find.button.find",tooltip="find.button.find.tt"),actionString="find")
public class Find implements FormManager<Object, Find>, ModuleAccessor {
	private final LoggerFacade	facade;
	private final JCreoleEditor	editor;

	@LocaleResource(value="find.string",tooltip="find.string.tt")
	@Format("30ms")
	public String	toFind = "";

	@LocaleResource(value="find.backward",tooltip="find.backward.tt")
	@Format("1")
	public boolean	backward = false;

	@LocaleResource(value="find.wholeWord",tooltip="find.wholeWord.tt")
	@Format("1")
	public boolean	wholeWord = false;

	@LocaleResource(value="find.useRegex",tooltip="find.useRegex.tt")
	@Format("1")
	public boolean	useRegex = false;
	
	public Find(final LoggerFacade facade, final JCreoleEditor editor) {
		if (facade == null) {
			throw new NullPointerException("Logger facade cn't be null");
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
	public RefreshMode onField(Find inst, Object id, String fieldName, Object oldValue, boolean beforeCommit) throws FlowException, LocalizationException {
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
	public RefreshMode onAction(final Find inst, final Object id, final String actionName, final Object... parameter) throws FlowException, LocalizationException {
		switch (actionName) {
			case "app:action:/Find.find"	:
				if(!InternalUtils.find(editor, toFind, backward, wholeWord, useRegex)) {
					getLogger().message(Severity.warning, Application.KEY_APPLICATION_MESSAGE_NOT_FOUND);
				}
				break;
			default :
				throw new UnsupportedOperationException("Action name ["+actionName+"] is not supported yet"); 
		}
		return RefreshMode.DEFAULT;
	}
}
