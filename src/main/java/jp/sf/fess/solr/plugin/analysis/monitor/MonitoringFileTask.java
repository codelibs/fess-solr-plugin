package jp.sf.fess.solr.plugin.analysis.monitor;

import java.io.File;

public class MonitoringFileTask {
    private static final boolean VERBOSE = true; // debug

    protected File file;

    protected long interval;

    protected volatile long lastChecked;

    protected long lastModified;

    protected Callback callback;

    public static final int DEFAULT_PERIOD = 60000;

    public MonitoringFileTask(final File file, final long interval,
            final Callback callback) {
        this.file = file;
        this.interval = interval;
        this.callback = callback;

        lastModified = file.lastModified();
    }

    public void process() {
        final long now = System.currentTimeMillis();
        if (now - lastChecked < interval) {
            // nothing
            return;
        }

        synchronized (this) {
            if (now - lastChecked < interval) {
                // nothing
                return;
            }

            lastChecked = now;
            final long currentLastModified = file.lastModified();
            if (VERBOSE) {
                System.out.println("Monitoring " + file + " (" + lastModified
                        + "," + currentLastModified + ")");
            }
            try {
                if (currentLastModified > lastModified) {
                    lastModified = currentLastModified;
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            callback.process();
                        }
                    }).start();
                }
            } catch (final Exception e) {
                // ignore
                if (VERBOSE) {
                    e.printStackTrace();
                }
            }
        }
    }

    public interface Callback {

        void process();

    }
}
