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

package jp.sf.fess.solr.plugin.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.util.RefCounted;

public class DocValueUpdateHandlerFilter extends UpdateHandlerFilter {

    private static final String TERM_PARAM = "term";

    private static final String EXTENDED_CMD = "excmd";

    private static final String UPDATE_CMD = "update";

    @Override
    public int addDoc(final AddUpdateCommand cmd,
            final UpdateHandlerFilterChain chain) throws IOException {
        final String exCmd = cmd.getReq().getParams().get(EXTENDED_CMD);
        if (UPDATE_CMD.equals(exCmd)) {
            final String termName = cmd.getReq().getParams().get(TERM_PARAM);
            if (termName == null) {
                throw new IllegalArgumentException("term is not specified.");
            }

            int rc = -1;
            final RefCounted<IndexWriter> iw = updateHandler.getSolrCoreState()
                    .getIndexWriter(updateHandler.getSolrCore());
            try {
                final IndexWriter writer = iw.get();

                if (cmd.isBlock()) {
                    for (final Iterable<? extends IndexableField> doc : cmd) {
                        updateNumericValue(writer, doc, termName);
                    }
                } else {
                    final Iterable<IndexableField> doc = cmd
                            .getLuceneDocument();
                    updateNumericValue(writer, doc, termName);
                }
                rc = 1;
            } finally {
                iw.decref();
            }
            return rc;
        } else {
            return chain.addDoc(cmd);
        }
    }

    private void updateNumericValue(final IndexWriter writer,
            final Iterable<? extends IndexableField> doc, final String termName)
            throws IOException {
        String termValue = null;
        final List<IndexableField> numericFieldList = new ArrayList<IndexableField>();
        for (final IndexableField field : doc) {
            if (termName.equals(field.name()) && field.stringValue() != null) {
                termValue = field.stringValue();
            } else if (field instanceof NumericDocValuesField) {
                numericFieldList.add(field);
            }
        }
        if (termValue == null) {
            throw new IllegalArgumentException(
                    "A value of term is not found in the doc.");
        }
        for (final IndexableField field : numericFieldList) {
            writer.updateNumericDocValue(new Term(termName, termValue),
                    field.name(), field.numericValue().longValue());
        }
    }
}
