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

import jp.sf.fess.solr.plugin.suggest.SuggestConverterCreator;
import jp.sf.fess.suggest.converter.SuggestConverter;

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

public class SuggestStringTokenizerFactory extends TokenizerFactory implements
        ResourceLoaderAware {

    private static final Logger logger = LoggerFactory
            .getLogger(SuggestStringTokenizerFactory.class);

    private static final String MODE = "mode";

    private static final String USER_DICT_PATH = "userDictionary";

    private static final String USER_DICT_ENCODING = "userDictionaryEncoding";

    private static final String BUFFER_SIZE = "bufferSize";

    private static final String WORD_SEPARATOR = "wordSeparator";

    private static final String DISCARD_PUNCTUATION = "discardPunctuation"; // Expert option

    private UserDictionary userDictionary;

    private final Mode mode;

    private final String userDictionaryPath;

    private final String userDictionaryEncoding;

    private final boolean discardPunctuation;

    private final int bufferSize;

    private String wordSeparator;

    private final List<SuggestConverter> preConverterList;

    private final List<SuggestConverter> converterList;

    public SuggestStringTokenizerFactory(final Map<String, String> args) {
        super(args);

        mode = getMode(args);
        userDictionaryPath = args.get(USER_DICT_PATH);
        userDictionaryEncoding = args.get(USER_DICT_ENCODING);
        bufferSize = getInt(args, BUFFER_SIZE, 256);
        discardPunctuation = getBoolean(args, DISCARD_PUNCTUATION, true);
        wordSeparator = args.get(WORD_SEPARATOR);
        if (wordSeparator == null) {
            wordSeparator = "_SP_";
        }

        preConverterList = SuggestConverterCreator.create(args
                .get("preConverters"));
        converterList = SuggestConverterCreator.create(args.get("converters"));
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

    @Override
    public SuggestStringTokenizer create(final AttributeFactory factory,
            final Reader input) {
        return new SuggestStringTokenizer(input, bufferSize, userDictionary,
                discardPunctuation, mode, preConverterList, converterList,
                wordSeparator);
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
