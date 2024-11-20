package org.folio.harvesteradmin.service.harvest.transformation;

import io.vertx.core.Future;
import io.vertx.core.Promise;
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

public class TransformationPipeline {

    private final List<Templates> listOfTemplates = new ArrayList<>();
    private TransformationPipeline(JsonObject transformation) {
        setTemplates(transformation);
    }

    public static Future<TransformationPipeline> build(LegacyHarvesterStorage legacyHarvesterStorage, String transformationId) {
        Promise<TransformationPipeline> promise = Promise.promise();
        legacyHarvesterStorage.getConfigRecordById(ApiPaths.HARVESTER_TRANSFORMATIONS_PATH, transformationId)
                .onSuccess(transformationResponse -> {
                    promise.complete(new TransformationPipeline(transformationResponse.jsonObject()));
                });
        return promise.future();
    }

    public void transformAndConvert (List<String> listOfRecords) {
        transform(listOfRecords);
        convertToJson(listOfRecords);
    }

    public void transform (List<String> listOfRecords) {
        for (int i=0; i< listOfRecords.size(); i++) {
            for (Templates templates : listOfTemplates) {
                listOfRecords.set(i,transform(listOfRecords.get(i), templates));
            }
        }
    }

    public String transform (String record, Templates templates) {
        try {
        Source sourceXml = new StreamSource(new StringReader(record));
        StreamResult resultXmlStream = new StreamResult(new StringWriter());
        Transformer transformer = templates.newTransformer();
        transformer.transform(sourceXml, resultXmlStream);
        return resultXmlStream.getWriter().toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public void convertToJson (List<String> listOfRecords) {
        for (int i=0; i< listOfRecords.size(); i++) {
            listOfRecords.set(i,convertToJson(listOfRecords.get(i)));
        }
    }

    public String convertToJson (String record) {
        return InventoryXmlToInventoryJson.convert(record).encodePrettily();
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


}
