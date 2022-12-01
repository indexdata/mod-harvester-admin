package org.folio.harvesteradmin.modulestorage;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.templates.RowMapper;
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
import org.folio.harvesteradmin.moduledata.RecordFailure;
import org.folio.harvesteradmin.moduledata.StoredEntity;
import org.folio.tlib.postgres.TenantPgPool;

public class Storage {
  TenantPgPool pool;
  private static final Logger logger = LogManager.getLogger(Storage.class);

  public enum Table {
    harvest_job,
    log_statement,
    record_failure
  }

  public Storage(Vertx vertx, String tenant) {
    pool = TenantPgPool.pool(vertx, tenant);
  }

  public String schemaTable(Table table) {
    return pool.getSchema() + "." + table.name();
  }

  public String schema() {
    return pool.getSchema();
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
    final Promise<Void> promise = Promise.promise();
    @SuppressWarnings("rawtypes") List<Future> tables = new ArrayList<>();
    tables.add(
        pool.query(
            HarvestJob.entity().getCreateTableSql(pool.getSchema())).execute().mapEmpty());
    tables.add(
        pool.query(
            LogLine.entity().getCreateTableSql(pool.getSchema())).execute().mapEmpty());
    tables.add(
        pool.query(
            RecordFailure.entity().getCreateTableSql(pool.getSchema())).execute().mapEmpty());
    CompositeFuture.all(tables).onComplete(creates -> promise.complete());
    return promise.future();

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
  }

  /**
   * Stores a harvest job.
   */
  public Future<UUID> storeHarvestJob(HarvestJob harvestJob) {
    return SqlTemplate.forUpdate(pool.getPool(),
            harvestJob.getInsertTemplate(pool.getSchema()))
        .mapFrom(harvestJob.getInsertValuesMapper())
        .execute(harvestJob)
        .onSuccess(res -> logger.info("Saved harvest job"))
        .onFailure(res -> logger.error("Couldn't save harvest job: " + res.getMessage()))
        .map(harvestJob.getId());
  }

  /**
   * Stores log statements.
   */
  public Future<Void> storeLogStatements(UUID harvestJobId, String log)  {
    if (log != null) {
      BufferedReader bufReader = new BufferedReader(new StringReader(log));
      try {
        List<StoredEntity> logLines = new ArrayList<>();
        String line;
        int sequence = 0;
        int nonMatches = 0;
        long startParse = System.currentTimeMillis();
        while ((line = bufReader.readLine()) != null) {
          if (line.length() > 100) {
            LogLine logLine = new LogLine(harvestJobId, line, ++sequence);
            if (logLine.getId() != null) {
              logLines.add(new LogLine(harvestJobId, line, sequence));
            } else {
              logger.error("Could not parse " + line);
              nonMatches++;
            }
          }
        }
        logger.info("Parsed " + sequence + " log lines in "
            + (System.currentTimeMillis() - startParse) / 1000 + " seconds."
            + (nonMatches > 0
            ? "There were " + nonMatches + " log statements that could not be parsed!" : ""));
        return SqlTemplate.forUpdate(pool.getPool(),
            LogLine.entity().getInsertTemplate(pool.getSchema()))
            .mapFrom(LogLine.entity().getInsertValuesMapper())
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
   * Stores failed records.
   */
  public Future<Void> storeFailedRecords(UUID harvestJobId, JsonArray failedRecords) {
    if (failedRecords != null && ! failedRecords.isEmpty()) {
      List<StoredEntity> list = new ArrayList<>();
      for (Object rec : failedRecords) {
        JsonObject failedRecord = (JsonObject) rec;
        list.add(RecordFailure.fromLegacyHarvesterJson(harvestJobId, failedRecord));
      }
      return SqlTemplate.forUpdate(pool.getPool(),
          RecordFailure.entity().getInsertTemplate(pool.getSchema()))
          .mapFrom(RecordFailure.entity().getInsertValuesMapper())
          .executeBatch(list)
          .onFailure(res -> logger.error("Didn't save record failures: " + res.getMessage()))
          .mapEmpty();

    } else {
      logger.info("No failure records to store for harvest job " + harvestJobId);
      return Future.succeededFuture();
    }
  }

  /**
   * Gets previous jobs from module's storage.
   */
  public Future<List<HarvestJob>> getPreviousJobs(String query) {
    List<HarvestJob> previousJobs = new ArrayList<>();
    return SqlTemplate.forQuery(pool.getPool(), query)
        .mapTo(HarvestJob.entity().getSelectListMapper())
        .execute(null)
        .onSuccess(rows -> {
          for (StoredEntity entity : rows) {
            previousJobs.add((HarvestJob) entity);
          }
        }).map(previousJobs);
  }

  /**
   * Retrieves past harvest job.
   */
  public Future<HarvestJob> getPreviousJobById(UUID id) {
    return SqlTemplate.forQuery(pool.getPool(),
            "SELECT * "
                + "FROM " + schemaTable(Table.harvest_job) + " "
                + "WHERE id = #{id}")
        .mapTo(HarvestJob.entity().getSelectListMapper())
        .execute(Collections.singletonMap("id", id))
        .map(rows -> {
          RowIterator<StoredEntity> iterator = rows.iterator();
          return iterator.hasNext() ? (HarvestJob) iterator.next() : null;
        });
  }

  /**
   * Retrieves log for past harvest job.
   */
  public Future<String> getLogsForPreviousJob(UUID id) {
    Promise<String> promise = Promise.promise();
    final StringBuilder log = new StringBuilder();
    SqlTemplate.forQuery(pool.getPool(),
            "SELECT * "
                + "FROM " + schemaTable(Table.log_statement) + " "
                + "WHERE harvest_job_id = #{id} "
                + "ORDER BY seq")
        .mapTo(LogLine.entity().getSelectListMapper())
        .execute(Collections.singletonMap("id", id))
        .onSuccess(rows -> {
          for (StoredEntity entity : rows) {
            log.append(entity).append(System.lineSeparator());
          }
          promise.complete(log.toString());
        });
    return promise.future();
  }

  public Future<String> getLogsForPreviousJobs(String query, String from, String until) {
    return null;
  }

  /**
   * Retrieves failed records for past harvest job.
   */
  public Future<List<RecordFailure>> getFailedRecordsForPreviousJob(UUID id) {
    List<RecordFailure> recordFailures = new ArrayList<>();
    return SqlTemplate.forQuery(pool.getPool(),
            "SELECT * "
                + "FROM " + schemaTable(Table.record_failure) + " "
                + "WHERE harvest_job_id = #{id} ")
        .mapTo(RecordFailure.entity().getSelectListMapper())
        .execute(Collections.singletonMap("id", id))
        .onSuccess(rows -> {
          for (StoredEntity entity : rows) {
            recordFailures.add((RecordFailure) entity);
          }
        }).map(recordFailures);
  }

  /**
   * Gets record count.
   */
  public Future<Integer> getCount(String sql) {
    Promise<Integer> promise = Promise.promise();
    SqlTemplate.forQuery(pool.getPool(), sql)
        .mapTo(countingMapper())
        .execute(null)
        .onComplete(rows -> {
          promise.complete(rows.result().iterator().next());
        });
    return promise.future();
  }

  private RowMapper<Integer> countingMapper() {
    return row -> row.getInteger("total_records");
  }
}
