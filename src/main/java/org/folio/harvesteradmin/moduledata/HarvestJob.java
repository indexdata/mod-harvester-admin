package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.folio.harvesteradmin.moduledata.database.Tables;
import org.folio.tlib.postgres.PgCqlDefinition;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldAlwaysMatches;


public class HarvestJob extends Entity {

    // Harvest job record
    public Record record;
    public record Record(UUID id, long harvestableId, String name, String type, String url, Boolean allowErrors,
                         Integer recordLimit, Integer batchSize, String transformation, String storage,
                         String status,
                         String started,
                         String finished, Integer amountHarvested,
                         String message) {
    }

    // Fields map.
    private static final Map<String, Field> FIELDS = new HashMap<>();
    // Keys.
    public static String ID = "ID", HARVESTABLE_ID = "HARVESTABLE_ID", HARVESTABLE_NAME = "HARVESTABLE_NAME",
            HARVESTABLE_TYPE = "HARVESTABLE_TYPE", URL = "URL", ALLOW_ERRORS = "ALLOW_ERRORS", RECORD_LIMIT = "RECORD_LIMIT",
            BATCH_SIZE = "BATCH_SIZE", TRANSFORMATION = "TRANSFORMATION", STORAGE = "STORAGE", STATUS = "STATUS", STARTED = "STARTED",
            FINISHED = "FINISHED", AMOUNT_HARVESTED = "AMOUNT_HARVESTED", MESSAGE = "MESSAGE";
    // Populated.
    static {
        FIELDS.put(ID, new Field("id", "id", PgColumn.Type.UUID, false, true, true));
        FIELDS.put(HARVESTABLE_ID, new Field("harvestableId", "harvestable_id", PgColumn.Type.BIGINT, false, true));
        FIELDS.put(HARVESTABLE_NAME, new Field("name", "harvestable_name", PgColumn.Type.TEXT, false, true));
        FIELDS.put(HARVESTABLE_TYPE, new Field("type", "type", PgColumn.Type.TEXT, false, true));
        FIELDS.put(URL, new Field("url", "url", PgColumn.Type.TEXT, false, false));
        FIELDS.put(ALLOW_ERRORS, new Field("allowErrors", "allow_errors", PgColumn.Type.BOOLEAN, false, false));
        FIELDS.put(RECORD_LIMIT, new Field("recordLimit", "record_limit", PgColumn.Type.INTEGER, true, false));
        FIELDS.put(BATCH_SIZE, new Field("batchSize", "batch_size", PgColumn.Type.INTEGER, true, false));
        FIELDS.put(TRANSFORMATION, new Field("transformation", "transformation", PgColumn.Type.TEXT, false, true));
        FIELDS.put(STORAGE, new Field("storage", "storage", PgColumn.Type.TEXT, false, true));
        FIELDS.put(STATUS, new Field("status", "status", PgColumn.Type.TEXT, true, true));
        FIELDS.put(STARTED, new Field("started", "started", PgColumn.Type.TIMESTAMP, false, true));
        FIELDS.put(FINISHED, new Field("finished", "finished", PgColumn.Type.TIMESTAMP, true, true));
        FIELDS.put(AMOUNT_HARVESTED, new Field("amountHarvested", "amount_harvested", PgColumn.Type.INTEGER, true, true));
        FIELDS.put(MESSAGE, new Field("message", "message", PgColumn.Type.TEXT, true, true));
    }

    @Override
    public Tables table() {
        return Tables.harvest_job;
    }

    @Override
    public Map<String, Field> fields() {
        return FIELDS;
    }

    private static final String DATE_FORMAT = "YYYY-MM-DD''T''HH24:MI:SS";

    /**
     * Maps legacy harvestable JSON, potentially with job status overrides, to a HarvestJob POJO.
     */
    public HarvestJob fromHarvestableJson(JsonObject harvestable, JsonObject overrides) {
        String started = overrides.isEmpty() || overrides.getString(jsonPropertyName(STARTED)) == null ?
                harvestable.getString("lastHarvestStarted") : overrides.getString(jsonPropertyName(STARTED));
        String finished = overrides.isEmpty() ? harvestable.getString("lastHarvestFinished") : overrides.getString(jsonPropertyName(FINISHED));

        record = new Record(
                UUID.randomUUID(),
                harvestable.getLong("id"),
                harvestable.getString("name"),
                harvestable.getString("type"),
                harvestable.getString("url"),
                harvestable.getBoolean("allowErrors"),
                harvestable.getInteger("recordLimit"),
                harvestable.getInteger("storageBatchLimit"),
                harvestable.getJsonObject("transformation").getString("name"),
                harvestable.getJsonObject("storage").getString("name"),
                (overrides.isEmpty() ? harvestable.getString("currentStatus") : overrides.getString(jsonPropertyName(STATUS))),
                started,
                ((finished != null && started != null && started.compareTo(finished) < 0) ? finished : null),
                overrides.containsKey(jsonPropertyName(AMOUNT_HARVESTED)) ?
                        overrides.getInteger(jsonPropertyName(AMOUNT_HARVESTED)) : harvestable.getInteger("amountHarvested"),
                overrides.isEmpty() ?
                        harvestable.getString("message") : overrides.getString(jsonPropertyName(MESSAGE)));
        return this;
    }

    /**
     * Test purposes: For importing harvest job samples without running jobs to create them first
     *
     * @param harvestJobJson Could be harvest job JSON exported from another service
     * @return POJO
     */
    public HarvestJob fromJson(JsonObject harvestJobJson) {
        String started = harvestJobJson.getString(jsonPropertyName(STARTED));
        String finished = harvestJobJson.getString(jsonPropertyName(FINISHED));
        record = new Record(
                UUID.fromString(harvestJobJson.getString(jsonPropertyName(ID))),
                harvestJobJson.getLong(jsonPropertyName(HARVESTABLE_ID)),
                harvestJobJson.getString(jsonPropertyName(HARVESTABLE_NAME)),
                harvestJobJson.getString(jsonPropertyName(HARVESTABLE_TYPE)),
                harvestJobJson.getString(jsonPropertyName(URL)),
                harvestJobJson.getBoolean(jsonPropertyName(ALLOW_ERRORS)),
                harvestJobJson.getInteger(jsonPropertyName(RECORD_LIMIT)),
                harvestJobJson.getInteger(jsonPropertyName(BATCH_SIZE)),
                harvestJobJson.getString(jsonPropertyName(TRANSFORMATION)),
                harvestJobJson.getString(jsonPropertyName(STORAGE)),
                harvestJobJson.getString(jsonPropertyName(STATUS)),
                harvestJobJson.getString(jsonPropertyName(STARTED)),
                finished != null && started != null && started.compareTo(finished) < 0 ?
                        harvestJobJson.getString(jsonPropertyName(FINISHED)) : null,
                harvestJobJson.getInteger(jsonPropertyName(AMOUNT_HARVESTED)),
                harvestJobJson.getString(jsonPropertyName(MESSAGE)));
        return this;
    }

    @Override
    public JsonObject asJson() {
        JsonObject json = new JsonObject();
        json.put(jsonPropertyName(ID), record.id);
        json.put(jsonPropertyName(HARVESTABLE_ID), record.harvestableId);
        json.put(jsonPropertyName(HARVESTABLE_NAME), record.name);
        json.put(jsonPropertyName(HARVESTABLE_TYPE), record.type);
        json.put(jsonPropertyName(URL), record.url);
        json.put(jsonPropertyName(ALLOW_ERRORS), record.allowErrors);
        json.put(jsonPropertyName(RECORD_LIMIT), record.recordLimit);
        json.put(jsonPropertyName(BATCH_SIZE), record.batchSize);
        json.put(jsonPropertyName(TRANSFORMATION), record.transformation);
        json.put(jsonPropertyName(STORAGE), record.storage);
        json.put(jsonPropertyName(STATUS), record.status);
        json.put(jsonPropertyName(STARTED), record.started);
        json.put(jsonPropertyName(FINISHED), record.finished);
        json.put(jsonPropertyName(AMOUNT_HARVESTED), record.amountHarvested);
        json.put(jsonPropertyName(MESSAGE), record.message);
        return json;
    }

    /**
     * INSERT INTO statement.
     */
    public String makeInsertTemplate(String schema) {
        return "INSERT INTO " + schema + "." + Tables.harvest_job
                + " ("
                + dbColumnName(ID) + ", "
                + dbColumnName(HARVESTABLE_ID) + ", "
                + dbColumnName(HARVESTABLE_NAME) + ", "
                + dbColumnName(HARVESTABLE_TYPE) + ", "
                + dbColumnName(URL) + ", "
                + dbColumnName(ALLOW_ERRORS) + ", "
                + dbColumnName(RECORD_LIMIT) + ", "
                + dbColumnName(BATCH_SIZE) + ", "
                + dbColumnName(TRANSFORMATION) + ", "
                + dbColumnName(STORAGE) + ", "
                + dbColumnName(STATUS) + ", "
                + dbColumnName(STARTED) + ", "
                + dbColumnName(FINISHED) + ", "
                + dbColumnName(AMOUNT_HARVESTED) + ", "
                + dbColumnName(MESSAGE)
                + ")"
                + " VALUES ("
                + "#{" + dbColumnName(ID) + "}, "
                + "#{" + dbColumnName(HARVESTABLE_ID) + "}, "
                + "#{" + dbColumnName(HARVESTABLE_NAME) + "}, "
                + "#{" + dbColumnName(HARVESTABLE_TYPE) + "}, "
                + "#{" + dbColumnName(URL) + "}, "
                + "#{" + dbColumnName(ALLOW_ERRORS) + "}, "
                + "#{" + dbColumnName(RECORD_LIMIT) + "}, "
                + "#{" + dbColumnName(BATCH_SIZE) + "}, "
                + "#{" + dbColumnName(TRANSFORMATION) + "}, "
                + "#{" + dbColumnName(STORAGE) + "}, "
                + "#{" + dbColumnName(STATUS) + "}, "
                + "TO_TIMESTAMP(#{" + dbColumnName(STARTED) + "},'" + DATE_FORMAT + "'), "
                + "TO_TIMESTAMP(#{" + dbColumnName(FINISHED) + "}, '" + DATE_FORMAT + "'), "
                + "#{" + dbColumnName(AMOUNT_HARVESTED) + "}, "
                + "#{" + dbColumnName(MESSAGE) + "}"
                + ")";
    }

    @Override
    public PgCqlDefinition getQueryableFields() {
        PgCqlDefinition pgCqlDefinition = PgCqlDefinition.create();
        pgCqlDefinition.addField("cql.allRecords", new PgCqlFieldAlwaysMatches());
        for (Field field : fields().values()) {
            if (field.queryable()) {
                pgCqlDefinition.addField(field.jsonPropertyName(), field.pgColumn().pgCqlField());
            }
        }
        return pgCqlDefinition;
    }

    /**
     * Maps values of the POJO into table columns for insert-into statement.
     */
    public TupleMapper<Entity> getTupleMapper() {
        return TupleMapper.mapper(
                harvestJob -> {
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put(dbColumnName(ID), record.id);
                    parameters.put(dbColumnName(HARVESTABLE_ID), record.harvestableId);
                    parameters.put(dbColumnName(HARVESTABLE_NAME), record.name);
                    parameters.put(dbColumnName(HARVESTABLE_TYPE), record.type);
                    parameters.put(dbColumnName(URL), record.url());
                    parameters.put(dbColumnName(ALLOW_ERRORS), record.allowErrors);
                    if (record.recordLimit() != null) {
                        parameters.put(dbColumnName(RECORD_LIMIT), record.recordLimit);
                    }
                    if (record.batchSize() != null) {
                        parameters.put(dbColumnName(BATCH_SIZE), record.batchSize);
                    }
                    parameters.put(dbColumnName(TRANSFORMATION), record.transformation);
                    parameters.put(dbColumnName(STORAGE), record.storage);
                    parameters.put(dbColumnName(STATUS), record.status);
                    parameters.put(dbColumnName(STARTED), record.started);
                    if (record.finished != null) {
                        parameters.put(dbColumnName(FINISHED), record.finished);
                    }
                    if (record.amountHarvested != null) {
                        parameters.put(
                                dbColumnName(AMOUNT_HARVESTED), record.amountHarvested);
                    }
                    parameters.put(dbColumnName(MESSAGE), record.message);
                    return parameters;
                });
    }

    /**
     * Maps values from columns of a row to properties of the POJO.
     */
    public RowMapper<Entity> getRowMapper() {
        return row -> {
            record = new Record(row.getUUID(dbColumnName(ID)),
                    row.getLong(dbColumnName(HARVESTABLE_ID)),
                    row.getString(dbColumnName(HARVESTABLE_NAME)),
                    row.getString(dbColumnName(HARVESTABLE_TYPE)),
                    row.getString(dbColumnName(URL)),
                    row.getBoolean(dbColumnName(ALLOW_ERRORS)),
                    (row.getValue(dbColumnName(RECORD_LIMIT)) != null ? row.getInteger(dbColumnName(RECORD_LIMIT)) : null),
                    row.getInteger(dbColumnName(BATCH_SIZE)),
                    row.getString(dbColumnName(TRANSFORMATION)),
                    row.getString(dbColumnName(STORAGE)),
                    row.getString(dbColumnName(STATUS)),
                    row.getLocalDateTime(dbColumnName(STARTED)).toString(),
                    (row.getValue(dbColumnName(FINISHED)) != null ? row.getLocalDateTime(dbColumnName(FINISHED)).toString() : null),
                    (row.getValue(dbColumnName(AMOUNT_HARVESTED)) != null ? row.getInteger(dbColumnName(AMOUNT_HARVESTED)) : null),
                    row.getString(dbColumnName(MESSAGE)));
            return this;
        };
    }

}
