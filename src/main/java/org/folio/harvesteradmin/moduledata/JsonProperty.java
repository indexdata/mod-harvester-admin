package org.folio.harvesteradmin.moduledata;

public class JsonProperty {

  public final String name;
  public final String type;
  public final String format;

  enum Type {
    string,
    integer,
    undefined
  }

  enum Format {
    undefined
  }

  /**
   * Constructor.
   */
  public JsonProperty(String name, Type type, Format format) {
    this.name = name;
    this.type = type.name();
    this.format = format.name();
  }

  /**
   * Constructor.
   */
  public JsonProperty(String name) {
    this.name = name;
    this.type = Type.undefined.name();
    this.format = Format.undefined.name();
  }

  public String toString() {
    return name;
  }
}
