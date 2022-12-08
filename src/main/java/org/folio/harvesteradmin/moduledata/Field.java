package org.folio.harvesteradmin.moduledata;

public interface Field {
  String columnName();

  String propertyName();

  PgColumn pgColumn();

  boolean isQueryable();
}
