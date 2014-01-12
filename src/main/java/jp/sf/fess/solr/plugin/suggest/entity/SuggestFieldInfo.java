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
