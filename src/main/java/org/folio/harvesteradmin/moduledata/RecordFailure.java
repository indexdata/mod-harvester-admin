package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.folio.harvesteradmin.modulestorage.Storage;
import org.folio.tlib.postgres.PgCqlDefinition;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldAlwaysMatches;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldNumber;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldText;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldTimestamp;


/** Logic for persisting in RECORD_FAILURE and retrieving from RECORD_FAILURE_VIEW.
 *
 */
public class RecordFailure extends StoredEntity {

  private UUID id;
  private Long harvestableId;
  private String harvestableName;
  private UUID harvestJobId;
  private String recordNumber;
  private String timeStamp;
  private String originalRecord;
  private JsonArray recordErrors;
  private JsonObject transformedRecord;

  public enum Column {
    id,
    harvest_job_id,
    record_number,
    time_stamp,
    record_errors,
    original_record,
    transformed_record,
    harvestable_id,  // view column
    harvestable_name // view column

  }

  private static final String DATE_FORMAT = "YYYY-MM-DD HH24:MI:SS";

  static Map<String,String> months = Stream.of(new String[][] {
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

  public static RecordFailure entity() {
    return new RecordFailure();
  }

  /**
   * Constructor.
   */
  public static RecordFailure fromLegacyHarvesterJson(UUID harvestJobId, JsonObject json) {
    RecordFailure recordFailure = new RecordFailure();
    recordFailure.id = UUID.randomUUID();
    recordFailure.harvestJobId = harvestJobId;
    recordFailure.recordNumber = json.getString("recordNumber").replace(".xml", "");
    String[] legacyDate = json.getString("timeStamp").split(" ");
    recordFailure.timeStamp =
        legacyDate[5] + "-" + months.get(legacyDate[1]) + "-" + legacyDate[2] + " " + legacyDate[3];
    recordFailure.originalRecord = json.getString("original");
    recordFailure.transformedRecord = json.getJsonObject("transformedRecord");
    recordFailure.recordErrors = json.getJsonArray("recordErrors");
    return recordFailure;
  }

  public static RecordFailure fromHarvesterAdminJson(UUID harvestJobId, JsonObject json) {
    RecordFailure recordFailure = new RecordFailure();
    recordFailure.id = UUID.fromString(json.getString("id"));
    recordFailure.harvestJobId = harvestJobId;
    recordFailure.recordNumber = json.getString("recordNumber");
    recordFailure.timeStamp = json.getString("timeStamp").replace("T", " ");
    recordFailure.originalRecord = json.getString("originalRecord");
    recordFailure.transformedRecord = json.getJsonObject("transformedRecord");
    recordFailure.recordErrors = json.getJsonArray("recordErrors");
    return recordFailure;

  }

  /**
   * CREATE TABLE statement.
   */
  public String makeCreateTableSql(String schema) {
    return "CREATE TABLE IF NOT EXISTS " + schema + "." + Storage.Table.record_failure
        + "("
        + Column.id + " UUID PRIMARY KEY, "
        + Column.harvest_job_id + " UUID NOT NULL REFERENCES "
        + schema + "." + Storage.Table.harvest_job + "(" + HarvestJobField.ID.columnName() + "), "
        + Column.record_number + " TEXT, "
        + Column.time_stamp + " TIMESTAMP, "
        + Column.record_errors + " JSONB NOT NULL, "
        + Column.original_record + " TEXT NOT NULL, "
        + Column.transformed_record + " JSONB NOT NULL"
        + ")";
  }

  /**
   * Maps rows from RECORD_FAILURE_VIEW to RecordFailure object.
   */
  public RowMapper<StoredEntity> getRowMapper() {
    return row -> {
      RecordFailure recordFailure = new RecordFailure();
      recordFailure.id = row.getUUID(Column.id.name());
      recordFailure.harvestableId = row.getLong(Column.harvestable_id.name());
      recordFailure.harvestableName = row.getString(Column.harvestable_name.name());
      recordFailure.harvestJobId = row.getUUID(Column.harvest_job_id.name());
      recordFailure.recordNumber = row.getString(Column.record_number.name());
      recordFailure.timeStamp = row.getLocalDateTime(Column.time_stamp.name()).toString();
      recordFailure.recordErrors = row.getJsonArray(Column.record_errors.name());
      recordFailure.originalRecord = row.getString(Column.original_record.name());
      recordFailure.transformedRecord = row.getJsonObject(Column.transformed_record.name());
      return recordFailure;
    };

  }

  @Override
  public String makeInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Storage.Table.record_failure
        + " ("
        + Column.id + ", "
        + Column.harvest_job_id + ", "
        + Column.record_number + ", "
        + Column.time_stamp + ", "
        + Column.record_errors + ", "
        + Column.original_record + ", "
        + Column.transformed_record
        + ")"
        + " VALUES ("
        + "#{" + Column.id + "}, "
        + "#{" + Column.harvest_job_id + "}, "
        + "#{" + Column.record_number + "}, "
        + "TO_TIMESTAMP(#{" + Column.time_stamp + "},'" + DATE_FORMAT + "'), "
        + "#{" + Column.record_errors + "}, "
        + "#{" + Column.original_record + "}, "
        + "#{" + Column.transformed_record + "}"
        + ")";
  }

  @Override
  public TupleMapper<StoredEntity> getTupleMapper() {
    return TupleMapper.mapper(
        recordFailure -> {
          RecordFailure entity = (RecordFailure) recordFailure;
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(Column.id.name(), entity.id);
          parameters.put(Column.harvest_job_id.name(), entity.harvestJobId);
          parameters.put(Column.record_number.name(), entity.recordNumber);
          parameters.put(Column.time_stamp.name(), entity.timeStamp);
          parameters.put(Column.original_record.name(), entity.originalRecord);
          parameters.put(Column.transformed_record.name(), entity.transformedRecord);
          parameters.put(Column.record_errors.name(), entity.recordErrors);
          return parameters;
        });
  }

  @Override
  public PgCqlDefinition getQueryableFields() {
    PgCqlDefinition pgCqlDefinition = PgCqlDefinition.create();
    pgCqlDefinition.addField("cql.allRecords", new PgCqlFieldAlwaysMatches());
    pgCqlDefinition.addField("recordNumber", new PgCqlFieldText().withExact().withLikeOps());
    pgCqlDefinition.addField("harvestableId", new PgCqlFieldNumber());
    pgCqlDefinition.addField(
        "harvestableName", new PgCqlFieldText().withExact().withLikeOps().withFullText());
    pgCqlDefinition.addField("timeStamp", new PgCqlFieldTimestamp());
    return pgCqlDefinition;
  }

  @Override
  public Map<String, PgColumn> getFieldMap() {
    Map<String, PgColumn> map = new HashMap<>();
    map.put("recordNumber", new PgColumn("record_number", PgColumn.Type.TEXT, false, false));
    map.put("timeStamp", new PgColumn("time_stamp", PgColumn.Type.TIMESTAMP, false, false));
    map.put("harvestableId", new PgColumn("harvestable_id", PgColumn.Type.BIGINT, true, false));
    map.put("harvestableName", new PgColumn("harvestable_name", PgColumn.Type.TEXT, true, false));
    return map;
  }

  /**
   * Gets JSON representation.
   */
  public JsonObject asJson() {
    JsonObject json = new JsonObject();
    json.put("id", id);
    json.put("harvestJobId", harvestJobId);
    json.put("harvestableId", harvestableId);
    json.put("harvestableName", harvestableName);
    json.put("recordNumber", recordNumber);
    json.put("timeStamp", timeStamp);
    json.put("recordErrors", recordErrors);
    json.put("originalRecord", originalRecord);
    json.put("transformedRecord", transformedRecord);
    return json;
  }

}

