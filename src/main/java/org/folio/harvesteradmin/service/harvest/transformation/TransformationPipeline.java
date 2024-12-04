package org.folio.harvesteradmin.service.harvest.transformation;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.harvesteradmin.legacydata.LegacyHarvesterStorage;
import org.folio.harvesteradmin.legacydata.statics.ApiPaths;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class TransformationPipeline implements RecordReceiver {

    private final List<Templates> listOfTemplates = new ArrayList<>();
    private final RecordReceiver recordReceiver;
    private TransformationPipeline(JsonObject transformation, RecordReceiver target) {
        this.recordReceiver = target;
        setTemplates(transformation);
    }

    public static Future<TransformationPipeline> build(Vertx vertx, String tenant, String transformationId, RecordReceiver recordReceiver) {
        Promise<TransformationPipeline> promise = Promise.promise();
        new LegacyHarvesterStorage(vertx, tenant)
                .getConfigRecordById(ApiPaths.HARVESTER_TRANSFORMATIONS_PATH, transformationId)
                .onSuccess(transformationResponse -> {
                    promise.complete(new TransformationPipeline(transformationResponse.jsonObject(), recordReceiver));
                });
        return promise.future();
    }

    private void transformConvertAndForward(String xmlRecord) {
        String transformedRecord = "";
        for (Templates templates : listOfTemplates) {
            transformedRecord = transform(xmlRecord, templates);
        }
        String jsonRecord = convertToJson(transformedRecord);
        recordReceiver.put(jsonRecord);
    }

    private String transform (String xmlRecord, Templates templates) {
        try {
        Source sourceXml = new StreamSource(new StringReader(xmlRecord));
        StreamResult resultXmlStream = new StreamResult(new StringWriter());
        Transformer transformer = templates.newTransformer();
        transformer.transform(sourceXml, resultXmlStream);
        return resultXmlStream.getWriter().toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private String convertToJson (String xmlRecord) {
        return InventoryXmlToInventoryJson.convert(xmlRecord).encodePrettily();
    }

    private void setTemplates(JsonObject transformation) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        for (Object o : transformation.getJsonArray("stepAssociations")) {
            JsonObject step = ((JsonObject) o).getJsonObject("step");
            try {
                if (step.getString("entityType").equals("xmlTransformationStep")) {
                    String script = step.getString("script").replaceAll("\\r[\\n]?", System.lineSeparator());
                    Source xslt = new StreamSource(new StringReader(script));
                    listOfTemplates.add(transformerFactory.newTemplates(xslt));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public void put(String record) {
       transformConvertAndForward(record);
    }

}
