module chav1961.creolenotepad {
	requires transitive chav1961.purelib;
	requires java.base;
	requires java.desktop;
	requires java.datatransfer;
	requires transitive vosk;
	requires com.sun.jna;
	requires tess4j;
	
	exports chav1961.creolenotepad to chav1961.purelib;
	exports chav1961.creolenotepad.interfaces to chav1961.purelib;
}
