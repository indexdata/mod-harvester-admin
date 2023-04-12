package org.folio.harvesteradmin.moduledata;

public enum HarvestJobField implements Field {
  ID("id", "id", PgColumn.Type.UUID, false, true, true),
  HARVESTABLE_ID("harvestableId", "harvestable_id", PgColumn.Type.BIGINT, false, true),
  HARVESTABLE_NAME("name", "harvestable_name", PgColumn.Type.TEXT, false, true),
  HARVESTABLE_TYPE("type", "type", PgColumn.Type.TEXT, false, true),
  URL("url", "url", PgColumn.Type.TEXT, false, false),
  ALLOW_ERRORS("allowErrors", "allow_errors", PgColumn.Type.BOOLEAN, false, false),
  RECORD_LIMIT("recordLimit", "record_limit", PgColumn.Type.INTEGER, true, false),
  BATCH_SIZE("batchSize", "batch_size", PgColumn.Type.INTEGER, true, false),
  TRANSFORMATION("transformation", "transformation", PgColumn.Type.TEXT, false, true),
  STORAGE("storage", "storage", PgColumn.Type.TEXT, false, true),
  STATUS("status", "status", PgColumn.Type.TEXT, true, true),
  STARTED("started", "started", PgColumn.Type.TIMESTAMP, false, false),
  FINISHED("finished", "finished", PgColumn.Type.TIMESTAMP, true, false),
  AMOUNT_HARVESTED("amountHarvested", "amount_harvested", PgColumn.Type.INTEGER, true, false),
  MESSAGE("message", "message", PgColumn.Type.TEXT, true, true);

  private final JsonProperty property;
  private final PgColumn column;
  private final Boolean queryable;

  public String columnName() {
    return column.name;
  }

  public String propertyName() {
    return property.name;
  }

  public PgColumn pgColumn() {
    return column;
  }

  public boolean isQueryable() {
    return queryable;
  }

  HarvestJobField(String jsonPropertyName,
                  String columnName, PgColumn.Type type, Boolean nullable, Boolean queryable) {
    this(jsonPropertyName, columnName, type, nullable, queryable, false);
  }

  HarvestJobField(String jsonPropertyName,
                  String columnName, PgColumn.Type type, Boolean nullable, Boolean queryable,
                  Boolean primaryKey) {
    this.property = new JsonProperty(jsonPropertyName);
    this.queryable = queryable;
    this.column = new PgColumn(columnName, type, nullable, primaryKey);
  }

}
