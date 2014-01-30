package jp.sf.fess.solr.plugin;

public class FessSolrPluginException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FessSolrPluginException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public FessSolrPluginException(final String message) {
        super(message);
    }

}
