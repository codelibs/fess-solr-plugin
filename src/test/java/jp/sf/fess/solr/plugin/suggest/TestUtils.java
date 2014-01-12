package jp.sf.fess.solr.plugin.suggest;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import jp.sf.fess.solr.plugin.suggest.index.SuggestSolrServer;
import jp.sf.fess.suggest.converter.AlphabetConverter;
import jp.sf.fess.suggest.converter.SuggestIntegrateConverter;
import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestIntegrateNormalizer;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import jp.sf.fess.suggest.util.SuggestUtil;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;

public class TestUtils {
    public static final String SOLR_URL = "http://localhost:8181/solr/core1-suggest";

    private static final String CONTEXT_PATH = "/solr";

    private static final int PORT = 8181;

    private static JettySolrRunner jettySolrRunner;

    public static TokenizerFactory getTokenizerFactory(
            final SuggestUpdateConfig config) {
        try {
            final Map<String, String> args = new HashMap<String, String>();
            final Class cls = Class.forName(config.getFieldConfigList().get(0)
                    .getTokenizerConfig().getClassName());
            final Constructor constructor = cls.getConstructor(Map.class);
            final TokenizerFactory tokenizerFactory = (TokenizerFactory) constructor
                    .newInstance(args);
            return tokenizerFactory;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SuggestUpdateConfig getSuggestUpdateConfig() {
        final SuggestUpdateConfig config = new SuggestUpdateConfig();
        config.setUpdateInterval(1 * 1000);
        config.setSolrUrl(SOLR_URL);
        config.setSolrUser("solradmin");
        config.setSolrPassword("solradmin");
        final SuggestUpdateConfig.FieldConfig fieldConfig = new SuggestUpdateConfig.FieldConfig();
        fieldConfig
                .setTokenizerConfig(new SuggestUpdateConfig.TokenizerConfig());
        config.addFieldConfig(fieldConfig);
        return config;
    }

    public static SuggestSolrServer createSuggestSolrServer() {
        return new SuggestSolrServer(SOLR_URL, "solradmin", "solradmin");
    }

    public static void startJerrySolrRunner() {
        if (jettySolrRunner != null) {
            return;
        }

        jettySolrRunner = new JettySolrRunner("./solr", CONTEXT_PATH, PORT);
        try {
            jettySolrRunner.start();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopJettySolrRunner() {
        if (jettySolrRunner == null) {
            return;
        }
        try {
            jettySolrRunner.stop();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        jettySolrRunner = null;
    }

    public static SuggestReadingConverter createConverter() {
        final SuggestIntegrateConverter suggestIntegrateConverter = new SuggestIntegrateConverter();
        suggestIntegrateConverter.addConverter(new AlphabetConverter());
        suggestIntegrateConverter.start();
        return suggestIntegrateConverter;
    }

    public static SuggestNormalizer createNormalizer() {
        final SuggestIntegrateNormalizer suggestIntegrateNormalizer = new SuggestIntegrateNormalizer();

        try {
            Map<String, String> map = new HashMap<String, String>();
            suggestIntegrateNormalizer
                    .addNormalizer(SuggestUtil
                            .createNormalizer(
                                    "jp.sf.fess.suggest.normalizer.FullWidthToHalfWidthAlphabetNormalizer",
                                    map));
            map = new HashMap<String, String>();
            map.put("transliteratorId", "Any-Lower");
            suggestIntegrateNormalizer
                    .addNormalizer(SuggestUtil.createNormalizer(
                            "jp.sf.fess.suggest.normalizer.ICUNormalizer", map));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        suggestIntegrateNormalizer.start();
        return suggestIntegrateNormalizer;
    }
}
