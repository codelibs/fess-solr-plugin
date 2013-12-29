package jp.sf.fess.solr.plugin.analysis;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeSource.AttributeFactory;

public class ReloadableTokenizerFactory extends TokenizerFactory implements
        ResourceLoaderAware {
    private static final boolean VERBOSE = true; // debug

    protected static final String MONITORING_PERIOD = "monitoringPeriod";

    protected static final String MONITORING_FILE = "monitoringFile";

    protected static final String BASE_CLASS = "baseClass";

    protected static final String CLASS = "class";

    protected static final Reader ILLEGAL_STATE_READER = new Reader() {
        @Override
        public int read(final char[] cbuf, final int off, final int len) {
            throw new IllegalStateException(
                    "TokenStream contract violation: reset()/close() call missing, "
                            + "reset() called multiple times, or subclass does not call super.reset(). "
                            + "Please see Javadocs of TokenStream class for more information about the correct consuming workflow.");
        }

        @Override
        public void close() {
        }
    };

    protected ResourceLoader loader;

    protected TokenizerFactory baseTokenizerFactory;

    protected final Map<String, String> baseArgs;

    protected final String baseClass;

    protected Timer monitorTimer;

    protected volatile long factoryTimestamp;

    public ReloadableTokenizerFactory(final Map<String, String> args) {
        super(args);

        baseArgs = new HashMap<String, String>(args);
        baseClass = baseArgs.remove(BASE_CLASS);
        baseArgs.put(CLASS, baseClass);

        try {
            baseTokenizerFactory = createTokenizerFactory();
        } catch (final IOException e) {
            throw new IllegalArgumentException(
                    "Invalid parameters to create TokenizerFactory.", e);
        }

        monitorTimer = createMonitorTimer();
    }

    @Override
    public void inform(final ResourceLoader loader) throws IOException {
        this.loader = loader;

        if (baseTokenizerFactory instanceof ResourceLoaderAware) {
            ((ResourceLoaderAware) baseTokenizerFactory).inform(loader);
        }
    }

    @Override
    public Tokenizer create(final AttributeFactory factory, final Reader input) {
        return new ReloadableTokenizer(factory, input, factoryTimestamp);
    }

    protected TokenizerFactory createTokenizerFactory() throws IOException {
        if (VERBOSE) {
            System.out.println("Create " + baseClass + " with " + baseArgs);
        }
        final TokenizerFactory tokenizerFactory = TokenizerFactory.forName(
                baseClass, baseArgs);
        if (loader != null && tokenizerFactory instanceof ResourceLoaderAware) {
            ((ResourceLoaderAware) tokenizerFactory).inform(loader);
        }
        factoryTimestamp = System.currentTimeMillis();
        return tokenizerFactory;
    }

    protected Timer createMonitorTimer() {
        final File monitoringFile = new File(baseArgs.remove(MONITORING_FILE));
        final long monitoringPeriod = Long.parseLong(baseArgs
                .remove(MONITORING_PERIOD));
        final long currentTime = monitoringFile.lastModified();

        final TimerTask task = new TimerTask() {
            long timestamp = currentTime;

            @Override
            public void run() {
                final long current = monitoringFile.lastModified();
                if (VERBOSE) {
                    System.out.println("Monitoring " + monitoringFile + " ("
                            + timestamp + "," + current + ")");
                }
                try {
                    if (current > timestamp) {
                        baseTokenizerFactory = createTokenizerFactory();
                    }
                } catch (final IOException e) {
                    // ignore
                    if (VERBOSE) {
                        e.printStackTrace();
                    }
                }
            }
        };
        final Timer timer = new Timer("ReloadableTokenizer:" + baseClass);
        timer.schedule(task, monitoringPeriod, monitoringPeriod);
        return timer;
    }

    @Override
    protected void finalize() throws Throwable {
        monitorTimer.cancel();
        super.finalize();
    }

    public class ReloadableTokenizer extends Tokenizer {

        protected Tokenizer tokenizer;

        protected AttributeFactory factory;

        protected Reader input;

        protected long tokenizerTimestamp;

        ReloadableTokenizer(final AttributeFactory factory, final Reader input,
                final long timestamp) {
            super(ILLEGAL_STATE_READER);

            this.factory = factory;
            this.input = input;
            tokenizerTimestamp = timestamp;
            tokenizer = baseTokenizerFactory.create(factory, input);
        }

        @Override
        public void close() throws IOException {
            tokenizer.close();
        }

        @Override
        public void reset() throws IOException {
            tokenizer.reset();
            if (factoryTimestamp > tokenizerTimestamp) {
                if (VERBOSE) {
                    System.out
                            .println("Update ReloadableTokenizer ("
                                    + tokenizerTimestamp + ","
                                    + factoryTimestamp + ")");
                }
                tokenizer = baseTokenizerFactory.create(factory, input);
                tokenizerTimestamp = factoryTimestamp;
            }
        }

        @Override
        public boolean incrementToken() throws IOException {
            return tokenizer.incrementToken();
        }

        @Override
        public void end() throws IOException {
            tokenizer.end();
        }

        @Override
        public int hashCode() {
            return tokenizer.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return tokenizer.equals(obj);
        }

        @Override
        public String toString() {
            return tokenizer.toString();
        }

    }

}
