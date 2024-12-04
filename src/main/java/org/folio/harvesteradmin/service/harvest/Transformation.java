package org.folio.harvesteradmin.service.harvest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.service.harvest.transformation.RecordReceivingArrayList;
import org.folio.harvesteradmin.service.harvest.transformation.TransformationPipeline;
import org.folio.harvesteradmin.service.harvest.transformation.XmlCollectionSplitter;

import java.util.concurrent.Callable;

public class Transformation implements Callable<Object> {

    String xmlContents;
    private final TransformationPipeline transformationPipeline;
    public static final Logger logger = LogManager.getLogger("harvester");


    public Transformation(String xmlContents, TransformationPipeline transformationPipeline) {
        this.xmlContents = xmlContents;
        this.transformationPipeline = transformationPipeline;
    }

    @Override
    public Object call() {
        RecordReceivingArrayList recordReceivingArrayList = new RecordReceivingArrayList();
        new XmlCollectionSplitter(xmlContents, recordReceivingArrayList).produceRecords();
        return transformationPipeline.transformAndConvert(recordReceivingArrayList.getListOfRecords());
    }


}
