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

public class UpdateHandlerFilter {

    protected FessUpdateHandler updateHandler;

    public void setFessUpdateHandler(final FessUpdateHandler updateHandler) {
        this.updateHandler = updateHandler;
    }

    public int addDoc(final AddUpdateCommand cmd,
            final UpdateHandlerFilterChain chain) throws IOException {
        return chain.addDoc(cmd);
    }

    public void delete(final DeleteUpdateCommand cmd,
            final UpdateHandlerFilterChain chain) throws IOException {
        chain.delete(cmd);
    }

    public void deleteByQuery(final DeleteUpdateCommand cmd,
            final UpdateHandlerFilterChain chain) throws IOException {
        chain.deleteByQuery(cmd);
    }

    public int mergeIndexes(final MergeIndexesCommand cmd,
            final UpdateHandlerFilterChain chain) throws IOException {
        return chain.mergeIndexes(cmd);
    }

    public void prepareCommit(final CommitUpdateCommand cmd,
            final UpdateHandlerFilterChain chain) throws IOException {
        chain.prepareCommit(cmd);
    }

    public void commit(final CommitUpdateCommand cmd,
            final UpdateHandlerFilterChain chain) throws IOException {
        chain.commit(cmd);
    }

    public void rollback(final RollbackUpdateCommand cmd,
            final UpdateHandlerFilterChain chain) throws IOException {
        chain.rollback(cmd);
    }

    public void split(final SplitIndexCommand cmd,
            final UpdateHandlerFilterChain chain) throws IOException {
        chain.split(cmd);
    }

    public void close(final UpdateHandlerFilterChain chain) throws IOException {
        chain.close();
    }
}
