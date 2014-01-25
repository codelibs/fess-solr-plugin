/*
 * Copyright 2009-2014 the CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

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
                System.out.println("Monitoring " + target + " (" + lastModified // NOSONAR
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
                    e.printStackTrace(); // NOSONAR
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
