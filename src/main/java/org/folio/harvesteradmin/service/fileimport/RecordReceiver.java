package org.folio.harvesteradmin.service.fileimport;

public interface RecordReceiver {
    void put(String record);

    void endOfDocument();
}
