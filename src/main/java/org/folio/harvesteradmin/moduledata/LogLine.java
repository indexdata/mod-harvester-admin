package org.folio.harvesteradmin.moduledata;

import io.vertx.sqlclient.templates.TupleMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.folio.harvesteradmin.modulestorage.HarvestAdminStorage.LogStatementTable;


public class LogLine {

  private final UUID id;
  private final UUID harvestJobId;
  private final String line;
  private final int sequenceNumber;

  /**
   * Constructor.
   */
  public LogLine(UUID harvestJobId, String line, int sequenceNumber) {
    id = UUID.randomUUID();
    this.harvestJobId = harvestJobId;
    this.line = line;
    this.sequenceNumber = sequenceNumber;
  }

  /**
   * Creates a TupleMapper.
   */
  public static TupleMapper<LogLine> tupleMapper() {
    return TupleMapper.mapper(
        logLine -> {
          Map<String, Object> parameters = new HashMap<>();
          parameters.put(LogStatementTable.id.name(), logLine.id);
          parameters.put(LogStatementTable.harvest_job_id.name(), logLine.harvestJobId);
          parameters.put(LogStatementTable.seq.name(), logLine.sequenceNumber);
          parameters.put(LogStatementTable.statement.name(), logLine.line);
          return parameters;
        });
  }

}
