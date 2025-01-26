package org.folio.harvesteradmin.moduledata;

public record Field(String jsonPropertyName, String columnName, PgColumn.Type pgType, boolean nullable, boolean queryable, boolean primaryKey) {
    public Field(String jsonPropertyName, String columnName, PgColumn.Type pgType, boolean nullable, boolean queryable) {
        this(jsonPropertyName, columnName, pgType, nullable, queryable, false);
    }
    public PgColumn pgColumn() {
        return new PgColumn(columnName, pgType, nullable, primaryKey);
    }
}
