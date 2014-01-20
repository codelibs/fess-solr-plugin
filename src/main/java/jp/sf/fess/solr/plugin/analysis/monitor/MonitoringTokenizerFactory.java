package jp.sf.fess.solr.plugin.analysis.monitor;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import jp.sf.fess.solr.plugin.util.MonitoringUtil;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.AttributeSource.AttributeFactory;

public class MonitoringTokenizerFactory extends TokenizerFactory implements
        ResourceLoaderAware {

    private static final boolean VERBOSE = true; // debug

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

    protected String baseClass;

    protected volatile long factoryTimestamp;

    protected Field inputPendingField;

    protected Field attributesField;

    protected Field attributeImplsField;

    protected Field currentStateField;

    protected long lastCheckedTime;

    protected MonitoringTask monitoringFileTask;

    public MonitoringTokenizerFactory(final Map<String, String> args) {
        super(args);

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

        baseArgs = new HashMap<String, String>(args);
    }

    @Override
    public void inform(final ResourceLoader loader) throws IOException {
        this.loader = loader;
        final Map<String, String> monitorArgs = MonitoringUtil
                .createMonitorArgs(baseArgs);

        baseClass = MonitoringUtil.initBaseArgs(baseArgs,
                luceneMatchVersion.toString());

        baseTokenizerFactory = MonitoringUtil.createFactory(baseClass,
                baseArgs, loader);
        factoryTimestamp = System.currentTimeMillis();

        monitoringFileTask = MonitoringUtil.createMonitoringTask(monitorArgs,
                loader, new MonitoringTask.Callback() {
                    @Override
                    public void process() {
                        baseTokenizerFactory = MonitoringUtil.createFactory(
                                baseClass, baseArgs, loader);
                        factoryTimestamp = System.currentTimeMillis();
                    }
                });

        if (baseTokenizerFactory instanceof ResourceLoaderAware) {
            ((ResourceLoaderAware) baseTokenizerFactory).inform(loader);
        }
    }

    @Override
    public Tokenizer create(final AttributeFactory factory, final Reader input) {
        return new TokenizerWrapper(factory, input);
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
                            .println("Update Tokenizer/" + baseClass + " ("
                                    + tokenizerTimestamp + ","
                                    + factoryTimestamp + ")"); // NOSONAR
                }
                tokenizer = createTokenizer(inputPending);
            } else if (inputPending != ILLEGAL_STATE_READER) {
                tokenizer.setReader(inputPending);
            }
            tokenizer.reset();

            monitoringFileTask.process();
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
            final Tokenizer newTokenizer = baseTokenizerFactory.create(factory,
                    inputPending);

            try {
                final Object attributesObj = attributesField.get(newTokenizer);
                attributesField.set(this, attributesObj);
                final Object attributeImplsObj = attributeImplsField
                        .get(newTokenizer);
                attributeImplsField.set(this, attributeImplsObj);
                final Object currentStateObj = currentStateField.get(newTokenizer);
                currentStateField.set(this, currentStateObj);
            } catch (final Exception e) {
                throw new IllegalStateException(
                        "Failed to update the tokenizer.", e);
            }

            return newTokenizer;
        }

    }

}
