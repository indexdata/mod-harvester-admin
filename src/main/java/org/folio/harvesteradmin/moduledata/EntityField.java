package org.folio.harvesteradmin.moduledata;

public interface EntityField {

  String columnName();

  String propertyName();

  PgColumn pgColumn();

  boolean isQueryable();
}
