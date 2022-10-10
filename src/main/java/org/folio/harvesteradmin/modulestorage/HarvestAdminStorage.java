package org.folio.harvesteradmin.modulestorage;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.templates.SqlTemplate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.harvesteradmin.moduledata.Book;
import org.folio.harvesteradmin.moduledata.BookRowMapper;
import org.folio.tlib.postgres.TenantPgPool;

public class HarvestAdminStorage {
  TenantPgPool pool;

  public HarvestAdminStorage(Vertx vertx, String tenant) {
    pool = TenantPgPool.pool(vertx, tenant);
  }

  private String getMyTable(TenantPgPool pool) {
    return pool.getSchema() + ".mytable";
  }

  /**
   * tenant init handling (including disable).
   *
   * @param tenantAttributes as passed in tenant init
   * @return async result.
   */
  public Future<Void> init(JsonObject tenantAttributes) {
    if (!tenantAttributes.containsKey("module_to")) {
      return Future.succeededFuture(); // doing nothing for disable
    }
    Future<Void> future = pool.query(
            "CREATE TABLE IF NOT EXISTS " + getMyTable(pool)
                + "(id UUID PRIMARY KEY, title TEXT, index_title TEXT)")
        .execute().mapEmpty();
    JsonArray parameters = tenantAttributes.getJsonArray("parameters");
    if (parameters != null) {
      for (int i = 0; i < parameters.size(); i++) {
        JsonObject parameter = parameters.getJsonObject(i);
        if ("loadSample".equals(parameter.getString("key"))
            && "true".equals(parameter.getString("value"))) {
          for (String title : List.of("First title", "Second title")) {
            Book book = new Book();
            book.setTitle(title);
            book.setIndexTitle(title.toLowerCase());
            book.setId(UUID.randomUUID());
            future = future.compose(x -> postBook(book));
          }
        }
      }
    }
    return future;
  }


  /**
   * Get book from identifier.
   *
   * @param id identifier
   * @return async with Book == null if not found
   */
  public Future<Book> getBook(UUID id) {
    return SqlTemplate.forQuery(pool.getPool(), "SELECT * FROM " + getMyTable(pool)
            + " WHERE id=#{id}")
        .mapTo(BookRowMapper.INSTANCE)
        .execute(Collections.singletonMap("id", id))
        .map(rowSet -> {
          RowIterator<Book> iterator = rowSet.iterator();
          return iterator.hasNext() ? iterator.next() : null;
        });
  }

  /**
   * Create book.
   *
   * @param book the book to add.
   * @return async result.
   */
  public Future<Void> postBook(Book book) {
    return SqlTemplate.forUpdate(pool.getPool(), "INSERT INTO " + getMyTable(pool)
            + " VALUES (#{id},#{title},#{indexTitle})")
        .mapFrom(Book.class)
        .execute(book)
        .mapEmpty();
  }

  /**
   * Create SQL query for books.
   *
   * @param ctx  routing context from HTTP request
   * @param pool PostgresQL Pool
   * @return async result
   */
  private String createQueryMyTable(RoutingContext ctx, TenantPgPool pool) {
    /*
    RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    PgCqlQuery pgCqlQuery = PgCqlQuery.query();
    RequestParameter query = params.queryParameter("query");
    pgCqlQuery.parse(query == null ? null : query.getString());
    pgCqlQuery.addField(new PgCqlField("cql.allRecords", PgCqlField.Type.ALWAYS_MATCHES));
    pgCqlQuery.addField(new PgCqlField("id", PgCqlField.Type.UUID));
    pgCqlQuery.addField(new PgCqlField("title", PgCqlField.Type.FULLTEXT));
    String sql = "SELECT * FROM " + getMyTable(pool);
    String where = pgCqlQuery.getWhereClause();
    if (where != null) {
      sql = sql + " WHERE " + where;
    }
    String orderBy = pgCqlQuery.getOrderByClause();
    if (orderBy != null) {
      sql = sql + " ORDER BY " + orderBy;
    }
    return sql;

     */
    return null;
  }


  /**
   * Get books with optional CQL query.
   *
   * @param ctx routing context for HTTP request
   * @return async result with books list
   */
  public Future<List<Book>> getBooks(RoutingContext ctx) {
    /*
    String sql = createQueryMyTable(ctx, pool);
    return SqlTemplate.forQuery(pool.getPool(), sql)
        .mapTo(BookRowMapper.INSTANCE)
        .execute(Collections.emptyMap())
        .map(rowSet -> {
          List<Book> books = new LinkedList<>();
          rowSet.forEach(books::add);
          return books;
        });

     */
    return Future.succeededFuture(null);
  }

}
