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

package jp.sf.fess.solr.plugin.suggest.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.sf.fess.solr.plugin.suggest.SuggestUpdateConfig;
import jp.sf.fess.solr.plugin.suggest.entity.SuggestFieldInfo;
import jp.sf.fess.suggest.SuggestConstants;
import jp.sf.fess.suggest.converter.SuggestIntegrateConverter;
import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestIntegrateNormalizer;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import jp.sf.fess.suggest.util.SuggestUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.core.SolrConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by yfujita on 2014/01/12.
 */
public class SolrConfigUtil {
    private static final Logger logger = LoggerFactory
            .getLogger(SolrConfigUtil.class);

    private static final String USER_DICT_PATH = "userDictionary";

    private static final String USER_DICT_ENCODING = "userDictionaryEncoding";

    private SolrConfigUtil() {
    }

    public static SuggestUpdateConfig getUpdateHandlerConfig(
            final SolrConfig config) {
        final SuggestUpdateConfig suggestUpdateConfig = new SuggestUpdateConfig();

        //setting config
        final String solrUrl = config.getVal("updateHandler/suggest/solrUrl", false);
        if (StringUtils.isNotBlank(solrUrl)) {
            suggestUpdateConfig.setSolrUrl(solrUrl);
        }

        final String solrUser = config.getVal("updateHandler/suggest/solrUser", false);
        if (StringUtils.isNotBlank(solrUser)) {
            suggestUpdateConfig.setSolrUser(solrUser);
        }
        final String solrPassword = config.getVal("updateHandler/suggest/solrPassword",
                false);
        if (StringUtils.isNotBlank(solrPassword)) {
            suggestUpdateConfig.setSolrPassword(solrPassword);
        }
        final String labelFields = config.getVal("updateHandler/suggest/labelFields",
                false);
        if (StringUtils.isNotBlank(labelFields)) {
            suggestUpdateConfig.setLabelFields(labelFields.trim().split(","));
        }
        final String roleFields = config.getVal("updateHandler/suggest/roleFields",
                false);
        if (StringUtils.isNotBlank(roleFields)) {
            suggestUpdateConfig.setRoleFields(roleFields.trim().split(","));
        }

        final String expiresField = config.getVal("updateHandler/suggest/expiresField",
                false);
        if (StringUtils.isNotBlank(expiresField)) {
            suggestUpdateConfig.setExpiresField(expiresField);
        }
        final String segmentField = config.getVal("updateHandler/suggest/segmentField",
                false);
        if (StringUtils.isNotBlank(segmentField)) {
            suggestUpdateConfig.setSegmentField(segmentField);
        }
        final String updateInterval = config.getVal(
                "updateHandler/suggest/updateInterval", false);
        if (StringUtils.isNotBlank(updateInterval)
                && StringUtils.isNumeric(updateInterval)) {
            suggestUpdateConfig.setUpdateInterval(Long
                    .parseLong(updateInterval));
        }

        //set suggestFieldInfo
        final NodeList nodeList = config.getNodeList(
                "updateHandler/suggest/suggestFieldInfo", true);
        for (int i = 0; i < nodeList.getLength(); i++) {
            try {
                final SuggestUpdateConfig.FieldConfig fieldConfig = new SuggestUpdateConfig.FieldConfig();
                final Node fieldInfoNode = nodeList.item(i);
                final NamedNodeMap fieldInfoAttributes = fieldInfoNode
                        .getAttributes();
                final Node fieldNameNode = fieldInfoAttributes
                        .getNamedItem("fieldName");
                final String fieldName = fieldNameNode.getNodeValue();
                if (StringUtils.isBlank(fieldName)) {
                    continue;
                }
                fieldConfig.setTargetFields(fieldName.trim().split(","));
                if (logger.isInfoEnabled()) {
                    for (final String s : fieldConfig.getTargetFields()) {
                        logger.info("fieldName : " + s);
                    }
                }

                final NodeList fieldInfoChilds = fieldInfoNode.getChildNodes();
                for (int j = 0; j < fieldInfoChilds.getLength(); j++) {
                    final Node fieldInfoChildNode = fieldInfoChilds.item(j);
                    final String fieldInfoChildNodeName = fieldInfoChildNode
                            .getNodeName();

                    if ("tokenizerFactory".equals(fieldInfoChildNodeName)) {
                        //field tokenier settings
                        final SuggestUpdateConfig.TokenizerConfig tokenizerConfig = new SuggestUpdateConfig.TokenizerConfig();

                        final NamedNodeMap tokenizerFactoryAttributes = fieldInfoChildNode
                                .getAttributes();
                        final Node tokenizerClassNameNode = tokenizerFactoryAttributes
                                .getNamedItem("class");
                        final String tokenizerClassName = tokenizerClassNameNode
                                .getNodeValue();
                        tokenizerConfig.setClassName(tokenizerClassName);
                        if (logger.isInfoEnabled()) {
                            logger.info("tokenizerFactory : "
                                    + tokenizerClassName);
                        }

                        final Map<String, String> args = new HashMap<String, String>();
                        for (int k = 0; k < tokenizerFactoryAttributes
                                .getLength(); k++) {
                            final Node attribute = tokenizerFactoryAttributes
                                    .item(k);
                            final String key = attribute.getNodeName();
                            final String value = attribute.getNodeValue();
                            if (!"class".equals(key)) {
                                args.put(key, value);
                            }
                        }
                        if (!args.containsKey(USER_DICT_PATH)) {
                            args.put(USER_DICT_PATH,
                                    SuggestConstants.USER_DICT_PATH);
                            args.put(USER_DICT_ENCODING,
                                    SuggestConstants.USER_DICT_ENCODING);
                        }
                        tokenizerConfig.setArgs(args);

                        fieldConfig.setTokenizerConfig(tokenizerConfig);
                    } else if ("suggestReadingConverter"
                            .equals(fieldInfoChildNodeName)) {
                        //field reading converter settings
                        final NodeList converterNodeList = fieldInfoChildNode
                                .getChildNodes();
                        for (int k = 0; k < converterNodeList.getLength(); k++) {
                            final SuggestUpdateConfig.ConverterConfig converterConfig = new SuggestUpdateConfig.ConverterConfig();

                            final Node converterNode = converterNodeList
                                    .item(k);
                            if (!"converter"
                                    .equals(converterNode.getNodeName())) {
                                continue;
                            }

                            final NamedNodeMap converterAttributes = converterNode
                                    .getAttributes();
                            final Node classNameNode = converterAttributes
                                    .getNamedItem("class");
                            final String className = classNameNode
                                    .getNodeValue();
                            converterConfig.setClassName(className);
                            if (logger.isInfoEnabled()) {
                                logger.info("converter : " + className);
                            }

                            final Map<String, String> properties = new HashMap<String, String>();
                            for (int l = 0; l < converterAttributes.getLength(); l++) {
                                final Node attribute = converterAttributes
                                        .item(l);
                                final String key = attribute.getNodeName();
                                final String value = attribute.getNodeValue();
                                if (!"class".equals(key)) {
                                    properties.put(key, value);
                                }
                            }
                            converterConfig.setProperties(properties);
                            if (logger.isInfoEnabled()) {
                                logger.info("converter properties = "
                                        + properties);
                            }
                            fieldConfig.addConverterConfig(converterConfig);
                        }
                    } else if ("suggestNormalizer"
                            .equals(fieldInfoChildNodeName)) {
                        //field normalizer settings
                        final NodeList normalizerNodeList = fieldInfoChildNode
                                .getChildNodes();
                        for (int k = 0; k < normalizerNodeList.getLength(); k++) {
                            final SuggestUpdateConfig.NormalizerConfig normalizerConfig = new SuggestUpdateConfig.NormalizerConfig();

                            final Node normalizerNode = normalizerNodeList
                                    .item(k);
                            if (!"normalizer".equals(normalizerNode
                                    .getNodeName())) {
                                continue;
                            }

                            final NamedNodeMap normalizerAttributes = normalizerNode
                                    .getAttributes();
                            final Node classNameNode = normalizerAttributes
                                    .getNamedItem("class");
                            final String className = classNameNode
                                    .getNodeValue();
                            normalizerConfig.setClassName(className);
                            if (logger.isInfoEnabled()) {
                                logger.info("normalizer : " + className);
                            }

                            final Map<String, String> properties = new HashMap<String, String>();
                            for (int l = 0; l < normalizerAttributes
                                    .getLength(); l++) {
                                final Node attribute = normalizerAttributes
                                        .item(l);
                                final String key = attribute.getNodeName();
                                final String value = attribute.getNodeValue();
                                if (!"class".equals(key)) {
                                    properties.put(key, value);
                                }
                            }
                            normalizerConfig.setProperties(properties);
                            if (logger.isInfoEnabled()) {
                                logger.info("normalize properties = "
                                        + properties);
                            }
                            fieldConfig.addNormalizerConfig(normalizerConfig);
                        }
                    }
                }

                suggestUpdateConfig.addFieldConfig(fieldConfig);
            } catch (final Exception e) {
                logger.warn("debug error", e);
            }
        }

        return suggestUpdateConfig;
    }

    public static List<SuggestFieldInfo> getSuggestFieldInfoList(
            final SuggestUpdateConfig config) {
        final List<SuggestFieldInfo> list = new ArrayList<SuggestFieldInfo>();

        for (final SuggestUpdateConfig.FieldConfig fieldConfig : config
                .getFieldConfigList()) {
            try {
                final List<String> fieldNameList = Arrays.asList(fieldConfig
                        .getTargetFields());
                final SuggestUpdateConfig.TokenizerConfig tokenizerConfig = fieldConfig
                        .getTokenizerConfig();

                //create tokenizerFactory
                TokenizerFactory tokenizerFactory = null;
                if (tokenizerConfig != null) {
                    final Class<?> cls = Class.forName(tokenizerConfig
                            .getClassName());
                    final Constructor<?> constructor = cls
                            .getConstructor(Map.class);
                    tokenizerFactory = (TokenizerFactory) constructor
                            .newInstance(tokenizerConfig.getArgs());
                }

                //create converter
                final SuggestIntegrateConverter suggestIntegrateConverter = new SuggestIntegrateConverter();
                for (final SuggestUpdateConfig.ConverterConfig converterConfig : fieldConfig
                        .getConverterConfigList()) {
                    final SuggestReadingConverter suggestReadingConverter = SuggestUtil
                            .createConverter(converterConfig.getClassName(),
                                    converterConfig.getProperties());
                    suggestIntegrateConverter
                            .addConverter(suggestReadingConverter);
                }
                suggestIntegrateConverter.start();

                //create normalizer
                final SuggestIntegrateNormalizer suggestIntegrateNormalizer = new SuggestIntegrateNormalizer();
                for (final SuggestUpdateConfig.NormalizerConfig normalizerConfig : fieldConfig
                        .getNormalizerConfigList()) {
                    final SuggestNormalizer suggestNormalizer = SuggestUtil
                            .createNormalizer(normalizerConfig.getClassName(),
                                    normalizerConfig.getProperties());
                    suggestIntegrateNormalizer.addNormalizer(suggestNormalizer);
                }
                suggestIntegrateNormalizer.start();

                final SuggestFieldInfo suggestFieldInfo = new SuggestFieldInfo(
                        fieldNameList, tokenizerFactory,
                        suggestIntegrateConverter, suggestIntegrateNormalizer);
                list.add(suggestFieldInfo);
            } catch (final Exception e) {
                logger.warn("Failed to create Tokenizer."
                        + fieldConfig.getTokenizerConfig().getClassName(), e);
            }
        }
        return list;
    }

}
