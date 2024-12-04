package org.folio.harvesteradmin.service.harvest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.service.harvest.transformation.RecordReceiver;
import org.folio.harvesteradmin.service.harvest.transformation.XmlCollectionSplitter;

import java.util.concurrent.Callable;

public class Ingest implements Callable<Object> {

    String xmlContents;
    private final RecordReceiver pipeline;
    public static final Logger logger = LogManager.getLogger("harvester");

    public Ingest(String xmlContents, RecordReceiver pipeline) {
        this.xmlContents = xmlContents;
        this.pipeline = pipeline;
    }

    @Override
    public Object call() {
        new XmlCollectionSplitter(xmlContents, pipeline).produceRecords();
        return null;
    }


}
