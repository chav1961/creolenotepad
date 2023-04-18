package chav1961.creolenotepad.dialogs;

import java.net.URI;

import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.ModuleAccessor;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;
import chav1961.purelib.ui.swing.useful.JCreoleEditor;

@LocaleResourceLocation("i18n:xml:root://chav1961.creolenotepad.dialogs.InsertLink/chav1961/creolenotepad/i18n/localization.xml")
@LocaleResource(value="insertlink.title",tooltip="insertlink.title.tt",help="insertlink.title.help")
public class InsertLink implements FormManager<Object, InsertLink>, ModuleAccessor {
	private final LoggerFacade	facade;
	private final JCreoleEditor	editor;

	@LocaleResource(value="insertlink.link",tooltip="insertlink.link.tt")
	@Format("30ms")
	public URI		link = URI.create("./");

	@LocaleResource(value="insertlink.title",tooltip="insertlink.title.tt")
	@Format("30ms")
	public String	title = "";
	
	public InsertLink(final LoggerFacade facade, final JCreoleEditor editor) {
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
	public RefreshMode onField(final InsertLink inst, final Object id, final String fieldName, final Object oldValue, final boolean beforeCommit) throws FlowException, LocalizationException {
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
}
