package jp.sf.fess.solr.plugin.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import jp.sf.fess.solr.plugin.analysis.monitor.MonitoringFileTask;
import jp.sf.fess.solr.plugin.analysis.monitor.MonitoringFileTask.Callback;

import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.solr.core.SolrResourceLoader;

public class MonitoringFileUtil {
    private static final boolean VERBOSE = true; // debug

    private static final String MONITORING_PERIOD = "monitoringPeriod";

    private static final String MONITORING_FILE = "monitoringFile";

    private static final String BASE_CLASS = "baseClass";

    private static final String CLASS = "class";

    private MonitoringFileUtil() {
    }

    public static Map<String, String> createMonitorArgs(
            final Map<String, String> baseArgs) {
        final Map<String, String> monitorArgs = new HashMap<String, String>();
        monitorArgs.put(MONITORING_FILE, baseArgs.remove(MONITORING_FILE));
        monitorArgs.put(MONITORING_PERIOD, baseArgs.remove(MONITORING_PERIOD));
        return monitorArgs;
    }

    public static String initBaseArgs(final Map<String, String> baseArgs,
            final String luceneVersion) {
        final String baseClass = baseArgs.remove(BASE_CLASS);
        baseArgs.put(CLASS, baseClass);
        baseArgs.put(AbstractAnalysisFactory.LUCENE_MATCH_VERSION_PARAM,
                luceneVersion);
        return baseClass;
    }

    public static <T> T createFactory(final String className,
            final Map<String, String> baseArgs, final ResourceLoader loader) {
        if (VERBOSE) {
            System.out.println("Create " + className + " with " + baseArgs);
        }

        try {
            @SuppressWarnings("unchecked")
            final Class<? extends T> clazz = (Class<? extends T>) Class
                    .forName(className);
            final Constructor<? extends T> constructor = clazz
                    .getConstructor(Map.class);
            final T factory = constructor
                    .newInstance(new HashMap<String, String>(baseArgs));

            if (loader != null && factory instanceof ResourceLoaderAware) {
                ((ResourceLoaderAware) factory).inform(loader);
            }
            return factory;
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Invalid parameters to create TokenizerFactory.", e);
        }
    }

    public static MonitoringFileTask createMonitoringFileTask(
            final Map<String, String> monitorArgs, final ResourceLoader loader,
            final Callback callback) {
        final File monitoringFile;
        final String monitoringFilePath = monitorArgs.get(MONITORING_FILE);
        final File file = new File(monitoringFilePath);
        if (file.exists()) {
            monitoringFile = file;
        } else {
            monitoringFile = new File(
                    ((SolrResourceLoader) loader).getConfigDir(),
                    monitoringFilePath);
        }
        final String monitoringPeriodStr = monitorArgs.get(MONITORING_PERIOD);
        final long monitoringPeriod = monitoringPeriodStr == null ? MonitoringFileTask.DEFAULT_PERIOD
                : Long.parseLong(monitoringPeriodStr);
        if (VERBOSE) {
            System.out.println("Create MonitoringFileTask(" + monitoringPeriod
                    + "ms) to monitor " + monitoringFile.getAbsolutePath());
        }

        return new MonitoringFileTask(monitoringFile, monitoringPeriod,
                callback);
    }
}
