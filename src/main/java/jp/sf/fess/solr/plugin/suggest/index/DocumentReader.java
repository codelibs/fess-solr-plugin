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

package jp.sf.fess.solr.plugin.suggest.index;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import jp.sf.fess.solr.plugin.suggest.entity.SuggestItem;
import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.util.DateUtil;

public class DocumentReader {
    private final List<String> targetFields;

    private int fieldPos = 0;

    private final List<String> targetLabelFields;

    private final List<String> targetRoleFields;

    private final SolrInputDocument solrInputDocument;

    private Tokenizer tokenizer;

    private final TokenizerFactory tokenizerFactory;

    private final SuggestReadingConverter suggestReadingConverter;

    private final SuggestNormalizer suggestNormalizer;

    private final String expiresField;

    private final String segmentField;

    private final String expire;

    private final String segment;

    private boolean hasNext = true;

    public DocumentReader(final TokenizerFactory tokenizerFactory,
            final SuggestReadingConverter suggestReadingConverter,
            final SuggestNormalizer suggestNormalizer,
            final SolrInputDocument solrInputDocument,
            final List<String> targetFields,
            final List<String> targetLabelFields,
            final List<String> targetRoleFields,
            final String expiresFidld,
            final String segmentField) {
        this.solrInputDocument = solrInputDocument;
        this.targetFields = targetFields;
        this.targetLabelFields = targetLabelFields;
        this.targetRoleFields = targetRoleFields;
        this.tokenizerFactory = tokenizerFactory;
        expiresField = expiresFidld;
        this.segmentField = segmentField;
        this.suggestReadingConverter = suggestReadingConverter;
        this.suggestNormalizer = suggestNormalizer;

        final Object expireObj = solrInputDocument.getFieldValue(expiresField);
        if (expireObj != null) {
            expire = expireObj.toString();
        } else {
            expire = DateUtil.getThreadLocalDateFormat().format(new Date());
        }

        final Object segmentObj = solrInputDocument.getFieldValue(segmentField);
        if (segmentObj != null) {
            segment = segmentObj.toString();
        } else {
            segment = "";
        }
    }

    public SuggestItem next() throws IOException {
        if (tokenizerFactory == null) {
            final String text = getNextFieldString();
            if (text == null) {
                return null;
            }
            final SuggestItem item = createSuggestItem(text,
                    targetFields.get(fieldPos));
            fieldPos++;
            return item;
        } else {
            while (hasNext) {
                if (tokenizer != null) {
                    if (tokenizer.incrementToken()) {
                        final CharTermAttribute att = tokenizer
                                .getAttribute(CharTermAttribute.class);
                        final SuggestItem item = createSuggestItem(
                                att.toString(), targetFields.get(fieldPos));
                        return item;
                    }
                    tokenizer.close();
                    tokenizer = null;
                    fieldPos++;
                }
                tokenizer = createTokenizer();
                if (tokenizer == null) {
                    hasNext = false;
                }
            }
        }
        return null;
    }

    private SuggestItem createSuggestItem(final String text,
            final String fieldName) {
        final SuggestItem item = new SuggestItem();
        item.setExpiresField(expiresField);
        item.setExpires(expire);
        item.setSegmentField(segmentField);
        item.setSegment(segment);
        final List<String> labels = item.getLabels();
        for (final String label : targetLabelFields) {
            final SolrInputField field = solrInputDocument.getField(label);
            if (field == null) {
                continue;
            }
            final Collection<Object> valList = field.getValues();
            if (valList == null || valList.size() == 0) {
                continue;
            }

            for (final Object val : valList) {
                labels.add(val.toString());
            }
            break;
        }

        final List<String> roles = item.getRoles();
        for (final String role : targetRoleFields) {
            final SolrInputField field = solrInputDocument.getField(role);
            if (field == null) {
                continue;
            }
            final Collection<Object> valList = field.getValues();
            if (valList == null || valList.size() == 0) {
                continue;
            }

            for (final Object val : valList) {
                roles.add(val.toString());
            }
            break;
        }


        item.addFieldName(fieldName);
        item.setText(text);
        if (suggestReadingConverter != null) {
            final List<String> readingList = suggestReadingConverter
                    .convert(item.getText());
            for (final String reading : readingList) {
                item.addReading(reading.toString());
            }
        } else {
            item.addReading(text);
        }

        return item;
    }

    private Tokenizer createTokenizer() throws IOException {
        final String nextFieldString = getNextFieldString();
        if (nextFieldString == null) {
            return null;
        }
        final Reader rd = new StringReader(nextFieldString);
        final Tokenizer t = tokenizerFactory.create(rd);
        t.reset();
        return t;
    }

    private String getNextFieldString() {
        StringBuilder fieldValue = null;

        for (; fieldPos < targetFields.size(); fieldPos++) {
            final String fieldName = targetFields.get(fieldPos);
            final SolrInputField field = solrInputDocument.getField(fieldName);
            if (field == null) {
                continue;
            }
            final Collection<Object> valList = field.getValues();
            if (valList == null || valList.size() == 0) {
                continue;
            }

            fieldValue = new StringBuilder();
            for (final Object val : valList) {
                fieldValue.append(val.toString());
                fieldValue.append(' ');
            }
            break;
        }

        if (fieldValue == null) {
            return null;
        }

        String nextFieldString = fieldValue.toString();
        if (suggestNormalizer != null) {
            nextFieldString = suggestNormalizer.normalize(nextFieldString);
        }
        return nextFieldString;
    }
}
