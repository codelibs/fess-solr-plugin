package jp.sf.fess.solr.plugin.suggest.index;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import jp.sf.fess.suggest.SuggestConstants;

import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.codelibs.solr.lib.server.SolrLibHttpSolrServer;
import org.codelibs.solr.lib.server.interceptor.PreemptiveAuthInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuggestSolrServer {
    private static final Logger logger = LoggerFactory
            .getLogger(SuggestSolrServer.class);

    private SolrLibHttpSolrServer server;

    public SuggestSolrServer(final String url) {
        try {
            server = new SolrLibHttpSolrServer(url);
            server.setConnectionTimeout(10 * 1000);
            server.setMaxRetries(3);
        } catch (final Exception e) {
            logger.warn("Failed to create SuggestSolrServer object.", e);
        }
    }

    public SuggestSolrServer(final String url, final String user,
            final String password) {
        try {
            server = new SolrLibHttpSolrServer(url);
            server.setConnectionTimeout(10 * 1000);
            server.setMaxRetries(3);

            if (StringUtils.isNotBlank(user)) {
                final URL u = new URL(url);
                final AuthScope authScope = new AuthScope(u.getHost(),
                        u.getPort());
                final Credentials credentials = new UsernamePasswordCredentials(
                        user, password);
                server.setCredentials(authScope, credentials);
                server.addRequestInterceptor(new PreemptiveAuthInterceptor());
            }
        } catch (final Exception e) {
            logger.warn("Failed to create SuggestSolrServer object.", e);
        }

        //TODO その他設定
    }

    public void add(final SolrInputDocument doc) throws IOException,
            SolrServerException {
        server.add(doc);
    }

    public void add(final List<SolrInputDocument> documents)
            throws IOException, SolrServerException {
        server.add(documents);
    }

    public void commit() throws IOException, SolrServerException {
        server.commit();
    }

    public void deleteAll() throws IOException, SolrServerException {
        server.deleteByQuery("*:*");
    }

    public void deleteByQuery(final String query) throws IOException,
            SolrServerException {
        server.deleteByQuery(query);
    }

    public SolrDocumentList select(final String query) throws IOException,
            SolrServerException {

        final SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFields(new String[] { "id",
                SuggestConstants.SuggestFieldNames.COUNT,
                SuggestConstants.SuggestFieldNames.LABELS,
                SuggestConstants.SuggestFieldNames.ROLES,
                SuggestConstants.SuggestFieldNames.FIELD_NAME });
        final QueryResponse queryResponse = server.query(solrQuery,
                SolrRequest.METHOD.POST);
        final SolrDocumentList responseList = queryResponse.getResults();
        return responseList;
    }

    public SolrDocumentList get(final String ids) throws IOException,
            SolrServerException {
        final SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRequestHandler("/get");
        solrQuery.set("ids", ids);
        final QueryResponse response = server.query(solrQuery,
                SolrRequest.METHOD.POST);
        final SolrDocumentList responseList = response.getResults();
        return responseList;
    }
}
