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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransformationPipeline implements RecordReceiver {

    private final List<Templates> listOfTemplates = new ArrayList<>();
    private final RecordReceiver target;
    private static final Map<String, Map<String, TransformationPipeline>> transformationPipelines = new HashMap<>();


    private TransformationPipeline(JsonObject transformation, RecordReceiver target) {
        this.target = target;
        setTemplates(transformation);
    }

    public static Future<TransformationPipeline> instance(Vertx vertx, String tenant, String jobId, String transformationId, RecordReceiver recordReceiver) {
        Promise<TransformationPipeline> promise = Promise.promise();
        if (! hasInstance(tenant, jobId)) {
            new LegacyHarvesterStorage(vertx, tenant)
                    .getConfigRecordById(ApiPaths.HARVESTER_TRANSFORMATIONS_PATH, transformationId)
                    .onSuccess(transformationConfig -> {
                        TransformationPipeline pipeline = new TransformationPipeline(transformationConfig.jsonObject(), recordReceiver);
                        cacheInstance(tenant, jobId, pipeline);
                        promise.complete(pipeline);
                    });
        } else {
            promise.complete(transformationPipelines.get(tenant).get(jobId));
        }
        return promise.future();
    }

    public static boolean hasInstance(String tenant, String jobId) {
        return (transformationPipelines.containsKey(tenant)
                && transformationPipelines.get(tenant).containsKey(jobId)
                && transformationPipelines.get(tenant).get(jobId) != null);
    }

    public static TransformationPipeline getInstance(String tenant, String jobId) {
        return transformationPipelines.get(tenant).get(jobId);
    }

    private static void cacheInstance(String tenant, String jobId,TransformationPipeline pipeline) {
        if (!transformationPipelines.containsKey(tenant)) {
            transformationPipelines.put(tenant, new HashMap<>());
        }
        transformationPipelines.get(tenant).putIfAbsent(jobId, pipeline);
    }

    private String transform(String xmlRecord) {
        String transformedRecord = "";
        for (Templates templates : listOfTemplates) {
            transformedRecord = transform(xmlRecord, templates);
        }
        return transformedRecord;
    }

    private String transform(String xmlRecord, Templates templates) {
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

    private String convertToJson(String xmlRecord) {
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
    public void put(String xmlRecord) {
        String transformedXmlRecord = transform(xmlRecord);
        String jsonRecord = convertToJson(transformedXmlRecord);
        target.put(jsonRecord);
    }

    @Override
    public void endOfDocument() {
        target.endOfDocument();
    }

}
