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

package jp.sf.fess.solr.plugin.handler.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocListAndSet;

public class DocValuesComponent extends SearchComponent {

    private static final String DCF = "dcf";

    private static final String DOC_VALUES = "docValues";

    @Override
    public void prepare(final ResponseBuilder rb) throws IOException {
    }

    @Override
    public void process(final ResponseBuilder rb) throws IOException {
        final DocListAndSet results = rb.getResults();
        if (results != null) {
            final SolrQueryRequest req = rb.req;
            final SolrParams params = req.getParams();
            final String[] docValuesFields = params.getParams(DCF);
            if (docValuesFields == null) {
                return;
            }

            final DocList docs = results.docList;
            final NamedList<List<Long>> fragments = new SimpleOrderedMap<List<Long>>();
            final AtomicReader reader = req.getSearcher().getAtomicReader();
            for (final String field : docValuesFields) {
                final NumericDocValues numericDocValues = reader
                        .getNumericDocValues(field);
                if (numericDocValues == null) {
                    continue;
                }
                final List<Long> valueList = new ArrayList<Long>();
                final DocIterator iterator = docs.iterator();
                for (int i = 0; i < docs.size(); i++) {
                    final int docId = iterator.nextDoc();
                    final long value = numericDocValues.get(docId);
                    valueList.add(value);
                }
                fragments.add(field, valueList);
            }
            if (fragments.size() != 0) {
                rb.rsp.add(DOC_VALUES, fragments);
            }
        }
    }

    @Override
    public String getDescription() {
        return "DocValues";
    }

    @Override
    public String getSource() {
        return "TBD";
    }

}
