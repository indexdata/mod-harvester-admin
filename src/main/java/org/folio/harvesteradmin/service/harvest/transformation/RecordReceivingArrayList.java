package org.folio.harvesteradmin.service.harvest.transformation;

import java.util.ArrayList;
import java.util.List;

public class RecordReceivingArrayList implements RecordReceiver {

    private final List<String> listOfRecords = new ArrayList<>();
    @Override
    public void put(String record) {
        listOfRecords.add(record);
    }

    @Override
    public void endOfDocument() {
    }

    public List<String> getListOfRecords () {
        return listOfRecords;
    }
}
