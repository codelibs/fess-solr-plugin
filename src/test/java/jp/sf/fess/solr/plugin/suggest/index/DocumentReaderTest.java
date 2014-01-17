package jp.sf.fess.solr.plugin.suggest.index;

import java.util.ArrayList;
import java.util.List;

import jp.sf.fess.solr.plugin.suggest.TestUtils;
import jp.sf.fess.solr.plugin.suggest.entity.SuggestItem;
import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import junit.framework.TestCase;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.SolrInputDocument;

public class DocumentReaderTest extends TestCase {
    public void test_getStandardDocumentItem() {
        final SolrInputDocument doc = new SolrInputDocument();
        doc.setField("content", "検索エンジン");
        doc.setField("content2", "隣の客はよく柿食う客だ");
        final List<String> targetFieldList = new ArrayList<String>();
        targetFieldList.add("content");
        targetFieldList.add("content2");
        final List<String> labelFieldList = new ArrayList<String>();
        final List<String> roleFieldList = new ArrayList<String>();

        final TokenizerFactory tokenizerFactory = TestUtils
                .getTokenizerFactory(TestUtils.getSuggestUpdateConfig());
        final SuggestReadingConverter suggestReadingConverter = TestUtils
                .createConverter();
        final SuggestNormalizer suggestNormalizer = TestUtils
                .createNormalizer();

        final DocumentReader reader = new DocumentReader(tokenizerFactory,
                suggestReadingConverter, suggestNormalizer, doc,
                targetFieldList, labelFieldList, roleFieldList, "", "");
        SuggestItem item;
        try {
            int count = 0;
            while ((item = reader.next()) != null) {
                switch (count) {
                case 0:
                    assertEquals("検索", item.getText());
                    assertTrue(item.getReadingList().contains("kensaku"));
                    assertTrue(item.getReadingList().contains("kennsaku"));
                    assertEquals("content", item.getFieldNameList().get(0));
                    break;
                case 1:
                    assertEquals("エンジン", item.getText());
                    assertTrue(item.getReadingList().contains("ennjinn"));
                    assertTrue(item.getReadingList().contains("enjinn"));
                    assertEquals("content", item.getFieldNameList().get(0));
                    break;
                case 2:
                    assertEquals("検索エンジン", item.getText());
                    assertTrue(item.getReadingList()
                            .contains("kennsakuennjinn"));
                    assertEquals("content", item.getFieldNameList().get(0));
                    break;
                case 3:
                    assertEquals("隣", item.getText());
                    assertTrue(item.getReadingList().contains("tonari"));
                    assertEquals("content2", item.getFieldNameList().get(0));
                    break;
                case 4:
                    assertEquals("客", item.getText());
                    assertTrue(item.getReadingList().contains("kyaku"));
                    assertEquals("content2", item.getFieldNameList().get(0));
                    break;
                case 5:
                    assertEquals("柿", item.getText());
                    assertTrue(item.getReadingList().contains("kaki"));
                    assertEquals("content2", item.getFieldNameList().get(0));
                    break;
                case 6:
                    assertEquals("客", item.getText());
                    assertTrue(item.getReadingList().contains("kyaku"));
                    assertEquals("content2", item.getFieldNameList().get(0));
                    break;
                }
                count++;
            }
            assertEquals(7, count);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void test_getLabelAndRoleTest() {
        final SolrInputDocument doc = new SolrInputDocument();
        doc.setField("content", "検索エンジン");
        doc.addField("label", "label1");
        doc.addField("label", "label2");
        doc.addField("role", "role1");
        doc.addField("role", "role2");
        final List<String> targetFieldList = new ArrayList<String>();
        targetFieldList.add("content");
        final List<String> labelFieldList = new ArrayList<String>();
        labelFieldList.add("label");
        final List<String> roleFieldList = new ArrayList<String>();
        roleFieldList.add("role");

        final TokenizerFactory tokenizerFactory = TestUtils
                .getTokenizerFactory(TestUtils.getSuggestUpdateConfig());
        final SuggestReadingConverter suggestReadingConverter = TestUtils
                .createConverter();
        final SuggestNormalizer suggestNormalizer = TestUtils
                .createNormalizer();

        final DocumentReader reader = new DocumentReader(tokenizerFactory,
                suggestReadingConverter, suggestNormalizer, doc,
                targetFieldList, labelFieldList, roleFieldList, "", "");
        SuggestItem item;
        try {
            int count = 0;
            while ((item = reader.next()) != null) {
                switch (count) {
                case 0:
                    assertEquals("検索", item.getText());
                    assertTrue(item.getReadingList().contains("kensaku"));
                    assertEquals("content", item.getFieldNameList().get(0));
                    for (final String label : item.getLabels()) {
                        assertTrue(label.equals("label1")
                                || label.equals("label2"));
                    }
                    for (final String role : item.getRoles()) {
                        assertTrue(role.equals("role1")
                                || role.equals("role2"));
                    }
                    break;
                case 1:
                    assertEquals("エンジン", item.getText());
                    assertTrue(item.getReadingList().contains("ennjinn"));
                    assertEquals("content", item.getFieldNameList().get(0));
                    for (final String label : item.getLabels()) {
                        assertTrue(label.equals("label1")
                                || label.equals("label2"));
                    }
                    for (final String role : item.getRoles()) {
                        assertTrue(role.equals("role1")
                                || role.equals("role2"));
                    }
                    break;
                case 2:
                    assertEquals("検索エンジン", item.getText());
                    assertTrue(item.getReadingList()
                            .contains("kennsakuennjinn"));
                    assertEquals("content", item.getFieldNameList().get(0));
                    for (final String label : item.getLabels()) {
                        assertTrue(label.equals("label1")
                                || label.equals("label2"));
                    }
                    for (final String role : item.getRoles()) {
                        assertTrue(role.equals("role1")
                                || role.equals("role2"));
                    }
                    break;
                }
                count++;
            }
            assertEquals(3, count);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
