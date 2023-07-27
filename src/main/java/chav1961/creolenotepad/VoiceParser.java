package chav1961.creolenotepad;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.State;
import java.net.URL;
import java.net.URLConnection;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat;

import org.vosk.Model;
import org.vosk.Recognizer;

import chav1961.purelib.basic.exceptions.ContentException;
import chav1961.purelib.concurrent.LightWeightListenerList;
import chav1961.purelib.concurrent.interfaces.ExecutionControlEvent;
import chav1961.purelib.concurrent.interfaces.ExecutionControlEvent.ExecutionControlEventType;
import chav1961.purelib.concurrent.interfaces.ListenableExecutionControl;
import chav1961.purelib.i18n.interfaces.SupportedLanguages;
import chav1961.purelib.json.JsonSerializer;
import chav1961.purelib.streams.charsource.StringCharSource;
import chav1961.purelib.streams.interfaces.CharacterSource;

public class VoiceParser implements ListenableExecutionControl, Closeable {
	private static final String				CAPTURE_URL = "capture://microphone?rate=48000&bits=16&channels=1&encoding=pcm&signed=signed&endian=big";
//    public final AudioFormat.Encoding ENCODING = AudioFormat.Encoding.PCM_SIGNED;
//    public final float RATE = 48000.0f;
//    public final int CHANNELS = 1;
//    public final int SAMPLE_SIZE = 16;
//    public final boolean BIG_ENDIAN = true;
	
	
	private final EnumMap<SupportedLanguages, Model>								models = new EnumMap<>(SupportedLanguages.class);
	private final LightWeightListenerList<ExecutionControlListener> 				listeners = new LightWeightListenerList<>(ExecutionControlListener.class);
	private final BlockingQueue<ExecutionControlEvent.ExecutionControlEventType>	queue = new ArrayBlockingQueue<>(10);
	private final JsonSerializer<JsonContent> 										serializer = JsonSerializer.buildSerializer(JsonContent.class);  
	private final Thread					t = new Thread(()->listen());
	private final Consumer<String> 			callback;
	private final int 						sampleRate;
	private final AtomicBoolean				started = new AtomicBoolean(false);
	private final AtomicBoolean				suspended = new AtomicBoolean(false);
	private volatile SupportedLanguages		currentLang = SupportedLanguages.getDefaultLanguage();
	
	public VoiceParser(final Consumer<String> callback, final int sampleRate) {
		if (callback == null) {
			throw new NullPointerException("Voice parser callback can't be null"); 
		}
		else if (sampleRate <= 0) {
			throw new IllegalArgumentException("Sample rate ["+sampleRate+"] must be greater than 0"); 
		}
		else if (!isMicrophoneExists()) {
			throw new IllegalStateException("Microphone is missing in your system"); 
		}
		else {
			this.callback = callback;
			this.sampleRate = sampleRate; 
					
			t.setName("Voice parser");
			t.setDaemon(true);
		}
	}
	
	public void setModel(final SupportedLanguages lang, final File modelDir) throws IOException {
		if (lang == null) {
			throw new NullPointerException("Language to set can't be null"); 
		}
		else if (modelDir == null) {
			throw new NullPointerException("Model directory to set can't be null"); 
		}
		else if (!modelDir.isDirectory() || !modelDir.canRead()) {
			throw new IllegalArgumentException("Model directory ["+modelDir.getAbsolutePath()+"] is not exists, not a directory or not accessible for you"); 
		}
		else {
			 final Model	oldModel = models.put(lang, new Model(modelDir.getAbsolutePath()));
			 
			 if (oldModel != null) {
				 oldModel.close();
			 }
		}
	}

	public void setPreferredLang(final SupportedLanguages lang) {
		if (lang == null) {
			throw new NullPointerException("Language to set can't be null"); 
		}
		else {
			this.currentLang = lang;
		}
	}
	
	public SupportedLanguages getPreferredLang() {
		return currentLang;
	}
	
	@Override
	public synchronized void start() throws RuntimeException {
		if (isStarted()) {
			throw new IllegalStateException("Voice parser is already started");
		}
		else if (t.getState() == State.TERMINATED) {
			throw new IllegalStateException("Can't start voice parser twise");
		}
		else {
			t.start();
			started.set(true);
			suspended.set(false);
			try{
				queue.put(ExecutionControlEvent.ExecutionControlEventType.STARTED);
			} catch (InterruptedException e) {
				throw new RuntimeException(e); 
			}
			final ExecutionControlEvent	ece = new ExecutionControlEvent(this, ExecutionControlEvent.ExecutionControlEventType.STARTED); 
			listeners.fireEvent((l)->l.processAction(ece));
		}
	}

	@Override
	public synchronized void suspend() throws RuntimeException {
		if (!isStarted()) {
			throw new IllegalStateException("Voice parser is not started yet");
		}
		else {
			suspended.set(true);
			
			final ExecutionControlEvent	ece = new ExecutionControlEvent(this, ExecutionControlEvent.ExecutionControlEventType.SUSPENDED); 
			listeners.fireEvent((l)->l.processAction(ece));
			try{
				queue.put(ExecutionControlEvent.ExecutionControlEventType.SUSPENDED);
			} catch (InterruptedException e) {
				throw new RuntimeException(e); 
			}
		}
	}

	@Override
	public synchronized void resume() throws RuntimeException {
		if (!isStarted()) {
			throw new IllegalStateException("Voice parser is not started yet");
		}
		else {
			suspended.set(false);
			
			final ExecutionControlEvent	ece = new ExecutionControlEvent(this, ExecutionControlEvent.ExecutionControlEventType.RESUMED); 
			listeners.fireEvent((l)->l.processAction(ece));
			try{
				queue.put(ExecutionControlEvent.ExecutionControlEventType.RESUMED);
			} catch (InterruptedException e) {
				throw new RuntimeException(e); 
			}
		}
	}

	@Override
	public synchronized void stop() throws RuntimeException {
		if (!isStarted()) {
			throw new IllegalStateException("Voice parser is not started yet");
		}
		else {
			suspended.set(false);
			started.set(false);
			try{
				queue.put(ExecutionControlEvent.ExecutionControlEventType.STOPPED);
			} catch (InterruptedException e) {
				throw new RuntimeException(e); 
			}
			t.interrupt();
			try{
				t.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			final ExecutionControlEvent	ece = new ExecutionControlEvent(this, ExecutionControlEvent.ExecutionControlEventType.STOPPED); 
			listeners.fireEvent((l)->l.processAction(ece));
		}
	}

	@Override
	public synchronized boolean isStarted() {
		return started.get();
	}

	@Override
	public synchronized boolean isSuspended() {
		return suspended.get();
	}

	@Override
	public void addExecutionControlListener(final ExecutionControlListener l) {
		if (l == null) {
			throw new NullPointerException("Listener to add can't be null");
		}
		else {
			listeners.addListener(l);
		}
	}

	@Override
	public void removeExecutionControlListener(final ExecutionControlListener l) {
		if (l == null) {
			throw new NullPointerException("Listener to remove can't be null");
		}
		else {
			listeners.removeListener(l);
		}
	}
	
	@Override
	public synchronized void close() throws IOException {
		if (isStarted()) {
			stop();
		}
		for(Entry<SupportedLanguages, Model> item : models.entrySet()) {
			item.getValue().close();
		}
	}

	private void listen() {
		try {
	        final byte[] b = new byte[8192];
	        
loop:		for (;;) {
				final ExecutionControlEventType action = queue.take();
				
				switch (action) {
					case STARTED : case RESUMED	:
						final Model	m = models.get(getPreferredLang());
						
						if (m != null) {
				        	try(final Recognizer	recognizer = new Recognizer(m, sampleRate);
				        		final Closeable		close = (Closeable)new URL(CAPTURE_URL).openConnection()) {
				        		final URLConnection	conn = (URLConnection)close;
				        		int	length;
				        		
				        		conn.connect();
				        		try(final InputStream	is = conn.getInputStream()) {
						            while (started.get() && !suspended.get() && (length = is.read(b, 0, b.length)) >= 0) {
						                if (recognizer.acceptWaveForm(b, length)) {
						                	callback.accept(toString(recognizer.getResult()));
						                } 
						                else {
						                	callback.accept(toString(recognizer.getPartialResult()));
						                }
						            }
				        		}
			                	callback.accept(toString(recognizer.getFinalResult()));
				        	}
						}
						break;
					case SUSPENDED	: 
						break;
					case STOPPED 	: 
					default			:
						break loop;
				}
			}
		} catch (Throwable exc) {
			exc.printStackTrace();
		} finally {
			started.set(false);
			suspended.set(false);
		}
	}

	
	private String toString(final String result) {
		try {
			return serializer.deserialize(new StringCharSource(result)).getContent();
		} catch (ContentException e) {
			return "";
		}
	}

	static boolean isMicrophoneExists() {
		try(final Closeable close = (Closeable) new URL(CAPTURE_URL).openConnection()) {
			final URLConnection conn = (URLConnection)close; 
			
			conn.connect();
			try(final InputStream	is = conn.getInputStream()) {
				return true;
			}
		} catch (IOException e) {
			return false;
		}
	}

}
