package jp.sf.fess.solr.plugin.analysis.monitor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import jp.sf.fess.solr.plugin.util.MonitoringUtil;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.util.AttributeSource;

public class MonitoringTokenFilterFactory extends TokenFilterFactory implements
        ResourceLoaderAware {
    private static final boolean VERBOSE = true; // debug

    protected ResourceLoader loader;

    protected TokenFilterFactory baseTokenFilterFactory;

    protected final Map<String, String> baseArgs;

    protected String baseClass;

    protected MonitoringTask monitoringTask;

    protected volatile long factoryTimestamp;

    protected Field attributesField;

    protected Field attributeImplsField;

    protected Field currentStateField;

    public MonitoringTokenFilterFactory(final Map<String, String> args) {
        super(args);

        try {
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

        baseTokenFilterFactory = MonitoringUtil.createFactory(baseClass,
                baseArgs, loader);
        factoryTimestamp = System.currentTimeMillis();

        monitoringTask = MonitoringUtil.createMonitoringTask(monitorArgs,
                loader, new MonitoringTask.Callback() {
                    @Override
                    public void process() {
                        baseTokenFilterFactory = MonitoringUtil.createFactory(
                                baseClass, baseArgs, loader);
                        factoryTimestamp = System.currentTimeMillis();
                    }
                });

        if (baseTokenFilterFactory instanceof ResourceLoaderAware) {
            ((ResourceLoaderAware) baseTokenFilterFactory).inform(loader);
        }
    }

    @Override
    public TokenStream create(final TokenStream input) {
        return new TokenStreamWrapper(input);
    }

    public class TokenStreamWrapper extends TokenStream {
        protected TokenStream tokenStream;

        protected TokenStream input;

        protected long tokenStreamTimestamp;

        TokenStreamWrapper(final TokenStream input) {
            super();
            this.input = input;
            tokenStream = createTokenStream(input);
        }

        @Override
        public void close() throws IOException {
            tokenStream.close();
        }

        @Override
        public void reset() throws IOException {
            if (factoryTimestamp > tokenStreamTimestamp) {
                if (VERBOSE) {
                    System.out.println("Update TokenStream/" + baseClass + " ("
                            + tokenStreamTimestamp + "," + factoryTimestamp
                            + ")"); // NOSONAR
                }
                tokenStream = createTokenStream(input);
            }
            tokenStream.reset();

            monitoringTask.process();
        }

        @Override
        public boolean incrementToken() throws IOException {
            return tokenStream.incrementToken();
        }

        @Override
        public void end() throws IOException {
            tokenStream.end();
        }

        @Override
        public int hashCode() {
            return tokenStream.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return tokenStream.equals(obj);
        }

        @Override
        public String toString() {
            return tokenStream.toString();
        }

        protected TokenStream createTokenStream(final TokenStream input) {
            tokenStreamTimestamp = factoryTimestamp;
            final TokenStream newTokenStream = baseTokenFilterFactory
                    .create(input);

            try {
                final Object attributesObj = attributesField.get(newTokenStream);
                attributesField.set(this, attributesObj);
                final Object attributeImplsObj = attributeImplsField
                        .get(newTokenStream);
                attributeImplsField.set(this, attributeImplsObj);
                final Object currentStateObj = currentStateField
                        .get(newTokenStream);
                currentStateField.set(this, currentStateObj);
            } catch (final Exception e) {
                throw new IllegalStateException(
                        "Failed to update the tokenStream.", e);
            }

            return newTokenStream;
        }

    }

}
