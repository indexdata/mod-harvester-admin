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
import org.folio.harvesteradmin.modulestorage.Storage;
import org.folio.tlib.postgres.PgCqlDefinition;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldAlwaysMatches;


public class HarvestJob extends StoredEntity {

  private final JsonObject json = new JsonObject();
  private static final String DATE_FORMAT = "YYYY-MM-DD''T''HH24:MI:SS";

  public HarvestJob() {
    super();
  }

  public static HarvestJob entity() {
    return new HarvestJob();
  }

  public enum Field {
    ID("id", "id", PgColumn.Type.UUID, false, true, true),
    HARVESTABLE_ID("harvestableId","harvestable_id", PgColumn.Type.INTEGER, false, true),
    HARVESTABLE_NAME("name","harvestable_name", PgColumn.Type.TEXT, false, true),
    HARVESTABLE_TYPE("type","type", PgColumn.Type.TEXT, false, true),
    URL("url", "url", PgColumn.Type.TEXT, false, false),
    ALLOW_ERRORS("allowErrors", "allow_errors", PgColumn.Type.BOOLEAN, false, false),
    RECORD_LIMIT("recordLimit", "record_limit", PgColumn.Type.INTEGER, true, false),
    BATCH_SIZE("batchSize", "batch_size", PgColumn.Type.INTEGER, true, false),
    TRANSFORMATION("transformation", "transformation", PgColumn.Type.TEXT, false, false),
    STORAGE("storage", "storage", PgColumn.Type.TEXT, false, false),
    STATUS("status", "status", PgColumn.Type.TEXT, false, true),
    STARTED("started", "started", PgColumn.Type.TIMESTAMP, false, false),
    FINISHED("finished", "finished", PgColumn.Type.TIMESTAMP, true, false),
    AMOUNT_HARVESTED("amountHarvested", "amount_harvested", PgColumn.Type.INTEGER, true, false),
    MESSAGE("message", "message", PgColumn.Type.TEXT, true, false);

    public final JsonProperty property;
    public final PgColumn column;
    public final Boolean queryable;

    Field(JsonProperty property, PgColumn column, Boolean queryable) {
      this.column = column;
      this.property = property;
      this.queryable = queryable;
    }

    Field(String jsonPropertyName,
          String columnName, PgColumn.Type type, Boolean nullable, Boolean queryable) {
      this(jsonPropertyName, columnName, type, nullable, queryable, false);
    }

    Field(String jsonPropertyName,
          String columnName, PgColumn.Type type, Boolean nullable, Boolean queryable,
          Boolean primaryKey) {
      this.property = new JsonProperty(jsonPropertyName);
      this.queryable = queryable;
      this.column = getColumnDefinition(columnName, type, nullable, queryable, primaryKey);
    }
  }

  /**
   * Gets map of JSON property names to PG column definitions, built from the Field enum.
   */
  public Map<String,PgColumn> getFieldMap() {
    return Arrays.stream(
        HarvestJob.Field.values()).collect(Collectors.toMap(f -> f.property.name, f -> f.column));
  }

  /**
   * Maps legacy harvestable JSON to HarvestJob Java Object.
   */
  public static HarvestJob fromHarvestableJson(JsonObject harvestableJson) {
    HarvestJob harvestJob = new HarvestJob();
    harvestJob.setId(UUID.randomUUID());
    harvestJob.setHarvestableId(harvestableJson.getString("id"));
    harvestJob.setName(harvestableJson.getString("name"));
    harvestJob.setType(harvestableJson.getString("type"));
    harvestJob.setUrl(harvestableJson.getString("url"));
    harvestJob.setAllowErrors(harvestableJson.getString("allowErrors"));
    harvestJob.setRecordLimit(harvestableJson.getString("recordLimit"));
    harvestJob.setBatchSize(harvestableJson.getString("storageBatchLimit"));
    harvestJob.setTransformation(
        harvestableJson.getJsonObject("transformation").getString("name"));
    harvestJob.setStorage(
        harvestableJson.getJsonObject("storage").getString("name"));
    harvestJob.setStatus(harvestableJson.getString("currentStatus"));
    harvestJob.setStartedAndFinished(
        harvestableJson.getString("lastHarvestStarted"),
        harvestableJson.getString("lastHarvestFinished"));
    harvestJob.setAmountHarvested(harvestableJson.getString("amountHarvested"));
    harvestJob.setMessage(harvestableJson.getString("message"));
    return harvestJob;
  }

  /**
   * HarvestJob to JSON mapping.
   */
  public JsonObject asJson() {
    return json;
  }

  /**
   * CREATE TABLE statement.
   */
  public String getCreateTableSql(String schema) {
    StringBuilder columnsDdl = new StringBuilder();
    Stream.of(Field.values())
        .forEach(field -> columnsDdl.append(field.column.getColumnDdl()).append(","));
    columnsDdl.deleteCharAt(columnsDdl.length() - 1); // remove ending comma

    return "CREATE TABLE IF NOT EXISTS " + schema + "." + Storage.Table.harvest_job
        + "("
        + columnsDdl
        + ")";
  }


  /**
   * INSERT INTO statement.
   */
  public String getInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Storage.Table.harvest_job
        + " ("
        + Field.ID + ", "
        + Field.HARVESTABLE_ID.column.name + ", "
        + Field.HARVESTABLE_NAME.column.name + ", "
        + Field.HARVESTABLE_TYPE.column.name + ", "
        + Field.URL.column.name + ", "
        + Field.ALLOW_ERRORS.column.name + ", "
        + Field.RECORD_LIMIT.column.name + ", "
        + Field.BATCH_SIZE.column.name + ", "
        + Field.TRANSFORMATION.column.name + ", "
        + Field.STORAGE.column.name + ", "
        + Field.STATUS.column.name + ", "
        + Field.STARTED.column.name + ", "
        + Field.FINISHED.column.name + ", "
        + Field.AMOUNT_HARVESTED.column.name + ", "
        + Field.MESSAGE.column.name
        + ")"
        + " VALUES ("
        + "#{" + Field.ID.column.name + "}, "
        + "#{" + Field.HARVESTABLE_ID.column.name + "}, "
        + "#{" + Field.HARVESTABLE_NAME.column.name + "}, "
        + "#{" + Field.HARVESTABLE_TYPE.column.name + "}, "
        + "#{" + Field.URL.column.name + "}, "
        + "#{" + Field.ALLOW_ERRORS.column.name + "}, "
        + "#{" + Field.RECORD_LIMIT.column.name + "}, "
        + "#{" + Field.BATCH_SIZE.column.name + "}, "
        + "#{" + Field.TRANSFORMATION.column.name + "}, "
        + "#{" + Field.STORAGE.column.name + "}, "
        + "#{" + Field.STATUS.column.name + "}, "
        + "TO_TIMESTAMP(#{" + Field.STARTED.column.name + "},'" + DATE_FORMAT + "'), "
        + "TO_TIMESTAMP(#{" + Field.FINISHED.column.name + "}, '" + DATE_FORMAT + "'), "
        + "#{" + Field.AMOUNT_HARVESTED.column.name + "}, "
        + "#{" + Field.MESSAGE.column.name + "}"
        + ")";
  }

  @Override
  public PgCqlDefinition getQueryableFields() {
    PgCqlDefinition pgCqlDefinition = PgCqlDefinition.create();
    pgCqlDefinition.addField("cql.allRecords", new PgCqlFieldAlwaysMatches());
    for (Field field : Field.values()) {
      if (field.queryable) {
        pgCqlDefinition.addField(field.property.name, field.column.pgCqlFieldObject);
      }
    }
    return pgCqlDefinition;
  }

  /**
   * Table input mapping.
   */
  public TupleMapper<StoredEntity> getInsertValuesMapper() {
    return TupleMapper.mapper(
        harvestJob -> {
          HarvestJob entity = (HarvestJob) harvestJob;
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(Field.ID.column.name, entity.getId());
          parameters.put(Field.HARVESTABLE_ID.column.name, entity.getHarvestableId());
          parameters.put(Field.HARVESTABLE_NAME.column.name, entity.getName());
          parameters.put(Field.HARVESTABLE_TYPE.column.name, entity.getType());
          parameters.put(Field.URL.column.name, entity.getUrl());
          parameters.put(Field.ALLOW_ERRORS.column.name, entity.getAllowErrors());
          if (entity.getRecordLimit() != null) {
            parameters.put(Field.RECORD_LIMIT.column.name, entity.getRecordLimit());
          }
          if (entity.getBatchSize() != null) {
            parameters.put(Field.BATCH_SIZE.column.name, entity.getBatchSize());
          }
          parameters.put(Field.TRANSFORMATION.column.name, entity.getTransformation());
          parameters.put(Field.STORAGE.column.name, entity.getStorage());
          parameters.put(Field.STATUS.column.name, entity.getStatus());
          parameters.put(Field.STARTED.column.name, entity.getStarted());
          parameters.put(Field.FINISHED.column.name, entity.getFinished());
          if (entity.getAmountHarvested() != null) {
            parameters.put(Field.AMOUNT_HARVESTED.column.name, entity.getAmountHarvested());
          }
          parameters.put(Field.MESSAGE.column.name, entity.getMessage());
          return parameters;
        });
  }

  /**
   * Maps values from columns of rows to properties of JSON objects.
   */
  public RowMapper<StoredEntity> getSelectListMapper() {
    return row -> {
      HarvestJob harvestJob = new HarvestJob();
      JsonObject j = new JsonObject();
      j.put(Field.ID.property.name, row.getUUID(Field.ID.column.name));
      j.put(
          Field.HARVESTABLE_NAME.property.name, row.getString(Field.HARVESTABLE_NAME.column.name));
      harvestJob.setId(row.getUUID(Field.ID.column.name));
      harvestJob.setName(row.getString(Field.HARVESTABLE_NAME.column.name));
      harvestJob.setHarvestableId(row.getInteger(Field.HARVESTABLE_ID.column.name));
      harvestJob.setType(row.getString(Field.HARVESTABLE_TYPE.column.name));
      harvestJob.setUrl(row.getString(Field.URL.column.name));
      harvestJob.setAllowErrors(row.getBoolean(Field.ALLOW_ERRORS.column.name));
      if (row.getValue(Field.RECORD_LIMIT.column.name) != null) {
        harvestJob.setRecordLimit(row.getInteger(Field.RECORD_LIMIT.column.name));
      }
      harvestJob.setTransformation(row.getString(Field.TRANSFORMATION.column.name));
      harvestJob.setStorage(row.getString(Field.STORAGE.column.name));
      harvestJob.setStatus(row.getString(Field.STATUS.column.name));
      harvestJob.setStarted(row.getLocalDateTime(Field.STARTED.column.name));
      if (row.getValue(Field.FINISHED.column.name) != null) {
        harvestJob.setFinished(row.getLocalDateTime(Field.FINISHED.column.name));
      }
      if (row.getValue(Field.AMOUNT_HARVESTED.column.name) != null) {
        harvestJob.setAmountHarvested(row.getInteger(Field.AMOUNT_HARVESTED.column.name));
      }
      harvestJob.setMessage(row.getString(Field.MESSAGE.column.name));
      return harvestJob;
    };
  }

  public UUID getId() {
    return UUID.fromString(json.getString(Field.ID.property.name));
  }

  public void setId(UUID id) {
    json.put(Field.ID.property.name, id);
  }

  public int getHarvestableId() {
    return json.getInteger(Field.HARVESTABLE_ID.property.name);
  }

  public void setHarvestableId(int harvestableId) {
    json.put(Field.HARVESTABLE_ID.property.name, harvestableId);
  }

  public void setHarvestableId(String harvestableId) {
    setHarvestableId(Integer.parseInt(harvestableId));
  }

  public String getName() {
    return json.getString(Field.HARVESTABLE_NAME.property.name);
  }

  public void setName(String name) {
    json.put(Field.HARVESTABLE_NAME.property.name, name);
  }

  public String getType() {
    return json.getString(Field.HARVESTABLE_TYPE.property.name);
  }

  public void setType(String type) {
    json.put(Field.HARVESTABLE_TYPE.property.name, type);
  }

  public String getUrl() {
    return json.getString(Field.URL.property.name);
  }

  public void setUrl(String url) {
    json.put(Field.URL.property.name, url);
  }

  public Boolean getAllowErrors() {
    return json.getBoolean(Field.ALLOW_ERRORS.property.name);
  }

  public void setAllowErrors(Boolean allowErrors) {
    json.put(Field.ALLOW_ERRORS.property.name, allowErrors);
  }

  public void setAllowErrors(String allowErrors) {
    setAllowErrors(Boolean.valueOf(allowErrors));
  }

  public Integer getRecordLimit() {
    return json.getInteger(Field.RECORD_LIMIT.property.name);
  }

  public void setRecordLimit(Integer recordLimit) {
    json.put(Field.RECORD_LIMIT.property.name, recordLimit);
  }

  /**
   * Sets record limit.
   */
  public void setRecordLimit(String recordLimit) {
    if (recordLimit != null) {
      setRecordLimit(Integer.parseInt(recordLimit));
    }
  }

  public Integer getBatchSize() {
    return json.getInteger(Field.BATCH_SIZE.property.name);
  }

  /**
   * Sets batch size.
   */
  public void setBatchSize(String batchSize) {
    if (batchSize != null) {
      json.put(Field.BATCH_SIZE.property.name, Integer.parseInt(batchSize));
    }
  }

  public String getTransformation() {
    return json.getString(Field.TRANSFORMATION.property.name);
  }

  public void setTransformation(String transformation) {
    json.put(Field.TRANSFORMATION.property.name, transformation);
  }

  public String getStorage() {
    return json.getString(Field.STORAGE.property.name);
  }

  public void setStorage(String storage) {
    json.put(Field.STORAGE.property.name, storage);
  }

  public String getStatus() {
    return json.getString(Field.STATUS.property.name);
  }

  public void setStatus(String status) {
    json.put(Field.STATUS.property.name, status);
  }

  public String getStarted() {
    return json.getString(Field.STARTED.property.name);
  }

  public String getFinished() {
    return json.getString(Field.FINISHED.property.name);
  }

  public void setStarted(LocalDateTime started) {
    json.put(Field.STARTED.property.name, started.toString());
  }

  public void setFinished(LocalDateTime finished) {
    json.put(Field.FINISHED.property.name, finished.toString());
  }

  /**
   * Sets start and finish dates.
   */
  public void setStartedAndFinished(String started, String finished) {
    if (started != null && finished != null) {
      json.put(Field.STARTED.property.name, started);
      if (started.compareTo(finished) < 0) { // or else it's the finish time of an earlier job
        json.put(Field.FINISHED.property.name, finished);
      }
    }
  }

  public Integer getAmountHarvested() {
    return json.getInteger(Field.AMOUNT_HARVESTED.property.name);
  }

  public void setAmountHarvested(Integer amountHarvested) {
    json.put(Field.AMOUNT_HARVESTED.property.name, amountHarvested);
  }

  /**
   * Sets amount harvested.
   */
  public void setAmountHarvested(String amountHarvested) {
    if (amountHarvested != null) {
      setAmountHarvested(Integer.parseInt(amountHarvested));
    }
  }

  public String getMessage() {
    return json.getString(Field.MESSAGE.property.name);
  }

  public void setMessage(String message) {
    json.put(Field.MESSAGE.property.name, message);
  }


}
