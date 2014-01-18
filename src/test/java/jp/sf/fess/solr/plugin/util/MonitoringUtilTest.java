package jp.sf.fess.solr.plugin.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.lucene.util.IOUtils;
import org.junit.Test;

public class MonitoringUtilTest {
    @Test
    public void testDiff() throws IOException {
        File file1 = null;
        File file2 = null;
        assertFalse(MonitoringUtil.diff(file1, file2));

        file1 = File.createTempFile("test", ".txt");
        file1.deleteOnExit();
        assertTrue(MonitoringUtil.diff(file1, file2));

        file2 = File.createTempFile("test", ".txt");
        file2.deleteOnExit();
        assertFalse(MonitoringUtil.diff(file1, file2));

        writeText(file1, "abc");
        assertTrue(MonitoringUtil.diff(file1, file2));

        writeText(file2, "abc");
        assertFalse(MonitoringUtil.diff(file1, file2));

        writeText(file1, "abc123");
        assertTrue(MonitoringUtil.diff(file1, file2));
    }

    private void writeText(final File file, final String text)
            throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(text.getBytes());
            fos.flush();
        } finally {
            IOUtils.closeWhileHandlingException(fos);
        }
    }
}
