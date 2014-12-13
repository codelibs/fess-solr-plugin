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

package jp.sf.fess.solr.plugin.analysis.synonym;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

/**
 * Factory for {@link NGramSynonymTokenizer}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_2gs" class="solr.TextField" positionIncrementGap="100" autoGeneratePhraseQueries="true"&gt;
 *   &lt;analyzer type="index"&gt;
 *     &lt;tokenizer class="jp.sf.fess.solr.plugin.analysis.synonym.NGramSynonymTokenizerFactory"
 *                expand="true" synonyms="synonyms.txt"/&gt;
 *   &lt;/analyzer&gt;
 *   &lt;analyzer type="query"&gt;
 *     &lt;tokenizer class="jp.sf.fess.solr.plugin.analysis.synonym.NGramSynonymTokenizerFactory"
 *                expand="false" synonyms="synonyms.txt"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
// https://issues.apache.org/jira/browse/LUCENE-5252
public final class NGramSynonymTokenizerFactory extends TokenizerFactory
        implements ResourceLoaderAware {

    private final String synonymFiles;

    private final boolean ignoreCase;

    private final int n;

    private final String delimiters;

    private final String format;

    private final boolean expand;

    private SynonymMap map;

    public NGramSynonymTokenizerFactory(final Map<String, String> args) {
        super(args);
        synonymFiles = get(args, "synonyms");
        ignoreCase = getBoolean(args, "ignoreCase", true);
        n = getInt(args, "n", NGramSynonymTokenizer.DEFAULT_N_SIZE);
        delimiters = get(args, "delimiters",
                NGramSynonymTokenizer.DEFAULT_DELIMITERS);
        format = get(args, "format");
        expand = getBoolean(args, "expand", true);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public Tokenizer create(final AttributeFactory factory, final Reader input) {
        return new NGramSynonymTokenizer(input, n, delimiters, expand,
                ignoreCase, map);
    }

    @Override
    public void inform(final ResourceLoader loader) throws IOException {
        if (synonymFiles == null) {
            map = null;
            return;
        }

        final Analyzer analyzer = getAnalyzer(ignoreCase);

        try {
            String formatClass = format;
            if (format == null || format.equals("solr")) {
                formatClass = SolrSynonymParser.class.getName();
            } else if (format.equals("wordnet")) {
                formatClass = WordnetSynonymParser.class.getName();
            }
            // TODO: expose dedup as a parameter?
            map = loadSynonyms(loader, formatClass, true, analyzer, true,
                    synonymFiles); // always expand=true in NGramSynonymTokenizer
        } catch (final ParseException e) {
            throw new IOException("Error parsing synonyms file:", e);
        }
    }

    public static Analyzer getAnalyzer(final boolean ignoreCase) {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(
                    final String fieldName, final Reader reader) {
                final Tokenizer tokenizer = new KeywordTokenizer(reader);
                @SuppressWarnings("resource")
                final TokenStream stream = ignoreCase ? new LowerCaseFilter(
                        tokenizer) : tokenizer;
                return new TokenStreamComponents(tokenizer, stream);
            }
        };
    }

    private SynonymMap loadSynonyms(final ResourceLoader loader,
            final String cname, final boolean dedup, final Analyzer analyzer,
            final boolean expand, final String synonyms) throws IOException,
            ParseException {
        final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        SynonymMap.Parser parser;
        final Class<? extends SynonymMap.Parser> clazz = loader.findClass(
                cname, SynonymMap.Parser.class);
        try {
            parser = clazz.getConstructor(boolean.class, boolean.class,
                    Analyzer.class).newInstance(dedup, expand, analyzer);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        final File synonymFile = new File(synonyms);
        if (synonymFile.exists()) {
            decoder.reset();
            parser.parse(new InputStreamReader(loader.openResource(synonyms),
                    decoder));
        } else {
            final List<String> files = splitFileNames(synonyms);
            for (final String file : files) {
                decoder.reset();
                parser.parse(new InputStreamReader(loader.openResource(file),
                        decoder));
            }
        }
        return parser.build();
    }
}
