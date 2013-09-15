/*
 * Copyright 2009-2013 the Fess Project and the Others.
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

package jp.sf.fess.solr.plugin.analysis;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.sf.fess.solr.plugin.analysis.SuggestTextTokenizer.TermChecker;
import jp.sf.fess.solr.plugin.suggest.SuggestConverterCreator;
import jp.sf.fess.suggest.converter.SuggestConverter;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeSource.AttributeFactory;
import org.apache.lucene.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuggestTextTokenizerFactory extends TokenizerFactory implements
        ResourceLoaderAware {

    private static final Logger logger = LoggerFactory
            .getLogger(SuggestTextTokenizerFactory.class);

    private static final String MODE = "mode";

    private static final String USER_DICT_PATH = "userDictionary";

    private static final String USER_DICT_ENCODING = "userDictionaryEncoding";

    private static final String BUFFER_SIZE = "bufferSize";

    private static final String WORD_SEPARATOR = "wordSeparator";

    private static final String INCLUDE_CHAR_TERM = "includeCharTerm";

    private static final String EXCLUDE_PART_OF_SPEECH = "excludePartOfSpeech";

    private static final String INCLUDE_PART_OF_SPEECH = "includePartOfSpeech";

    private static final String DISCARD_PUNCTUATION = "discardPunctuation"; // Expert option

    private static final String MAX_LENGTH = "maxLength";

    private UserDictionary userDictionary;

    private final Mode mode;

    private final String userDictionaryPath;

    private final String userDictionaryEncoding;

    private final boolean discardPunctuation;

    private final int bufferSize;

    private final String wordSeparator;

    private final TermChecker termChecker;

    private final List<SuggestConverter> preConverterList;

    private final List<SuggestConverter> converterList;

    private final int maxLength;

    public SuggestTextTokenizerFactory(final Map<String, String> args) {
        super(args);

        mode = getMode(args);
        userDictionaryPath = args.get(USER_DICT_PATH);
        userDictionaryEncoding = args.get(USER_DICT_ENCODING);
        bufferSize = getInt(args, BUFFER_SIZE, 256);
        wordSeparator = get(args, WORD_SEPARATOR, "_SP_");
        discardPunctuation = getBoolean(args, DISCARD_PUNCTUATION, true);
        maxLength = getInt(args, MAX_LENGTH, 0);

        termChecker = new TermChecker();
        // ex. start:名詞,middle:動詞
        final String includePartOfSpeech = args.get(INCLUDE_PART_OF_SPEECH);
        if (includePartOfSpeech != null) {
            for (String text : includePartOfSpeech.split(",")) {
                text = text.trim();
                if (text.length() > 0) {
                    final String[] values = text.split(":");
                    if (values.length == 2) {
                        termChecker.includePartOfSpeech(values[0].trim(),
                                values[1].trim());
                    }
                }
            }
        }
        final String excludePartOfSpeech = args.get(EXCLUDE_PART_OF_SPEECH);
        if (excludePartOfSpeech != null) {
            for (String text : excludePartOfSpeech.split(",")) {
                text = text.trim();
                if (text.length() > 0) {
                    final String[] values = text.split(":");
                    if (values.length == 2) {
                        termChecker.excludePartOfSpeech(values[0].trim(),
                                values[1].trim());
                    }
                }
            }
        }
        final String includeCharTerm = args.get(INCLUDE_CHAR_TERM);
        if (includeCharTerm != null) {
            for (String text : includeCharTerm.split(",")) {
                text = text.trim();
                if (text.length() > 0) {
                    final String[] values = text.split(":");
                    if (values.length == 2) {
                        termChecker.includeCharTerm(values[0].trim(),
                                values[1].trim());
                    }
                }
            }
        }
        preConverterList = SuggestConverterCreator.create(args
                .get("preConverters"));
        converterList = SuggestConverterCreator.create(args.get("converters"));
    }

    @Override
    public Tokenizer create(final AttributeFactory factory, final Reader input) {
        return new SuggestTextTokenizer(input, bufferSize, userDictionary,
                discardPunctuation, mode, termChecker, preConverterList,
                converterList, wordSeparator, maxLength);
    }

    @Override
    public void inform(final ResourceLoader loader) {
        try {

            if (userDictionaryPath != null) {
                final InputStream stream = loader
                        .openResource(userDictionaryPath);
                String encoding = userDictionaryEncoding;
                if (encoding == null) {
                    encoding = IOUtils.UTF_8;
                }
                final CharsetDecoder decoder = Charset.forName(encoding)
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
                final Reader reader = new InputStreamReader(stream, decoder);
                userDictionary = new UserDictionary(reader);
            } else {
                userDictionary = null;
            }

        } catch (final Exception e) {
            logger.warn("Initialization failed.", e);
        }
    }

    private Mode getMode(final Map<String, String> args) {
        final String modeArg = args.get(MODE);
        if (modeArg != null) {
            return Mode.valueOf(modeArg.toUpperCase(Locale.ROOT));
        } else {
            return JapaneseTokenizer.Mode.NORMAL;
        }
    }

}
