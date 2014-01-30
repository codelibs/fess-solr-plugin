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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;

public class SuggestUpdateConfig {
    private SolrServer solrServer;

    private String[] labelFields = null;

    private String[] roleFields = null;

    private long updateInterval = 10 * 1000;

    private String expiresField = "expires_dt";

    private String segmentField = "segment";

    private final List<FieldConfig> fieldConfigList = new ArrayList<FieldConfig>();

    public SolrServer getSolrServer() {
        return solrServer;
    }

    public void setSolrServer(final SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    public long getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(final long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public String getExpiresField() {
        return expiresField;
    }

    public void setExpiresField(final String expiresField) {
        this.expiresField = expiresField;
    }

    public String getSegmentField() {
        return segmentField;
    }

    public void setSegmentField(final String segmentField) {
        this.segmentField = segmentField;
    }

    public String[] getLabelFields() {
        return labelFields;
    }

    public void setLabelFields(final String[] labelFields) {
        this.labelFields = labelFields;
    }

    public String[] getRoleFields() {
        return roleFields;
    }

    public void setRoleFields(final String[] roleFields) {
        this.roleFields = roleFields;
    }

    public List<FieldConfig> getFieldConfigList() {
        return fieldConfigList;
    }

    public void addFieldConfig(final FieldConfig fieldConfig) {
        fieldConfigList.add(fieldConfig);
    }

    public static class FieldConfig {
        private String[] targetFields = new String[] { "content", "title" };

        private TokenizerConfig tokenizerConfig = null;

        private final List<ConverterConfig> converterConfigList = new ArrayList<ConverterConfig>();

        private final List<NormalizerConfig> normalizerConfigList = new ArrayList<NormalizerConfig>();

        public String[] getTargetFields() {
            return targetFields;
        }

        public void setTargetFields(final String[] targetFields) {
            this.targetFields = targetFields;
        }

        public TokenizerConfig getTokenizerConfig() {
            return tokenizerConfig;
        }

        public void setTokenizerConfig(final TokenizerConfig tokenizerConfig) {
            this.tokenizerConfig = tokenizerConfig;
        }

        public List<ConverterConfig> getConverterConfigList() {
            return converterConfigList;
        }

        public void addConverterConfig(final ConverterConfig converterConfig) {
            converterConfigList.add(converterConfig);
        }

        public List<NormalizerConfig> getNormalizerConfigList() {
            return normalizerConfigList;
        }

        public void addNormalizerConfig(final NormalizerConfig normalizerConfig) {
            normalizerConfigList.add(normalizerConfig);
        }
    }

    public static class TokenizerConfig {
        private String className = "jp.sf.fess.suggest.analysis.SuggestTokenizerFactory";

        Map<String, String> args;

        public String getClassName() {
            return className;
        }

        public void setClassName(final String className) {
            this.className = className;
        }

        public Map<String, String> getArgs() {
            return args;
        }

        public void setArgs(final Map<String, String> args) {
            this.args = args;
        }
    }

    public static class ConverterConfig {
        private String className;

        private Map<String, String> properties;

        public String getClassName() {
            return className;
        }

        public void setClassName(final String className) {
            this.className = className;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(final Map<String, String> properties) {
            this.properties = properties;
        }
    }

    public static class NormalizerConfig {
        private String className;

        private Map<String, String> properties;

        public String getClassName() {
            return className;
        }

        public void setClassName(final String className) {
            this.className = className;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(final Map<String, String> properties) {
            this.properties = properties;
        }
    }

}
