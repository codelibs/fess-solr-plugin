package jp.sf.fess.solr.plugin.analysis.monitor;

public class MonitoringTask {
    private static final boolean VERBOSE = true; // debug

    protected Target target;

    protected long interval;

    protected volatile long lastChecked;

    protected long lastModified;

    protected Callback callback;

    public static final int DEFAULT_PERIOD = 60000;

    public MonitoringTask(final Target target, final long interval,
            final Callback callback) {
        this.target = target;
        this.interval = interval;
        this.callback = callback;

        lastModified = target.lastModified();
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
            final long currentLastModified = target.lastModified();
            if (VERBOSE) {
                System.out.println("Monitoring " + target + " (" + lastModified
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

    public interface Target {
        long lastModified();
    }
}
