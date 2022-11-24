package org.folio.harvesteradmin.moduledata;

import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.folio.harvesteradmin.modulestorage.Storage;

public class LogLine extends StoredEntity {

  /**
   * Entity properties.
   */
  private final UUID id;
  private final UUID harvestJobId;
  private final String timeStamp;
  private final String logLevel;
  private final String jobLabel;
  private final String line;
  private int sequenceNumber;

  private static final String DATE_FORMAT = "YYYY-MM-DD HH24:MI:SS,MS";
  private static final Pattern logPattern // (timestamp) (level) (job) (statement)
      = Pattern.compile("([0-9\\- :,]{23}) ([A-Z]{4,5}) {1,2}(\\[.*?\\(.*?\\)]) (.*)");


  /**
   * LOG_STATEMENT table columns.
   */
  public enum Column {
    id,
    harvest_job_id,
    seq,
    time_stamp,
    log_level,
    job_label,
    statement
  }

  /**
   * Constructor.
   */
  public LogLine() {
    id = null;
    harvestJobId = null;
    line = null;
    timeStamp = null;
    logLevel = null;
    jobLabel = null;
  }

  /**
   * Constructor.
   */
  public LogLine(UUID harvestJobId, String line, int sequenceNumber) {

    Matcher matcher = logPattern.matcher(line);
    if (matcher.matches()) {
      id = UUID.randomUUID();
      this.harvestJobId = harvestJobId;
      this.timeStamp = matcher.group(1).trim();
      this.logLevel = matcher.group(2).trim();
      this.jobLabel = matcher.group(3);
      this.line = matcher.group(4);
      this.sequenceNumber = sequenceNumber;
    } else {
      this.id = this.harvestJobId = null;
      this.timeStamp = this.logLevel = this.jobLabel = this.line = null;
    }
  }

  public UUID getId() {
    return id;
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
        + Column.harvest_job_id + " UUID NOT NULL REFERENCES "
        +         schema + "." + Storage.Table.harvest_job + "(" + HarvestJob.Column.id + "), "
        + Column.seq + " INTEGER NOT NULL, "
        + Column.time_stamp + " TIMESTAMP NOT NULL, "
        + Column.log_level + " TEXT NOT NULL, "
        + Column.job_label + " TEXT NOT NULL, "
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
        + Column.time_stamp + ", "
        + Column.log_level + ", "
        + Column.job_label + ", "
        + Column.statement
        + ")"
        + " VALUES ("
        + "#{" + Column.id + "}, "
        + "#{" + Column.harvest_job_id + "}, "
        + "#{" + Column.seq + "}, "
        + "TO_TIMESTAMP(#{" + Column.time_stamp + "},'" + DATE_FORMAT + "'), "
        + "#{" + Column.log_level + "}, "
        + "#{" + Column.job_label + "}, "
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
          parameters.put(Column.time_stamp.name(), logLine.timeStamp);
          parameters.put(Column.log_level.name(), logLine.logLevel);
          parameters.put(Column.job_label.name(), logLine.jobLabel);
          parameters.put(Column.statement.name(), logLine.line);
          return parameters;
        });
  }

}
