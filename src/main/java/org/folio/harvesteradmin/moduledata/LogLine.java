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

import org.folio.harvesteradmin.moduledata.database.SqlQuery;
import org.folio.harvesteradmin.moduledata.database.Tables;
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

  public enum Field implements EntityField {
    ID("id", "id", PgColumn.Type.UUID, false, false, true),
    HARVEST_JOB_ID("harvestJobId", "harvest_job_id", PgColumn.Type.UUID, false, true),
    SEQUENCE_NUMBER("sequenceNumber", "seq", PgColumn.Type.INTEGER, false, false),
    TIME_STAMP("timeStamp", "time_stamp", PgColumn.Type.TIMESTAMP, false, false),
    LOG_LEVEL("logLevel", "log_level", PgColumn.Type.TEXT, false, true),
    JOB_LABEL("jobLabel", "job_label", PgColumn.Type.TEXT, false, true),
    LOG_STATEMENT("line", "statement", PgColumn.Type.TEXT, false, true)
    ;

    private final String propertyName;
    private final PgColumn column;

    private final Boolean queryable;

    public String columnName() {
      return column.name;
    }

    @Override
    public String propertyName() {
      return propertyName;
    }

    @Override
    public PgColumn pgColumn() {
      return column;
    }

    @Override
    public boolean isQueryable() {
      return false;
    }

    Field(String propertyName,
          String columnName, PgColumn.Type type, Boolean nullable, Boolean queryable) {
      this(propertyName, columnName, type, nullable, queryable, false);
    }

    Field(String propertyName,
          String columnName, PgColumn.Type type, Boolean nullable, Boolean queryable,
          Boolean primaryKey) {
      this.propertyName = propertyName;
      this.queryable = queryable;
      this.column = new PgColumn(columnName, type, nullable, primaryKey);
    }
  }

  @Override
  public Map<String, PgColumn> getFieldMap() {
    return Arrays.stream(
        Field.values()).collect(Collectors.toMap(f -> f.propertyName, f -> f.column));
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
  public String makeCreateTableSql(String schema) {
    return  "CREATE TABLE IF NOT EXISTS " + schema + "." + Tables.log_statement
        + "("
        + Field.ID.columnName() + " UUID PRIMARY KEY, "
        + Field.HARVEST_JOB_ID.columnName() + " UUID NOT NULL REFERENCES "
        + schema + "." + Tables.harvest_job + "(" + HarvestJob.Field.ID.columnName() + "), "
        + Field.SEQUENCE_NUMBER.columnName() + " INTEGER NOT NULL, "
        + Field.TIME_STAMP.columnName() + " TIMESTAMP NOT NULL, "
        + Field.LOG_LEVEL.columnName() + " TEXT NOT NULL, "
        + Field.JOB_LABEL.columnName() + " TEXT NOT NULL, "
        + Field.LOG_STATEMENT.columnName() + " TEXT NOT NULL"
        + ")";
  }

  @Override
  public RowMapper<StoredEntity> getRowMapper() {
    return row -> {
      LogLine logLine = new LogLine();
      logLine.id = row.getUUID(Field.ID.columnName());
      logLine.harvestJobId = row.getUUID(Field.HARVEST_JOB_ID.columnName());
      // Display in original legacy harvester date format, not the pg date format (supports importing the output)
      logLine.timeStamp = row.getLocalDateTime(Field.TIME_STAMP.columnName())
              .toString().replace("T", " ").replace(".",",");
      logLine.logLevel = row.getString(Field.LOG_LEVEL.columnName());
      logLine.jobLabel = row.getString(Field.JOB_LABEL.columnName());
      logLine.line = row.getString(Field.LOG_STATEMENT.columnName());
      return logLine;
    };
  }

  /**
   * INSERT INTO statement.
   */
  public String makeInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Tables.log_statement
        + " ("
        + Field.ID.columnName() + ", "
        + Field.HARVEST_JOB_ID.columnName() + ", "
        + Field.SEQUENCE_NUMBER.columnName() + ", "
        + Field.TIME_STAMP.columnName() + ", "
        + Field.LOG_LEVEL.columnName() + ", "
        + Field.JOB_LABEL.columnName() + ", "
        + Field.LOG_STATEMENT.columnName()
        + ")"
        + " VALUES ("
        + "#{" + Field.ID.columnName() + "}, "
        + "#{" + Field.HARVEST_JOB_ID.columnName() + "}, "
        + "#{" + Field.SEQUENCE_NUMBER.columnName() + "}, "
        + "TO_TIMESTAMP(#{" + Field.TIME_STAMP.columnName() + "},'" + DATE_FORMAT + "'), "
        + "#{" + Field.LOG_LEVEL.columnName() + "}, "
        + "#{" + Field.JOB_LABEL.columnName() + "}, "
        + "#{" + Field.LOG_STATEMENT.columnName() + "}"
        + ")";
  }

  /**
   * Creates a TupleMapper for input mapping.
   */
  public TupleMapper<StoredEntity> getTupleMapper() {
    return TupleMapper.mapper(
        entity -> {
          LogLine logLine = (LogLine) entity;
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(Field.ID.columnName(), logLine.id);
          parameters.put(Field.HARVEST_JOB_ID.columnName(), logLine.harvestJobId);
          parameters.put(Field.SEQUENCE_NUMBER.columnName(), logLine.sequenceNumber);
          parameters.put(Field.TIME_STAMP.columnName(), logLine.timeStamp);
          parameters.put(Field.LOG_LEVEL.columnName(), logLine.logLevel);
          parameters.put(Field.JOB_LABEL.columnName(), logLine.jobLabel);
          parameters.put(Field.LOG_STATEMENT.columnName(), logLine.line);
          return parameters;
        });
  }

  @Override
  public PgCqlDefinition getQueryableFields() {
    PgCqlDefinition pgCqlDefinition = PgCqlDefinition.create();
    pgCqlDefinition.addField("cql.allRecords", new PgCqlFieldAlwaysMatches());
    for (Field field : Field.values()) {
      if (field.queryable) {
        pgCqlDefinition.addField(field.propertyName(), field.pgColumn().pgCqlField());
      }
    }
    return pgCqlDefinition;
  }

  public SqlQuery makeSqlFromCqlQuery(RoutingContext routingContext, String schemaDotTable) {
    SqlQuery sql = super.makeSqlFromCqlQuery(routingContext, schemaDotTable);
    sql.withAdditionalOrderByField(Field.TIME_STAMP.columnName());
    sql.withAdditionalOrderByField(Field.SEQUENCE_NUMBER.columnName());
    return sql;
  }

  public String toString() {
    return String.format("%s %-5s %s %s",this.timeStamp, this.logLevel, this.jobLabel, this.line);
  }

  /**
   * Get log line as JSON.
   */
  public JsonObject asJson() {
    JsonObject json = new JsonObject();
    json.put(Field.ID.propertyName, this.id.toString());
    json.put(Field.TIME_STAMP.propertyName, this.timeStamp);
    json.put(Field.LOG_LEVEL.propertyName, this.logLevel);
    json.put(Field.JOB_LABEL.propertyName, this.jobLabel);
    json.put(Field.LOG_STATEMENT.propertyName, this.line);
    return json;
  }

}
