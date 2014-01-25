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

package jp.sf.fess.solr.plugin.update;

import java.io.IOException;

import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.MergeIndexesCommand;
import org.apache.solr.update.RollbackUpdateCommand;
import org.apache.solr.update.SplitIndexCommand;

public class UpdateHandlerFilterChain {
    FessUpdateHandler updateHandler;

    private final UpdateHandlerFilter[] filters;

    private int position = 0;

    public UpdateHandlerFilterChain(final FessUpdateHandler updateHandler,
            final UpdateHandlerFilter[] filters) {
        this.updateHandler = updateHandler;
        this.filters = filters;
    }

    public int addDoc(final AddUpdateCommand cmd) throws IOException {
        if (position < filters.length) {
            final UpdateHandlerFilter filter = filters[position];
            position++;
            return filter.addDoc(cmd, this);
        } else {
            return updateHandler.doAddDoc(cmd);
        }
    }

    public void delete(final DeleteUpdateCommand cmd) throws IOException {
        if (position < filters.length) {
            final UpdateHandlerFilter filter = filters[position];
            position++;
            filter.delete(cmd, this);
        } else {
            updateHandler.doDelete(cmd);
        }
    }

    public void deleteByQuery(final DeleteUpdateCommand cmd) throws IOException {
        if (position < filters.length) {
            final UpdateHandlerFilter filter = filters[position];
            position++;
            filter.deleteByQuery(cmd, this);
        } else {
            updateHandler.doDeleteByQuery(cmd);
        }
    }

    public int mergeIndexes(final MergeIndexesCommand cmd) throws IOException {
        if (position < filters.length) {
            final UpdateHandlerFilter filter = filters[position];
            position++;
            return filter.mergeIndexes(cmd, this);
        } else {
            return updateHandler.doMergeIndexes(cmd);
        }
    }

    public void prepareCommit(final CommitUpdateCommand cmd) throws IOException {
        if (position < filters.length) {
            final UpdateHandlerFilter filter = filters[position];
            position++;
            filter.prepareCommit(cmd, this);
        } else {
            updateHandler.doPrepareCommit(cmd);
        }
    }

    public void commit(final CommitUpdateCommand cmd) throws IOException {
        if (position < filters.length) {
            final UpdateHandlerFilter filter = filters[position];
            position++;
            filter.commit(cmd, this);
        } else {
            updateHandler.doCommit(cmd);
        }
    }

    public void rollback(final RollbackUpdateCommand cmd) throws IOException {
        if (position < filters.length) {
            final UpdateHandlerFilter filter = filters[position];
            position++;
            filter.rollback(cmd, this);
        } else {
            updateHandler.doRollback(cmd);
        }
    }

    public void split(final SplitIndexCommand cmd) throws IOException {
        if (position < filters.length) {
            final UpdateHandlerFilter filter = filters[position];
            position++;
            filter.split(cmd, this);
        } else {
            updateHandler.doSplit(cmd);
        }
    }

    public void close() throws IOException {
        if (position < filters.length) {
            final UpdateHandlerFilter filter = filters[position];
            position++;
            filter.close(this);
        } else {
            updateHandler.doClose();
        }
    }

}
