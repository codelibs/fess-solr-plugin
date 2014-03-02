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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.apache.solr.update.TransactionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public final class TransactionLogUtil {
    private static final Logger logger = LoggerFactory
            .getLogger(TransactionLogUtil.class);

    private static final String PREFIX = "suggest-";

    private static volatile Constructor<TransactionLog> transactionLogConstructor;

    private TransactionLogUtil() {
    }

    public static TransactionLog createSuggestTransactionLog(
            final File tlogFile, final Collection<String> globalStrings,
            final boolean openExisting) throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IOException,
            InvocationTargetException {
        final long start = System.currentTimeMillis();
        final File file = new File(tlogFile.getParent(), PREFIX
                + tlogFile.getName());
        Files.copy(tlogFile, file);
        if (logger.isInfoEnabled()) {
            logger.info("Create suggest trans log. took="
                    + (System.currentTimeMillis() - start) + " file="
                    + file.getAbsolutePath());
        }

        if (transactionLogConstructor == null) {
            synchronized (TransactionLogUtil.class) {
                if (transactionLogConstructor == null) {
                    final Class<TransactionLog> cls = TransactionLog.class;
                    transactionLogConstructor = cls.getDeclaredConstructor(
                            File.class, Collection.class, Boolean.TYPE);
                    transactionLogConstructor.setAccessible(true);
                }
            }
        }
        return transactionLogConstructor.newInstance(file, globalStrings,
                openExisting);
    }

    public static void clearSuggestTransactionLog(final String dir) {
        final File d = new File(dir);
        if (!d.isDirectory()) {
            return;
        }
        for (final File f : d.listFiles()) {
            if (f.isFile() && f.getName().startsWith(PREFIX) && !f.delete()) {
                logger.warn("Failed to delete " + f.getAbsolutePath());
            }
        }
    }
}
