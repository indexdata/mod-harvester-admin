package org.folio.harvesteradmin.moduledata;

import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import org.folio.harvesteradmin.modulestorage.Storage;

public class RecordFailure extends StoredEntity {

  public enum Column {
    id,
    record_errors,
    original_record,
    transformed_record
  }

  public static LogLine entity() {
    return new LogLine();
  }

  /**
   * CREATE TABLE statement.
   */
  public String getCreateTableSql(String schema) {
    return "CREATE TABLE IF NOT EXISTS " + schema + "." + Storage.Table.record_failure
        + "("
        + Column.id + " UUID PRIMARY KEY, "
        + Column.record_errors + " JSONB NOT NULL, "
        + Column.original_record + " TEXT NOT NULL, "
        + Column.transformed_record + " JSONB NOT NULL"
        + ")";
  }

  @Override
  public RowMapper<StoredEntity> getSelectListMapper() {
    return null;
  }

  @Override
  public TupleMapper<StoredEntity> getInsertValuesMapper() {
    return null;
  }

  @Override
  public String getInsertTemplate(String schema) {
    return "INSERT INTO " + schema + "." + Storage.Table.harvest_job
        + " ("
        + Column.id + ", "
        + Column.record_errors + ", "
        + Column.original_record + ", "
        + Column.transformed_record
        + ")"
        + " VALUES ("
        + "#{" + Column.id + "}, "
        + "#{" + Column.record_errors + "}, "
        + "#{" + Column.original_record + "}, "
        + "#{" + Column.transformed_record + "}"
        + ")";
  }

}

