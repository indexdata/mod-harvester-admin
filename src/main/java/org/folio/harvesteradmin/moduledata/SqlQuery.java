package org.folio.harvesteradmin.moduledata;

import io.vertx.ext.web.validation.RequestParameter;

public class SqlQuery {

  private final String select;
  private final String from;
  private String where;
  private String orderBy;
  private final String offset;
  private final String limit;
  private String defaultLimit = null;

  /**
   * Constructor.
   */
  public SqlQuery(String select, String from, String where, String orderBy,
                  RequestParameter offset, RequestParameter limit) {
    this.select = select;
    this.from = from;
    this.where = where != null ? where : "";
    this.orderBy = orderBy != null ? orderBy : "";
    this.offset = offset != null ? offset.toString() : null;
    this.limit = limit != null ? limit.toString() : null;
  }

  /**
   * Gets count query.
   */
  public String getCountingSql() {
    return "SELECT COUNT(*) as total_records "
        + from
        + where;
  }

  /**
   * Gets SQL with limits applied.
   */
  public String getQueryWithLimits() {
    return select
        + from
        + (where.isEmpty() ? "" : " WHERE " + where)
        + (orderBy.isEmpty() ? "" : " ORDER BY " + orderBy)
        + limits(offset, (limit == null ? defaultLimit : limit));
  }

  /**
   * Adds ANDed where clause to query.
   */
  public SqlQuery withAdditionalWhereClause(String clause) {
    if (clause != null && !clause.isEmpty()) {
      if (where.isEmpty()) {
        where = " (" + clause + ")";
      } else {
        where += " AND " + "(" + clause + ") ";
      }
    }
    return this;
  }

  public SqlQuery withAdditionalOrderByField(String clause) {
    if (clause != null && !clause.isEmpty()) {
      if (orderBy.isEmpty()) {
        orderBy = clause;
      } else {
        orderBy += ", " + clause;
      }
    }
    return this;
  }

  public SqlQuery withDefaultLimit(String defaultLimit) {
    this.defaultLimit = defaultLimit;
    return this;
  }

  /**
   * Applies offset and limit if any.
   */
  private static String limits(String offset, String limit) {
    return
        (offset == null || offset.isEmpty() ? "" : " offset " + offset)
            + (limit == null || limit.isEmpty() ? "" : " limit " + limit);
  }

  public String toString() {
    return getQueryWithLimits();
  }
}
