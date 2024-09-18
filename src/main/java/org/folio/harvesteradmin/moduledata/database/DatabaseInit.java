package org.folio.harvesteradmin.moduledata.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Query;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.moduledata.HarvestJob;
import org.folio.harvesteradmin.moduledata.LogLine;
import org.folio.harvesteradmin.moduledata.RecordFailure;
import org.folio.tlib.postgres.TenantPgPool;

public class DatabaseInit {

    /**
     * Creates tables and views.
     */
    public static Future<Void> createDatabase(TenantPgPool pool) {
        final Promise<Void> promise = Promise.promise();
        Query<RowSet<Row>> createHarvestJob = pool.query(HarvestJob.entity().makeCreateTableSql(pool.getSchema()));
        Query<RowSet<Row>> createLogLine = pool.query(LogLine.entity().makeCreateTableSql(pool.getSchema()));
        Query<RowSet<Row>> createRecordFailure = pool.query(RecordFailure.entity().makeCreateTableSql(pool.getSchema()));
        Query<RowSet<Row>> createRecordFailureView = pool.query(createRecordFailureView(pool.getSchema()));
        createHarvestJob.execute().onSuccess(a ->
                createLogLine.execute().onSuccess(
                        b -> createRecordFailure.execute().onSuccess(
                                c -> createRecordFailureView.execute().onSuccess(
                                        d -> promise.complete(null)
                                ).onFailure(d -> promise.fail("CREATE VIEW record_failure_view failed " + d.getMessage()))
                        ).onFailure(c -> promise.fail("CREATE TABLE record_failure failed " + c.getMessage()))
                ).onFailure(b -> promise.fail("CREATE TABLE log_statement failed " + b.getMessage()))
        ).onFailure(a -> promise.fail("CREATE TABLE harvest_job failed: " + a.getMessage()));
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
     * Creates view.
     */
    public static String createRecordFailureView(String schema) {
        String ddl;
        ddl = "CREATE OR REPLACE VIEW " + schema + "." + Tables.record_failure_view
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
        return ddl;
    }
}
