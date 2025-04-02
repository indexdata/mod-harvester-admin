package org.folio.harvesteradmin.moduledata.database;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.moduledata.HarvestJob;
import org.folio.harvesteradmin.moduledata.LogLine;
import org.folio.harvesteradmin.moduledata.RecordFailure;
import org.folio.harvesteradmin.moduledata.StoredEntity;
import org.folio.tlib.postgres.TenantPgPool;

public final class DatabaseInit {
  private static final Logger logger = LogManager.getLogger(DatabaseInit.class);

  private DatabaseInit() {
    // a utility class cannot be instantiated
  }

  /**
   * Creates tables and views.
   */
  public static Future<Void> createDatabase(TenantPgPool pool) {
    return createEntity(pool, HarvestJob.entity())
        .compose(x -> createEntity(pool, LogLine.entity()))
        .compose(x -> createEntity(pool, RecordFailure.entity()))
        .compose(x -> createRecordFailureView(pool))
        .compose(x -> createPurgeFunction(pool))
        .mapEmpty();
  }

  private static Future<RowSet<Row>> createEntity(TenantPgPool pool, StoredEntity storedEntity) {
    var sqls = storedEntity.makeCreateSqls(pool.getSchema());
    Future<RowSet<Row>> future = Future.succeededFuture();
    for (var sql : sqls) {
      future = future.compose(x -> pool.query(sql).execute()
          .onFailure(e -> logger.error("createEntity {} failed: {}: {}",
            storedEntity.getClass().getSimpleName(), sql, e.getMessage())));
    }
    return future;
  }

  /**
   * Creates view.
   */
  private static Future<RowSet<Row>> createRecordFailureView(TenantPgPool pool) {
    var schema = pool.getSchema();
    String ddl
        = "CREATE OR REPLACE VIEW " + schema + "." + Tables.record_failure_view
        + " AS SELECT rf.id AS id, "
        + "          rf.harvest_job_Id AS harvest_job_id, "
        + "          hj.harvestable_id AS harvestable_id, "
        + "          hj.harvestable_name AS harvestable_name, "
        + "          rf.record_number AS record_number, "
        + "          rf.time_stamp AS time_stamp, "
        + "          rf.record_errors AS record_errors, "
        + "          rf.original_record AS original_record, "
        + "          rf.transformed_record AS transformed_record "
        + "  FROM " + schema + ".record_failure AS rf, "
        + "       " + schema + ".harvest_job as hj "
        + "  WHERE rf.harvest_job_id = hj.id";
    return pool.query(ddl).execute();
  }

  private static Future<RowSet<Row>> createPurgeFunction(TenantPgPool pool) {
    var sql = "CREATE OR REPLACE FUNCTION " + pool.getSchema() + ".purge_previous_jobs_by_age(timestamp) " +
        """
        RETURNS void
        AS $$
        BEGIN
          CREATE TEMP TABLE harvest_job_ids ON COMMIT DROP AS
            SELECT id FROM {schema}.harvest_job
            WHERE started < $1
            FOR UPDATE SKIP LOCKED;
          CREATE INDEX ON harvest_job_ids(id);
          CREATE TEMP TABLE log_statenemt_ids ON COMMIT DROP AS
            SELECT id FROM {schema}.log_statement
            WHERE harvest_job_id IN (SELECT id FROM harvest_job_ids)
            FOR UPDATE SKIP LOCKED;
          DELETE FROM {schema}.log_statement WHERE id IN (SELECT id FROM log_statenemt_ids);
          DELETE FROM harvest_job_ids WHERE EXISTS
            (SELECT * FROM {schema}.log_statement WHERE log_statement.harvest_job_id = harvest_job_ids.id);
          CREATE TEMP TABLE record_failure_ids ON COMMIT DROP AS
            SELECT id FROM {schema}.record_failure
            WHERE harvest_job_id IN (SELECT id FROM harvest_job_ids)
            FOR UPDATE SKIP LOCKED;
          DELETE FROM {schema}.record_failure WHERE id IN (SELECT id FROM record_failure_ids);
          DELETE FROM harvest_job_ids WHERE EXISTS
            (SELECT * FROM {schema}.record_failure WHERE record_failure.harvest_job_id = harvest_job_ids.id);
          DELETE FROM {schema}.harvest_job WHERE id IN (SELECT id FROM harvest_job_ids);
        END
        $$ LANGUAGE plpgsql;
        """.replace("{schema}", pool.getSchema());
    return pool.query(sql).execute();
  }
}
