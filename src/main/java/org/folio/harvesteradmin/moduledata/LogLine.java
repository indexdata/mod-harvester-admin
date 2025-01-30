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


public class LogLine extends Entity {

  public Record record;
  public record Record(UUID id, UUID harvestJobId, String timeStamp, String logLevel, String jobLabel, String line, int sequenceNumber) {}

  // Fields map.
  private static final Map<String, org.folio.harvesteradmin.moduledata.Field> FIELDS = new HashMap<>();
  // Field keys.
  public static String ID="ID", HARVEST_JOB_ID="HARVEST_JOB_ID", SEQUENCE_NUMBER="SEQUENCE_NUMBER", TIME_STAMP="TIME_STAMP",
  LOG_LEVEL="LOG_LEVEL", JOB_LABEL="JOB_LABEL", LOG_STATEMENT="LOG_STATEMENT";
  // Populate.
  static {
    FIELDS.put(ID, new Field("id", "id", PgColumn.Type.UUID, false, false, true));
    FIELDS.put(HARVEST_JOB_ID, new Field("harvestJobId", "harvest_job_id", PgColumn.Type.UUID, false, true));
    FIELDS.put(SEQUENCE_NUMBER, new Field("sequenceNumber", "seq", PgColumn.Type.INTEGER, false, false));
    FIELDS.put(TIME_STAMP, new Field("timeStamp", "time_stamp", PgColumn.Type.TIMESTAMP, false, false));
    FIELDS.put(LOG_LEVEL, new Field("logLevel", "log_level", PgColumn.Type.TEXT, false, true));
    FIELDS.put(JOB_LABEL, new Field("jobLabel", "job_label", PgColumn.Type.TEXT, false, true));
    FIELDS.put(LOG_STATEMENT, new Field("line", "statement", PgColumn.Type.TEXT, false, true));
  }
  // Get.
  @Override
  public Map<String, Field> fields() {
    return FIELDS;
  }

  private static final String DATE_FORMAT = "YYYY-MM-DD HH24:MI:SS,MS";
  private static final Pattern logPattern // (timestamp) (level) (job) (statement)
      = Pattern.compile("([0-9\\- :,]{23}) ([A-Z]{4,5}) {1,2}(\\[.*?\\(.*?\\)]) (.*)");



  /**
   * Constructor.
   */
  public LogLine() {}

  /**
   * Constructor.
   */
  public LogLine(UUID harvestJobId, String line, int sequenceNumber) {

    Matcher matcher = logPattern.matcher(line);
    if (matcher.matches()) {
      record = new Record(
              UUID.randomUUID(),
              harvestJobId,
              matcher.group(1).trim(),
              matcher.group(2).trim(),
              matcher.group(3),
              matcher.group(4),
              sequenceNumber);
    } else {
      record = new Record(null, null, null, null, null, null, -1);
    }
  }

  /**
   * CREATE TABLE SQL template.
   */
  public String makeCreateTableSql(String schema) {
    return  "CREATE TABLE IF NOT EXISTS " + schema + "." + Tables.log_statement
        + "("
        + dbColumnName(ID) + " UUID PRIMARY KEY, "
        + dbColumnName(HARVEST_JOB_ID) + " UUID NOT NULL REFERENCES "
        + schema + "." + Tables.harvest_job + "(" + new HarvestJob().dbColumnName(ID) + "), "
        + dbColumnName(SEQUENCE_NUMBER) + " INTEGER NOT NULL, "
        + dbColumnName(TIME_STAMP) + " TIMESTAMP NOT NULL, "
        + dbColumnName(LOG_LEVEL) + " TEXT NOT NULL, "
        + dbColumnName(JOB_LABEL) + " TEXT NOT NULL, "
        + dbColumnName(LOG_STATEMENT) + " TEXT NOT NULL"
        + ")";
  }

  @Override
  public RowMapper<Entity> getRowMapper() {
    return row -> {
      record = new Record(
              row.getUUID(dbColumnName(ID)),
              row.getUUID(dbColumnName(HARVEST_JOB_ID)),
              // Present in original legacy harvester date format, not the pg date format (supports importing the output)
              row.getLocalDateTime(dbColumnName(TIME_STAMP)).toString()
                      .replace("T", " ").replace(".",","),
              row.getString(dbColumnName(LOG_LEVEL)),
              row.getString(dbColumnName(JOB_LABEL)),
              row.getString(dbColumnName(LOG_STATEMENT)),
              row.getInteger(dbColumnName(SEQUENCE_NUMBER)));
      return this;
    };
  }

  /**
   * INSERT INTO statement.
   */
  public String makeInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Tables.log_statement
        + " ("
        + dbColumnName(ID) + ", "
        + dbColumnName(HARVEST_JOB_ID) + ", "
        + dbColumnName(SEQUENCE_NUMBER) + ", "
        + dbColumnName(TIME_STAMP) + ", "
        + dbColumnName(LOG_LEVEL) + ", "
        + dbColumnName(JOB_LABEL) + ", "
        + dbColumnName(LOG_STATEMENT)
        + ")"
        + " VALUES ("
        + "#{" + dbColumnName(ID) + "}, "
        + "#{" + dbColumnName(HARVEST_JOB_ID) + "}, "
        + "#{" + dbColumnName(SEQUENCE_NUMBER) + "}, "
        + "TO_TIMESTAMP(#{" + dbColumnName(TIME_STAMP) + "},'" + DATE_FORMAT + "'), "
        + "#{" + dbColumnName(LOG_LEVEL) + "}, "
        + "#{" + dbColumnName(JOB_LABEL) + "}, "
        + "#{" + dbColumnName(LOG_STATEMENT) + "}"
        + ")";
  }

  /**
   * Creates a TupleMapper for input mapping.
   */
  public TupleMapper<Entity> getTupleMapper() {
    return TupleMapper.mapper(
        entity -> {
          LogLine logLine = (LogLine) entity;
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(dbColumnName(ID), record.id);
          parameters.put(dbColumnName(HARVEST_JOB_ID), record.harvestJobId);
          parameters.put(dbColumnName(SEQUENCE_NUMBER), record.sequenceNumber);
          parameters.put(dbColumnName(TIME_STAMP), record.timeStamp);
          parameters.put(dbColumnName(LOG_LEVEL), record.logLevel);
          parameters.put(dbColumnName(JOB_LABEL), record.jobLabel);
          parameters.put(dbColumnName(LOG_STATEMENT), record.line);
          return parameters;
        });
  }


  public SqlQuery makeSqlFromCqlQuery(RoutingContext routingContext, String schemaDotTable) {
    SqlQuery sql = super.makeSqlFromCqlQuery(routingContext, schemaDotTable);
    sql.withAdditionalOrderByField(dbColumnName(TIME_STAMP));
    sql.withAdditionalOrderByField(dbColumnName(SEQUENCE_NUMBER));
    return sql;
  }

  public String toString() {
    return record.toString();
    //return String.format("%s %-5s %s %s",this.timeStamp, this.logLevel, this.jobLabel, this.line);
  }

  @Override
  public Tables table() {
    return Tables.log_statement;
  }

  @Override
  public Entity fromJson(JsonObject json) {
    return null;
  }

  /**
   * Get log line as JSON.
   */
  public JsonObject asJson() {
    JsonObject json = new JsonObject();
    json.put(jsonPropertyName(ID), record.id);
    json.put(jsonPropertyName(TIME_STAMP), record.timeStamp);
    json.put(jsonPropertyName(LOG_LEVEL), record.logLevel);
    json.put(jsonPropertyName(JOB_LABEL), record.jobLabel);
    json.put(jsonPropertyName(LOG_STATEMENT), record.line);
    return json;
  }

}
