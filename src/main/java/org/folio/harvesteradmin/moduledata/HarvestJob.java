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


public class HarvestJob extends StoredEntity {

  private final JsonObject json = new JsonObject();
  private static final String DATE_FORMAT = "YYYY-MM-DD''T''HH24:MI:SS";

  public HarvestJob() {
    super();
  }

  public static HarvestJob entity() {
    return new HarvestJob();
  }

  /**
   * Maps legacy harvestable JSON to a HarvestJob POJO.
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
        harvestableJson.getString("lastHarvestFinished")
    );
    harvestJob.setAmountHarvested(harvestableJson.getString("amountHarvested"));
    harvestJob.setMessage(harvestableJson.getString("message"));
    return harvestJob;
  }

  /**
   * Test purposes: For importing harvest job samples without running jobs to create them first
   * @param harvestJobJson Could be harvest job JSON exported from another service
   * @return POJO
   */
  public static HarvestJob fromHarvestJobJson(JsonObject harvestJobJson) {
    HarvestJob harvestJob = new HarvestJob();
    harvestJob.setId(UUID.fromString(harvestJobJson.getString(Field.ID.propertyName())));
    harvestJob.setHarvestableId(harvestJobJson.getString(Field.HARVESTABLE_ID.propertyName()));
    harvestJob.setName(harvestJobJson.getString(Field.HARVESTABLE_NAME.propertyName()));
    harvestJob.setType(harvestJobJson.getString(Field.HARVESTABLE_TYPE.propertyName()));
    harvestJob.setUrl(harvestJobJson.getString(Field.URL.propertyName()));
    harvestJob.setAllowErrors(harvestJobJson.getString(Field.ALLOW_ERRORS.propertyName()));
    harvestJob.setRecordLimit(harvestJobJson.getString(Field.RECORD_LIMIT.propertyName()));
    harvestJob.setBatchSize(harvestJobJson.getString(Field.BATCH_SIZE.propertyName()));
    harvestJob.setTransformation(harvestJobJson.getString(Field.TRANSFORMATION.propertyName()));
    harvestJob.setStorage(harvestJobJson.getString(Field.STORAGE.propertyName()));
    harvestJob.setStatus(harvestJobJson.getString(Field.STATUS.propertyName()));
    harvestJob.setStartedAndFinished(
            harvestJobJson.getString(Field.STARTED.propertyName()),
            harvestJobJson.getString(Field.FINISHED.propertyName()));
    harvestJob.setAmountHarvested(harvestJobJson.getInteger(Field.AMOUNT_HARVESTED.propertyName()));
    harvestJob.setMessage(harvestJobJson.getString(Field.MESSAGE.propertyName()));
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
  public String makeCreateTableSql(String schema) {
    StringBuilder columnsDdl = new StringBuilder();
    Stream.of(Field.values())
        .forEach(field -> columnsDdl.append(field.pgColumn().getColumnDdl()).append(","));
    columnsDdl.deleteCharAt(columnsDdl.length() - 1); // remove ending comma

    return "CREATE TABLE IF NOT EXISTS " + schema + "." + Tables.harvest_job
        + "("
        + columnsDdl
        + ")";
  }

  /**
   * INSERT INTO statement.
   */
  public String makeInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Tables.harvest_job
        + " ("
        + Field.ID + ", "
        + Field.HARVESTABLE_ID.columnName() + ", "
        + Field.HARVESTABLE_NAME.columnName() + ", "
        + Field.HARVESTABLE_TYPE.columnName() + ", "
        + Field.URL.columnName() + ", "
        + Field.ALLOW_ERRORS.columnName() + ", "
        + Field.RECORD_LIMIT.columnName() + ", "
        + Field.BATCH_SIZE.columnName() + ", "
        + Field.TRANSFORMATION.columnName() + ", "
        + Field.STORAGE.columnName() + ", "
        + Field.STATUS.columnName() + ", "
        + Field.STARTED.columnName() + ", "
        + Field.FINISHED.columnName() + ", "
        + Field.AMOUNT_HARVESTED.columnName() + ", "
        + Field.MESSAGE.columnName()
        + ")"
        + " VALUES ("
        + "#{" + Field.ID.columnName() + "}, "
        + "#{" + Field.HARVESTABLE_ID.columnName() + "}, "
        + "#{" + Field.HARVESTABLE_NAME.columnName() + "}, "
        + "#{" + Field.HARVESTABLE_TYPE.columnName() + "}, "
        + "#{" + Field.URL.columnName() + "}, "
        + "#{" + Field.ALLOW_ERRORS.columnName() + "}, "
        + "#{" + Field.RECORD_LIMIT.columnName() + "}, "
        + "#{" + Field.BATCH_SIZE.columnName() + "}, "
        + "#{" + Field.TRANSFORMATION.columnName() + "}, "
        + "#{" + Field.STORAGE.columnName() + "}, "
        + "#{" + Field.STATUS.columnName() + "}, "
        + "TO_TIMESTAMP(#{" + Field.STARTED.columnName() + "},'" + DATE_FORMAT + "'), "
        + "TO_TIMESTAMP(#{" + Field.FINISHED.columnName() + "}, '" + DATE_FORMAT + "'), "
        + "#{" + Field.AMOUNT_HARVESTED.columnName() + "}, "
        + "#{" + Field.MESSAGE.columnName() + "}"
        + ")";
  }

  @Override
  public PgCqlDefinition getQueryableFields() {
    PgCqlDefinition pgCqlDefinition = PgCqlDefinition.create();
    pgCqlDefinition.addField("cql.allRecords", new PgCqlFieldAlwaysMatches());
    for (EntityField entityField : Field.values()) {
      if (entityField.isQueryable()) {
        pgCqlDefinition.addField(entityField.propertyName(), entityField.pgColumn().pgCqlField());
      }
    }
    return pgCqlDefinition;
  }

  @Override
  public Map<String, PgColumn> getFieldMap() {
    return Arrays.stream(
        Field.values()).collect(
        Collectors.toMap(Field::propertyName, Field::pgColumn));
  }

  /**
   * Maps values of the POJO into table columns for insert-into statement.
   */
  public TupleMapper<StoredEntity> getTupleMapper() {
    return TupleMapper.mapper(
        harvestJob -> {
          HarvestJob entity = (HarvestJob) harvestJob;
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(Field.ID.columnName(), entity.getId());
          parameters.put(Field.HARVESTABLE_ID.columnName(), entity.getHarvestableId());
          parameters.put(Field.HARVESTABLE_NAME.columnName(), entity.getName());
          parameters.put(Field.HARVESTABLE_TYPE.columnName(), entity.getType());
          parameters.put(Field.URL.columnName(), entity.getUrl());
          parameters.put(Field.ALLOW_ERRORS.columnName(), entity.getAllowErrors());
          if (entity.getRecordLimit() != null) {
            parameters.put(Field.RECORD_LIMIT.columnName(), entity.getRecordLimit());
          }
          if (entity.getBatchSize() != null) {
            parameters.put(Field.BATCH_SIZE.columnName(), entity.getBatchSize());
          }
          parameters.put(Field.TRANSFORMATION.columnName(), entity.getTransformation());
          parameters.put(Field.STORAGE.columnName(), entity.getStorage());
          parameters.put(Field.STATUS.columnName(), entity.getStatus());
          parameters.put(Field.STARTED.columnName(), entity.getStarted());
          if (entity.getFinished() != null) {
            parameters.put(Field.FINISHED.columnName(), entity.getFinished());
          }
          if (entity.getAmountHarvested() != null) {
            parameters.put(
                Field.AMOUNT_HARVESTED.columnName(), entity.getAmountHarvested());
          }
          parameters.put(Field.MESSAGE.columnName(), entity.getMessage());
          return parameters;
        });
  }

  /**
   * Maps values from columns of a row to properties of the POJO.
   */
  public RowMapper<StoredEntity> getRowMapper() {
    return row -> {
      HarvestJob harvestJob = new HarvestJob();
      harvestJob.setId(row.getUUID(Field.ID.columnName()));
      harvestJob.setName(row.getString(Field.HARVESTABLE_NAME.columnName()));
      harvestJob.setHarvestableId(row.getLong(Field.HARVESTABLE_ID.columnName()));
      harvestJob.setType(row.getString(Field.HARVESTABLE_TYPE.columnName()));
      harvestJob.setUrl(row.getString(Field.URL.columnName()));
      harvestJob.setAllowErrors(row.getBoolean(Field.ALLOW_ERRORS.columnName()));
      if (row.getValue(Field.RECORD_LIMIT.columnName()) != null) {
        harvestJob.setRecordLimit(row.getInteger(Field.RECORD_LIMIT.columnName()));
      }
      harvestJob.setTransformation(row.getString(Field.TRANSFORMATION.columnName()));
      harvestJob.setStorage(row.getString(Field.STORAGE.columnName()));
      harvestJob.setStatus(row.getString(Field.STATUS.columnName()));
      harvestJob.setStarted(row.getLocalDateTime(Field.STARTED.columnName()));
      if (row.getValue(Field.FINISHED.columnName()) != null) {
        harvestJob.setFinished(row.getLocalDateTime(Field.FINISHED.columnName()));
      }
      if (row.getValue(Field.AMOUNT_HARVESTED.columnName()) != null) {
        harvestJob.setAmountHarvested(
            row.getInteger(Field.AMOUNT_HARVESTED.columnName()));
      }
      harvestJob.setMessage(row.getString(Field.MESSAGE.columnName()));
      return harvestJob;
    };
  }

  public UUID getId() {
    return UUID.fromString(json.getString(Field.ID.propertyName()));
  }

  public void setId(UUID id) {
    json.put(Field.ID.propertyName(), id);
  }

  public long getHarvestableId() {
    return json.getLong(Field.HARVESTABLE_ID.propertyName());
  }

  public void setHarvestableId(long harvestableId) {
    json.put(Field.HARVESTABLE_ID.propertyName(), harvestableId);
  }

  public void setHarvestableId(String harvestableId) {
    setHarvestableId(Long.parseLong(harvestableId));
  }

  public String getName() {
    return json.getString(Field.HARVESTABLE_NAME.propertyName());
  }

  public void setName(String name) {
    json.put(Field.HARVESTABLE_NAME.propertyName(), name);
  }

  public String getType() {
    return json.getString(Field.HARVESTABLE_TYPE.propertyName());
  }

  public void setType(String type) {
    json.put(Field.HARVESTABLE_TYPE.propertyName(), type);
  }

  public String getUrl() {
    return json.getString(Field.URL.propertyName());
  }

  public void setUrl(String url) {
    json.put(Field.URL.propertyName(), url);
  }

  public Boolean getAllowErrors() {
    return json.getBoolean(Field.ALLOW_ERRORS.propertyName());
  }

  public void setAllowErrors(Boolean allowErrors) {
    json.put(Field.ALLOW_ERRORS.propertyName(), allowErrors);
  }

  public void setAllowErrors(String allowErrors) {
    setAllowErrors(Boolean.valueOf(allowErrors));
  }

  public Integer getRecordLimit() {
    return json.getInteger(Field.RECORD_LIMIT.propertyName());
  }

  public void setRecordLimit(Integer recordLimit) {
    json.put(Field.RECORD_LIMIT.propertyName(), recordLimit);
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
    return json.getInteger(Field.BATCH_SIZE.propertyName());
  }

  /**
   * Sets batch size.
   */
  public void setBatchSize(String batchSize) {
    if (batchSize != null) {
      json.put(Field.BATCH_SIZE.propertyName(), Integer.parseInt(batchSize));
    }
  }

  public String getTransformation() {
    return json.getString(Field.TRANSFORMATION.propertyName());
  }

  public void setTransformation(String transformation) {
    json.put(Field.TRANSFORMATION.propertyName(), transformation);
  }

  public String getStorage() {
    return json.getString(Field.STORAGE.propertyName());
  }

  public void setStorage(String storage) {
    json.put(Field.STORAGE.propertyName(), storage);
  }

  public String getStatus() {
    return json.getString(Field.STATUS.propertyName());
  }

  public void setStatus(String status) {
    json.put(Field.STATUS.propertyName(), status);
  }

  public String getStarted() {
    return json.getString(Field.STARTED.propertyName());
  }

  public String getFinished() {
    return json.getString(Field.FINISHED.propertyName());
  }

  public void setStarted(LocalDateTime started) {
    json.put(Field.STARTED.propertyName(), started.toString());
  }

  public void setStarted(String started) {
    json.put(Field.STARTED.propertyName(), started);
  }

  public void setFinished(LocalDateTime finished) {
    setFinished(finished.toString());
  }

  public void setFinished(String finished) {
    json.put(Field.FINISHED.propertyName(), finished);
  }

  /**
   * Sets start and finish dates.
   */
  public void setStartedAndFinished(String started, String finished) {
    if (started != null) {
      json.put(Field.STARTED.propertyName(), started);
      if (finished != null && started.compareTo(finished) < 0) {
        json.put(Field.FINISHED.propertyName(), finished);
      }
    }
  }

  public Integer getAmountHarvested() {
    return json.getInteger(Field.AMOUNT_HARVESTED.propertyName());
  }

  public void setAmountHarvested(Integer amountHarvested) {
    json.put(Field.AMOUNT_HARVESTED.propertyName(), amountHarvested);
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
    return json.getString(Field.MESSAGE.propertyName());
  }

  public void setMessage(String message) {
    json.put(Field.MESSAGE.propertyName(), message);
  }

  public enum Field implements EntityField {
    ID("id", "id", PgColumn.Type.UUID, false, true, true),
    HARVESTABLE_ID("harvestableId", "harvestable_id", PgColumn.Type.BIGINT, false, true),
    HARVESTABLE_NAME("name", "harvestable_name", PgColumn.Type.TEXT, false, true),
    HARVESTABLE_TYPE("type", "type", PgColumn.Type.TEXT, false, true),
    URL("url", "url", PgColumn.Type.TEXT, false, false),
    ALLOW_ERRORS("allowErrors", "allow_errors", PgColumn.Type.BOOLEAN, false, false),
    RECORD_LIMIT("recordLimit", "record_limit", PgColumn.Type.INTEGER, true, false),
    BATCH_SIZE("batchSize", "batch_size", PgColumn.Type.INTEGER, true, false),
    TRANSFORMATION("transformation", "transformation", PgColumn.Type.TEXT, false, true),
    STORAGE("storage", "storage", PgColumn.Type.TEXT, false, true),
    STATUS("status", "status", PgColumn.Type.TEXT, true, true),
    STARTED("started", "started", PgColumn.Type.TIMESTAMP, false, true),
    FINISHED("finished", "finished", PgColumn.Type.TIMESTAMP, true, true),
    AMOUNT_HARVESTED("amountHarvested", "amount_harvested", PgColumn.Type.INTEGER, true, true),
    MESSAGE("message", "message", PgColumn.Type.TEXT, true, true);

    private final JsonProperty property;
    private final PgColumn column;
    private final Boolean queryable;

    public String columnName() {
      return column.name;
    }

    public String propertyName() {
      return property.name;
    }

    public PgColumn pgColumn() {
      return column;
    }

    public boolean isQueryable() {
      return queryable;
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
      this.column = new PgColumn(columnName, type, nullable, primaryKey);
    }

  }

}
