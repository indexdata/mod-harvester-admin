package org.folio.harvesteradmin.moduledata;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.tlib.postgres.PgCqlDefinition;
import org.folio.tlib.postgres.PgCqlQuery;

public abstract class StoredEntity {

  public abstract String getCreateTableSql(String schema);

  public abstract RowMapper<StoredEntity> getSelectListMapper();

  public abstract TupleMapper<StoredEntity> getInsertValuesMapper();

  public abstract String getInsertTemplate(String schema);

  public abstract PgCqlDefinition getQueryableFields();

  public abstract Map<String,String> getPropertyColumnMap();

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
        Map<String,String> prop2col = getPropertyColumnMap();
        for (String property  : prop2col.keySet()) {
          whereClause = whereClause.replaceAll(property.toLowerCase(), prop2col.get(property));
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

}
