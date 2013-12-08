package jp.sf.fess.solr.plugin.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.solr.core.SolrCore;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.DirectUpdateHandler2;
import org.apache.solr.update.UpdateHandler;
import org.apache.solr.util.RefCounted;

public class FessDirectUpdateHandler extends DirectUpdateHandler2 {

    private static final String TERM_PARAM = "term";

    private static final String EXTENDED_CMD = "excmd";

    private static final String UPDATE_CMD = "update";

    public FessDirectUpdateHandler(final SolrCore core) {
        super(core);
    }

    public FessDirectUpdateHandler(final SolrCore core,
            final UpdateHandler updateHandler) {
        super(core, updateHandler);
    }

    @Override
    public int addDoc(final AddUpdateCommand cmd) throws IOException {
        final String exCmd = cmd.getReq().getParams().get(EXTENDED_CMD);
        if (UPDATE_CMD.equals(exCmd)) {
            final String termName = cmd.getReq().getParams().get(TERM_PARAM);
            if (termName == null) {
                throw new IllegalArgumentException("term is not specified.");
            }

            int rc = -1;
            final RefCounted<IndexWriter> iw = solrCoreState
                    .getIndexWriter(core);
            try {
                final IndexWriter writer = iw.get();

                if (cmd.isBlock()) {
                    for (final Iterable<? extends IndexableField> doc : cmd) {
                        updateNumericValue(writer, doc, termName);
                    }
                } else {
                    final Iterable<IndexableField> doc = cmd
                            .getLuceneDocument();
                    updateNumericValue(writer, doc, termName);
                }
                rc = 1;
            } finally {
                iw.decref();
            }
            return rc;
        } else {
            return super.addDoc(cmd);
        }
    }

    private void updateNumericValue(final IndexWriter writer,
            final Iterable<? extends IndexableField> doc, final String termName)
            throws IOException {
        String termValue = null;
        final List<IndexableField> numericFieldList = new ArrayList<IndexableField>();
        for (final IndexableField field : doc) {
            if (termName.equals(field.name()) && field.stringValue() != null) {
                termValue = field.stringValue();
            } else if (field instanceof NumericDocValuesField) {
                numericFieldList.add(field);
            }
        }
        if (termValue == null) {
            throw new IllegalArgumentException(
                    "A value of term is not found in the doc.");
        }
        for (final IndexableField field : numericFieldList) {
            writer.updateNumericDocValue(new Term(termName, termValue),
                    field.name(), field.numericValue().longValue());
        }
    }
}
