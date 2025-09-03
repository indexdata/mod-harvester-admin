package org.folio.harvesteradmin.moduledata;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatedRequest;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.TupleMapper;
import java.util.List;
import java.util.Map;

import org.folio.harvesteradmin.moduledata.database.SqlQuery;
import org.folio.tlib.postgres.PgCqlDefinition;
import org.folio.tlib.postgres.PgCqlQuery;

public abstract class StoredEntity {

  /**
   * Builds the Postgres DDL SQLs for creating the table and indexes.
   */
  public abstract List<String> makeCreateSqls(String schema);

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
  public abstract Map<String, PgColumn> getFieldMap();

  /**
   * Gets a SQL query string.
   */
  public SqlQuery makeSqlFromCqlQuery(RoutingContext routingContext, String schemaDotTable) {
    PgCqlDefinition definition = getQueryableFields();
    ValidatedRequest validatedRequest =
            routingContext.get(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST);
    Map<String, io.vertx.openapi.validation.RequestParameter> params = validatedRequest.getQuery();
    RequestParameter query = params != null ? params.get("query") : null;
    RequestParameter offset =  params != null ? params.get("offset") : null;
    RequestParameter limit = params != null ? params.get("limit") : null;

    String select = "SELECT * ";
    String from = "FROM " + schemaDotTable;
    String whereClause = "";
    String orderByClause = "";
    if (query != null && !query.isEmpty()) {
      PgCqlQuery pgCqlQuery = definition.parse(query.getString());
      if (pgCqlQuery.getWhereClause() != null) {
        whereClause = jsonPropertiesToColumnNames(pgCqlQuery.getWhereClause());
      }
      if (pgCqlQuery.getOrderByClause() != null) {
        orderByClause = jsonPropertiesToColumnNames(pgCqlQuery.getOrderByClause());
      }
    }
    return new SqlQuery(select, from, whereClause, orderByClause, offset, limit);
  }

  /**
   * Crosswalk JSON property names to table column names.
   * @param clause string containing names to translate
   * @return translated string
   */
  private String jsonPropertiesToColumnNames(String clause) {
    if (clause != null) {
      Map<String, PgColumn> prop2col = getFieldMap();
      for (String property : prop2col.keySet()) {
        clause = clause.replaceAll(property.toLowerCase(), prop2col.get(property).name);
      }
    }
    return clause;
  }

}
