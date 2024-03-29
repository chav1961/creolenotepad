package chav1961.creolenotepad.dialogs;

import java.io.File;

import chav1961.creolenotepad.Application;
import chav1961.creolenotepad.interfaces.SupportedSamples;
import chav1961.purelib.basic.SubstitutableProperties;
import chav1961.purelib.basic.exceptions.FlowException;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.interfaces.LoggerFacade;
import chav1961.purelib.basic.interfaces.ModuleAccessor;
import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;
import chav1961.purelib.model.interfaces.ContentMetadataInterface.ContentNodeMetadata;
import chav1961.purelib.ui.interfaces.FormManager;
import chav1961.purelib.ui.interfaces.Format;
import chav1961.purelib.ui.interfaces.RefreshMode;
import chav1961.purelib.ui.interfaces.UIItemState;

@LocaleResourceLocation("i18n:xml:root://chav1961.creolenotepad.dialogs.Settings/chav1961/creolenotepad/i18n/localization.xml")
@LocaleResource(value="settings.title",tooltip="settings.title.tt",help="settings.title.help")
public class Settings implements FormManager<Object, Settings>, ModuleAccessor, UIItemState {
	private final LoggerFacade	facade;

	@LocaleResource(value="settings.cssFile",tooltip="settings.cssFile.tt")
	@Format(value="30s",wizardType="selectFile=true;selectDir=false;mustExists=true;forOpen=true")
	public File				cssFile = new File("./");

	@LocaleResource(value="settings.useVoiceInput",tooltip="settings.useVoiceInput.tt")
	@Format("1s")
	public boolean			useVoiceInput = false;
	
	@LocaleResource(value="settings.ruModelDir",tooltip="settings.ruModelDir.tt")
	@Format(value="30s",wizardType="selectFile=false;selectDir=true;mustExists=true;forOpen=true")
	public File				ruModelDir = new File("./");

	@LocaleResource(value="settings.enModelDir",tooltip="settings.enModelDir.tt")
	@Format(value="30s",wizardType="selectFile=false;selectDir=true;mustExists=true;forOpen=true")
	public File				enModelDir = new File("./");

	@LocaleResource(value="settings.togglePause",tooltip="settings.togglePause.tt")
	@Format("1s")
	public boolean			togglePause = false;

	@LocaleResource(value="settings.useOCR",tooltip="settings.useOCR.tt")
	@Format("1s")
	public boolean			useOCR = false;
	
	@LocaleResource(value="settings.tesseractModelDir",tooltip="settings.tesseractModelDir.tt")
	@Format(value="30s",wizardType="selectFile=false;selectDir=true;mustExists=true;forOpen=true")
	public File				tesseractModelDir = new File("./");
	
//	@LocaleResource(value="settings.sampleRate",tooltip="settings.sampleRate.tt")
//	@Format("5ms")
	public SupportedSamples	sampleRate = SupportedSamples.S16000;
	
	public Settings(final LoggerFacade facade, final SubstitutableProperties props) {
		if (facade == null) {
			throw new NullPointerException("Logger facade can't be null");
		}
		else if (props == null) {
			throw new NullPointerException("Properties can't be null");
		}
		else {
			this.facade = facade;
			this.cssFile = props.getProperty(Application.PROP_CSS_FILE, File.class, "./");
			this.useVoiceInput = props.getProperty(Application.PROP_USE_VOICE_INPUT, boolean.class, "false");
			this.ruModelDir = props.getProperty(Application.PROP_RU_MODEL, File.class, "./");
			this.enModelDir = props.getProperty(Application.PROP_EN_MODEL, File.class, "./");
			this.togglePause = props.getProperty(Application.PROP_TOGGLE_PAUSE, boolean.class, "false");
			this.useOCR = props.getProperty(Application.PROP_USE_OCR, boolean.class, "false");
			this.tesseractModelDir = props.getProperty(Application.PROP_TESSERACT_MODEL, File.class, "./");
			this.sampleRate = SupportedSamples.valueOf(props.getProperty(Application.PROP_SAMPLE_RATE, int.class, Application.PROP_DEFAULT_SAMPLE_RATE));
		}
	}

	@Override
	public RefreshMode onField(final Settings inst, final Object id, final String fieldName, final Object oldValue, final boolean beforeCommit) throws FlowException, LocalizationException {
		switch (fieldName) {
			case "useVoiceInput" : case "useOCR" :
				return RefreshMode.RECORD_ONLY;
			default :
				return RefreshMode.DEFAULT;
		}
	}

	@Override
	public LoggerFacade getLogger() {
		return facade;
	}

	@Override
	public void allowUnnamedModuleAccess(final Module... unnamedModules) {
		for (Module item : unnamedModules) {
			this.getClass().getModule().addExports(this.getClass().getPackageName(), item);
			SupportedSamples.class.getModule().addExports(SupportedSamples.class.getPackageName(), item);
		}
	}

	@Override
	public AvailableAndVisible getItemState(final ContentNodeMetadata meta) {
		switch (meta.getName()) {
			case "ruModelDir" : case "enModelDir" : case "togglePause" :
				return useVoiceInput ? AvailableAndVisible.AVAILABLE : AvailableAndVisible.NOTAVAILABLE;
			case "tesseractModelDir"	:
				return useOCR ? AvailableAndVisible.AVAILABLE : AvailableAndVisible.NOTAVAILABLE;
			default :
				return AvailableAndVisible.AVAILABLE;
		}
	}
	
	public void storeProperties(final SubstitutableProperties props) {
		if (cssFile.isFile() && cssFile.canRead()) {
			props.setProperty(Application.PROP_CSS_FILE, cssFile.getAbsolutePath());
		}
		else {
			props.remove(Application.PROP_CSS_FILE);
		}
		if (ruModelDir.isDirectory() && ruModelDir.canRead()) {
			props.setProperty(Application.PROP_RU_MODEL, ruModelDir.getAbsolutePath());
		}
		else {
			props.remove(Application.PROP_RU_MODEL);
		}
		if (enModelDir.isDirectory() && enModelDir.canRead()) {
			props.setProperty(Application.PROP_EN_MODEL, enModelDir.getAbsolutePath());
		}
		else {
			props.remove(Application.PROP_EN_MODEL);
		}
		if (tesseractModelDir.isDirectory() && tesseractModelDir.canRead()) {
			props.setProperty(Application.PROP_TESSERACT_MODEL, tesseractModelDir.getAbsolutePath());
		}
		else {
			props.remove(Application.PROP_TESSERACT_MODEL);
		}
		props.setProperty(Application.PROP_USE_VOICE_INPUT, String.valueOf(useVoiceInput));
		props.setProperty(Application.PROP_TOGGLE_PAUSE, String.valueOf(togglePause));
		props.setProperty(Application.PROP_USE_OCR, String.valueOf(useOCR));
		props.setProperty(Application.PROP_SAMPLE_RATE, String.valueOf(sampleRate.getSample()));
	}
}
