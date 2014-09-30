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

package jp.sf.fess.solr.plugin.suggest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import jp.sf.fess.solr.plugin.suggest.entity.SuggestFieldInfo;
import jp.sf.fess.solr.plugin.suggest.util.TransactionLogUtil;
import jp.sf.fess.suggest.SuggestConstants;
import jp.sf.fess.suggest.Suggester;
import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import jp.sf.fess.suggest.server.SuggestSolrServer;
import junit.framework.TestCase;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.apache.solr.update.TransactionLog;

public class SuggestUpdateControllerTest extends TestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.startJerrySolrRunner();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        TestUtils.stopJettySolrRunner();
    }

    public void test_updateAndSuggest() {
        try {
            final SuggestSolrServer suggestSolrServer = TestUtils
                    .createSuggestSolrServer();
            suggestSolrServer.deleteAll();
            final SuggestUpdateConfig config = TestUtils
                    .getSuggestUpdateConfig();
            final SuggestUpdateController controller = new SuggestUpdateController(
                    config, getSuggestFieldInfoList(config, false));
            controller.start();

            final SolrInputDocument doc = new SolrInputDocument();
            doc.setField(
                    "content",
                    "Fess は「5 分で簡単に構築可能な全文検索サーバー」です。Java 実行環境があればどの OS でも実行可能です。Fess は Apache ライセンスで提供され、無料 (フリーソフト) でご利用いただけます。\n"
                            + "\n"
                            + "Seasar2 ベースで構築され、検索エンジン部分には 2 億ドキュメントもインデックス可能と言われる Solr を利用しています。 ドキュメントクロールには S2Robot を利用することで、Web やファイルシステムに対するクロールが可能になり、MS Office 系のドキュメントや zip などの圧縮ファイルも検索対象とすることができます。");
            doc.setField(config.getExpiresField(), DateUtil
                    .getThreadLocalDateFormat().format(new Date()));
            controller.add(doc);
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertTrue(suggestSolrServer.select("*:*").getNumFound() > 10);
            assertTrue(suggestSolrServer.select(
                    SuggestConstants.SuggestFieldNames.READING + ":jav*")
                    .getNumFound() > 0);
            assertTrue(suggestSolrServer.select(
                    SuggestConstants.SuggestFieldNames.READING
                            + ":kensakuenjinn*").getNumFound() > 0);
            assertTrue(suggestSolrServer.select(
                    SuggestConstants.SuggestFieldNames.READING + ":inde*")
                    .getNumFound() > 0);

            //suggest check
            final Suggester suggester = new Suggester();
            suggester.setNormalizer(TestUtils.createNormalizer());
            suggester.setConverter(TestUtils.createConverter());

            String q = suggester.buildSuggestQuery("jav",
                    Arrays.asList(new String[] { "content" }), null, null);
            assertTrue(suggestSolrServer.select(q).getNumFound() > 0);
            q = suggester.buildSuggestQuery("kensakuenj",
                    Arrays.asList(new String[] { "content" }), null, null);
            assertTrue(suggestSolrServer.select(q).getNumFound() > 0);
            q = suggester.buildSuggestQuery("inde",
                    Arrays.asList(new String[] { "content" }), null, null);
            assertTrue(suggestSolrServer.select(q).getNumFound() > 0);

        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void test_update_multifield() {
        final SuggestSolrServer suggestSolrServer = TestUtils
                .createSuggestSolrServer();

        try {
            suggestSolrServer.deleteAll();

            final SuggestUpdateConfig config = TestUtils
                    .getSuggestUpdateConfig();
            final SuggestUpdateController controller = new SuggestUpdateController(
                    config, getSuggestFieldInfoList(config, true));
            controller.start();
            final SolrInputDocument doc = new SolrInputDocument();
            doc.setField(
                    "content",
                    "Fess は「5 分で簡単に構築可能な全文検索サーバー」です。Java 実行環境があればどの OS でも実行可能です。Fess は Apache ライセンスで提供され、無料 (フリーソフト) でご利用いただけます。\n"
                            + "\n"
                            + "Seasar2 ベースで構築され、検索エンジン部分には 2 億ドキュメントもインデックス可能と言われる Solr を利用しています。 ドキュメントクロールには S2Robot を利用することで、Web やファイルシステムに対するクロールが可能になり、MS Office 系のドキュメントや zip などの圧縮ファイルも検索対象とすることができます。");
            doc.setField("title", "Fessについての説明　page");
            doc.setField(config.getExpiresField(), DateUtil
                    .getThreadLocalDateFormat().format(new Date()));
            controller.add(doc);
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertTrue(suggestSolrServer.select("*:*").getNumFound() > 10);
            assertEquals(
                    1,
                    suggestSolrServer.select(
                            SuggestConstants.SuggestFieldNames.READING
                                    + ":jav*").getNumFound());
            assertTrue(suggestSolrServer.select(
                    SuggestConstants.SuggestFieldNames.READING
                            + ":kensakuenjinn*").getNumFound() > 0);
            assertTrue(suggestSolrServer.select(
                    SuggestConstants.SuggestFieldNames.READING + ":inde*")
                    .getNumFound() > 0);
            assertTrue(suggestSolrServer.select(
                    SuggestConstants.SuggestFieldNames.READING
                            + ":fessnituitenose*").getNumFound() > 0);

            //suggester check
            final Suggester suggester = new Suggester();
            suggester.setNormalizer(TestUtils.createNormalizer());
            suggester.setConverter(TestUtils.createConverter());

            String q = suggester.buildSuggestQuery("fessnituitenosetumei",
                    Arrays.asList(new String[] { "title" }), null, null);
            assertEquals(1, suggestSolrServer.select(q).getNumFound());
            q = suggester.buildSuggestQuery("fessnituitenosetumei",
                    Arrays.asList(new String[] { "content" }), null, null);
            assertEquals(0, suggestSolrServer.select(q).getNumFound());
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void test_ttl_delete() {
        final SuggestSolrServer suggestSolrServer = TestUtils
                .createSuggestSolrServer();

        try {
            suggestSolrServer.deleteAll();

            final SuggestUpdateConfig config = TestUtils
                    .getSuggestUpdateConfig();
            final SuggestUpdateController controller = new SuggestUpdateController(
                    config, getSuggestFieldInfoList(config, false));
            controller.start();

            final String prevDate = DateUtil.getThreadLocalDateFormat().format(
                    new Date());

            final SolrInputDocument doc = new SolrInputDocument();
            doc.setField("content", "りんご");
            doc.setField(config.getExpiresField(), prevDate);
            controller.add(doc);
            controller.commit();
            Thread.sleep(5 * 1000);
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 1);
            final SolrInputDocument doc2 = new SolrInputDocument();
            doc2.setField("content", "みかん");
            doc2.setField(config.getExpiresField(), DateUtil
                    .getThreadLocalDateFormat().format(new Date()));
            controller.add(doc2);
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertEquals(2, suggestSolrServer.select("*:*").getNumFound());
            assertEquals(
                    1,
                    suggestSolrServer.select(
                            SuggestConstants.SuggestFieldNames.READING
                                    + ":mikan").getNumFound());
            assertEquals(
                    1,
                    suggestSolrServer.select(
                            SuggestConstants.SuggestFieldNames.READING
                                    + ":rinngo").getNumFound());

            controller.deleteByQuery(config.getExpiresField() + ":[* TO "
                    + prevDate + "] NOT segment:hogehoge");
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertEquals(1, suggestSolrServer.select("*:*").getNumFound());
            assertEquals(
                    1,
                    suggestSolrServer.select(
                            SuggestConstants.SuggestFieldNames.READING
                                    + ":mikan").getNumFound());
            assertEquals(
                    0,
                    suggestSolrServer.select(
                            SuggestConstants.SuggestFieldNames.READING
                                    + ":rinngo").getNumFound());

        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void test_segment_delete() {
        final SuggestSolrServer suggestSolrServer = TestUtils
                .createSuggestSolrServer();

        try {
            suggestSolrServer.deleteAll();

            final SuggestUpdateConfig config = TestUtils
                    .getSuggestUpdateConfig();
            final SuggestUpdateController controller = new SuggestUpdateController(
                    config, getSuggestFieldInfoList(config, false));
            controller.start();

            final SolrInputDocument doc = new SolrInputDocument();
            doc.setField("content", "りんご");
            doc.setField(config.getSegmentField(), "1");
            controller.add(doc);
            controller.commit();
            Thread.sleep(5 * 1000);
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 1);
            final SolrInputDocument doc2 = new SolrInputDocument();
            doc2.setField("content", "みかん");
            doc2.setField(config.getSegmentField(), "2");
            controller.add(doc2);
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 2);
            assertTrue(suggestSolrServer.select(
                    SuggestConstants.SuggestFieldNames.READING + ":mikan")
                    .getNumFound() == 1);
            assertTrue(suggestSolrServer.select(
                    SuggestConstants.SuggestFieldNames.READING + ":ringo")
                    .getNumFound() == 1);

            controller.deleteByQuery(config.getSegmentField() + ":1");
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 1);
            assertTrue(suggestSolrServer.select(
                    SuggestConstants.SuggestFieldNames.READING + ":mikan")
                    .getNumFound() == 1);
            assertTrue(suggestSolrServer.select(
                    SuggestConstants.SuggestFieldNames.READING + ":ringo")
                    .getNumFound() == 0);

        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void test_incrementUpdate() {
        final SuggestSolrServer suggestSolrServer = TestUtils
                .createSuggestSolrServer();

        try {
            suggestSolrServer.deleteAll();
            suggestSolrServer.commit();

            final SuggestUpdateConfig config = TestUtils
                    .getSuggestUpdateConfig();
            final SuggestUpdateController controller = new SuggestUpdateController(
                    config, getSuggestFieldInfoList(config, false));
            controller.addLabelFieldName("label");
            controller.addRoleFieldName("role");
            controller.start();

            final SolrInputDocument doc = new SolrInputDocument();
            for (int i = 0; i < 5; i++) {
                doc.setField("content", "みかん");
                doc.setField("label", "label" + i);
                doc.setField("role", "role" + i);
                doc.setField(config.getExpiresField(), DateUtil
                        .getThreadLocalDateFormat().format(new Date()));
                controller.add(doc);
                Thread.sleep(5 * 1000);
            }
            controller.commit();
            Thread.sleep(5 * 1000);
            final SolrDocumentList solrDocuments = suggestSolrServer
                    .select("*:*");
            assertEquals(1, solrDocuments.getNumFound());
            final SolrDocument solrDocument = solrDocuments.get(0);
            final Object count = solrDocument
                    .getFieldValue(SuggestConstants.SuggestFieldNames.COUNT);
            assertEquals("5", count.toString());
            final Collection<Object> labels = solrDocument
                    .getFieldValues(SuggestConstants.SuggestFieldNames.LABELS);
            assertEquals(5, labels.size());
            for (int i = 0; i < 5; i++) {
                assertTrue(labels.contains("label" + i));
            }
            final Collection<Object> roles = solrDocument
                    .getFieldValues(SuggestConstants.SuggestFieldNames.ROLES);
            assertEquals(5, roles.size());
            for (int i = 0; i < 5; i++) {
                assertTrue(roles.contains("role" + i));
            }
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private List<SuggestFieldInfo> getSuggestFieldInfoList(
            final SuggestUpdateConfig config, final boolean multi) {
        final List<SuggestFieldInfo> list = new ArrayList<SuggestFieldInfo>();

        final List<String> fieldNameList = new ArrayList<String>();
        fieldNameList.add("content");

        final TokenizerFactory tokenizerFactory = TestUtils
                .getTokenizerFactory(config);
        final SuggestReadingConverter suggestReadingConverter = TestUtils
                .createConverter();
        final SuggestNormalizer suggestNormalizer = TestUtils
                .createNormalizer();

        final SuggestFieldInfo suggestFieldInfo = new SuggestFieldInfo(
                fieldNameList, tokenizerFactory, suggestReadingConverter,
                suggestNormalizer);
        list.add(suggestFieldInfo);

        if (multi) {
            final List<String> fieldNameList2 = new ArrayList<String>();
            fieldNameList2.add("title");

            final SuggestReadingConverter suggestReadingConverter2 = TestUtils
                    .createConverter();
            final SuggestNormalizer suggestNormalizer2 = TestUtils
                    .createNormalizer();

            final SuggestFieldInfo suggestFieldInfo2 = new SuggestFieldInfo(
                    fieldNameList2, null, suggestReadingConverter2,
                    suggestNormalizer2);
            list.add(suggestFieldInfo2);
        }

        return list;
    }

    public void test_UpdateFromTransactionLog() {
        final SuggestSolrServer suggestSolrServer = TestUtils
                .createSuggestSolrServer();

        try {
            suggestSolrServer.deleteAll();
            suggestSolrServer.commit();

            final SuggestUpdateConfig config = TestUtils
                    .getSuggestUpdateConfig();
            final SuggestUpdateController controller = new SuggestUpdateController(
                    config, getSuggestFieldInfoList(config, false));
            controller.addLabelFieldName("label");
            controller.start();

            final File classPath = new File(this.getClass().getClassLoader()
                    .getResource("").getPath());
            final File file = new File(classPath, "tlog.0000000000000000059");
            controller.addTransactionLog(file);

            Thread.sleep(10 * 1000);
            assertTrue(suggestSolrServer.select("*:*").getNumFound() > 100);
            assertTrue(suggestSolrServer.select("reading_s_m:kensa*")
                    .getNumFound() > 0);

            controller.close();
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
