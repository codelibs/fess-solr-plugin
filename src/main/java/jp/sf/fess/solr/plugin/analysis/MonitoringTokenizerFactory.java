package jp.sf.fess.solr.plugin.analysis;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.AttributeSource.AttributeFactory;

public class MonitoringTokenizerFactory extends TokenizerFactory implements
        ResourceLoaderAware {

    private static final boolean VERBOSE = true; // debug

    protected static final int DEFAULT_TIMER_PERIOD = 60000;

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

    protected Field inputPendingField;

    protected Field attributesField;

    protected Field attributeImplsField;

    protected Field currentStateField;

    public MonitoringTokenizerFactory(final Map<String, String> args) {
        super(args);

        baseArgs = new HashMap<String, String>(args);
        baseClass = baseArgs.remove(BASE_CLASS);
        baseArgs.put(CLASS, baseClass);
        baseArgs.put(LUCENE_MATCH_VERSION_PARAM, luceneMatchVersion.toString());

        monitorTimer = createMonitorTimer();
        baseTokenizerFactory = createTokenizerFactory();

        try {
            inputPendingField = Tokenizer.class
                    .getDeclaredField("inputPending");
            inputPendingField.setAccessible(true);
            attributesField = AttributeSource.class
                    .getDeclaredField("attributes");
            attributesField.setAccessible(true);
            attributeImplsField = AttributeSource.class
                    .getDeclaredField("attributeImpls");
            attributeImplsField.setAccessible(true);
            currentStateField = AttributeSource.class
                    .getDeclaredField("currentState");
            currentStateField.setAccessible(true);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to load fields.", e);
        }
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
        return new TokenizerWrapper(factory, input);
    }

    protected TokenizerFactory createTokenizerFactory() {
        if (VERBOSE) {
            System.out.println("Create " + baseClass + " with " + baseArgs);
        }

        try {
            @SuppressWarnings("unchecked")
            final Class<? extends TokenizerFactory> clazz = (Class<? extends TokenizerFactory>) Class
                    .forName(baseClass);
            final Constructor<? extends TokenizerFactory> constructor = clazz
                    .getConstructor(Map.class);
            final TokenizerFactory tokenizerFactory = constructor
                    .newInstance(new HashMap<String, String>(baseArgs));

            if (loader != null
                    && tokenizerFactory instanceof ResourceLoaderAware) {
                ((ResourceLoaderAware) tokenizerFactory).inform(loader);
            }
            factoryTimestamp = System.currentTimeMillis();
            return tokenizerFactory;

        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Invalid parameters to create TokenizerFactory.", e);
        }
    }

    protected Timer createMonitorTimer() {
        final File monitoringFile = new File(
                resolve(baseArgs.remove(MONITORING_FILE)));
        final String monitoringPeriodStr = baseArgs.remove(MONITORING_PERIOD);
        final long monitoringPeriod = monitoringPeriodStr == null ? DEFAULT_TIMER_PERIOD
                : Long.parseLong(monitoringPeriodStr);
        final long currentTime = monitoringFile.lastModified();
        if (VERBOSE) {
            System.out.println("Starting a timer(" + monitoringPeriod
                    + "ms) to monitor " + monitoringFile.getAbsolutePath());
        }

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
                } catch (final Exception e) {
                    // ignore
                    if (VERBOSE) {
                        e.printStackTrace();
                    }
                }
            }
        };
        final Timer timer = new Timer("ReloadableTokenizer:" + baseClass);
        timer.schedule(task,
                monitoringPeriod > DEFAULT_TIMER_PERIOD ? monitoringPeriod
                        : DEFAULT_TIMER_PERIOD, monitoringPeriod);
        return timer;
    }

    @Override
    protected void finalize() throws Throwable {
        if (monitorTimer != null) {
            monitorTimer.cancel();
        }
        super.finalize();
    }

    protected static String resolve(final String value) {
        if (value == null) {
            return null;
        }

        final StringBuffer tunedText = new StringBuffer(value.length());
        final Pattern pattern = Pattern.compile("(\\$\\{([\\w\\.]+)\\})");
        final Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            final String key = matcher.group(2);
            String replacement = System.getProperty(key);
            if (replacement == null) {
                replacement = matcher.group(1);
            }
            matcher.appendReplacement(tunedText,
                    replacement.replace("$", "\\$"));

        }
        matcher.appendTail(tunedText);
        return tunedText.toString();
    }

    public class TokenizerWrapper extends Tokenizer {

        protected Tokenizer tokenizer;

        protected AttributeFactory factory;

        protected long tokenizerTimestamp;

        TokenizerWrapper(final AttributeFactory factory, final Reader input) {
            super(ILLEGAL_STATE_READER);

            this.factory = factory;
            tokenizer = createTokenizer(input);
        }

        @Override
        public void close() throws IOException {
            tokenizer.close();
        }

        @Override
        public void reset() throws IOException {
            final Reader inputPending = getInputPending();
            if (factoryTimestamp > tokenizerTimestamp) {
                if (VERBOSE) {
                    System.out
                            .println("Update ReloadableTokenizer ("
                                    + tokenizerTimestamp + ","
                                    + factoryTimestamp + ")");
                }
                tokenizer = createTokenizer(inputPending);
            } else if (inputPending != ILLEGAL_STATE_READER) {
                tokenizer.setReader(inputPending);
            }
            tokenizer.reset();
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

        protected Reader getInputPending() {
            try {
                return (Reader) inputPendingField.get(this);
            } catch (final Exception e) {
                return null;
            }
        }

        protected Tokenizer createTokenizer(final Reader inputPending) {
            tokenizerTimestamp = factoryTimestamp;
            final Tokenizer tokenizer = baseTokenizerFactory.create(factory,
                    inputPending);

            try {
                final Object attributesObj = attributesField.get(tokenizer);
                attributesField.set(this, attributesObj);
                final Object attributeImplsObj = attributeImplsField
                        .get(tokenizer);
                attributeImplsField.set(this, attributeImplsObj);
                final Object currentStateObj = currentStateField.get(tokenizer);
                currentStateField.set(this, currentStateObj);
            } catch (final Exception e) {
                throw new IllegalStateException(
                        "Failed to update the tokenizer.", e);
            }

            return tokenizer;
        }

    }

}
