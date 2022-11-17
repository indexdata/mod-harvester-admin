package org.folio.harvesteradmin.modulestorage;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.templates.SqlTemplate;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.moduledata.HarvestJob;
import org.folio.harvesteradmin.moduledata.LogLine;
import org.folio.tlib.postgres.TenantPgPool;

public class Storage {
  TenantPgPool pool;
  private static final Logger logger = LogManager.getLogger(Storage.class);


  public enum Table {
    harvest_job,
    log_statement
  }


  public Storage(Vertx vertx, String tenant) {
    pool = TenantPgPool.pool(vertx, tenant);
  }

  public String schemaTable(Table table) {
    return pool.getSchema() + "." + table.name();
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
    Promise<Void> promise = Promise.promise();
    @SuppressWarnings("rawtypes") List<Future> tables = new ArrayList<>();
    tables.add(pool.query(HarvestJob.getCreateTableSql(pool.getSchema())).execute().mapEmpty());
    tables.add(pool.query(LogLine.getCreateTableSql(pool.getSchema())).execute().mapEmpty());
    CompositeFuture.all(tables).onComplete(creates -> promise.complete());
    return promise.future();
  }

  /* Template for processing parameters in init.
  JsonArray parameters = tenantAttributes.getJsonArray("parameters");
  if (parameters != null) {
    for (int i = 0; i < parameters.size(); i++) {
      JsonObject parameter = parameters.getJsonObject(i);
      if ("loadSample".equals(parameter.getString("key"))
          && "true".equals(parameter.getString("value"))) {
      }
    }
  }
  */


  /**
   * Stores a harvest job.
   */
  public Future<UUID> storeHarvestJob(HarvestJob harvestJob) {
    return SqlTemplate.forUpdate(pool.getPool(),
            HarvestJob.getInsertTemplate(pool.getSchema()))
        .mapFrom(HarvestJob.getInsertValuesMapper())
        .execute(harvestJob)
        .onSuccess(res -> logger.info("Saved harvest job"))
        .onFailure(res -> logger.error("Couldn't save harvest job: " + res.getMessage()))
        .map(harvestJob.id());
  }

  /**
   * Stores log statements.
   */
  public Future<Void> storeLogStatements(UUID harvestJobId, String log)  {
    if (log != null) {
      BufferedReader bufReader = new BufferedReader(new StringReader(log));
      try {
        List<LogLine> logLines = new ArrayList<>();
        String line;
        int sequence = 0;
        while ((line = bufReader.readLine()) != null) {
          sequence++;
          logLines.add(new LogLine(harvestJobId, line, sequence));
        }
        return SqlTemplate.forUpdate(pool.getPool(),
            LogLine.getInsertTemplate(pool.getSchema()))
            .mapFrom(LogLine.getInsertValuesMapper())
            .executeBatch(logLines)
            .onFailure(res -> logger.error("Didn't save log lines: " + res.getMessage()))
            .mapEmpty();

      } catch (IOException ioe) {
        return Future.succeededFuture();
      }
    } else {
      logger.info("No logs found for harvest job " + harvestJobId);
      return Future.succeededFuture();
    }
  }

  /**
   * Gets previous jobs from module's storage.
   */
  public Future<List<HarvestJob>> getPreviousJobs() {
    List<HarvestJob> previousJobs = new ArrayList<>();
    return SqlTemplate.forQuery(pool.getPool(),
            "SELECT * "
                + "FROM " + schemaTable(Table.harvest_job))
        .mapTo(HarvestJob.getSelectListMapper())
        .execute(null)
        .onSuccess(rows -> rows.forEach(previousJobs::add)).map(previousJobs);
  }

  /**
   * Retrieves past harvest job.
   */
  public Future<HarvestJob> getPreviousJobById(UUID id) {
    return SqlTemplate.forQuery(pool.getPool(),
            "SELECT * "
                + "FROM " + schemaTable(Table.harvest_job) + " "
                + "WHERE id = #{id}")
        .mapTo(HarvestJob.getSelectListMapper())
        .execute(Collections.singletonMap("id", id))
        .map(rows -> {
          RowIterator<HarvestJob> iterator = rows.iterator();
          return iterator.hasNext() ? iterator.next() : null;
        });
  }

  /**
   * Retrieves log for past harvest job.
   */
  public Future<String> getPreviousJobLog(UUID id) {
    Promise<String> promise = Promise.promise();
    final StringBuilder log = new StringBuilder();
    SqlTemplate.forQuery(pool.getPool(),
            "SELECT statement "
                + "FROM " + schemaTable(Table.log_statement) + " "
                + "WHERE harvest_job_id = #{id} "
                + "ORDER BY seq")
        .execute(Collections.singletonMap("id", id))
        .onComplete(rows -> {
          if (rows.succeeded()) {
            rows.result().forEach(row -> log.append(row.getString(LogLine.Column.statement.name()))
                .append(System.lineSeparator()));
            promise.complete(log.toString());
          }
        });
    return promise.future();
  }

  /*
   Template for creating query:
      private String createQueryMyTable(RoutingContext ctx, TenantPgPool pool) {
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
      }
   */

}
