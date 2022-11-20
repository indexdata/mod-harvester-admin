package org.folio.harvesteradmin.moduledata;

import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;

public abstract class StoredEntity {

  public abstract String getCreateTableSql(String schema);

  public abstract RowMapper<StoredEntity> getSelectListMapper();

  public abstract TupleMapper<StoredEntity> getInsertValuesMapper();

  public abstract String getInsertTemplate(String schema);
}
