package org.folio.harvesteradmin.moduledata;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.folio.harvesteradmin.modulestorage.Storage;
import org.folio.tlib.postgres.PgCqlDefinition;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldAlwaysMatches;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldBase;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldText;

public class LogLine extends StoredEntity {

  /**
   * Entity properties.
   */
  private UUID id;
  private UUID harvestJobId;
  private String timeStamp;
  private String logLevel;
  private String jobLabel;
  private String line;
  private int sequenceNumber;

  private static final String DATE_FORMAT = "YYYY-MM-DD HH24:MI:SS,MS";
  private static final Pattern logPattern // (timestamp) (level) (job) (statement)
      = Pattern.compile("([0-9\\- :,]{23}) ([A-Z]{4,5}) {1,2}(\\[.*?\\(.*?\\)]) (.*)");

  public enum Field {
    ID("id", "id"),
    HARVEST_JOB_ID("harvestJobId", "harvest_job_id"),
    SEQUENCE_NUMBER("sequenceNumber", "seq"),
    TIME_STAMP("timeStamp", "time_stamp"),
    LOG_LEVEL("logLevel", "log_level", new PgCqlFieldText()),
    JOB_LABEL("jobLabel", "job_label"),
    LOG_STATEMENT("line", "statement")
    ;

    public final String column;
    public final String property;

    public final PgCqlFieldBase pgCqlFieldDefinition;

    Field(String propertyName, String columnName) {
      this.column = columnName;
      this.property = propertyName;
      this.pgCqlFieldDefinition = null;
    }

    Field(String jsonPropertyName, String tableColumnName, PgCqlFieldBase pgCqlField) {
      this.column = tableColumnName;
      this.property = jsonPropertyName;
      this.pgCqlFieldDefinition = pgCqlField;
    }
  }

  @Override
  public Map<String, String> getPropertyColumnMap() {
    return Arrays.stream(
        LogLine.Field.values()).collect(Collectors.toMap(f -> f.property, f -> f.column));
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
        + Field.ID.column + " UUID PRIMARY KEY, "
        + Field.HARVEST_JOB_ID.column + " UUID NOT NULL REFERENCES "
        +     schema + "." + Storage.Table.harvest_job + "(" + HarvestJob.Field.ID.column + "), "
        + Field.SEQUENCE_NUMBER.column + " INTEGER NOT NULL, "
        + Field.TIME_STAMP.column + " TIMESTAMP NOT NULL, "
        + Field.LOG_LEVEL.column + " TEXT NOT NULL, "
        + Field.JOB_LABEL.column + " TEXT NOT NULL, "
        + Field.LOG_STATEMENT.column + " TEXT NOT NULL"
        + ")";
  }

  @Override
  public RowMapper<StoredEntity> getSelectListMapper() {
    return row -> {
      LogLine logLine = new LogLine();
      logLine.id = row.getUUID(Field.ID.column);
      logLine.harvestJobId = row.getUUID(Field.HARVEST_JOB_ID.column);
      logLine.timeStamp = row.getLocalDateTime(Field.TIME_STAMP.column).toString();
      logLine.logLevel = row.getString(Field.LOG_LEVEL.column);
      logLine.jobLabel = row.getString(Field.JOB_LABEL.column);
      logLine.line = row.getString(Field.LOG_STATEMENT.column);
      return logLine;
    };
  }

  /**
   * INSERT INTO SQL template.
   */
  public String getInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Storage.Table.log_statement
        + " ("
        + Field.ID.column + ", "
        + Field.HARVEST_JOB_ID.column + ", "
        + Field.SEQUENCE_NUMBER.column + ", "
        + Field.TIME_STAMP.column + ", "
        + Field.LOG_LEVEL.column + ", "
        + Field.JOB_LABEL.column + ", "
        + Field.LOG_STATEMENT.column
        + ")"
        + " VALUES ("
        + "#{" + Field.ID.column + "}, "
        + "#{" + Field.HARVEST_JOB_ID.column + "}, "
        + "#{" + Field.SEQUENCE_NUMBER.column + "}, "
        + "TO_TIMESTAMP(#{" + Field.TIME_STAMP.column + "},'" + DATE_FORMAT + "'), "
        + "#{" + Field.LOG_LEVEL.column + "}, "
        + "#{" + Field.JOB_LABEL.column + "}, "
        + "#{" + Field.LOG_STATEMENT.column + "}"
        + ")";
  }

  @Override
  public PgCqlDefinition getQueryableFields() {
    PgCqlDefinition pgCqlDefinition = PgCqlDefinition.create();
    pgCqlDefinition.addField("cql.allRecords", new PgCqlFieldAlwaysMatches());
    for (HarvestJob.Field field : HarvestJob.Field.values()) {
      if (field.pgCqlFieldDefinition != null) {
        pgCqlDefinition.addField(field.property, field.pgCqlFieldDefinition);
      }
    }
    return pgCqlDefinition;
  }

  @Override
  public SqlQuery getSqlQueryFromRequest(RoutingContext routingContext, String schema) {

    String offset = routingContext.request().getParam("offset");
    String limit = routingContext.request().getParam("limit");

    String select = "SELECT * ";
    String from = "FROM " + schema + "." + Storage.Table.log_statement;
    String where = "";
    String orderBy = "";

    return new SqlQuery(select, from, where, orderBy, offset, limit);
  }

  /**
   * Creates a TupleMapper for input mapping.
   */
  public TupleMapper<StoredEntity> getInsertValuesMapper() {
    return TupleMapper.mapper(
        entity -> {
          LogLine logLine = (LogLine) entity;
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(Field.ID.column, logLine.id);
          parameters.put(Field.HARVEST_JOB_ID.column, logLine.harvestJobId);
          parameters.put(Field.SEQUENCE_NUMBER.column, logLine.sequenceNumber);
          parameters.put(Field.TIME_STAMP.column, logLine.timeStamp);
          parameters.put(Field.LOG_LEVEL.column, logLine.logLevel);
          parameters.put(Field.JOB_LABEL.column, logLine.jobLabel);
          parameters.put(Field.LOG_STATEMENT.column, logLine.line);
          return parameters;
        });
  }

  public String toString() {
    return String.format("%s %-5s %s %s",this.timeStamp, this.logLevel, this.jobLabel, this.line);
  }

}
