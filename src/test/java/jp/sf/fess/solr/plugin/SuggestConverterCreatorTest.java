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

package jp.sf.fess.solr.plugin;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import jp.sf.fess.solr.plugin.suggest.SuggestConverterCreator;
import jp.sf.fess.suggest.converter.SuggestConverter;

import org.junit.Test;

public class SuggestConverterCreatorTest {
    @Test
    public void createTwoInstance() {
        final String text = "["
                + //
                "{\"class\":\"jp.sf.fess.suggest.converter.SymbolConverter\","
                + "\"method\":[{\"name\":\"addSymbol\",\"args\":[[\"A\"]]}]}"
                + ","
                + //
                "{\"class\":\"jp.sf.fess.suggest.converter.SymbolConverter\",\"args\":[\"B\",\"E\"],"
                + "\"method\":[{\"name\":\"addSymbol\",\"args\":[[\"X\"]]},{\"name\":\"addSymbol\",\"args\":[[\"Y\"]]}]}"
                + ","
                + //
                "{\"class\":\"jp.sf.fess.suggest.converter.ReplaceConverter\","
                + "\"method\":[{\"name\":\"addReplaceString\",\"args\":[\"x\",\"X\"]},{\"name\":\"addReplaceString\",\"args\":[\"y\",\"Y\"]}]}"
                + //
                "]";
        final List<SuggestConverter> list = SuggestConverterCreator
                .create(text);
        assertThat(list.size(), is(3));
        assertThat(list.get(0).getClass().getName(),
                is("jp.sf.fess.suggest.converter.SymbolConverter"));
        assertThat(list.get(0).convert("abcABC"), is("abc__ID0__BC"));
        assertThat(list.get(1).getClass().getName(),
                is("jp.sf.fess.suggest.converter.SymbolConverter"));
        assertThat(list.get(1).convert("xyzXYZ"), is("xyzB0EB1EZ"));
        assertThat(list.get(2).getClass().getName(),
                is("jp.sf.fess.suggest.converter.ReplaceConverter"));
        assertThat(list.get(2).convert("xyzXYZ"), is("XYzXYZ"));

    }

    @Test
    public void createOneInstance() {
        final String text = "["
                + //
                "{\"class\":\"jp.sf.fess.suggest.converter.ICUConverter\",\"args\":[\"Fullwidth-Halfwidth\"]}"
                + //
                "]";
        final List<SuggestConverter> list = SuggestConverterCreator
                .create(text);
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getClass().getName(),
                is("jp.sf.fess.suggest.converter.ICUConverter"));

    }

    @Test
    public void createEmpty() {
        List<SuggestConverter> list;

        list = SuggestConverterCreator.create("");
        assertThat(list.size(), is(0));

        list = SuggestConverterCreator.create(null);
        assertThat(list.size(), is(0));
    }
}
