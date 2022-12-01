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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.modulestorage.Storage;
import org.folio.tlib.postgres.PgCqlDefinition;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldAlwaysMatches;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldBase;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldNumber;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldText;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldUuid;


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
    ID("id", "id", new PgCqlFieldUuid()),
    HARVESTABLE_ID("harvestableId", "harvestable_id", new PgCqlFieldNumber()),
    HARVESTABLE_NAME("name", "harvestable_name", new PgCqlFieldText()),
    HARVESTABLE_TYPE("type", "type", new PgCqlFieldText()),
    URL("url", "url"),
    ALLOW_ERRORS("allowErrors", "allow_errors"),
    RECORD_LIMIT("recordLimit", "record_limit"),
    BATCH_SIZE("batchSize", "batch_size"),
    TRANSFORMATION("transformation", "transformation"),
    STORAGE("storage", "storage"),
    STATUS("status", "status", new PgCqlFieldText()),
    STARTED("started", "started"),
    FINISHED("finished", "finished"),
    AMOUNT_HARVESTED("amountHarvested", "amount_harvested"),
    MESSAGE("message", "message");

    public final String column;
    public final String property;
    public final PgCqlFieldBase pgCqlFieldDefinition;

    Field(String jsonPropertyName, String tableColumnName) {
      this.column = tableColumnName;
      this.property = jsonPropertyName;
      this.pgCqlFieldDefinition = null;
    }

    Field(String jsonPropertyName, String tableColumnName, PgCqlFieldBase pgCqlField) {
      this.column = tableColumnName;
      this.property = jsonPropertyName;
      this.pgCqlFieldDefinition = pgCqlField;
    }
  }

  public Map<String,String> getPropertyColumnMap() {
    return Arrays.stream(
        HarvestJob.Field.values()).collect(Collectors.toMap(f -> f.property, f -> f.column));
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
    return "CREATE TABLE IF NOT EXISTS " + schema + "." + Storage.Table.harvest_job
        + "("
        + Field.ID.column + " UUID PRIMARY KEY, "
        + Field.HARVESTABLE_ID.column + " INTEGER NOT NULL, "
        + Field.HARVESTABLE_NAME.column + " TEXT NOT NULL, "
        + Field.HARVESTABLE_TYPE.column + " TEXT NOT NULL, "
        + Field.URL.column + " TEXT NOT NULL, "
        + Field.ALLOW_ERRORS.column + " BOOLEAN NOT NULL, "
        + Field.RECORD_LIMIT.column + " INTEGER, "
        + Field.BATCH_SIZE.column + " INTEGER, "
        + Field.TRANSFORMATION.column + " TEXT NOT NULL, "
        + Field.STORAGE.column + " TEXT NOT NULL, "
        + Field.STATUS.column + " TEXT NOT NULL, "
        + Field.STARTED.column + " TIMESTAMP NOT NULL, "
        + Field.FINISHED.column + " TIMESTAMP, "
        + Field.AMOUNT_HARVESTED.column + " INTEGER, "
        + Field.MESSAGE.column + " TEXT"
        + ")";
  }

  /**
   * INSERT INTO statement.
   */
  public String getInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Storage.Table.harvest_job
        + " ("
        + Field.ID + ", "
        + Field.HARVESTABLE_ID.column + ", "
        + Field.HARVESTABLE_NAME.column + ", "
        + Field.HARVESTABLE_TYPE.column + ", "
        + Field.URL.column + ", "
        + Field.ALLOW_ERRORS.column + ", "
        + Field.RECORD_LIMIT.column + ", "
        + Field.BATCH_SIZE.column + ", "
        + Field.TRANSFORMATION.column + ", "
        + Field.STORAGE.column + ", "
        + Field.STATUS.column + ", "
        + Field.STARTED.column + ", "
        + Field.FINISHED.column + ", "
        + Field.AMOUNT_HARVESTED.column + ", "
        + Field.MESSAGE.column
        + ")"
        + " VALUES ("
        + "#{" + Field.ID.column + "}, "
        + "#{" + Field.HARVESTABLE_ID.column + "}, "
        + "#{" + Field.HARVESTABLE_NAME.column + "}, "
        + "#{" + Field.HARVESTABLE_TYPE.column + "}, "
        + "#{" + Field.URL.column + "}, "
        + "#{" + Field.ALLOW_ERRORS.column + "}, "
        + "#{" + Field.RECORD_LIMIT.column + "}, "
        + "#{" + Field.BATCH_SIZE.column + "}, "
        + "#{" + Field.TRANSFORMATION.column + "}, "
        + "#{" + Field.STORAGE.column + "}, "
        + "#{" + Field.STATUS.column + "}, "
        + "TO_TIMESTAMP(#{" + Field.STARTED.column + "},'" + DATE_FORMAT + "'), "
        + "TO_TIMESTAMP(#{" + Field.FINISHED.column + "}, '" + DATE_FORMAT + "'), "
        + "#{" + Field.AMOUNT_HARVESTED.column + "}, "
        + "#{" + Field.MESSAGE.column + "}"
        + ")";
  }

  @Override
  public PgCqlDefinition getQueryableFields() {
    PgCqlDefinition pgCqlDefinition = PgCqlDefinition.create();
    pgCqlDefinition.addField("cql.allRecords", new PgCqlFieldAlwaysMatches());
    for (Field field : Field.values()) {
      if (field.pgCqlFieldDefinition != null) {
        pgCqlDefinition.addField(field.property, field.pgCqlFieldDefinition);
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
          parameters.put(Field.ID.column, entity.getId());
          parameters.put(Field.HARVESTABLE_ID.column, entity.getHarvestableId());
          parameters.put(Field.HARVESTABLE_NAME.column, entity.getName());
          parameters.put(Field.HARVESTABLE_TYPE.column, entity.getType());
          parameters.put(Field.URL.column, entity.getUrl());
          parameters.put(Field.ALLOW_ERRORS.column, entity.getAllowErrors());
          if (entity.getRecordLimit() != null) {
            parameters.put(Field.RECORD_LIMIT.column, entity.getRecordLimit());
          }
          if (entity.getBatchSize() != null) {
            parameters.put(Field.BATCH_SIZE.column, entity.getBatchSize());
          }
          parameters.put(Field.TRANSFORMATION.column, entity.getTransformation());
          parameters.put(Field.STORAGE.column, entity.getStorage());
          parameters.put(Field.STATUS.column, entity.getStatus());
          parameters.put(Field.STARTED.column, entity.getStarted());
          parameters.put(Field.FINISHED.column, entity.getFinished());
          if (entity.getAmountHarvested() != null) {
            parameters.put(Field.AMOUNT_HARVESTED.column, entity.getAmountHarvested());
          }
          parameters.put(Field.MESSAGE.column, entity.getMessage());
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
      j.put(Field.ID.property, row.getUUID(Field.ID.column));
      harvestJob.setId(row.getUUID(Field.ID.column));
      j.put(Field.HARVESTABLE_NAME.property, row.getString(Field.HARVESTABLE_NAME.column));
      harvestJob.setName(row.getString(Field.HARVESTABLE_NAME.column));
      harvestJob.setHarvestableId(row.getInteger(Field.HARVESTABLE_ID.column));
      harvestJob.setType(row.getString(Field.HARVESTABLE_TYPE.column));
      harvestJob.setUrl(row.getString(Field.URL.column));
      harvestJob.setAllowErrors(row.getBoolean(Field.ALLOW_ERRORS.column));
      if (row.getValue(Field.RECORD_LIMIT.column) != null) {
        harvestJob.setRecordLimit(row.getInteger(Field.RECORD_LIMIT.column));
      }
      harvestJob.setTransformation(row.getString(Field.TRANSFORMATION.column));
      harvestJob.setStorage(row.getString(Field.STORAGE.column));
      harvestJob.setStatus(row.getString(Field.STATUS.column));
      harvestJob.setStarted(row.getLocalDateTime(Field.STARTED.column));
      if (row.getValue(Field.FINISHED.column) != null) {
        harvestJob.setFinished(row.getLocalDateTime(Field.FINISHED.column));
      }
      if (row.getValue(Field.AMOUNT_HARVESTED.column) != null) {
        harvestJob.setAmountHarvested(row.getInteger(Field.AMOUNT_HARVESTED.column));
      }
      harvestJob.setMessage(row.getString(Field.MESSAGE.column));
      return harvestJob;
    };
  }

  public UUID getId() {
    return UUID.fromString(json.getString(Field.ID.name()));
  }

  public void setId(UUID id) {
    json.put(Field.ID.name(), id);
  }

  public int getHarvestableId() {
    return json.getInteger(Field.HARVESTABLE_ID.property);
  }

  public void setHarvestableId(int harvestableId) {
    json.put(Field.HARVESTABLE_ID.property, harvestableId);
  }

  public void setHarvestableId(String harvestableId) {
    setHarvestableId(Integer.parseInt(harvestableId));
  }

  public String getName() {
    return json.getString(Field.HARVESTABLE_NAME.property);
  }

  public void setName(String name) {
    json.put(Field.HARVESTABLE_NAME.property, name);
  }

  public String getType() {
    return json.getString(Field.HARVESTABLE_TYPE.property);
  }

  public void setType(String type) {
    json.put(Field.HARVESTABLE_TYPE.property, type);
  }

  public String getUrl() {
    return json.getString(Field.URL.property);
  }

  public void setUrl(String url) {
    json.put(Field.URL.property, url);
  }

  public Boolean getAllowErrors() {
    return json.getBoolean(Field.ALLOW_ERRORS.property);
  }

  public void setAllowErrors(Boolean allowErrors) {
    json.put(Field.ALLOW_ERRORS.property, allowErrors);
  }

  public void setAllowErrors(String allowErrors) {
    setAllowErrors(Boolean.valueOf(allowErrors));
  }

  public Integer getRecordLimit() {
    return json.getInteger(Field.RECORD_LIMIT.property);
  }

  public void setRecordLimit(Integer recordLimit) {
    json.put(Field.RECORD_LIMIT.property, recordLimit);
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
    return json.getInteger(Field.BATCH_SIZE.property);
  }

  /**
   * Sets batch size.
   */
  public void setBatchSize(String batchSize) {
    if (batchSize != null) {
      json.put(Field.BATCH_SIZE.property, Integer.parseInt(batchSize));
    }
  }

  public String getTransformation() {
    return json.getString(Field.TRANSFORMATION.property);
  }

  public void setTransformation(String transformation) {
    json.put(Field.TRANSFORMATION.property, transformation);
  }

  public String getStorage() {
    return json.getString(Field.STORAGE.property);
  }

  public void setStorage(String storage) {
    json.put(Field.STORAGE.property, storage);
  }

  public String getStatus() {
    return json.getString(Field.STATUS.property);
  }

  public void setStatus(String status) {
    json.put(Field.STATUS.property, status);
  }

  public String getStarted() {
    return json.getString(Field.STARTED.property);
  }

  public String getFinished() {
    return json.getString(Field.FINISHED.property);
  }

  public void setStarted(LocalDateTime started) {
    json.put(Field.STARTED.property, started.toString());
  }

  public void setFinished(LocalDateTime finished) {
    json.put(Field.FINISHED.property, finished.toString());
  }

  /**
   * Sets start and finish dates.
   */
  public void setStartedAndFinished(String started, String finished) {
    if (started != null && finished != null) {
      json.put(Field.STARTED.property, started);
      if (started.compareTo(finished) < 0) { // or else it's the finish time of an earlier job
        json.put(Field.FINISHED.property, finished);
      }
    }
  }

  public Integer getAmountHarvested() {
    return json.getInteger(Field.AMOUNT_HARVESTED.property);
  }

  public void setAmountHarvested(Integer amountHarvested) {
    json.put(Field.AMOUNT_HARVESTED.property, amountHarvested);
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
    return json.getString(Field.MESSAGE.property);
  }

  public void setMessage(String message) {
    json.put(Field.MESSAGE.property, message);
  }


}
