package org.folio.harvesteradmin.test.fakestorage;

import org.folio.harvesteradmin.foliodata.ConfigurationsClient;

public class ConfigurationStorage extends RecordStorage {
    public String getResultSetName() {
        return ConfigurationsClient.RECORDS;
    }

    @Override
    protected void declareDependencies() {
        // Instances have none in fake storage
    }

    @Override
    protected void declareMandatoryProperties() {}


}
