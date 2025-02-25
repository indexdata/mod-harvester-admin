package org.folio.harvesteradmin.service.fileimport.transformation;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.moduledata.Entity;
import org.folio.harvesteradmin.moduledata.Step;
import org.folio.harvesteradmin.moduledata.TransformationStep;
import org.folio.harvesteradmin.moduledata.database.ModuleStorageAccess;
import org.folio.harvesteradmin.service.fileimport.RecordReceiver;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class TransformationPipeline implements RecordReceiver {

    private final List<Templates> listOfTemplates = new ArrayList<>();
    private RecordReceiver target;
    private int records = 0;
    private long transformationTime = 0;
    public static final Logger logger = LogManager.getLogger("TransformationPipeline");

    private TransformationPipeline(JsonObject transformation) {
        setTemplates(transformation);
    }

    public TransformationPipeline setTarget(RecordReceiver target) {
        this.target = target;
        records = 0;
        transformationTime = 0;
        return this;
    }

    public static Future<TransformationPipeline> create(Vertx vertx, String tenant, UUID transformationId) {
        Promise<TransformationPipeline> promise = Promise.promise();
        ModuleStorageAccess access = new ModuleStorageAccess(vertx, tenant);
        TransformationStep tsasDef = new TransformationStep();
        Step stepDef = new Step();
        access.getEntities("SELECT step.* " +
                        " FROM " + stepDef.table(access.schema()) + " as step," +
                        "      " + tsasDef.table(access.schema()) + " as tsa " +
                        "  WHERE step.id = tsa.step_id " +
                        "    AND tsa.transformation_id = '" + transformationId.toString() + "'" +
                        "  ORDER BY tsa.position", stepDef)
                .onSuccess(steps ->
                {
                    JsonObject json = new JsonObject().put("stepAssociations", new JsonArray());
                    for (Entity step : steps) {
                        JsonObject o = new JsonObject().put("step", step.asJson());
                        o.getJsonObject("step").put("entityType", "xmlTransformationStep");
                        json.getJsonArray("stepAssociations").add(o);
                    }
                    TransformationPipeline pipeline = new TransformationPipeline(json);
                    promise.complete(pipeline);
                })
                .onFailure(handler -> logger.error("Problem retrieving steps " + handler.getMessage()));
        return promise.future();
    }

    private String transform(String xmlRecord) {
        String transformedRecord = xmlRecord;
        for (Templates templates : listOfTemplates) {
            transformedRecord = transform(transformedRecord, templates);
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
        long transformationStarted = System.currentTimeMillis();
        records++;
        xmlRecord = "<collection>" + xmlRecord + "</collection>";
        String transformedXmlRecord = transform(xmlRecord);
        String jsonRecord = convertToJson(transformedXmlRecord);
        transformationTime += (System.currentTimeMillis() - transformationStarted);
        target.put(jsonRecord);
    }

    @Override
    public void endOfDocument() {
        target.endOfDocument();
    }

    public long transformationTime() {
        return transformationTime;
    }

    public int records() {
        return records;
    }
}
