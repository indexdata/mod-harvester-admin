package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import org.folio.harvesteradmin.moduledata.database.Tables;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Logic for persisting in RECORD_FAILURE and retrieving from RECORD_FAILURE_VIEW.
 */
public class RecordFailure extends Entity {

    public RecordFailure() {}

    public RecordFailure(UUID id, UUID harvestJobId, String recordNumber, String timeStamp,
                         JsonArray recordErrors, String originalRecord, JsonObject transformedRecord,
                         Long harvestableId, String harvestableName) {
        record = new Record(
                id, harvestJobId, recordNumber, timeStamp, recordErrors, originalRecord, transformedRecord, harvestableId, harvestableName);
    }

    // Record failure record, the entity data.
    public Record record;

    public record Record(UUID id, UUID harvestJobId, String recordNumber, String timeStamp,
                         JsonArray recordErrors, String originalRecord, JsonObject transformedRecord,
                         Long harvestableId, String harvestableName) {
    }

    // Static map of Entity Fields.
    private static final Map<String, Field> FIELDS = new HashMap<>();
    // Field keys.
    public static String ID = "ID", HARVEST_JOB_ID = "HARVEST_JOB_ID", RECORD_NUMBER = "RECORD_NUMBER", TIME_STAMP = "TIME_STAMP",
            RECORD_ERRORS = "RECORD_ERRORS", ORIGINAL_RECORD = "ORIGINAL_RECORD", TRANSFORMED_RECORD = "TRANSFORMED_RECORD",
            HARVESTABLE_ID = "HARVESTABLE_ID", HARVESTABLE_NAME = "HARVESTABLE_NAME";

    static {
        FIELDS.put(ID, new Field("id", "id", PgColumn.Type.UUID, false, false, true));
        FIELDS.put(HARVEST_JOB_ID, new Field("harvestJobId", "harvest_job_id", PgColumn.Type.UUID, false, true));
        FIELDS.put(RECORD_NUMBER, new Field("recordNumber", "record_number", PgColumn.Type.TEXT, true, true));
        FIELDS.put(TIME_STAMP, new Field("timeStamp", "time_stamp", PgColumn.Type.TIMESTAMP, false, false));
        FIELDS.put(RECORD_ERRORS, new Field("recordErrors", "record_errors", PgColumn.Type.JSONB, true, false));
        FIELDS.put(ORIGINAL_RECORD, new Field("originalRecord", "original_record", PgColumn.Type.TEXT, false, false));
        FIELDS.put(TRANSFORMED_RECORD, new Field("transformedRecord", "transformed_record", PgColumn.Type.JSONB, true, true));
        FIELDS.put(HARVESTABLE_ID, new Field("harvestableId", "harvestable_id", PgColumn.Type.INTEGER, false, true));
        FIELDS.put(HARVESTABLE_NAME, new Field("harvestableName", "harvestable_name", PgColumn.Type.TEXT, true, true));
    }

    @Override
    public Map<String, Field> fields() {
        return FIELDS;
    }

    @Override
    public String jsonCollectionName() {
        return "failedRecords";
    }

    @Override
    public String entityName() {
        return "Record failure";
    }

    @Override
    public Entity fromJson(JsonObject json) {
        return null;
    }

    private static final String DATE_FORMAT = "YYYY-MM-DD HH24:MI:SS";

    static Map<String, String> months = Stream.of(new String[][]{
            {"Jan", "01"},
            {"Feb", "02"},
            {"Mar", "03"},
            {"Apr", "04"},
            {"May", "05"},
            {"Jun", "06"},
            {"Jul", "07"},
            {"Aug", "08"},
            {"Sep", "09"},
            {"Oct", "10"},
            {"Nov", "11"},
            {"Dec", "12"}}).collect(Collectors.toMap(month -> month[0], month -> month[1]));

    /**
     * Constructor.
     */
    public RecordFailure fromLegacyHarvesterJson(UUID harvestJobId, JsonObject json) {
        String[] legacyDate = json.getString("timeStamp").split(" ");

        record = new Record(
                UUID.randomUUID(),
                harvestJobId,
                json.getString("recordNumber").replace(".xml", ""),
                legacyDate[5] + "-" + months.get(legacyDate[1]) + "-" + legacyDate[2] + " " + legacyDate[3],
                json.getJsonArray("recordErrors"),
                json.getString("original"),
                json.getJsonObject("transformedRecord"),
                null,
                null);
        return this;
    }

    public RecordFailure fromHarvesterAdminJson(UUID harvestJobId, JsonObject json) {
        record = new Record(
                UUID.fromString(json.getString("id")),
                harvestJobId,
                json.getString("recordNumber"),
                json.getString("timeStamp").replace("T", " "),
                json.getJsonArray("recordErrors"),
                json.getString("originalRecord"),
                json.getJsonObject("transformedRecord"),
                null,
                null);
        return this;
    }

    /**
     * Maps rows from RECORD_FAILURE_VIEW to RecordFailure object.
     */
    @Override
    public RowMapper<Entity> getRowMapper() {
        return row -> new RecordFailure(
                row.getUUID(dbColumnName(ID)),
                row.getUUID(dbColumnName(HARVEST_JOB_ID)),
                row.getString(dbColumnName(RECORD_NUMBER)),
                row.getLocalDateTime(dbColumnName(TIME_STAMP)).toString(),
                row.getJsonArray(dbColumnName(RECORD_ERRORS)),
                row.getString(dbColumnName(ORIGINAL_RECORD)),
                row.getJsonObject(dbColumnName(TRANSFORMED_RECORD)),
                row.getLong(dbColumnName(HARVESTABLE_ID)),
                row.getString(dbColumnName(HARVESTABLE_NAME)));
    }

    @Override
    public String makeInsertTemplate(String schema) {
        return "INSERT INTO " + schema + "." + Tables.record_failure
                + " ("
                + dbColumnName(ID) + ", "
                + dbColumnName(HARVEST_JOB_ID) + ", "
                + dbColumnName(RECORD_NUMBER) + ", "
                + dbColumnName(TIME_STAMP) + ", "
                + dbColumnName(RECORD_ERRORS) + ", "
                + dbColumnName(ORIGINAL_RECORD) + ", "
                + dbColumnName(TRANSFORMED_RECORD)
                + ")"
                + " VALUES ("
                + "#{" + dbColumnName(ID) + "}, "
                + "#{" + dbColumnName(HARVEST_JOB_ID) + "}, "
                + "#{" + dbColumnName(RECORD_NUMBER) + "}, "
                + "TO_TIMESTAMP(#{" + dbColumnName(TIME_STAMP) + "},'" + DATE_FORMAT + "'), "
                + "#{" + dbColumnName(RECORD_ERRORS) + "}, "
                + "#{" + dbColumnName(ORIGINAL_RECORD) + "}, "
                + "#{" + dbColumnName(TRANSFORMED_RECORD) + "}"
                + ")";
    }

    @Override
    public TupleMapper<Entity> getTupleMapper() {
        return TupleMapper.mapper(
                entity -> {
                    Record rec = ((RecordFailure) entity).record;
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put(dbColumnName(ID), rec.id);
                    parameters.put(dbColumnName(HARVEST_JOB_ID), rec.harvestJobId);
                    parameters.put(dbColumnName(RECORD_NUMBER), rec.recordNumber);
                    parameters.put(dbColumnName(TIME_STAMP), rec.timeStamp);
                    parameters.put(dbColumnName(RECORD_ERRORS), rec.recordErrors);
                    parameters.put(dbColumnName(ORIGINAL_RECORD), rec.originalRecord);
                    parameters.put(dbColumnName(TRANSFORMED_RECORD), rec.transformedRecord);
                    return parameters;
                });
    }

    @Override
    public JsonObject asJson() {
        JsonObject json = new JsonObject();
        json.put(jsonPropertyName(ID), record.id);
        json.put(jsonPropertyName(HARVEST_JOB_ID), record.harvestJobId);
        json.put(jsonPropertyName(HARVESTABLE_ID), record.harvestableId);
        json.put(jsonPropertyName(HARVESTABLE_NAME), record.harvestableName);
        json.put(jsonPropertyName(RECORD_NUMBER), record.recordNumber);
        json.put(jsonPropertyName(TIME_STAMP), record.timeStamp);
        json.put(jsonPropertyName(RECORD_ERRORS), record.recordErrors);
        json.put(jsonPropertyName(ORIGINAL_RECORD), record.originalRecord);
        json.put(jsonPropertyName(TRANSFORMED_RECORD), record.transformedRecord);
        return json;
    }

    @Override
    public Tables table() {
        return Tables.record_failure;
    }

    @Override
    public String makeCreateTableSql(String schema) {
        return "CREATE TABLE IF NOT EXISTS " + schema + "." + table()
                + "("
                + dbColumnName(ID) + " UUID PRIMARY KEY, "
                + dbColumnName(HARVEST_JOB_ID) + " UUID NOT NULL "
                + " REFERENCES " + schema + "." + Tables.harvest_job + " (" + new HarvestJob().field(HarvestJob.ID).columnName() + "), "
                + dbColumnName(RECORD_NUMBER) + " TEXT, "
                + dbColumnName(TIME_STAMP) + " TIMESTAMP, "
                + dbColumnName(RECORD_ERRORS) + " JSONB NOT NULL, "
                + dbColumnName(ORIGINAL_RECORD) + " TEXT NOT NULL, "
                + dbColumnName(TRANSFORMED_RECORD) + " JSONB NOT NULL"
                + ")";
    }

}

