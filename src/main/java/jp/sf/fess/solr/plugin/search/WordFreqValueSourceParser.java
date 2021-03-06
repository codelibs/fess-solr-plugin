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

package jp.sf.fess.solr.plugin.search;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.IntDocValues;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

public class WordFreqValueSourceParser extends ValueSourceParser {

    @Override
    public ValueSource parse(final FunctionQParser fp) throws SyntaxError {
        final String field = fp.parseArg();
        final String word = fp.parseArg();
        final boolean normalized = !"false".equals(fp.parseArg());
        return new WordFreqValueSource(field, word, normalized);
    }

    public static class WordFreqValueSource extends ValueSource {
        protected final String field;

        protected final String word;

        protected final boolean normalized;

        public WordFreqValueSource(final String field, final String word,
                final boolean normalized) {
            super();
            this.field = field;
            this.word = normalized ? normalize(word) : word;
            this.normalized = normalized;
        }

        public String name() {
            return "wordfreq";
        }

        protected String normalize(final String value) {
            return value.toLowerCase(Locale.getDefault());
        }

        @Override
        public FunctionValues getValues(
                @SuppressWarnings("rawtypes") final Map context,
                final AtomicReaderContext readerContext) throws IOException {
            return new IntDocValues(this) {
                @Override
                public int intVal(final int docId) {
                    final IndexSearcher searcher = (IndexSearcher) context
                            .get("searcher");
                    final Set<String> fieldSet = new HashSet<String>();
                    fieldSet.add(field);
                    try {
                        final Document doc = searcher.doc(docId, fieldSet);
                        if (doc != null) {
                            String value = doc.get(field);
                            if (normalized) {
                                value = normalize(value);
                            }
                            return StringUtils.countMatches(value, word);
                        }
                    } catch (final IOException e) {
                        // ignore
                    }
                    return 0;
                }
            };
        }

        @Override
        public boolean equals(final Object o) {
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final WordFreqValueSource other = (WordFreqValueSource) o;
            return field.equals(other.field) && word.equals(other.word)
                    && normalized == other.normalized;

        }

        @Override
        public int hashCode() {
            return (field + word).hashCode() + (normalized ? 1231 : 1237);

        }

        @Override
        public String description() {
            return name() + '(' + field + ',' + word + ')';
        }

    }
}
