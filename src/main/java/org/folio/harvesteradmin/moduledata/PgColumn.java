package org.folio.harvesteradmin.moduledata;

import org.folio.tlib.postgres.cqlfield.PgCqlFieldBase;

public class PgColumn {
  public final String name;
  public final String type;
  public final String nullable;
  public final Boolean isPrimaryKey;
  public final PgCqlFieldBase pgCqlFieldObject;

  enum Type {
    TEXT,
    INTEGER,
    TIMESTAMP,
    UUID,
    BOOLEAN
  }

  /**
   * Constructor.
   */
  public PgColumn(String name, Type type, Boolean nullable, PgCqlFieldBase pgCqlFieldObject) {
    this(name, type, nullable, pgCqlFieldObject, false);
  }

  /**
   * Constructor.
   */
  public PgColumn(String name, Type type, Boolean nullable, PgCqlFieldBase pgCqlFieldObject,
                  Boolean isPrimaryKey) {
    this.name = name;
    this.type = type.name();
    this.isPrimaryKey = isPrimaryKey;
    this.nullable = nullable ? "NULL" : "NOT NULL";
    this.pgCqlFieldObject = pgCqlFieldObject;
  }

  public String getColumnDdl() {
    return name + " " + type + " " + (isPrimaryKey ? " PRIMARY KEY " : nullable);
  }

  public String toString() {
    return name;
  }

}
