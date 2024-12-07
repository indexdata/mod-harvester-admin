package org.folio.harvesteradmin.service.harvest.transformation;

public interface RecordReceiver {
    void put(String record);

    void endOfDocument();
}
