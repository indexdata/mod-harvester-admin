package org.folio.harvesteradmin.moduledata.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.templates.SqlTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.moduledata.*;
import org.folio.tlib.postgres.TenantPgPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.folio.harvesteradmin.moduledata.HarvestJob.ID;
import static org.folio.harvesteradmin.moduledata.HarvestJob.STARTED;
import static org.folio.harvesteradmin.moduledata.LogLine.HARVEST_JOB_ID;

public class ModuleStorageAccess {
    TenantPgPool pool;
    private static final Logger logger = LogManager.getLogger(ModuleStorageAccess.class);


    /**
     * Constructor.
     */
    public ModuleStorageAccess(Vertx vertx, String tenant) {
        pool = TenantPgPool.pool(vertx, tenant);
    }

    public String schemaDotTable(Tables table) {
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
        } else {
            return DatabaseInit.createDatabase(pool);
        }
    }

    public Future<Step> getStepById(UUID id) {
        return SqlTemplate.forQuery(pool.getPool(),
                        "SELECT * "
                                + "FROM " + schemaDotTable(Tables.step) + " "
                                + "WHERE id = #{id}")
                .mapTo(new Step().getRowMapper())
                .execute(Collections.singletonMap("id", id))
                .map(rows -> {
                    RowIterator<Entity> iterator = rows.iterator();
                    return iterator.hasNext() ? (Step) iterator.next() : null;
                });
    }

    public Future<String> getScript(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        return getScript(UUID.fromString(id));
    }

    public Future<String> getScript(UUID stepId) {
        Promise<String> promise = Promise.promise();
        getStepById(stepId).onComplete(step -> {
            if (step.result() != null) {
                String script = step.result().record.script();
                script = script.replaceAll("\\r[\\n]?", System.lineSeparator());
                promise.complete(script);
            } else {
                promise.fail("Did not find step with ID " + stepId + " to GET script from");
            }
        });
        return promise.future();
    }

    public Future<UUID> storeEntity(Entity entity) {
        return SqlTemplate.forUpdate(pool.getPool(),
                        entity.makeInsertTemplate(pool.getSchema()))
                .mapFrom(entity.getTupleMapper())
                .execute(entity)
                .onSuccess(res -> logger.info("Saved " + entity.entityName().toLowerCase()))
                .onFailure(res -> logger.error("Couldn't save " + entity.entityName().toLowerCase() + ": " + res.getMessage()))
                .map(UUID.fromString(entity.asJson().getString("id")));
    }

    public Future<Void> storeEntities(Entity definition, List<Entity> entities) {
        if (entities!=null && !entities.isEmpty()) {
            return SqlTemplate.forUpdate(pool.getPool(),
                            definition.makeInsertTemplate(pool.getSchema()))
                    .mapFrom(definition.getTupleMapper())
                    .executeBatch(entities)
                    .onSuccess(res -> logger.info("Saved batch of " + definition.entityName().toLowerCase()))
                    .onFailure(res -> logger.error("Couldn't save batch of " + definition.entityName().toLowerCase() + ": " + res.getMessage()))
                    .mapEmpty();
        } else {
            return Future.succeededFuture();
        }
    }


    public Future<List<Entity>> getEntities(String query, Entity definition) {
        List<Entity> records = new ArrayList<>();
        return SqlTemplate.forQuery(pool.getPool(), query)
                .mapTo(definition.getRowMapper())
                .execute(null)
                .onSuccess(rows -> {
                    for (Entity record : rows) {
                        records.add(record);
                    }
                }).map(records);
    }

    public Future<Entity> getEntityById(UUID id, Entity definition) {
        return SqlTemplate.forQuery(pool.getPool(),
                        "SELECT * "
                                + "FROM " + schemaDotTable(definition.table()) + " "
                                + "WHERE id = #{id}")
                .mapTo(definition.getRowMapper())
                .execute(Collections.singletonMap("id", id))
                .map(rows -> {
                    RowIterator<Entity> iterator = rows.iterator();
                    return iterator.hasNext() ? iterator.next() : null;
                });
    }

    /**
     * Stores a harvest job.
     */
    public Future<UUID> storeHarvestJob(HarvestJob harvestJob) {
        return storeEntity(harvestJob);
    }

    /**
     * Stores log statements.
     */
    public Future<Void> storeLogStatements(UUID harvestJobId, String log) {
        if (log != null) {
            BufferedReader bufReader = new BufferedReader(new StringReader(log));
            try {
                List<Entity> logLines = new ArrayList<>();
                String line;
                int sequence = 0;
                int nonMatches = 0;
                long startParse = System.currentTimeMillis();
                while ((line = bufReader.readLine()) != null) {
                    if (line.length() > 100) {
                        StringBuilder str = new StringBuilder(line);
                        str.setCharAt(10, ' ');
                        str.setCharAt(19, ',');
                        LogLine logLine = new LogLine(harvestJobId, str.toString(), ++sequence);
                        if (logLine.record.id() != null) {
                            logLines.add(logLine);
                        } else {
                            logger.error("Could not parse " + line);
                            nonMatches++;
                        }
                    }
                }
                for (Entity ll : logLines) {
                    if (((LogLine) ll).record == null) {
                        System.out.println("Found null record");
                    }
                }
                logger.info("Parsed " + sequence + " log lines in "
                        + (System.currentTimeMillis() - startParse) / 1000 + " seconds."
                        + (nonMatches > 0
                        ? "There were " + nonMatches + " log statements that could not be parsed!" : ""));
                return SqlTemplate.forUpdate(pool.getPool(),
                                new LogLine().makeInsertTemplate(pool.getSchema()))
                        .mapFrom(new LogLine().getTupleMapper())
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
    public Future<Void> storeFailedRecords(UUID harvestJobId, List<Entity> failedRecords) {
        if (failedRecords != null && !failedRecords.isEmpty()) {
            return SqlTemplate.forUpdate(pool.getPool(),
                            new RecordFailure().makeInsertTemplate(pool.getSchema()))
                    .mapFrom(new RecordFailure().getTupleMapper())
                    .executeBatch(failedRecords)
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
                .mapTo(new HarvestJob().getRowMapper())
                .execute(null)
                .onSuccess(rows -> {
                    for (Entity entity : rows) {
                        previousJobs.add((HarvestJob) entity);
                    }
                }).map(previousJobs);
    }

    /**
     * New version of previous jobs.
     * @param query
     * @return
     */
    public Future<List<ImportJob>> getImportJobs(String query) {
        List<ImportJob> importJobs = new ArrayList<>();
        return SqlTemplate.forQuery(pool.getPool(), query)
                .mapTo(new ImportJob().getRowMapper())
                .execute(null)
                .onSuccess(rows -> {
                    for (Entity entity : rows) {
                        importJobs.add((ImportJob) entity);
                    }
                }).map(importJobs);
    }

    /**
     * Retrieves past harvest job.
     */
    public Future<HarvestJob> getPreviousJobById(UUID id) {
        return SqlTemplate.forQuery(pool.getPool(),
                        "SELECT * "
                                + "FROM " + schemaDotTable(Tables.harvest_job) + " "
                                + "WHERE id = #{id}")
                .mapTo(new HarvestJob().getRowMapper())
                .execute(Collections.singletonMap("id", id))
                .map(rows -> {
                    RowIterator<Entity> iterator = rows.iterator();
                    return iterator.hasNext() ? (HarvestJob) iterator.next() : null;
                });
    }

    public Future<ImportJob> getImportJobById(UUID id) {
        return SqlTemplate.forQuery(pool.getPool(),
                        "SELECT * "
                                + "FROM " + schemaDotTable(Tables.import_job) + " "
                                + "WHERE id = #{id}")
                .mapTo(new HarvestJob().getRowMapper())
                .execute(Collections.singletonMap("id", id))
                .map(rows -> {
                    RowIterator<Entity> iterator = rows.iterator();
                    return iterator.hasNext() ? (ImportJob) iterator.next() : null;
                });
    }

    /**
     * Retrieves log for past harvest job.
     */
    public Future<String> getLogsForPreviousJob(UUID id, SqlQuery queryFromCql) {
        Promise<String> promise = Promise.promise();
        final StringBuilder log = new StringBuilder();
        String query = queryFromCql
                .withAdditionalWhereClause("harvest_job_id = #{id}").toString();
        SqlTemplate.forQuery(pool.getPool(), query)
                .mapTo(new LogLine().getRowMapper())
                .execute(Collections.singletonMap("id", id))
                .onSuccess(rows -> {
                    for (Entity entity : rows) {
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
     * Retrieves log for past harvest job.
     */
    public Future<JsonObject> getLogsAsJsonForPreviousJob(UUID id, SqlQuery queryFromCql) {
        Promise<JsonObject> promise = Promise.promise();
        final JsonArray array = new JsonArray();
        String query = queryFromCql.withAdditionalWhereClause("harvest_job_id = #{id}").toString();
        SqlTemplate.forQuery(pool.getPool(), query)
                .mapTo(new LogLine().getRowMapper())
                .execute(Collections.singletonMap("id", id))
                .onSuccess(rows -> {
                    for (Entity entity : rows) {
                        array.add(entity.asJson());
                    }
                    JsonObject json = new JsonObject();
                    json.put("logLines", array);
                    json.put("totalRecords", array.size());
                    promise.complete(json);
                })
                .onFailure(
                        rows -> promise.fail(rows.getMessage())
                );
        return promise.future();
    }

    /**
     * Retrieves failed records for past harvest jobs.
     */
    public Future<List<RecordFailure>> getFailedRecordsForPreviousJobs(
            SqlQuery query) {
        List<RecordFailure> recordFailures = new ArrayList<>();
        return SqlTemplate.forQuery(pool.getPool(), query.getQueryWithLimits())
                .mapTo(new RecordFailure().getRowMapper())
                .execute(null)
                .onSuccess(rows -> {
                    for (Entity entity : rows) {
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
                                + "FROM " + schemaDotTable(Tables.record_failure) + " "
                                + "WHERE id = #{id} ")
                .mapTo(new RecordFailure().getRowMapper())
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
        getPreviousJobById(id).onComplete(previousJob -> {
            if (previousJob.result() == null) {
                promise.fail("No job history found with job ID " + id + ". Nothing deleted.");
            } else {
                logger.info("Found job to delete: " + previousJob.result().record.id());
                SqlTemplate.forUpdate(pool.getPool(),
                                "DELETE FROM " + schemaDotTable(Tables.log_statement)
                                        + " WHERE " + new LogLine().dbColumnName(LogLine.HARVEST_JOB_ID) + " = #{id} ")
                        .execute(Collections.singletonMap("id", id))
                        .onComplete(deletedLogs -> {
                            if (deletedLogs.succeeded()) {
                                SqlTemplate.forUpdate(pool.getPool(),
                                                "DELETE FROM " + schemaDotTable(Tables.record_failure)
                                                        + " WHERE " + new RecordFailure().dbColumnName(RecordFailure.HARVEST_JOB_ID) + " = #{id} ")
                                        .execute(Collections.singletonMap("id", id))
                                        .onComplete(deletedFailedRecords -> {
                                            if (deletedFailedRecords.succeeded()) {
                                                SqlTemplate.forUpdate(pool.getPool(),
                                                                "DELETE FROM " + schemaDotTable(Tables.harvest_job)
                                                                        + " WHERE " + new HarvestJob().field(HarvestJob.ID).columnName() + " = #{id} ")
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
            }
        });
        return promise.future();
    }

    public Future<SqlResult<Void>> purgePreviousJobsByAge(LocalDateTime untilDate) {
        Promise<Void> promise = Promise.promise();
        return SqlTemplate.forUpdate(pool.getPool(),
                        "DELETE FROM " + schemaDotTable(Tables.log_statement)
                                + " WHERE " + new LogLine().dbColumnName(HARVEST_JOB_ID) +
                                "    IN (SELECT " + new HarvestJob().dbColumnName(ID) +
                                "        FROM " + schemaDotTable(Tables.harvest_job) +
                                "        WHERE " + new HarvestJob().dbColumnName(STARTED) + " < #{untilDate} )")
                .execute(Collections.singletonMap("untilDate", untilDate))
                .onComplete(deletedLogs -> {
                    if (deletedLogs.succeeded()) {
                        SqlTemplate.forUpdate(pool.getPool(),
                                        "DELETE FROM " + schemaDotTable(Tables.record_failure)
                                                + " WHERE " + new RecordFailure().dbColumnName(RecordFailure.HARVEST_JOB_ID) +
                                                "    IN (SELECT " + new HarvestJob().dbColumnName(HarvestJob.ID) +
                                                "        FROM " + schemaDotTable(Tables.harvest_job) +
                                                "        WHERE " + new HarvestJob().dbColumnName(HarvestJob.STARTED) + " < #{untilDate} )")
                                .execute(Collections.singletonMap("untilDate", untilDate))
                                .onComplete(deletedFailedRecords -> {
                                    if (deletedFailedRecords.succeeded()) {
                                        SqlTemplate.forUpdate(pool.getPool(),
                                                        "DELETE FROM " + schemaDotTable(Tables.harvest_job) +
                                                                "        WHERE " + new HarvestJob().dbColumnName(HarvestJob.STARTED) + " < #{untilDate} ")
                                                .execute(Collections.singletonMap("untilDate", untilDate))
                                                .onSuccess(result -> {
                                                    logger.info("Timer process purged " + result.rowCount() + " harvest job runs from before " + untilDate);
                                                    promise.complete();
                                                })
                                                .onFailure(result -> {
                                                    logger.error("Timer process: Purge of previous jobs failed." + result.getCause().getMessage());
                                                    promise.fail("Could not delete job runs with finish dates before  " + untilDate
                                                            + result.getCause().getMessage());
                                                });
                                    } else {
                                        logger.error("Purge of failed records failed." + deletedFailedRecords.cause().getMessage());
                                        promise.fail("Could not delete job runs with finish dates before  " + untilDate
                                                + " because deletion of its failed records failed: "
                                                + deletedFailedRecords.cause().getMessage());
                                    }
                                });
                    } else {
                        logger.error("Purge of log statements failed." + deletedLogs.cause().getMessage());
                        promise.fail("Could not delete job runs with finish dates before  " + untilDate
                                + " because deletion of its logs failed: "
                                + deletedLogs.cause().getMessage());
                    }
                });
    }

    /**
     * Gets record count.
     */
    public Future<Long> getCount(String sql) {
        return SqlTemplate.forQuery(pool.getPool(), sql)
                .execute(null)
                .map(rows -> rows.iterator().next().getLong("total_records"));
    }

}
