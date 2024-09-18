package org.folio.harvesteradmin.legacydata.responsehandlers;

import io.vertx.core.json.JsonObject;

/**
 * Base class for<br/> (1) capturing certain unconventional protocols and REST responses from
 * the legacy API and
 * transform them into conventional, FOLIO compliant protocols and responses.<br/>
 * (2) converting the legacy XML
 * response (if any) to FOLIO like JSON.
 */
public abstract class ProcessedHarvesterResponse {
  protected int statusCode;

  protected String errorMessage;
  protected String bodyAsString;
  protected JsonObject jsonObject = new JsonObject();
  protected String harvesterPath;
  protected int recordCount = -1;

  public int statusCode() {
    return statusCode;
  }

  public JsonObject jsonObject() {
    return jsonObject;
  }

  public String errorMessage() {
    return errorMessage;
  }

  public boolean wasOK() {
    return statusCode == 200;
  }

  public boolean wasCreated() {
    return statusCode == 201;
  }

  public boolean wasNotFound() {
    return statusCode == 404;
  }

  public boolean found() {
    return statusCode != 404;
  }

  public boolean wasNoContent() {
    return statusCode == 204;
  }

}
