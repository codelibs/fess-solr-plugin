package jp.sf.fess.solr.plugin.suggest.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jp.sf.fess.solr.plugin.suggest.entity.SuggestItem;
import jp.sf.fess.solr.plugin.suggest.enums.RequestType;
import jp.sf.fess.suggest.SuggestConstants;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexUpdater extends Thread {
    private final Logger logger = LoggerFactory.getLogger(IndexUpdater.class);

    protected Queue<Request> suggestRequestQueue = new ConcurrentLinkedQueue<Request>();

    protected SuggestSolrServer suggestSolrServer;

    protected AtomicBoolean running = new AtomicBoolean(false);

    protected AtomicLong updateInterval = new AtomicLong(10 * 1000);

    protected AtomicInteger maxUpdateNum = new AtomicInteger(10000);

    public IndexUpdater(final SuggestSolrServer suggestSolrServer) {
        this.suggestSolrServer = suggestSolrServer;
    }

    public void setUpdateInterval(final long updateInterval) {
        this.updateInterval.set(updateInterval);
    }

    public int getQueuingItemNum() {
        return suggestRequestQueue.size();
    }

    public synchronized void addSuggestItem(final SuggestItem item) {
        boolean exist = false;
        final Iterator<Request> it = suggestRequestQueue.iterator();
        while (it.hasNext()) {
            final Request request = it.next();
            if (request.type == RequestType.ADD) {
                final SuggestItem existItem = (SuggestItem) request.obj;
                if (existItem.equals(item)) {
                    exist = true;
                    existItem.setCount(existItem.getCount() + 1);
                    existItem.setExpires(item.getExpires());
                    existItem.setSegment(item.getSegment());
                    final List<String> fieldNameList = existItem
                            .getFieldNameList();
                    for (final String fieldName : item.getFieldNameList()) {
                        if (!fieldNameList.contains(fieldName)) {
                            fieldNameList.add(fieldName);
                        }
                    }
                    final List<String> labelFieldNameList = existItem
                            .getLabels();
                    for (final String label : item.getLabels()) {
                        if (!labelFieldNameList.contains(label)) {
                            labelFieldNameList.add(label);
                        }
                    }
                    break;
                }
            }
        }

        if (!exist) {
            final Request request = new Request(RequestType.ADD, item);
            suggestRequestQueue.add(request);
            notify();
        }
    }

    public void commit() {
        final Request request = new Request(RequestType.COMMIT, null);
        suggestRequestQueue.add(request);
        synchronized (this) {
            notify();
        }
    }

    public void deleteByQuery(final String query) {
        final Request request = new Request(RequestType.DELETE_BY_QUERY, query);
        suggestRequestQueue.add(request);
        synchronized (this) {
            notify();
        }
    }

    public void close() {
        running.set(false);
    }

    @Override
    public void run() {
        logger.info("Start indexUpdater");
        running.set(true);
        boolean doCommit = false;
        while (running.get()) {
            final int maxUpdateNum = this.maxUpdateNum.get();
            final Request[] requestArray = new Request[maxUpdateNum];
            int requestNum = 0;
            synchronized (this) {
                for (int i = 0; i < maxUpdateNum; i++) {
                    final Request request = suggestRequestQueue.peek();
                    if (request == null) {
                        break;
                    }
                    if (request.type == RequestType.ADD) {
                        requestArray[requestNum] = suggestRequestQueue.poll();
                        requestNum++;
                    } else if (requestNum == 0) {
                        requestArray[requestNum] = suggestRequestQueue.poll();
                        requestNum++;
                        break;
                    } else {
                        break;
                    }
                }
            }

            if (requestNum == 0) {
                try {
                    if (doCommit) {
                        suggestSolrServer.commit();
                        doCommit = false;
                    }
                    try {
                        synchronized (this) {
                            this.wait(updateInterval.get());
                        }
                    } catch (final InterruptedException e) {
                        break;
                    }
                } catch (final Exception e) {
                }
                continue;
            }
            doCommit = true;

            switch (requestArray[0].type) {
            case ADD:
                final long start = System.currentTimeMillis();
                if (logger.isDebugEnabled()) {
                    logger.debug("Add " + requestNum + "documents");
                }

                final SuggestItem[] suggestItemArray = new SuggestItem[requestNum];
                int itemSize = 0;
                final StringBuilder ids = new StringBuilder(10000);
                for (int i = 0; i < requestNum; i++) {
                    final Request request = requestArray[i];
                    final SuggestItem item = (SuggestItem) request.obj;
                    suggestItemArray[itemSize] = item;
                    if (ids.length() > 0) {
                        ids.append(',');
                    }
                    ids.append(item.getDocumentId());
                    itemSize++;
                }
                mergeSolrIndex(suggestItemArray, itemSize, ids.toString());

                final List<SolrInputDocument> solrInputDocumentList = new ArrayList<SolrInputDocument>(
                        itemSize);
                for (int i = 0; i < itemSize; i++) {
                    final SuggestItem item = suggestItemArray[i];
                    solrInputDocumentList.add(item.toSolrInputDocument());
                }
                try {
                    suggestSolrServer.add(solrInputDocumentList);
                    if (logger.isInfoEnabled()) {
                        logger.info("Done add " + itemSize + " terms. took: "
                                + (System.currentTimeMillis() - start));
                    }
                } catch (final Exception e) {
                    logger.warn("Failed to add document.", e);
                }
                break;
            case COMMIT:
                if (logger.isDebugEnabled()) {
                    logger.debug("Commit.");
                }
                try {
                    suggestSolrServer.commit();
                    doCommit = false;
                } catch (final Exception e) {
                    logger.warn("Failed to commit.", e);
                }
                break;
            case DELETE_BY_QUERY:
                if (logger.isInfoEnabled()) {
                    logger.info("DeleteByQuery. query="
                            + requestArray[0].obj.toString());
                }
                try {
                    suggestSolrServer
                            .deleteByQuery((String) requestArray[0].obj);
                    suggestSolrServer.commit();
                } catch (final Exception e) {
                    logger.warn("Failed to deleteByQuery.", e);
                }
                break;
            default:
                break;
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Stop IndexUpdater");
            suggestRequestQueue.clear();
        }
    }

    protected void mergeSolrIndex(final SuggestItem[] suggestItemArray,
            final int itemSize, final String ids) {
        final long startTime = System.currentTimeMillis();
        if (itemSize > 0) {
            SolrDocumentList documentList = null;
            try {
                documentList = suggestSolrServer.get(ids);
                if (logger.isDebugEnabled()) {
                    logger.debug("search end. " + "getNum="
                            + documentList.size() + "  took:"
                            + (System.currentTimeMillis() - startTime));
                }
            } catch (final Exception e) {
                logger.warn("Failed merge solr index.", e);
            }
            if (documentList != null) {
                int itemCount = 0;
                for (final SolrDocument doc : documentList) {
                    final Object idObj = doc.getFieldValue("id");
                    if (idObj == null) {
                        continue;
                    }
                    final String id = idObj.toString();

                    for (; itemCount < itemSize; itemCount++) {
                        final SuggestItem item = suggestItemArray[itemCount];

                        if (item.getDocumentId().equals(id)) {
                            final Object count = doc
                                    .getFieldValue(SuggestConstants.SuggestFieldNames.COUNT);
                            if (count != null) {
                                item.setCount(item.getCount()
                                        + Long.parseLong(count.toString()));
                            }
                            final Collection<Object> labels = doc
                                    .getFieldValues(SuggestConstants.SuggestFieldNames.LABELS);
                            if (labels != null) {
                                final List<String> itemLabelList = item
                                        .getLabels();
                                for (final Object label : labels) {
                                    if (!itemLabelList.contains(label
                                            .toString())) {
                                        itemLabelList.add(label.toString());
                                    }
                                }
                            }
                            final Collection<Object> fields = doc
                                    .getFieldValues(SuggestConstants.SuggestFieldNames.FIELD_NAME);
                            if (fields != null) {
                                final List<String> fieldNameList = item
                                        .getFieldNameList();
                                for (final Object field : fields) {
                                    if (!fieldNameList.contains(field
                                            .toString())) {
                                        fieldNameList.add(field.toString());
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
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
}
