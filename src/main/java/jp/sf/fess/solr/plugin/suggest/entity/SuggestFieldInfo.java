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

package jp.sf.fess.solr.plugin.suggest.entity;

import java.util.List;

import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;

import org.apache.lucene.analysis.util.TokenizerFactory;

public class SuggestFieldInfo {
    private final List<String> fieldNameList;

    private final TokenizerFactory tokenizerFactory;

    private final SuggestReadingConverter suggestReadingConverter;

    private final SuggestNormalizer suggestNormalizer;

    public SuggestFieldInfo(final List<String> fieldNameList,
            final TokenizerFactory tokenizerFactory,
            final SuggestReadingConverter suggestReadingConverter,
            final SuggestNormalizer suggestNormalizer) {
        this.fieldNameList = fieldNameList;
        this.tokenizerFactory = tokenizerFactory;
        this.suggestReadingConverter = suggestReadingConverter;
        this.suggestNormalizer = suggestNormalizer;
    }

    public List<String> getFieldNameList() {
        return fieldNameList;
    }

    public TokenizerFactory getTokenizerFactory() {
        return tokenizerFactory;
    }

    public SuggestReadingConverter getSuggestReadingConverter() {
        return suggestReadingConverter;
    }

    public SuggestNormalizer getSuggestNormalizer() {
        return suggestNormalizer;
    }
}
