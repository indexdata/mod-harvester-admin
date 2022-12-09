package org.folio.harvesteradmin.moduledata;

public class SqlQuery {

  private final String select;
  private final String from;
  private String where;
  private final String orderBy;
  private final String offset;
  private final String limit;

  /**
   * Constructor.
   */
  public SqlQuery(String select, String from, String where, String orderBy,
                  String offset, String limit) {
    this.select = select;
    this.from = from;
    this.where = where != null ? where : "";
    this.orderBy = orderBy != null ? orderBy : "";
    this.offset = offset;
    this.limit = limit;
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
        + where
        + orderBy
        + limits(offset, limit);
  }

  /**
   * Adds ANDed where clause to query.
   */
  public SqlQuery withAdditionalWhereClause(String clause) {
    if (clause != null && !clause.isEmpty()) {
      if (where.isEmpty()) {
        where = " where (" + clause + ")";
      } else {
        where += " AND " + "(" + clause + ") ";
      }
    }
    return this;
  }

  /**
   * Applies offset and limit if any.
   */
  public static String limits(String offset, String limit) {
    return
        (offset == null || offset.isEmpty() ? "" : " offset " + offset)
            + (limit == null || limit.isEmpty() ? "" : " limit " + limit);
  }

  public String toString() {
    return getQueryWithLimits();
  }
}
