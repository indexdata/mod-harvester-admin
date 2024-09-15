package org.folio.harvesteradmin.test.fakestorage;

public class ConfigurationStorage extends RecordStorage {
    public String getResultSetName() {
        return CONFIGURATION_ENTRIES;
    }

    @Override
    protected void declareDependencies() {
        // Instances have none in fake storage
    }

    @Override
    protected void declareMandatoryProperties() {
        mandatoryProperties.add("source");
    }

    protected void declareUniqueProperties() {
        uniqueProperties.add("hrid");
    }


}
