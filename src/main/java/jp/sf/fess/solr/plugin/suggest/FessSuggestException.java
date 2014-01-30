package jp.sf.fess.solr.plugin.suggest;

import jp.sf.fess.solr.plugin.FessSolrPluginException;

public class FessSuggestException extends FessSolrPluginException {

    private static final long serialVersionUID = 1L;

    public FessSuggestException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public FessSuggestException(final String message) {
        super(message);
    }

}
