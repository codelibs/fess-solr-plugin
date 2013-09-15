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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import jp.sf.fess.suggest.converter.SuggestConverter;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.Transliterator;

public class SuggestStringTokenizer extends Tokenizer {
    private static final Logger logger = LoggerFactory
            .getLogger(SuggestStringTokenizer.class);

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private int offset = 0;

    private final List<String> termListByKuromoji = new ArrayList<String>();

    private final List<String> readingList = new ArrayList<String>();

    private String[] titleArray = null;

    private final UserDictionary userDictionary;

    private final boolean discardPunctuation;

    private final Mode tokenizerMode;

    private final String wordSeparator;

    private final List<SuggestConverter> preConverterList;

    private final List<SuggestConverter> converterList;

    public SuggestStringTokenizer(final Reader input, final int bufferSize,
            final UserDictionary userDictionaryPara,
            final boolean discardPunctuationPara, final Mode modePara,
            final List<SuggestConverter> preconverterList,
            final List<SuggestConverter> converterList,
            final String wordSeparator) {
        super(input);

        userDictionary = userDictionaryPara;
        discardPunctuation = discardPunctuationPara;
        tokenizerMode = modePara;
        termAtt.resizeBuffer(bufferSize);
        this.wordSeparator = wordSeparator;
        preConverterList = preconverterList;
        this.converterList = converterList;

        initialize();
    }

    public void initialize() {
        termListByKuromoji.clear();
        readingList.clear();
        titleArray = null;
        offset = 0;
        String inputStr = "";

        try {
            final String s = IOUtils.toString(input);
            if (s != null && s.length() > 0) {
                inputStr = s;
                for (final SuggestConverter converter : preConverterList) {
                    inputStr = converter.convert(inputStr);
                }
                titleArray = inputStr.split("\\$\\{and\\}");
                inputStr = inputStr.replace("${and}", " ");
            }
        } catch (final IOException e) {
        }

        final Reader rd = new StringReader(inputStr);

        TokenStream stream = null;

        try {
            stream = new JapaneseTokenizer(rd, userDictionary,
                    discardPunctuation, tokenizerMode);

            stream.reset();
            while (stream.incrementToken()) {
                final CharTermAttribute att = stream
                        .getAttribute(CharTermAttribute.class);
                termListByKuromoji.add(att.toString());

                final ReadingAttribute rdAttr = stream
                        .getAttribute(ReadingAttribute.class);

                String reading;
                if (rdAttr.getReading() != null) {
                    reading = rdAttr.getReading();
                } else {
                    reading = att.toString();
                }

                for (final SuggestConverter converter : converterList) {
                    reading = converter.convert(reading);
                }
                readingList.add(reading);

            }

        } catch (final Exception e) {
            logger.warn("JapaneseTokenizer stream error", e);
        } finally {
            try {
                input.reset();
            } catch (final Exception e) {
            }
            try {
                stream.end();
            } catch (final Exception e) {
            }
            try {
                rd.close();
            } catch (final Exception e) {
            }
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (titleArray == null || offset >= titleArray.length) {
            return false;
        }

        termAtt.setEmpty();
        termAtt.append(convertSuggestString(titleArray[offset],
                getReading(titleArray[offset])));
        offset++;
        return true;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        initialize();
    }

    private String convertSuggestString(final String term, final String reading) {
        String suggestString;
        if (reading != null && reading.length() > 0) {
            suggestString = reading + wordSeparator + term;
        } else {
            suggestString = term;
        }

        return suggestString;
    }

    private String getReading(final String s) {

        final StringBuilder buf = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            String term = "";
            int length = 0;

            for (int j = 0; j < termListByKuromoji.size(); j++) {
                final String tmpStr = termListByKuromoji.get(j);
                if (s.substring(i).indexOf(tmpStr) == 0
                        && tmpStr.length() > term.length()) {
                    term = readingList.get(j);
                    length = tmpStr.length();
                }
            }
            if (term.length() > 0) {
                buf.append(term);
                i += length - 1;
            } else {
                char c = s.charAt(i);

                c = Transliterator.getInstance("Hiragana-Katakana")
                        .transliterate(String.valueOf(c)).charAt(0);

                buf.append(c);
            }
        }

        String reading = buf.toString();
        for (final SuggestConverter converter : converterList) {
            reading = converter.convert(reading);
        }

        return reading;
    }
}
