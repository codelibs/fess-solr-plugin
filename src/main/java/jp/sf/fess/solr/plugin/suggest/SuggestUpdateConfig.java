package jp.sf.fess.solr.plugin.suggest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SuggestUpdateConfig {
    private String solrUrl = "";

    private String solrUser = "";

    private String solrPassword = "";

    private String[] labelFields = null;

    private String[] roleFields = null;

    private long updateInterval = 10 * 1000;

    private String expiresField = "expires_dt";

    private String segmentField = "segment";

    private final List<FieldConfig> fieldConfigList = new ArrayList<FieldConfig>();

    public String getSolrUrl() {
        return solrUrl;
    }

    public void setSolrUrl(final String solrUrl) {
        this.solrUrl = solrUrl;
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

    public String getSolrUser() {
        return solrUser;
    }

    public void setSolrUser(final String solrUser) {
        this.solrUser = solrUser;
    }

    public String getSolrPassword() {
        return solrPassword;
    }

    public void setSolrPassword(final String solrPassword) {
        this.solrPassword = solrPassword;
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
