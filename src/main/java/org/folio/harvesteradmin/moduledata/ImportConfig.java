package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import org.folio.harvesteradmin.moduledata.database.Tables;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ImportConfig extends Entity {

    // Import config record
    public record Record(UUID id, String name, String type, String URL, Boolean allowErrors,
                         Integer recordLimit, Integer batchSize, UUID transformationId, UUID storageId) {
    }
    public Record record;

    // Data definitions

    public static final String ID = "ID", NAME="NAME", TYPE="TYPE",
            URL="URL", ALLOW_ERRORS="ALLOW_ERRORS", RECORD_LIMIT="RECORD_LIMIT", BATCH_SIZE="BATCH_SIZE",
            TRANSFORMATION_ID="TRANSFORMATION_ID", STORAGE_ID="STORAGE_ID";

    private static final Map<String, Field> IMPORT_CONFIG_FIELDS = new HashMap<>();
    static {
        IMPORT_CONFIG_FIELDS.put(ID, new Field("id", "id", PgColumn.Type.UUID, false, true, true));
        IMPORT_CONFIG_FIELDS.put(NAME, new Field("name", "name", PgColumn.Type.TEXT, false, true));
        IMPORT_CONFIG_FIELDS.put(TYPE, new Field("type", "type", PgColumn.Type.TEXT, false, true));
        IMPORT_CONFIG_FIELDS.put(URL, new Field("url", "url", PgColumn.Type.TEXT, false, false));
        IMPORT_CONFIG_FIELDS.put(ALLOW_ERRORS, new Field("allowErrors", "allow_errors", PgColumn.Type.BOOLEAN, false, false));
        IMPORT_CONFIG_FIELDS.put(RECORD_LIMIT, new Field("recordLimit", "record_limit", PgColumn.Type.INTEGER, true, false));
        IMPORT_CONFIG_FIELDS.put(BATCH_SIZE, new Field("batchSize", "batch_size", PgColumn.Type.INTEGER, true, false));
        IMPORT_CONFIG_FIELDS.put(TRANSFORMATION_ID, new Field("transformationId", "transformation_id", PgColumn.Type.UUID, false, true));
        IMPORT_CONFIG_FIELDS.put(STORAGE_ID, new Field("storageId", "storage_id", PgColumn.Type.UUID, true, true));
    }

    @Override
    public Tables table() {
        return Tables.import_config;
    }

    @Override
    public Map<String, Field> fields() {
        return IMPORT_CONFIG_FIELDS;
    }

    public ImportConfig fromJson(JsonObject importConfigJson) {
        System.out.println("importConfig.fromJson: " + importConfigJson.getString(jsonPropertyName(NAME)));
        record = new Record(
                UUID.fromString(importConfigJson.getString(jsonPropertyName(ID))),
                importConfigJson.getString(jsonPropertyName(NAME)),
                importConfigJson.getString(jsonPropertyName(TYPE)),
                importConfigJson.getString(jsonPropertyName(URL)),
                Boolean.valueOf(importConfigJson.getString(jsonPropertyName(ALLOW_ERRORS))),
                importConfigJson.getInteger(jsonPropertyName(RECORD_LIMIT)),
                importConfigJson.getInteger(jsonPropertyName(BATCH_SIZE)),
                Util.getUUID(importConfigJson, jsonPropertyName(TRANSFORMATION_ID), UUID.randomUUID()),
                UUID.fromString("aa97fbcd-f0ff-45e1-83a3-d76e003fab19")); //UUID.fromString(importConfigJson.getString(jsonPropertyName(STORAGE_ID))));
        System.out.println("after importConfig.fromJson");
        return this;
    }

    @Override
    public RowMapper<Entity> getRowMapper() {
        return row -> {
            record = new Record(
            row.getUUID(dbColumnName(ID)),
            row.getString(dbColumnName(NAME)),
            row.getString(dbColumnName(TYPE)),
            row.getString(dbColumnName(URL)),
            row.getBoolean(dbColumnName(ALLOW_ERRORS)),
            row.getInteger(dbColumnName(RECORD_LIMIT)),
            row.getInteger(dbColumnName(BATCH_SIZE)),
            row.getUUID(dbColumnName(TRANSFORMATION_ID)),
            row.getUUID(dbColumnName(STORAGE_ID)));
            return this;
        };
    }

    @Override
    public TupleMapper<Entity> getTupleMapper() {
        return TupleMapper.mapper(
                importConfig -> {
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put(dbColumnName(ID), record.id());
                    parameters.put(dbColumnName(NAME), record.name());
                    parameters.put(dbColumnName(TYPE), record.type());
                    parameters.put(dbColumnName(URL), record.URL());
                    parameters.put(dbColumnName(ALLOW_ERRORS), record.allowErrors());
                    if (record.recordLimit() != null) {
                        parameters.put(dbColumnName(RECORD_LIMIT), record.recordLimit());
                    }
                    if (record.batchSize() != null) {
                        parameters.put(dbColumnName(BATCH_SIZE), record.batchSize());
                    }
                    parameters.put(dbColumnName(TRANSFORMATION_ID), record.transformationId());
                    parameters.put(dbColumnName(STORAGE_ID), record.storageId());
                    return parameters;
                });
    }

    @Override
    public String makeInsertTemplate(String schema) {
        return "INSERT INTO " + schema + "." + Tables.import_config
                + " ("
                + dbColumnName(ID) + ", "
                + dbColumnName(NAME) + ", "
                + dbColumnName(TYPE) + ", "
                + dbColumnName(URL) + ", "
                + dbColumnName(ALLOW_ERRORS) + ", "
                + dbColumnName(RECORD_LIMIT) + ", "
                + dbColumnName(BATCH_SIZE) + ", "
                + dbColumnName(TRANSFORMATION_ID) + ", "
                + dbColumnName(STORAGE_ID)
                + ")"
                + " VALUES ("
                + "#{" + dbColumnName(ID) + "}, "
                + "#{" + dbColumnName(NAME) + "}, "
                + "#{" + dbColumnName(TYPE) + "}, "
                + "#{" + dbColumnName(URL) + "}, "
                + "#{" + dbColumnName(ALLOW_ERRORS) + "}, "
                + "#{" + dbColumnName(RECORD_LIMIT) + "}, "
                + "#{" + dbColumnName(BATCH_SIZE) + "}, "
                + "#{" + dbColumnName(TRANSFORMATION_ID) + "}, "
                + "#{" + dbColumnName(STORAGE_ID) + "}"
                + ")";
    }

    /**
     * HarvestJob to JSON mapping.
     */
    public JsonObject asJson() {
        JsonObject json = new JsonObject();
        json.put(jsonPropertyName(ID), record.id());
        json.put(jsonPropertyName(NAME), record.name());
        json.put(jsonPropertyName(TYPE), record.type());
        json.put(jsonPropertyName(URL), record.URL());
        json.put(jsonPropertyName(ALLOW_ERRORS), record.allowErrors());
        json.put(jsonPropertyName(RECORD_LIMIT), record.recordLimit());
        json.put(jsonPropertyName(BATCH_SIZE), record.batchSize());
        json.put(jsonPropertyName(TRANSFORMATION_ID), record.transformationId());
        if (record.storageId() != null) {
            json.put(jsonPropertyName(STORAGE_ID), record.storageId().toString());
        }
        return json;
    }

}
