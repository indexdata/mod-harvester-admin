package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.folio.harvesteradmin.moduledata.database.Tables;
import org.folio.tlib.postgres.PgCqlDefinition;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldAlwaysMatches;

public class HarvestJob extends StoredEntity {

  private final JsonObject json = new JsonObject();
  public static final String DATE_FORMAT = "YYYY-MM-DD''T''HH24:MI:SS";

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
    harvestJob.setId(UUID.fromString(harvestJobJson.getString(HarvestJobField.ID.propertyName())));
    harvestJob.setHarvestableId(harvestJobJson.getString(HarvestJobField.HARVESTABLE_ID.propertyName()));
    harvestJob.setName(harvestJobJson.getString(HarvestJobField.HARVESTABLE_NAME.propertyName()));
    harvestJob.setType(harvestJobJson.getString(HarvestJobField.HARVESTABLE_TYPE.propertyName()));
    harvestJob.setUrl(harvestJobJson.getString(HarvestJobField.URL.propertyName()));
    harvestJob.setAllowErrors(harvestJobJson.getString(HarvestJobField.ALLOW_ERRORS.propertyName()));
    harvestJob.setRecordLimit(harvestJobJson.getString(HarvestJobField.RECORD_LIMIT.propertyName()));
    harvestJob.setBatchSize(harvestJobJson.getString(HarvestJobField.BATCH_SIZE.propertyName()));
    harvestJob.setTransformation(harvestJobJson.getString(HarvestJobField.TRANSFORMATION.propertyName()));
    harvestJob.setStorage(harvestJobJson.getString(HarvestJobField.STORAGE.propertyName()));
    harvestJob.setStatus(harvestJobJson.getString(HarvestJobField.STATUS.propertyName()));
    harvestJob.setStartedAndFinished(
            harvestJobJson.getString(HarvestJobField.STARTED.propertyName()),
            harvestJobJson.getString(HarvestJobField.FINISHED.propertyName()));
    harvestJob.setAmountHarvested(harvestJobJson.getInteger(HarvestJobField.AMOUNT_HARVESTED.propertyName()));
    harvestJob.setMessage(harvestJobJson.getString(HarvestJobField.MESSAGE.propertyName()));
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
  @Override
  public List<String> makeCreateSqls(String schema) {
    StringBuilder columnsDdl = new StringBuilder();
    Stream.of(HarvestJobField.values())
        .forEach(field -> columnsDdl.append(field.pgColumn().getColumnDdl()).append(","));
    columnsDdl.deleteCharAt(columnsDdl.length() - 1); // remove ending comma
    return List.of(
        "CREATE TABLE IF NOT EXISTS " + schema + "." + Tables.harvest_job
        + "("
        + columnsDdl
        + ")"
        );
  }

  /**
   * INSERT INTO statement.
   */
  public String makeInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Tables.harvest_job
        + " ("
        + HarvestJobField.ID + ", "
        + HarvestJobField.HARVESTABLE_ID.columnName() + ", "
        + HarvestJobField.HARVESTABLE_NAME.columnName() + ", "
        + HarvestJobField.HARVESTABLE_TYPE.columnName() + ", "
        + HarvestJobField.URL.columnName() + ", "
        + HarvestJobField.ALLOW_ERRORS.columnName() + ", "
        + HarvestJobField.RECORD_LIMIT.columnName() + ", "
        + HarvestJobField.BATCH_SIZE.columnName() + ", "
        + HarvestJobField.TRANSFORMATION.columnName() + ", "
        + HarvestJobField.STORAGE.columnName() + ", "
        + HarvestJobField.STATUS.columnName() + ", "
        + HarvestJobField.STARTED.columnName() + ", "
        + HarvestJobField.FINISHED.columnName() + ", "
        + HarvestJobField.AMOUNT_HARVESTED.columnName() + ", "
        + HarvestJobField.MESSAGE.columnName()
        + ")"
        + " VALUES ("
        + "#{" + HarvestJobField.ID.columnName() + "}, "
        + "#{" + HarvestJobField.HARVESTABLE_ID.columnName() + "}, "
        + "#{" + HarvestJobField.HARVESTABLE_NAME.columnName() + "}, "
        + "#{" + HarvestJobField.HARVESTABLE_TYPE.columnName() + "}, "
        + "#{" + HarvestJobField.URL.columnName() + "}, "
        + "#{" + HarvestJobField.ALLOW_ERRORS.columnName() + "}, "
        + "#{" + HarvestJobField.RECORD_LIMIT.columnName() + "}, "
        + "#{" + HarvestJobField.BATCH_SIZE.columnName() + "}, "
        + "#{" + HarvestJobField.TRANSFORMATION.columnName() + "}, "
        + "#{" + HarvestJobField.STORAGE.columnName() + "}, "
        + "#{" + HarvestJobField.STATUS.columnName() + "}, "
        + "TO_TIMESTAMP(#{" + HarvestJobField.STARTED.columnName() + "},'" + DATE_FORMAT + "'), "
        + "TO_TIMESTAMP(#{" + HarvestJobField.FINISHED.columnName() + "}, '" + DATE_FORMAT + "'), "
        + "#{" + HarvestJobField.AMOUNT_HARVESTED.columnName() + "}, "
        + "#{" + HarvestJobField.MESSAGE.columnName() + "}"
        + ")";
  }

  @Override
  public PgCqlDefinition getQueryableFields() {
    PgCqlDefinition pgCqlDefinition = PgCqlDefinition.create();
    pgCqlDefinition.addField("cql.allRecords", new PgCqlFieldAlwaysMatches());
    for (Field field : HarvestJobField.values()) {
      if (field.isQueryable()) {
        pgCqlDefinition.addField(field.propertyName(), field.pgColumn().pgCqlField());
      }
    }
    return pgCqlDefinition;
  }

  @Override
  public Map<String, PgColumn> getFieldMap() {
    return Arrays.stream(
        HarvestJobField.values()).collect(
        Collectors.toMap(HarvestJobField::propertyName, HarvestJobField::pgColumn));
  }

  /**
   * Maps values of the POJO into table columns for insert-into statement.
   */
  public TupleMapper<StoredEntity> getTupleMapper() {
    return TupleMapper.mapper(
        harvestJob -> {
          HarvestJob entity = (HarvestJob) harvestJob;
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(HarvestJobField.ID.columnName(), entity.getId());
          parameters.put(HarvestJobField.HARVESTABLE_ID.columnName(), entity.getHarvestableId());
          parameters.put(HarvestJobField.HARVESTABLE_NAME.columnName(), entity.getName());
          parameters.put(HarvestJobField.HARVESTABLE_TYPE.columnName(), entity.getType());
          parameters.put(HarvestJobField.URL.columnName(), entity.getUrl());
          parameters.put(HarvestJobField.ALLOW_ERRORS.columnName(), entity.getAllowErrors());
          if (entity.getRecordLimit() != null) {
            parameters.put(HarvestJobField.RECORD_LIMIT.columnName(), entity.getRecordLimit());
          }
          if (entity.getBatchSize() != null) {
            parameters.put(HarvestJobField.BATCH_SIZE.columnName(), entity.getBatchSize());
          }
          parameters.put(HarvestJobField.TRANSFORMATION.columnName(), entity.getTransformation());
          parameters.put(HarvestJobField.STORAGE.columnName(), entity.getStorage());
          parameters.put(HarvestJobField.STATUS.columnName(), entity.getStatus());
          parameters.put(HarvestJobField.STARTED.columnName(), entity.getStarted());
          if (entity.getFinished() != null) {
            parameters.put(HarvestJobField.FINISHED.columnName(), entity.getFinished());
          }
          if (entity.getAmountHarvested() != null) {
            parameters.put(
                HarvestJobField.AMOUNT_HARVESTED.columnName(), entity.getAmountHarvested());
          }
          parameters.put(HarvestJobField.MESSAGE.columnName(), entity.getMessage());
          return parameters;
        });
  }

  /**
   * Maps values from columns of a row to properties of the POJO.
   */
  public RowMapper<StoredEntity> getRowMapper() {
    return row -> {
      HarvestJob harvestJob = new HarvestJob();
      harvestJob.setId(row.getUUID(HarvestJobField.ID.columnName()));
      harvestJob.setName(row.getString(HarvestJobField.HARVESTABLE_NAME.columnName()));
      harvestJob.setHarvestableId(row.getLong(HarvestJobField.HARVESTABLE_ID.columnName()));
      harvestJob.setType(row.getString(HarvestJobField.HARVESTABLE_TYPE.columnName()));
      harvestJob.setUrl(row.getString(HarvestJobField.URL.columnName()));
      harvestJob.setAllowErrors(row.getBoolean(HarvestJobField.ALLOW_ERRORS.columnName()));
      if (row.getValue(HarvestJobField.RECORD_LIMIT.columnName()) != null) {
        harvestJob.setRecordLimit(row.getInteger(HarvestJobField.RECORD_LIMIT.columnName()));
      }
      harvestJob.setTransformation(row.getString(HarvestJobField.TRANSFORMATION.columnName()));
      harvestJob.setStorage(row.getString(HarvestJobField.STORAGE.columnName()));
      harvestJob.setStatus(row.getString(HarvestJobField.STATUS.columnName()));
      harvestJob.setStarted(row.getLocalDateTime(HarvestJobField.STARTED.columnName()));
      if (row.getValue(HarvestJobField.FINISHED.columnName()) != null) {
        harvestJob.setFinished(row.getLocalDateTime(HarvestJobField.FINISHED.columnName()));
      }
      if (row.getValue(HarvestJobField.AMOUNT_HARVESTED.columnName()) != null) {
        harvestJob.setAmountHarvested(
            row.getInteger(HarvestJobField.AMOUNT_HARVESTED.columnName()));
      }
      harvestJob.setMessage(row.getString(HarvestJobField.MESSAGE.columnName()));
      return harvestJob;
    };
  }

  public UUID getId() {
    return UUID.fromString(json.getString(HarvestJobField.ID.propertyName()));
  }

  public void setId(UUID id) {
    json.put(HarvestJobField.ID.propertyName(), id);
  }

  public long getHarvestableId() {
    return json.getLong(HarvestJobField.HARVESTABLE_ID.propertyName());
  }

  public void setHarvestableId(long harvestableId) {
    json.put(HarvestJobField.HARVESTABLE_ID.propertyName(), harvestableId);
  }

  public void setHarvestableId(String harvestableId) {
    setHarvestableId(Long.parseLong(harvestableId));
  }

  public String getName() {
    return json.getString(HarvestJobField.HARVESTABLE_NAME.propertyName());
  }

  public void setName(String name) {
    json.put(HarvestJobField.HARVESTABLE_NAME.propertyName(), name);
  }

  public String getType() {
    return json.getString(HarvestJobField.HARVESTABLE_TYPE.propertyName());
  }

  public void setType(String type) {
    json.put(HarvestJobField.HARVESTABLE_TYPE.propertyName(), type);
  }

  public String getUrl() {
    return json.getString(HarvestJobField.URL.propertyName());
  }

  public void setUrl(String url) {
    json.put(HarvestJobField.URL.propertyName(), url);
  }

  public Boolean getAllowErrors() {
    return json.getBoolean(HarvestJobField.ALLOW_ERRORS.propertyName());
  }

  public void setAllowErrors(Boolean allowErrors) {
    json.put(HarvestJobField.ALLOW_ERRORS.propertyName(), allowErrors);
  }

  public void setAllowErrors(String allowErrors) {
    setAllowErrors(Boolean.valueOf(allowErrors));
  }

  public Integer getRecordLimit() {
    return json.getInteger(HarvestJobField.RECORD_LIMIT.propertyName());
  }

  public void setRecordLimit(Integer recordLimit) {
    json.put(HarvestJobField.RECORD_LIMIT.propertyName(), recordLimit);
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
    return json.getInteger(HarvestJobField.BATCH_SIZE.propertyName());
  }

  /**
   * Sets batch size.
   */
  public void setBatchSize(String batchSize) {
    if (batchSize != null) {
      json.put(HarvestJobField.BATCH_SIZE.propertyName(), Integer.parseInt(batchSize));
    }
  }

  public String getTransformation() {
    return json.getString(HarvestJobField.TRANSFORMATION.propertyName());
  }

  public void setTransformation(String transformation) {
    json.put(HarvestJobField.TRANSFORMATION.propertyName(), transformation);
  }

  public String getStorage() {
    return json.getString(HarvestJobField.STORAGE.propertyName());
  }

  public void setStorage(String storage) {
    json.put(HarvestJobField.STORAGE.propertyName(), storage);
  }

  public String getStatus() {
    return json.getString(HarvestJobField.STATUS.propertyName());
  }

  public void setStatus(String status) {
    json.put(HarvestJobField.STATUS.propertyName(), status);
  }

  public String getStarted() {
    return json.getString(HarvestJobField.STARTED.propertyName());
  }

  public String getFinished() {
    return json.getString(HarvestJobField.FINISHED.propertyName());
  }

  public void setStarted(LocalDateTime started) {
    json.put(HarvestJobField.STARTED.propertyName(), started.toString());
  }

  public void setStarted(String started) {
    json.put(HarvestJobField.STARTED.propertyName(), started);
  }

  public void setFinished(LocalDateTime finished) {
    setFinished(finished.toString());
  }

  public void setFinished(String finished) {
    json.put(HarvestJobField.FINISHED.propertyName(), finished);
  }

  /**
   * Sets start and finish dates.
   */
  public void setStartedAndFinished(String started, String finished) {
    if (started != null) {
      json.put(HarvestJobField.STARTED.propertyName(), started);
      if (finished != null && started.compareTo(finished) < 0) {
        json.put(HarvestJobField.FINISHED.propertyName(), finished);
      }
    }
  }

  public Integer getAmountHarvested() {
    return json.getInteger(HarvestJobField.AMOUNT_HARVESTED.propertyName());
  }

  public void setAmountHarvested(Integer amountHarvested) {
    json.put(HarvestJobField.AMOUNT_HARVESTED.propertyName(), amountHarvested);
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
    return json.getString(HarvestJobField.MESSAGE.propertyName());
  }

  public void setMessage(String message) {
    json.put(HarvestJobField.MESSAGE.propertyName(), message);
  }


}
