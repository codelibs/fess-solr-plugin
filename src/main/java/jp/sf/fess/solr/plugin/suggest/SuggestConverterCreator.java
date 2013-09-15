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

package jp.sf.fess.solr.plugin.suggest;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jp.sf.fess.suggest.converter.SuggestConverter;

import org.apache.commons.lang.StringUtils;
import org.noggit.ObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuggestConverterCreator {
    private static final Logger logger = LoggerFactory
            .getLogger(SuggestConverterCreator.class);

    protected SuggestConverterCreator() {
        // nothing
    }

    public static List<SuggestConverter> create(final String val) {
        if (StringUtils.isBlank(val)) {
            return Collections.emptyList();
        }

        try {
            final Object obj = ObjectBuilder.fromJSON(val);
            if (obj instanceof List<?>) {

                final List<SuggestConverter> converterList = new ArrayList<SuggestConverter>();
                for (final Object map : (List<Object>) obj) {
                    if (map instanceof Map<?, ?>) {
                        try {
                            final Map<Object, Object> dataMap = (Map<Object, Object>) map;
                            final String className = (String) dataMap
                                    .get("class");
                            final Class<SuggestConverter> clazz = (Class<SuggestConverter>) Class
                                    .forName(className);
                            final List<?> constructorArgs = (List<?>) dataMap
                                    .get("args");
                            SuggestConverter converter;
                            if (constructorArgs == null
                                    || constructorArgs.isEmpty()) {
                                converter = clazz.newInstance();
                            } else {
                                final List<Class<?>> classList = new ArrayList<Class<?>>(
                                        constructorArgs.size());
                                for (final Object arg : constructorArgs) {
                                    classList.add(getArgClass(arg));
                                }
                                final Constructor<SuggestConverter> constructor = clazz
                                        .getConstructor(classList
                                                .toArray(new Class<?>[constructorArgs
                                                        .size()]));
                                converter = constructor
                                        .newInstance(constructorArgs
                                                .toArray(new Object[constructorArgs
                                                        .size()]));
                            }
                            updateInstance(dataMap, clazz, converter);
                            converterList.add(converter);
                        } catch (final Exception e) {
                            logger.warn("Could not create a converter.", e);
                        }
                    } else {
                        logger.info("Data for a converter should be an object: "
                                + map.toString());
                    }
                }
                return converterList;
            } else {
                logger.info("Could not create a converter list from " + val);
            }
        } catch (final IOException e) {
            logger.warn("Failed to parse " + val, e);
        }

        return Collections.emptyList();
    }

    private static void updateInstance(final Map<Object, Object> dataMap,
            final Class<SuggestConverter> clazz,
            final SuggestConverter converter) {
        if (clazz == null) {
            logger.warn("class is null. data:" + dataMap + ", converter: "
                    + converter);
            return;
        }
        final List<?> methodList = (List<?>) dataMap.get("method");
        if (methodList != null && !methodList.isEmpty()) {
            for (final Object obj : methodList) {
                try {
                    if (obj instanceof Map<?, ?>) {
                        final Map<Object, Object> paramMap = (Map<Object, Object>) obj;
                        final String methodName = (String) paramMap.get("name");
                        final List<?> methodArgs = (List<?>) paramMap
                                .get("args");
                        final Class<?>[] argClasses;
                        if (methodArgs == null || methodArgs.isEmpty()) {
                            argClasses = null;
                        } else {
                            final List<Class<?>> classList = new ArrayList<Class<?>>(
                                    methodArgs.size());
                            for (final Object arg : methodArgs) {
                                classList.add(getArgClass(arg));
                            }
                            argClasses = classList
                                    .toArray(new Class<?>[classList.size()]);
                        }
                        clazz.getMethod(methodName, argClasses)
                                .invoke(converter,
                                        methodArgs
                                                .toArray(new Object[methodArgs
                                                        .size()]));
                    }
                } catch (final Exception e) {
                    logger.warn("Failed to invoke: " + obj.toString(), e);
                }
            }
        }

    }

    private static Class<? extends Object> getArgClass(final Object arg) {
        final Class<? extends Object> clazz = arg.getClass();
        if (clazz.equals(ArrayList.class)) {
            return List.class;
        }
        return clazz;
    }
}
