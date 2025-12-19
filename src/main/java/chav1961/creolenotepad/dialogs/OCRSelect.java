package chav1961.creolenotepad.dialogs;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;

import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.ModuleAccessor;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.i18n.interfaces.SupportedLanguages;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;

@LocaleResourceLocation("i18n:xml:root://chav1961.creolenotepad.dialogs.OCRSelect/chav1961/creolenotepad/i18n/localization.xml")
@LocaleResource(value="OCRSelect.title",tooltip="OCRSelect.title.tt",help="OCRSelect.title.help")
public class OCRSelect implements FormManager<Object, OCRSelect>, ModuleAccessor {
	@LocaleResource(value="OCRSelect.file",tooltip="OCRSelect.file.tt")
	@Format("30s")
	public File					file = new File("./");

	@LocaleResource(value="OCRSelect.lang",tooltip="OCRSelect.lang.tt")
	@Format("10m")
	public SupportedLanguages	lang = SupportedLanguages.getDefaultLanguage();
	
	public OCRSelect() {
	}
	
	@Override
	public RefreshMode onField(LoggerFacade logger,final OCRSelect inst, final Object id, final String fieldName, final Object oldValue, final boolean beforeCommit) throws FlowException, LocalizationException {
		return "fromClipboard".equals(fieldName) ? RefreshMode.RECORD_ONLY : RefreshMode.DEFAULT;
	}

	@Override
	public void allowUnnamedModuleAccess(final Module... unnamedModules) {
		for (Module item : unnamedModules) {
			this.getClass().getModule().addExports(this.getClass().getPackageName(),item);
		}
	}

	public static boolean isImageInClipboard() {
		return Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.imageFlavor);
	}
}
