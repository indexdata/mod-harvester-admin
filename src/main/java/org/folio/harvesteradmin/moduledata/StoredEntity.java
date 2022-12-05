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

  Logger logger = LogManager.getLogger(StoredEntity.class);

  /**
   * Builds the Postgres DDL SQL for creating a table.
   */
  public abstract String makeCreateTableSql(String schema);

  /**
   * Gets vert.x row mapper: Postgres select list results mapped to i.e. JSON or text strings.
   */
  public abstract RowMapper<StoredEntity> getRowMapper();

  /**
   * Gets vert.x tuple mapper - Postgres insert values list, ie from JSON.
   */
  public abstract TupleMapper<StoredEntity> getTupleMapper();

  /**
   * Vert.x / Postgres template for table insert.
   */
  public abstract String makeInsertTemplate(String schema);

  /**
   * Gets Postgres/CQL definition, containing listing of queryable fields.
   */
  public abstract PgCqlDefinition getQueryableFields();

  /**
   * Map of JSON property names to Postgres table column definitions (PgColumns).
   */
  public abstract Map<String,PgColumn> getFieldMap();

  /**
   * Gets a SQL query string.
   */
  public SqlQuery makeSqlFromCqlQuery(RoutingContext routingContext, String schemaDotTable) {

    PgCqlDefinition definition = getQueryableFields();

    String offset = routingContext.request().getParam("offset");
    String limit = routingContext.request().getParam("limit");

    RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    RequestParameter query = params.queryParameter("query");
    String select = "SELECT * ";
    String from = "FROM " + schemaDotTable;
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
      String orderByClause = pgCqlQuery.getOrderByClause();
      if (orderByClause != null) {
        orderBy = " ORDER BY " + orderBy;
      }
    }
    return new SqlQuery(select, from, where, orderBy, offset, limit);
  }


}
