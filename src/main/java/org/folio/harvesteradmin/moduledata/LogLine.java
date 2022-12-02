package org.folio.harvesteradmin.moduledata;

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
    ID("id", "id", PgColumn.Type.UUID, false, false),
    HARVEST_JOB_ID("harvestJobId", "harvest_job_id", PgColumn.Type.UUID, false, true),
    SEQUENCE_NUMBER("sequenceNumber", "seq", PgColumn.Type.INTEGER, false, false),
    TIME_STAMP("timeStamp", "time_stamp", PgColumn.Type.TIMESTAMP, false, false),
    LOG_LEVEL("logLevel", "log_level", PgColumn.Type.TEXT, false, true),
    JOB_LABEL("jobLabel", "job_label", PgColumn.Type.TEXT, false, false),
    LOG_STATEMENT("line", "statement", PgColumn.Type.TEXT, false, false)
    ;

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
      this.property = new JsonProperty(jsonPropertyName);
      this.queryable = queryable;
      this.column = getColumnDefinition(columnName, type, nullable, queryable);
    }
  }

  @Override
  public Map<String, PgColumn> getFieldMap() {
    return Arrays.stream(
        LogLine.Field.values()).collect(Collectors.toMap(f -> f.property.name, f -> f.column));
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
        + Field.ID.column.name + " UUID PRIMARY KEY, "
        + Field.HARVEST_JOB_ID.column.name + " UUID NOT NULL REFERENCES "
        + schema + "." + Storage.Table.harvest_job + "(" + HarvestJob.Field.ID.column.name + "), "
        + Field.SEQUENCE_NUMBER.column.name + " INTEGER NOT NULL, "
        + Field.TIME_STAMP.column.name + " TIMESTAMP NOT NULL, "
        + Field.LOG_LEVEL.column.name + " TEXT NOT NULL, "
        + Field.JOB_LABEL.column.name + " TEXT NOT NULL, "
        + Field.LOG_STATEMENT.column.name + " TEXT NOT NULL"
        + ")";
  }

  @Override
  public RowMapper<StoredEntity> getSelectListMapper() {
    return row -> {
      LogLine logLine = new LogLine();
      logLine.id = row.getUUID(Field.ID.column.name);
      logLine.harvestJobId = row.getUUID(Field.HARVEST_JOB_ID.column.name);
      logLine.timeStamp = row.getLocalDateTime(Field.TIME_STAMP.column.name).toString();
      logLine.logLevel = row.getString(Field.LOG_LEVEL.column.name);
      logLine.jobLabel = row.getString(Field.JOB_LABEL.column.name);
      logLine.line = row.getString(Field.LOG_STATEMENT.column.name);
      return logLine;
    };
  }

  /**
   * INSERT INTO SQL template.
   */
  public String getInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Storage.Table.log_statement
        + " ("
        + Field.ID.column.name + ", "
        + Field.HARVEST_JOB_ID.column.name + ", "
        + Field.SEQUENCE_NUMBER.column.name + ", "
        + Field.TIME_STAMP.column.name + ", "
        + Field.LOG_LEVEL.column.name + ", "
        + Field.JOB_LABEL.column.name + ", "
        + Field.LOG_STATEMENT.column.name
        + ")"
        + " VALUES ("
        + "#{" + Field.ID.column.name + "}, "
        + "#{" + Field.HARVEST_JOB_ID.column.name + "}, "
        + "#{" + Field.SEQUENCE_NUMBER.column.name + "}, "
        + "TO_TIMESTAMP(#{" + Field.TIME_STAMP.column.name + "},'" + DATE_FORMAT + "'), "
        + "#{" + Field.LOG_LEVEL.column.name + "}, "
        + "#{" + Field.JOB_LABEL.column.name + "}, "
        + "#{" + Field.LOG_STATEMENT.column.name + "}"
        + ")";
  }

  @Override
  public PgCqlDefinition getQueryableFields() {
    PgCqlDefinition pgCqlDefinition = PgCqlDefinition.create();
    pgCqlDefinition.addField("cql.allRecords", new PgCqlFieldAlwaysMatches());
    for (LogLine.Field field : LogLine.Field.values()) {
      if (field.queryable) {
        pgCqlDefinition.addField(field.property.name, field.column.pgCqlFieldObject);
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
          parameters.put(Field.ID.column.name, logLine.id);
          parameters.put(Field.HARVEST_JOB_ID.column.name, logLine.harvestJobId);
          parameters.put(Field.SEQUENCE_NUMBER.column.name, logLine.sequenceNumber);
          parameters.put(Field.TIME_STAMP.column.name, logLine.timeStamp);
          parameters.put(Field.LOG_LEVEL.column.name, logLine.logLevel);
          parameters.put(Field.JOB_LABEL.column.name, logLine.jobLabel);
          parameters.put(Field.LOG_STATEMENT.column.name, logLine.line);
          return parameters;
        });
  }

  public String toString() {
    return String.format("%s %-5s %s %s",this.timeStamp, this.logLevel, this.jobLabel, this.line);
  }

}
