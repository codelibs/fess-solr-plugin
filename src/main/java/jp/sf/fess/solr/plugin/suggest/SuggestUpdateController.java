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

package jp.sf.fess.solr.plugin.suggest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import jp.sf.fess.solr.plugin.suggest.entity.SuggestFieldInfo;
import jp.sf.fess.solr.plugin.suggest.entity.SuggestItem;
import jp.sf.fess.solr.plugin.suggest.enums.RequestType;
import jp.sf.fess.solr.plugin.suggest.index.DocumentReader;
import jp.sf.fess.solr.plugin.suggest.index.IndexUpdater;
import jp.sf.fess.solr.plugin.suggest.index.SuggestSolrServer;
import jp.sf.fess.solr.plugin.suggest.util.TransactionLogUtil;
import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.exception.FessSuggestException;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.update.TransactionLog;
import org.apache.solr.update.UpdateLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuggestUpdateController {
    private static final Logger logger = LoggerFactory
            .getLogger(SuggestUpdateController.class);

    protected final UpdateTask updateTask;

    protected final TransactionLogParseTask transactionLogParseTask;

    protected final IndexUpdater indexUpdater;

    protected int limitDocumentQueuingNum = 50;

    protected int limitTermQueuingNum = 50000;

    protected final BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<Request>();

    protected final List<String> labelFieldNameList = Collections
            .synchronizedList(new ArrayList<String>()); // TODO

    protected final List<String> roleFieldNameList = Collections
            .synchronizedList(new ArrayList<String>()); // TODO

    protected final List<SuggestFieldInfo> suggestFieldInfoList;

    protected final SuggestUpdateConfig config;

    public SuggestUpdateController(final SuggestUpdateConfig config,
            final List<SuggestFieldInfo> fieldInfoList) {
        final SuggestSolrServer suggestSolrServer = new SuggestSolrServer(
                config.getSolrServer());
        indexUpdater = new IndexUpdater(suggestSolrServer);
        indexUpdater.setUpdateInterval(config.getUpdateInterval());
        suggestFieldInfoList = fieldInfoList;

        this.config = config;

        updateTask = new UpdateTask();

        transactionLogParseTask = new TransactionLogParseTask(
                new TransactionLogParseListener() {
                    @Override
                    public void addCBK(final SolrInputDocument solrInputDocument) {
                        add(solrInputDocument);
                    }

                    @Override
                    public void deleteByQueryCBK(final String query) {
                        deleteByQuery(query);
                    }

                    @Override
                    public void commitCBK() {
                        commit();
                    }
                });
    }

    public void start() {
        indexUpdater.start();
        updateTask.start();
        transactionLogParseTask.start();
    }

    public void setLimitTermQueuingNum(final int limitTermQueuingNum) {
        this.limitTermQueuingNum = limitTermQueuingNum;
    }

    public void setLimitDocumentQueuingNum(final int limitDocumentQueuingNum) {
        this.limitDocumentQueuingNum = limitDocumentQueuingNum;
    }

    public void add(final SolrInputDocument doc) {
        request(new Request(RequestType.ADD, doc));
    }

    public void commit() {
        request(new Request(RequestType.COMMIT, null));
    }

    public void deleteByQuery(final String query) {
        request(new Request(RequestType.DELETE_BY_QUERY, query));
    }

    public void addTransactionLog(final File translog) {
        transactionLogParseTask.addTransactionLog(translog);
    }

    public void close() {
        if (logger.isInfoEnabled()) {
            logger.info("closing suggestController");
        }
        updateTask.close();
        transactionLogParseTask.close();
        indexUpdater.close();
        requestQueue.clear();
        try {
            transactionLogParseTask.join();
        } catch (final InterruptedException e) {
            //ignore
        }
        try {
            updateTask.join();
        } catch (final InterruptedException e) {
            //ignore
        }
        try {
            indexUpdater.join();
        } catch (final InterruptedException e) {
            //ignore
        }
    }

    protected void request(final Request request) {
        while ((requestQueue.size() > limitDocumentQueuingNum || indexUpdater
                .getQueuingItemNum() > limitTermQueuingNum)
                && updateTask.isRunning()) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("waiting dequeue documents... doc:"
                            + requestQueue.size() + " term:"
                            + indexUpdater.getQueuingItemNum());
                }
                Thread.sleep(1000);
            } catch (final Exception e) {
                break;
            }
        }

        try {
            requestQueue.put(request);
        } catch (final Exception e) {
            logger.warn("Failed to add " + request, e);
        }
    }

    public void addLabelFieldName(final String labelFieldName) {
        labelFieldNameList.add(labelFieldName);
    }

    public void addRoleFieldName(final String roleFieldName) {
        roleFieldNameList.add(roleFieldName);
    }

    protected class UpdateTask extends Thread {
        protected AtomicBoolean running = new AtomicBoolean(false);

        @Override
        public void run() {
            running.set(true);
            while (running.get()) {
                Request request;
                try {
                    request = requestQueue.take();
                } catch (final InterruptedException e) {
                    break;
                }

                switch (request.type) {
                case ADD:
                    int count = 0;
                    final long start = System.currentTimeMillis();
                    for (final SuggestFieldInfo fieldInfo : suggestFieldInfoList) {
                        final List<String> fieldNameList = fieldInfo
                                .getFieldNameList();
                        final TokenizerFactory tokenizerFactory = fieldInfo
                                .getTokenizerFactory();
                        final SuggestReadingConverter converter = fieldInfo
                                .getSuggestReadingConverter();
                        final SuggestNormalizer normalizer = fieldInfo
                                .getSuggestNormalizer();
                        final SolrInputDocument doc = (SolrInputDocument) request.obj;

                        // create documentReader
                        final DocumentReader reader = new DocumentReader(
                                tokenizerFactory, converter, normalizer, doc,
                                fieldNameList, labelFieldNameList,
                                roleFieldNameList, config.getExpiresField(),
                                config.getSegmentField());
                        SuggestItem item;
                        try {
                            while ((item = reader.next()) != null) {
                                while (count % 10000 == 0
                                        && indexUpdater.getQueuingItemNum() > limitTermQueuingNum
                                        && isRunning()) {
                                    Thread.sleep(1000);
                                }
                                indexUpdater.addSuggestItem(item);
                                count++;
                            }
                        } catch (final InterruptedException e) {
                            logger.warn("updateTask is interrupted");
                            break;
                        } catch (final Exception e) {
                            logger.warn("Failed to tokenize document.", e);
                        }
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("updateTask finish add. took:"
                                + (System.currentTimeMillis() - start)
                                + " count: " + count);
                    }
                    break;
                case COMMIT:
                    indexUpdater.commit();
                    break;
                case DELETE_BY_QUERY:
                    indexUpdater.deleteByQuery(request.obj.toString());
                    break;
                default:
                    break;
                }
            }
        }

        private void close() {
            running.set(false);
            interrupt();
        }

        public boolean isRunning() {
            return running.get();
        }
    }

    protected static class Request {
        public RequestType type;

        public Object obj;

        public Request(final RequestType type, final Object o) {
            this.type = type;
            obj = o;
        }
    }

    protected static class TransactionLogParseTask extends Thread {
        protected static final Logger logger = LoggerFactory
                .getLogger(TransactionLogParseTask.class);

        protected AtomicBoolean isRunning = new AtomicBoolean(false);

        protected BlockingQueue<File> transactionLogQueue = new LinkedBlockingQueue<File>();

        protected final TransactionLogParseListener listener;

        public TransactionLogParseTask(
                final TransactionLogParseListener listener) {
            super();
            this.listener = listener;
        }

        public void close() {
            transactionLogQueue.clear();
            isRunning.set(false);
            interrupt();
        }

        public void addTransactionLog(final File translog) {
            try {
                transactionLogQueue.put(translog);
            } catch (final Exception e) {
                logger.warn("Failed to add " + translog, e);
            }
        }

        @Override
        public void run() {
            if (logger.isInfoEnabled()) {
                logger.info("Starting TransactionLogParseTask...");
            }

            isRunning.set(true);
            while (isRunning.get()) {
                File file;
                TransactionLog translog;
                try {
                    file = transactionLogQueue.take();
                } catch (final InterruptedException e1) {
                    break;
                }
                if(!file.exists()) {
                    logger.warn(file.getAbsolutePath() + " doesn't exist.");
                    continue;
                }
                if(logger.isInfoEnabled()) {
                    logger.info("Loading... " + file.getAbsolutePath());
                }

                try {
                    translog = TransactionLogUtil
                            .createSuggestTransactionLog(file, null, true);
                } catch (Exception e) {
                    logger.warn("Failed to create transactionLog instance. " + file.getAbsolutePath()
                            , e);
                    continue;
                }

                if (logger.isInfoEnabled()) {
                    logger.info("Getting LogReader");
                }
                final TransactionLog.LogReader tlogReader = translog
                        .getReader(0);
                if (tlogReader == null) {
                    logger.warn("Failed to get reader.");
                    continue;
                }

                while (true) {
                    Object o = null;
                    if (!isRunning.get()) {
                        break;
                    }
                    try {
                        o = tlogReader.next();
                    } catch (final Exception e) {
                        logger.warn("Failed to read transaction log. ", e);
                    }
                    if (o == null) {
                        break;
                    }

                    try {
                        // should currently be a List<Oper,Ver,Doc/Id>
                        @SuppressWarnings("unchecked")
                        final List<Object> entry = (List<Object>) o;

                        final int operationAndFlags = (Integer) entry.get(0);
                        final int oper = operationAndFlags
                                & UpdateLog.OPERATION_MASK;

                        switch (oper) {
                        case UpdateLog.ADD: {
                            // byte[] idBytes = (byte[]) entry.get(2);
                            final SolrInputDocument sdoc = (SolrInputDocument) entry
                                    .get(entry.size() - 1);
                            if (logger.isDebugEnabled()) {
                                logger.debug("add " + sdoc);
                            }
                            listener.addCBK(sdoc);
                            break;
                        }
                        case UpdateLog.DELETE_BY_QUERY: {
                            final String query = (String) entry.get(2);
                            if (logger.isDebugEnabled()) {
                                logger.debug("deleteByQuery " + query);
                            }
                            listener.deleteByQueryCBK(query);
                            break;
                        }
                        case UpdateLog.COMMIT: {
                            if (logger.isDebugEnabled()) {
                                logger.debug("commit");
                            }
                            listener.commitCBK();
                            break;
                        }
                        default:
                            throw new FessSuggestException(
                                    "Unknown Operation! " + oper);
                        }
                    } catch (final FessSuggestException e) {
                        logger.warn("Unknown Operation.", e);
                    }
                }
                tlogReader.close();
                translog.decref();
            }
        }
    }

    protected interface TransactionLogParseListener {
        void addCBK(SolrInputDocument solrInputDocument);

        void deleteByQueryCBK(String query);

        void commitCBK();
    }
}
