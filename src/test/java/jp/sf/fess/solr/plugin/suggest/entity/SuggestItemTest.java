package jp.sf.fess.solr.plugin.suggest.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jp.sf.fess.solr.plugin.suggest.TestUtils;
import jp.sf.fess.suggest.SuggestConstants;
import junit.framework.TestCase;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;

public class SuggestItemTest extends TestCase {
    public void test_equals_True() {
        final SuggestItem item1 = new SuggestItem();
        item1.setText("test");
        item1.addReading("テスト");
        item1.addFieldName("content");

        final SuggestItem item2 = new SuggestItem();
        item2.setText("test");
        item2.addReading("テスト");
        item2.addFieldName("content");
        item2.setLabels(new ArrayList<String>());

        assertTrue(item1.equals(item2));
    }

    public void test_equals_False() {
        final SuggestItem item1 = new SuggestItem();
        item1.setText("test");
        item1.addReading("テスト");
        item1.addFieldName("content");

        final SuggestItem item2 = new SuggestItem();
        item2.setText("test2");
        item2.addReading("テスト");
        item2.addFieldName("content");
        assertFalse(item1.equals(item2));

        item2.setText("test");
        item2.addReading("テスト");
        item2.addFieldName("content2");
        assertTrue(item1.equals(item2));
    }

    public void test_toSolrInputDocument() {
        final SuggestItem item1 = new SuggestItem();
        item1.setText("test");
        item1.addReading("テスト");
        item1.addReading("テスト2");
        item1.addFieldName("content");
        final String date = DateUtil.getThreadLocalDateFormat().format(
                new Date());
        item1.setExpires(date);
        item1.setExpiresField(TestUtils.getSuggestUpdateConfig()
                .getExpiresField());
        item1.setCount(10);
        final List<String> labels = Arrays.asList(new String[] { "label1",
                "label2" });
        item1.setLabels(labels);

        final SolrInputDocument doc = item1.toSolrInputDocument();
        assertEquals("test",
                doc.getFieldValue(SuggestConstants.SuggestFieldNames.TEXT));
        assertEquals("テスト",
                doc.getFieldValue(SuggestConstants.SuggestFieldNames.READING));
        assertEquals(
                "content",
                doc.getFieldValue(SuggestConstants.SuggestFieldNames.FIELD_NAME));
        assertEquals(
                date,
                doc.getFieldValue(
                        TestUtils.getSuggestUpdateConfig().getExpiresField())
                        .toString());
        assertEquals(
                10,
                Integer.parseInt(doc.getFieldValue(
                        SuggestConstants.SuggestFieldNames.COUNT).toString()));
        assertTrue(labels.equals(doc
                .getFieldValues(SuggestConstants.SuggestFieldNames.LABELS)));
        assertEquals("test", doc.getFieldValue("id"));
    }
}
