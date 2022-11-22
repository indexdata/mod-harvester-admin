package org.folio.harvesteradmin.moduledata;

import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.folio.harvesteradmin.modulestorage.Storage;

public class LogLine extends StoredEntity {

  /**
   * Entity properties.
   */
  private final UUID id;
  private final UUID harvestJobId;
  private final String line;
  private int sequenceNumber;

  /**
   * LOG_STATEMENT table columns.
   */
  public enum Column {
    id,
    harvest_job_id,
    seq,
    statement
  }

  /**
   * Constructor.
   */
  public LogLine() {
    id = null;
    harvestJobId = null;
    line = null;
  }

  /**
   * Constructor.
   */
  public LogLine(UUID harvestJobId, String line, int sequenceNumber) {
    id = UUID.randomUUID();
    this.harvestJobId = harvestJobId;
    this.line = line;
    this.sequenceNumber = sequenceNumber;
  }

  public static LogLine entity() {
    return new LogLine();
  }

  /**
   * CREATE TABLE SQL template.
   */
  public String getCreateTableSql(String schema) {
    return  "CREATE TABLE IF NOT EXISTS " + schema + "." + Storage.Table.log_statement
        + "("
        + Column.id + " UUID PRIMARY KEY, "
        + Column.harvest_job_id + " UUID REFERENCES "
        +         schema + "." + Storage.Table.harvest_job + "(" + HarvestJob.Column.id + "), "
        + Column.seq + " INTEGER NOT NULL, "
        + Column.statement + " TEXT NOT NULL"
        + ")";
  }

  @Override
  public RowMapper<StoredEntity> getSelectListMapper() {
    return null;
  }

  /**
   * INSERT INTO SQL template.
   */
  public String getInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Storage.Table.log_statement
        + " ("
        + Column.id + ", "
        + Column.harvest_job_id + ", "
        + Column.seq + ", "
        + Column.statement
        + ")"
        + " VALUES ("
        + "#{" + Column.id + "}, "
        + "#{" + Column.harvest_job_id + "}, "
        + "#{" + Column.seq + "}, "
        + "#{" + Column.statement + "}"
        + ")";
  }

  /**
   * Creates a TupleMapper for input mapping.
   */
  public TupleMapper<StoredEntity> getInsertValuesMapper() {
    return TupleMapper.mapper(
        entity -> {
          LogLine logLine = (LogLine) entity;
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(Column.id.name(), logLine.id);
          parameters.put(Column.harvest_job_id.name(), logLine.harvestJobId);
          parameters.put(Column.seq.name(), logLine.sequenceNumber);
          parameters.put(Column.statement.name(), logLine.line);
          return parameters;
        });
  }

}
