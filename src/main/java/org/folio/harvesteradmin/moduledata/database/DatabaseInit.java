package org.folio.harvesteradmin.moduledata.database;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.moduledata.HarvestJob;
import org.folio.harvesteradmin.moduledata.LogLine;
import org.folio.harvesteradmin.moduledata.RecordFailure;
import org.folio.harvesteradmin.moduledata.StoredEntity;
import org.folio.tlib.postgres.TenantPgPool;

public class DatabaseInit {

  private static final Logger logger = LogManager.getLogger(ModuleStorageAccess.class);

    /**
   * Creates tables and views.
   */
  public static Future<Void> createDatabase(TenantPgPool pool) {
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
            pool.query(createRecordFailureView(pool.getSchema()))
                .execute().mapEmpty().onComplete(view -> {
                  if (view.succeeded()) {
                    promise.complete();
                  } else {
                    promise.fail(view.cause().getMessage());
                  }
                });
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
    logger.info("View DDL " + ddl);
    return ddl;
  }
}
