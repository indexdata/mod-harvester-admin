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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.moduledata.HarvestJob;
import org.folio.harvesteradmin.moduledata.HarvestJobField;
import org.folio.harvesteradmin.moduledata.LogLine;
import org.folio.harvesteradmin.moduledata.RecordFailure;
import org.folio.harvesteradmin.moduledata.SqlQuery;
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

  public String schemaDotTable(Table table) {
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
    for (StoredEntity entity : Arrays.asList(
        HarvestJob.entity(),
        LogLine.entity(),
        RecordFailure.entity())) {
      tables.add(pool.query(entity.makeCreateTableSql(pool.getSchema())).execute().mapEmpty());
    }
    CompositeFuture.all(tables).onComplete(
        creates -> {
          if (creates.succeeded()) {
            promise.complete();
          } else {
            promise.fail(creates.cause().getMessage());
          }
        });
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
            harvestJob.makeInsertTemplate(pool.getSchema()))
        .mapFrom(harvestJob.getTupleMapper())
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
            LogLine.entity().makeInsertTemplate(pool.getSchema()))
            .mapFrom(LogLine.entity().getTupleMapper())
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
          RecordFailure.entity().makeInsertTemplate(pool.getSchema()))
          .mapFrom(RecordFailure.entity().getTupleMapper())
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
        .mapTo(HarvestJob.entity().getRowMapper())
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
                + "FROM " + schemaDotTable(Table.harvest_job) + " "
                + "WHERE id = #{id}")
        .mapTo(HarvestJob.entity().getRowMapper())
        .execute(Collections.singletonMap("id", id))
        .map(rows -> {
          RowIterator<StoredEntity> iterator = rows.iterator();
          return iterator.hasNext() ? (HarvestJob) iterator.next() : null;
        });
  }

  /**
   * Retrieves log for past harvest job.
   */
  public Future<String> getLogsForPreviousJob(UUID id, SqlQuery queryFromCql) {
    Promise<String> promise = Promise.promise();
    final StringBuilder log = new StringBuilder();
    String query = queryFromCql.withAdditionalWhereClause("harvest_job_id = #{id}").toString();
    SqlTemplate.forQuery(pool.getPool(), query)
        .mapTo(LogLine.entity().getRowMapper())
        .execute(Collections.singletonMap("id", id))
        .onSuccess(rows -> {
          for (StoredEntity entity : rows) {
            log.append(entity).append(System.lineSeparator());
          }
          promise.complete(log.toString());
        })
        .onFailure(
            rows -> promise.fail(rows.getMessage())
        );
    return promise.future();
  }

  /**
   * Retrieves failed records for past harvest job.
   */
  public Future<List<RecordFailure>> getFailedRecordsForPreviousJob(
      UUID id, String offset, String limit) {
    List<RecordFailure> recordFailures = new ArrayList<>();
    return SqlTemplate.forQuery(pool.getPool(),
            "SELECT * "
                + "FROM " + schemaDotTable(Table.record_failure) + " "
                + "WHERE harvest_job_id = #{id} "
                + SqlQuery.limits(offset, limit))
        .mapTo(RecordFailure.entity().getRowMapper())
        .execute(Collections.singletonMap("id", id))
        .onSuccess(rows -> {
          for (StoredEntity entity : rows) {
            recordFailures.add((RecordFailure) entity);
          }
        }).map(recordFailures);
  }

  /**
   * Retrieves failed records for past harvest job.
   */
  public Future<RecordFailure> getFailedRecordForPreviousJob(UUID id) {
    Promise<RecordFailure> promise = Promise.promise();
    SqlTemplate.forQuery(pool.getPool(),
            "SELECT * "
                + "FROM " + schemaDotTable(Table.record_failure) + " "
                + "WHERE id = #{id} ")
        .mapTo(RecordFailure.entity().getRowMapper())
        .execute(Collections.singletonMap("id", id))
        .onComplete(rows -> {
          if (rows.succeeded()) {
            if (rows.result().size() > 0) {
              promise.complete((RecordFailure) rows.result().iterator().next());
            } else {
              promise.fail("No failed record found with ID " + id.toString());
            }
          } else {
            promise.fail("There was a problem getting failed record by ID "
                + rows.cause().getMessage());
          }
        });
    return promise.future();
  }

  /**
   * Deletes the history of a previous job run.
   */
  public Future<Void> deletePreviousJob(UUID id) {
    Promise<Void> promise = Promise.promise();
    SqlTemplate.forUpdate(pool.getPool(),
        "DELETE FROM " + schemaDotTable(Table.log_statement)
            + " WHERE " + LogLine.LogLineField.HARVEST_JOB_ID + " = #{id} ")
        .execute(Collections.singletonMap("id", id))
        .onComplete(deletedLogs -> {
          if (deletedLogs.succeeded()) {
            SqlTemplate.forUpdate(pool.getPool(),
                "DELETE FROM " + schemaDotTable(Table.record_failure)
                + " WHERE " + RecordFailure.Column.harvest_job_id + " = #{id} ")
                .execute(Collections.singletonMap("id", id))
                .onComplete(deletedFailedRecords -> {
                  if (deletedFailedRecords.succeeded()) {
                    SqlTemplate.forUpdate(pool.getPool(),
                        "DELETE FROM " + schemaDotTable(Table.harvest_job)
                        + " WHERE " + HarvestJobField.ID + " = #{id} ")
                        .execute(Collections.singletonMap("id", id))
                        .onComplete(deletedJobRun -> {
                          if (deletedJobRun.succeeded()) {
                            promise.complete();
                          } else {
                            promise.fail("Could not delete the job run with ID " + id
                                + deletedJobRun.cause().getMessage());
                          }
                        });
                  } else {
                    promise.fail("Could not delete the job run with ID " + id
                        + " because deletion of its failed records failed: "
                        + deletedFailedRecords.cause().getMessage());
                  }
                });
          } else {
            promise.fail("Could not delete the job run with ID " + id
                + " because deletion of its logs failed: "
                + deletedLogs.cause().getMessage());
          }
        });
    return promise.future();
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
          if (rows.result() != null) {
            promise.complete(rows.result().iterator().next());
          } else {
            promise.fail("No result from counting by " + sql);
          }
        });
    return promise.future();
  }

  private RowMapper<Integer> countingMapper() {
    return row -> row.getInteger("total_records");
  }
}
