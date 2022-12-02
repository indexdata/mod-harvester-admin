package org.folio.harvesteradmin.moduledata;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.Map;
import org.folio.tlib.postgres.PgCqlDefinition;
import org.folio.tlib.postgres.PgCqlQuery;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldBase;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldNumber;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldText;
import org.folio.tlib.postgres.cqlfield.PgCqlFieldUuid;

public abstract class StoredEntity {

  public abstract String getCreateTableSql(String schema);

  public abstract RowMapper<StoredEntity> getSelectListMapper();

  public abstract TupleMapper<StoredEntity> getInsertValuesMapper();

  public abstract String getInsertTemplate(String schema);

  public abstract PgCqlDefinition getQueryableFields();

  public abstract Map<String,PgColumn> getFieldMap();

  /**
   * Gets a SQL query string.
   */
  public SqlQuery getSqlQueryFromRequest(RoutingContext routingContext, String schemaTable) {

    PgCqlDefinition definition = getQueryableFields();

    String fromDateTime = routingContext.request().getParam("from");
    String untilDateTime = routingContext.request().getParam("until");
    String offset = routingContext.request().getParam("offset");
    String limit = routingContext.request().getParam("limit");

    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    RequestParameter query = params.queryParameter("query");
    String select = "SELECT * ";
    String from = "FROM " + schemaTable;
    String where = "";
    String orderBy = "";
    if (query != null && !query.isEmpty()) {
      PgCqlQuery pgCqlQuery = definition.parse(query.getString());
      String whereClause = pgCqlQuery.getWhereClause();
      if (whereClause != null) {
        Map<String,PgColumn> prop2col = getFieldMap();
        for (String property  : prop2col.keySet()) {
          whereClause = whereClause.replaceAll(property.toLowerCase(), prop2col.get(property).name);
        }
        where = " WHERE " + whereClause;
      }
      if (fromDateTime != null) {
        where += " AND started >= '" + fromDateTime + "'";   // not generic
      }
      if (untilDateTime != null) {
        where += " AND started <= '" + untilDateTime + "'";  // not generic
      }
      String orderByClause = pgCqlQuery.getOrderByClause();
      if (orderByClause != null) {
        orderBy = " ORDER BY " + orderBy;
      }
    }
    return new SqlQuery(select, from, where, orderBy, offset, limit);
  }

  /**
   * Instantiates PG column definition.
   */
  public static PgColumn getColumnDefinition(
      String columnName, PgColumn.Type type, Boolean nullable, Boolean queryable) {
    return getColumnDefinition(columnName, type, nullable, queryable, false);
  }

  /**
   * Instantiates PG column definition.
   */
  public static PgColumn getColumnDefinition(
      String columnName, PgColumn.Type type, Boolean nullable, Boolean queryable,
      Boolean primaryKey) {
    PgCqlFieldBase cqlField = null;
    if (queryable) {
      switch (type) {
        case TEXT:
          cqlField = new PgCqlFieldText();
          break;
        case INTEGER:
          cqlField = new PgCqlFieldNumber();
          break;
        case UUID:
          cqlField = new PgCqlFieldUuid();
          break;
        default:
          cqlField = new PgCqlFieldText();
      }
    }
    return new PgColumn(columnName, type, nullable, cqlField, primaryKey);
  }

}
