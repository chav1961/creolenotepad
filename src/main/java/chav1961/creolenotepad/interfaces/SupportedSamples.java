package chav1961.creolenotepad.interfaces;

import chav1961.purelib.i18n.interfaces.LocaleResource;
import chav1961.purelib.i18n.interfaces.LocaleResourceLocation;

@LocaleResourceLocation("i18n:xml:root://chav1961.creolenotepad.interfaces.SupportedSamples/chav1961/creolenotepad/i18n/localization.xml")
public enum SupportedSamples {
	@LocaleResource(value="SupportedSamples.96000",tooltip="SupportedSamples.96000.tt")
	S96000(96000), 
	@LocaleResource(value="SupportedSamples.48000",tooltip="SupportedSamples.48000.tt")
	S48000(48000), 
	@LocaleResource(value="SupportedSamples.44100",tooltip="SupportedSamples.44100.tt")
	S44100(44100), 
	@LocaleResource(value="SupportedSamples.22050",tooltip="SupportedSamples.22050.tt")
	S22050(22050), 
	@LocaleResource(value="SupportedSamples.16000",tooltip="SupportedSamples.16000.tt")
	S16000(16000), 
	@LocaleResource(value="SupportedSamples.11025",tooltip="SupportedSamples.11025.tt")
	S11025(11025), 
	@LocaleResource(value="SupportedSamples.8000",tooltip="SupportedSamples.8000.tt")
	S8000(8000);
	
	private final int sample;
	
	private SupportedSamples(final int sample) {
		this.sample = sample; 
	}
	
	public int getSample() {
		return sample;
	}
	
	public static SupportedSamples valueOf(final int sample) {
		for(SupportedSamples item : values()) {
			if (item.getSample() == sample) {
				return item;
			}
		}
		throw new IllegalArgumentException("Sample ["+sample+"] is not available");
	}
}
