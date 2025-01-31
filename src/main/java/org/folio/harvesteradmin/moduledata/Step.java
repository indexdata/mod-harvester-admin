package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import org.folio.harvesteradmin.moduledata.database.Tables;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Step extends Entity {

    // Step record, the entity data.
    public record Record(UUID id, String name, boolean enabled, String description, String type, String inputFormat,
                         String outputFormat, String testData, String testOutput, String script) {
    }
    public Record record;

    // Static map of Entity Fields.
    private static final Map<String, Field> FIELDS = new HashMap<>();
    public static final String ID="ID", NAME="NAME", TYPE="TYPE", DESCRIPTION="DESCRIPTION", INPUT_FORMAT="INPUT_FORMAT",
            OUTPUT_FORMAT="OUTPUT_FORMAT", TEST_DATA="TEST_DATA", TEST_OUTPUT="TEST_OUTPUT", SCRIPT = "SCRIPT";
    static {
        FIELDS.put(ID,new Field("id", "id", PgColumn.Type.UUID, false, true, true));
        FIELDS.put(NAME,new Field("name", "name", PgColumn.Type.TEXT, false, true));
        FIELDS.put(DESCRIPTION, new Field("description", "description", PgColumn.Type.TEXT, true, true));
        FIELDS.put(TYPE, new Field("type", "type", PgColumn.Type.TEXT, true, true));
        FIELDS.put(INPUT_FORMAT, new Field("inputFormat", "input_format", PgColumn.Type.TEXT, true, true));
        FIELDS.put(OUTPUT_FORMAT, new Field("outputFormat", "output_format", PgColumn.Type.TEXT, true, true));
        FIELDS.put(TEST_DATA, new Field("testData", "test_data", PgColumn.Type.TEXT, true, false));
        FIELDS.put(TEST_OUTPUT, new Field("testOutput", "test_output", PgColumn.Type.TEXT, true, false));
        FIELDS.put(SCRIPT, new Field("script", "script", PgColumn.Type.TEXT, true, false));
    }
    @Override
    public Map<String, Field> fields() {
        return FIELDS;
    }

    @Override
    public Tables table() {
        return Tables.step;
    }

    /**
     * Creates record from JSON.
     * @param stepJson Step JSON
     * @return Data object
     */
    public Step fromJson(JsonObject stepJson) {
        record = new Record(
                UUID.fromString(stepJson.getString(jsonPropertyName(ID))),
                stepJson.getString(jsonPropertyName(NAME)),
                true,
                stepJson.getString(jsonPropertyName(TYPE)),
                stepJson.getString(jsonPropertyName(DESCRIPTION)),
                stepJson.getString(jsonPropertyName(INPUT_FORMAT)),
                stepJson.getString(jsonPropertyName(OUTPUT_FORMAT)),
                stepJson.getString(jsonPropertyName(TEST_DATA)),
                stepJson.getString(jsonPropertyName(TEST_OUTPUT)),
                stepJson.getString(jsonPropertyName(SCRIPT)));
        return this;
    }

    public JsonObject asJson() {
        JsonObject json = new JsonObject();
        json.put(jsonPropertyName(ID), record.id());
        json.put(jsonPropertyName(NAME), record.name());
        json.put(jsonPropertyName(TYPE), record.type());
        json.put("enabled", record.enabled());
        json.put(jsonPropertyName(DESCRIPTION), record.description());
        json.put(jsonPropertyName(INPUT_FORMAT), record.inputFormat());
        json.put(jsonPropertyName(OUTPUT_FORMAT), record.outputFormat());
        json.put(jsonPropertyName(TEST_DATA), record.testData());
        json.put(jsonPropertyName(TEST_OUTPUT), record.testOutput());
        json.put(jsonPropertyName(SCRIPT), record.script());
        return json;
    }

    /**
     * Maps from PG row to POJO
     * @return Step data object
     */
    @Override
    public RowMapper<Entity> getRowMapper() {
        return row -> {
            record = new Record(
                    row.getUUID(dbColumnName(ID)),
                    row.getString(dbColumnName(NAME)),
                    true,
                    row.getString(dbColumnName(TYPE)),
                    row.getString(dbColumnName(DESCRIPTION)),
                    row.getString(dbColumnName(INPUT_FORMAT)),
                    row.getString(dbColumnName(OUTPUT_FORMAT)),
                    row.getString(dbColumnName(TEST_DATA)),
                    row.getString(dbColumnName(TEST_OUTPUT)),
                    row.getString(dbColumnName(SCRIPT)));
            return this;
        };
    }

    /**
     * Maps from entity data object to PG columns
     * @return a mapper to be used by PG insert statement
     */
    @Override
    public TupleMapper<Entity> getTupleMapper() {
        return TupleMapper.mapper(
                entity -> {
                    Record rec = ((Step) entity).record;
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put(dbColumnName(ID), rec.id);
                    parameters.put(dbColumnName(NAME), rec.name);
                    parameters.put(dbColumnName(TYPE), rec.type);
                    parameters.put(dbColumnName(DESCRIPTION), rec.description);
                    parameters.put(dbColumnName(INPUT_FORMAT), rec.inputFormat);
                    parameters.put(dbColumnName(OUTPUT_FORMAT), rec.outputFormat);
                    parameters.put(dbColumnName(TEST_DATA), rec.testData);
                    parameters.put(dbColumnName(TEST_OUTPUT), rec.testOutput);
                    parameters.put(dbColumnName(SCRIPT), rec.script);
                    return parameters;
                });
    }

    /**
     * Checks for valid XML.
     */
    public static String validateScriptAsXml(String script) {
        try {
            DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
            builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder parser = builder.newDocumentBuilder();
            parser.parse(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)));
        } catch (ParserConfigurationException | IOException | SAXException pe) {
            return "Validation failed for script [ " + script + "]: " + pe.getMessage();
        }
        return "OK";
    }

}
