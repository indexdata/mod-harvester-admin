package org.folio.harvesteradmin.dataaccess;

import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_STEPS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_STORAGES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_TRANSFORMATIONS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_TSAS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.harvesterPathByRequestPath;
import static org.folio.harvesteradmin.dataaccess.statics.EntityRootNames.mapToNameOfArrayOfEntities;
import static org.folio.harvesteradmin.dataaccess.statics.EntityRootNames.mapToNameOfRootOfEntity;
import static org.folio.harvesteradmin.dataaccess.statics.EntityRootNames.typeToEmbeddedTypeMap;
import static org.folio.harvesteradmin.dataaccess.statics.RequestParameters.crosswalkCqlFieldNames;
import static org.folio.harvesteradmin.dataaccess.statics.RequestParameters.crosswalkRequestParameterNames;
import static org.folio.harvesteradmin.dataaccess.statics.RequestParameters.supportedGetRequestParameters;
import static org.folio.okapi.common.HttpResponse.responseText;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.harvesteradmin.dataaccess.dataconverters.JsonToHarvesterXml;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponse;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponseDelete;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponseGet;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponseGetById;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponseGetUniqueByName;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponsePost;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponsePut;
import org.folio.harvesteradmin.dataaccess.statics.ApiPaths;
import org.folio.harvesteradmin.dataaccess.statics.EntityRootNames;
import org.folio.harvesteradmin.dataaccess.statics.LegacyServiceConfig;
import org.folio.okapi.common.GenericCompositeFuture;
import org.xml.sax.SAXException;


public class LegacyHarvesterStorage {


  public static final String VALID_STEP_TYPE = "XmlTransformStep";
  public static final String STEP_NAME_KEY = "name";
  public static final String STEP_TYPE_KEY = "type";
  public static final String STEP_SCRIPT_KEY = "script";
  private final String tenant;
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final int NOT_FOUND = 404;
  public static final int INTERNAL_SERVER_ERROR = 500;
  public static final int NO_CONTENT = 204;
  public static final int CREATED = 201;
  protected final WebClient restClient;

  private static final DateTimeFormatter iso_instant = DateTimeFormatter.ISO_INSTANT;
  private static final Logger logger = LogManager.getLogger(LegacyHarvesterStorage.class);

  private static final Iterator<Long> fifteenDigitLongs =
      new Random().longs(100000000000000L, 999999999999999L).iterator();

  public LegacyHarvesterStorage(Vertx vertx, String tenant) {
    this.tenant = tenant;
    restClient = WebClient.create(vertx);
  }

  /**
   * Primary key generator.
   * @return random 12 digit number as string.
   */
  public static String getRandomFifteenDigitString() {
    return Long.toString(fifteenDigitLongs.next());
  }

  /**
   * Gets config records based on path and tenant.
   */
  public Future<ProcessedHarvesterResponseGet> getConfigRecords(RoutingContext routingContext) {
    String harvesterPath = mapToHarvesterPath(routingContext);
    Map<String, String> queryParameters =
        getSupportedGetRequestParameters(routingContext.request());
    String query = buildQueryString(queryParameters);
    return getConfigRecords(harvesterPath, query);
  }

  /**
   * Gets Harvester config records.
   */
  public Future<ProcessedHarvesterResponseGet> getConfigRecords(String path, String query) {
    Promise<ProcessedHarvesterResponseGet> promise = Promise.promise();
    String pathAndQuery = path
        + ((query == null || query.isEmpty()) ? aclFilter(tenant)
        : (query.startsWith("?") ? query : "?" + query) + andAclFilter(tenant));
    harvesterGetRequest(pathAndQuery)
        .send(ar ->
            promise.complete(new ProcessedHarvesterResponseGet(ar, path, query)));
    return promise.future();
  }

  /**
   * Gets config record based on harvester path and id parameter from context.
   */
  public Future<ProcessedHarvesterResponseGetById> getConfigRecordById(
      RoutingContext routingContext, String id) {
    String harvesterPath = mapToHarvesterPath(routingContext);
    return getConfigRecordById(harvesterPath, id);
  }

  /**
   * Gets config record based on harvester path and id parameter.
   */
  public Future<ProcessedHarvesterResponseGetById> getConfigRecordById(
      String harvesterPath, String id) {
    Promise<ProcessedHarvesterResponseGetById> promise = Promise.promise();
    logger.debug("GET " + harvesterPath + "/" + id);
    harvesterGetRequest(harvesterPath + "/" + id).send(ar -> promise.complete(
        new ProcessedHarvesterResponseGetById(ar, harvesterPath, id, tenant)));
    return promise.future();
  }

  private Future<ProcessedHarvesterResponse> getConfigRecordByIdOrName(
      String harvesterPath, String id, String name) {
    Promise<ProcessedHarvesterResponse> promise = Promise.promise();
    if (id == null || id.isEmpty()) {
      Map<String,String> identifierParam = Map.of("query", "name=" + name);
      return getConfigRecords(harvesterPath, buildQueryString(identifierParam))
          .compose(recordsByName -> {
            JsonObject recordsJson = recordsByName.jsonObject();
            int recordsFoundByName = recordsJson.getInteger("totalRecords");
            if (recordsFoundByName == 1) {
              JsonObject briefJson =
                  recordsJson.getJsonArray(
                      mapToNameOfArrayOfEntities(harvesterPath)).getJsonObject(0);
              String resolvedId = briefJson.getString("id");
              getConfigRecordById(harvesterPath, resolvedId).onComplete(
                  recordById -> promise.complete(recordById.result()));
            } else if (recordsFoundByName == 0) {
              promise.complete(
                  new ProcessedHarvesterResponseGetUniqueByName(
                      recordsByName.jsonObject(),
                      422,
                      "Record with name \"" + name + "\" not found", recordsFoundByName));
            } else {
              promise.complete(
                  new ProcessedHarvesterResponseGetUniqueByName(
                      recordsByName.jsonObject(),
                      422,
                      "Found multiple records with name \"" + name + "\"", recordsFoundByName));
            }
            return promise.future();
          }).onFailure(recordsByName ->
              promise.fail("Lookup of records by name \"" + name + "\"failed: "
                  + recordsByName.getMessage()));
    } else {
      getConfigRecordById(harvesterPath, id).onComplete(
          recordById -> promise.complete(recordById.result()));
      return promise.future();
    }
  }

  /**
   * Posts config record, after checking the ID, and -- if no ID provided -- for possible
   * duplicate name, and retrieves the record for the response.
   */
  public Future<ProcessedHarvesterResponsePost> postConfigRecord(RoutingContext routingContext) {
    Promise<ProcessedHarvesterResponsePost> promise = Promise.promise();
    String harvesterPath = mapToHarvesterPath(routingContext);
    JsonObject jsonToPost = routingContext.body().asJsonObject();
    String idInPostedRecord = jsonToPost.getString("id");
    if (idInPostedRecord == null) {
      nameExistsAlready(harvesterPath,jsonToPost).onComplete(
          exists -> {
            if (exists.result()) {
              promise.complete(
                  new ProcessedHarvesterResponsePost(
                      422,
                      "The name '"
                          + jsonToPost.getString("name")
                          + "' exists already in "
                          + harvesterPath
                          + "."));
            } else {
              doPostConfigRecord(routingContext)
                  .onComplete(post -> promise.complete(post.result()));
            }
          }
      );
    } else {
      getConfigRecordById(routingContext, idInPostedRecord)
          .onComplete(lookUp -> {
            if (lookUp.succeeded()) {
              if (lookUp.result().wasOK()) {
                promise.complete(
                    new ProcessedHarvesterResponsePost(
                        422,
                        harvesterPath + "/" + idInPostedRecord + " already exists."));
              } else if (lookUp.result().wasNotFound()) {
                doPostConfigRecord(routingContext)
                    .onComplete(post -> promise.complete(post.result()));
              } else {
                promise.complete(new ProcessedHarvesterResponsePost(lookUp.result().statusCode(),
                    "There was an error ("
                        + lookUp.result().statusCode() + ") looking up "
                        + harvesterPath + "/" + idInPostedRecord
                        + " before POST: "
                        + lookUp.result().errorMessage()));
              }
            }
          });
    }
    return promise.future();
  }

  /**
   * Checks for referential constraints, and possibly mutates the JSON in argument `entity`.
   * If the reference is by name, the method will update
   * `entity` with the id of the record with that name.
   */
  public Future<ProcessedHarvesterResponsePost> resolveReferencedEntities(
      String api, JsonObject entity) {
    Promise<ProcessedHarvesterResponsePost> promise = Promise.promise();
    List<String> constraintViolation = new ArrayList<>();
    List<String> fatalError = new ArrayList<>();
    if (api.contains("harvestables")) {
      // check transformation and storage
      final String storageId = entity.getJsonObject("storage").getString("id");
      final String transformationId = entity.getJsonObject("transformation").getString("id");
      final String storageName = entity.getJsonObject("storage").getString("name");
      final String transformationName = entity.getJsonObject("transformation").getString("name");
      getConfigRecordByIdOrName(HARVESTER_STORAGES_PATH, storageId, storageName)
          .onComplete(storage -> {
            if (storage.succeeded()) {
              if (storage.result().wasOK()) {
                if (storageId == null || storageId.isEmpty()) {
                  entity.getJsonObject("storage")
                      .put("id", storage.result().jsonObject().getString("id"));
                }
              } else {
                constraintViolation.add(
                    "Could not resolve references: " + storage.result().errorMessage());
              }
            } else {
              fatalError.add("Error looking up storage by id or name "
                  + storage.cause().getMessage());
            }
            getConfigRecordByIdOrName(HARVESTER_TRANSFORMATIONS_PATH,
                transformationId, transformationName)
                .onComplete(transformation -> {
                  if (transformation.succeeded()) {
                    if (transformation.result().wasOK()) {
                      if (transformationId == null || transformationId.isEmpty()) {
                        entity.getJsonObject("transformation")
                            .put("id", transformation.result().jsonObject().getString("id"));
                      }
                    } else {
                      constraintViolation.add(transformation.result().errorMessage());

                    }
                    if (constraintViolation.size() > 0) {
                      promise.complete(
                          new ProcessedHarvesterResponsePost(422, constraintViolation.toString())
                      );
                    } else {
                      promise.complete(
                          new ProcessedHarvesterResponsePost(200, "References OK")
                      );
                    }
                  } else {
                    fatalError.add("Error looking up transformation by id or name "
                        + transformation.cause().getMessage());
                  }
                });
            if (fatalError.size() > 0) {
              promise.complete(new ProcessedHarvesterResponsePost(500,fatalError.toString()));
            }
          });
    } else {
      promise.complete(
          new ProcessedHarvesterResponsePost(200, "References not checked")
      );
    }
    return promise.future();
  }

  private Future<Boolean> nameExistsAlready(
      String harvesterPath, JsonObject entity) {
    Promise<Boolean> promise = Promise.promise();
    if (entity.containsKey("name")) {
      Map<String,String> identifierParam = Map.of("query", "name=" + entity.getString("name"));
      getConfigRecords(harvesterPath, buildQueryString(identifierParam)).onComplete(records -> {
        if (records.succeeded()) {
          if (records.result().jsonObject().getInteger("totalRecords") > 0) {
            promise.complete(true);
          } else {
            promise.complete(false);
          }
        }
      });
    } else {
      promise.complete(false);
    }
    return promise.future();
  }

  /**
   * Posts configuration record and retrieves the persisted result for the response.
   */
  public Future<ProcessedHarvesterResponsePost> doPostConfigRecord(RoutingContext routingContext) {
    String harvesterPath = mapToHarvesterPath(routingContext);
    if (harvesterPath.equals(HARVESTER_TRANSFORMATIONS_PATH)) {
      return doPostAndPutTransformation(routingContext);
    } else if (harvesterPath.equals(HARVESTER_TSAS_PATH)) {
      return doPostTsaPutTransformation(routingContext);
    } else {
      JsonObject jsonToPost = routingContext.body().asJsonObject();
      if (harvesterPath.equals(HARVESTER_HARVESTABLES_PATH)) {
        jsonToPost.put("lastUpdated", iso_instant.format(Instant.now()));
        JsonObject transformationReference = jsonToPost.getJsonObject("transformation");
        if (!transformationReference.containsKey("entityType")) {
          transformationReference.put("entityType", "basicTransformation");
        }
        JsonObject storageReference = jsonToPost.getJsonObject("storage");
        if (!storageReference.containsKey("entityType")) {
          storageReference.put("entityType", "inventoryStorageEntity");
        }
      }
      String requestUri = routingContext.request().absoluteURI();
      return doPostConfigRecord(requestUri, harvesterPath, jsonToPost);
    }
  }

  /**
   * Posts configuration record and retrieves the persisted result.
   */
  public Future<ProcessedHarvesterResponsePost> doPostConfigRecord(
      String requestUri, String harvesterPath, JsonObject json) {
    Promise<ProcessedHarvesterResponsePost> promise = Promise.promise();
    if (!json.containsKey("id")) {
      json.put("id", getRandomFifteenDigitString());
    }
    resolveReferencedEntities(harvesterPath, json)
        .onComplete(result -> {
          if (result.result().wasOK()) {
            try {
              String xml = JsonToHarvesterXml.convertToHarvesterRecord(json,
                  mapToNameOfRootOfEntity(harvesterPath), tenant);
              harvesterPostRequest(harvesterPath).sendBuffer(Buffer.buffer(xml), ar -> {
                if (ar.succeeded()) {
                  String location = ar.result().getHeader("Location");
                  if (ar.result().statusCode() == CREATED && location != null) {
                    String idFromLocation = location.split("/")[location.split("/").length - 1];
                    logger.debug("Got id from location: " + location);
                    getConfigRecordById(harvesterPath, idFromLocation).onComplete(
                        // going to return 500, internal server error if not found,
                        // 201, Created if found
                        lookUpNewlyCreatedRecord -> promise.complete(
                            new ProcessedHarvesterResponsePost(
                                ar, requestUri, harvesterPath,
                                lookUpNewlyCreatedRecord.result())));
                  } else {
                    promise.complete(
                        new ProcessedHarvesterResponsePost(
                            ar, requestUri, harvesterPath, null));
                  }
                } else {
                  promise.complete(
                      new ProcessedHarvesterResponsePost(
                          ar, requestUri, harvesterPath, null));
                }
              });
            } catch (TransformerException | ParserConfigurationException e) {
              promise.complete(
                  new ProcessedHarvesterResponsePost(INTERNAL_SERVER_ERROR, e.getMessage()));
            }
          } else {
            promise.complete(result.result());
          }
        });
    return promise.future();
  }

  /**
   * Puts a config record.
   */
  public Future<ProcessedHarvesterResponsePut> putConfigRecord(RoutingContext routingContext) {
    String harvesterPath = mapToHarvesterPath(routingContext);
    JsonObject jsonToPut = routingContext.body().asJsonObject();
    String id = routingContext.request().getParam("id");
    if (harvesterPath != null && harvesterPath.equals(HARVESTER_HARVESTABLES_PATH)) {
      jsonToPut.put("lastUpdated", iso_instant.format(Instant.now()));
    }
    return putConfigRecord(routingContext, harvesterPath, jsonToPut, id);
  }

  /**
   * Puts a config record.
   */
  public Future<ProcessedHarvesterResponsePut> putConfigRecord(
      RoutingContext routingContext,
      String harvesterPath,
      JsonObject jsonToPut,
      String id) {
    Promise<ProcessedHarvesterResponsePut> promisedResponse = Promise.promise();
    getConfigRecordById(harvesterPath, id).onComplete(
        idLookUp -> {    // going to return 404 if not found
          if (idLookUp.succeeded()) {
            if (idLookUp.result().wasNotFound()) {
              promisedResponse.complete(
                  new ProcessedHarvesterResponsePut(
                      404,"Record to update not found")
              );
            } else if (idLookUp.result().wasOK()) {
              try {
                String xml = JsonToHarvesterXml.convertToHarvesterRecord(jsonToPut,
                    mapToNameOfRootOfEntity(harvesterPath), tenant);
                harvesterPutRequest(harvesterPath + "/" + id).sendBuffer(Buffer.buffer(xml),
                    put -> {
                      if (put.succeeded()) {
                        promisedResponse.complete(
                            new ProcessedHarvesterResponsePut(
                                put, routingContext.request().path(), harvesterPath));
                      } else {
                        promisedResponse.complete(
                            new ProcessedHarvesterResponsePut(INTERNAL_SERVER_ERROR,
                            "There was an error PUTting to " + harvesterPath + "/" + id + ": "
                                + put.cause().getMessage()));
                      }
                    });
              } catch (TransformerException | ParserConfigurationException e) {
                logger.error("Error parsing json " + jsonToPut);

                promisedResponse.complete(
                    new ProcessedHarvesterResponsePut(INTERNAL_SERVER_ERROR,
                    "There was an error PUTting to " + harvesterPath + "/" + id + ": "
                        + "Error parsing json " + jsonToPut));
              }
            } else {

              promisedResponse.complete(
                  new ProcessedHarvesterResponsePut(idLookUp.result().statusCode(),
                      "There was an error (" + idLookUp.result().statusCode() + ") looking up "
                      + harvesterPath + "/" + id + " before PUT: " + idLookUp.result()
                      .errorMessage()));
            }
          } else {
            promisedResponse.complete(
                new ProcessedHarvesterResponsePut(INTERNAL_SERVER_ERROR,
                    "Could not look up record " + harvesterPath + "/" + id + " before PUT: "
                    + idLookUp.cause().getMessage()));
          }
        });
    return promisedResponse.future();
  }

  /**
   * Deletes a config record.
   */
  public Future<ProcessedHarvesterResponseDelete> deleteConfigRecord(
      RoutingContext routingContext) {
    Promise<ProcessedHarvesterResponseDelete> promise = Promise.promise();
    String id = routingContext.request().getParam("id");
    String harvesterPath = mapToHarvesterPath(routingContext);
    String requestUri = routingContext.request().absoluteURI();
    checkForReferencingEntities(harvesterPath, id)
        .onComplete(check -> {
          if (check.result() == 0) {
            deleteConfigRecord(requestUri, id, harvesterPath)
                .onComplete(result -> promise.complete(result.result()));
          } else {
            promise.complete(
                new ProcessedHarvesterResponseDelete(
                    400,
                    "Cannot delete " + harvesterPath + "/" + id
                        + " due to " + check.result() + " dependent record"
                        + (check.result() == 1 ? "" : "s")));
          }
        });
    return promise.future();
  }

  /**
   * Deletes a config record.
   */
  public Future<ProcessedHarvesterResponseDelete> deleteConfigRecord(
      String requestUri, String id, String harvesterPath) {
    Promise<ProcessedHarvesterResponseDelete> promisedResponse = Promise.promise();
    logger.debug("Looking up " + harvesterPath + "/" + id + " before attempting delete");
    getConfigRecordById(harvesterPath, id).onComplete(
        idLookUp -> {  // going to return 404 if not found, else 204 (no content/deleted)
          if (idLookUp.succeeded()) {
            logger.debug("Look-up of " + harvesterPath + "/" + id + " complete. Status code: "
                + idLookUp.result().statusCode());
            if (idLookUp.result().wasNotFound()) {
              promisedResponse.complete(
                  new ProcessedHarvesterResponseDelete(
                      NOT_FOUND, "Could not delete " + requestUri));
            } else if (idLookUp.result().wasOK()) {
              harvesterDeleteRequest(harvesterPath + "/" + id).send(ar -> {
                if (ar.succeeded()) {
                  if (ar.result().statusCode() == NO_CONTENT) {
                    promisedResponse.complete(
                        new ProcessedHarvesterResponseDelete(ar,requestUri,harvesterPath));
                  } else {
                    promisedResponse.complete(
                        new ProcessedHarvesterResponseDelete(
                            ar.result().statusCode(),"Could not delete " + requestUri));
                  }
                } else {
                  promisedResponse.complete(
                      new ProcessedHarvesterResponseDelete(500,
                          "There was an error deleting "
                          + requestUri + ": "  + ar.cause().getMessage()));
                }
              });
            } else {
              promisedResponse.complete(
                  new ProcessedHarvesterResponseDelete(500,
                  "There was an error (" + idLookUp.result().statusCode() + ") looking up "
                      + requestUri + " before DELETE: "
                      + idLookUp.result().errorMessage()));
            }
          } else {
            promisedResponse.complete(
                new ProcessedHarvesterResponseDelete(500,
                "Could not look up record " + harvesterPath + "/" + id + " before DELETE: "
                    + idLookUp.cause().getMessage()));
          }
        });
    return promisedResponse.future();
  }

  /**
   * Checks for referencing entities (before deletion).
   */
  public Future<Integer> checkForReferencingEntities(String api, String id) {
    Promise<Integer> promise = Promise.promise();
    if (api.contains("storages")) {
      getConfigRecords(HARVESTER_HARVESTABLES_PATH, "query=storage.id=" + id)
          .onComplete(harvestables -> {
            if (harvestables.succeeded()) {
              promise.complete(harvestables.result().jsonObject().getInteger("totalRecords"));
            } else {
              promise.complete(-1);
            }
          });
    } else if (api.contains("transformations")) {
      getConfigRecords(HARVESTER_HARVESTABLES_PATH, "query=transformation.id=" + id)
          .onComplete(harvestables -> {
            if (harvestables.succeeded()) {
              promise.complete(harvestables.result().jsonObject().getInteger("totalRecords"));
            } else {
              promise.complete(-1);
            }
          });
    } else {
      promise.complete(0);
    }
    return promise.future();
  }



  private Future<ProcessedHarvesterResponsePost> doPostAndPutTransformation(
      RoutingContext routingContext) {
    JsonObject transformationJson = routingContext.body().asJsonObject();
    logger.debug("About to POST-then-PUT " + transformationJson.encodePrettily());
    JsonArray stepsIdsJson =
        transformationJson.containsKey("stepAssociations") ? transformationJson.getJsonArray(
            "stepAssociations").copy() : new JsonArray();
    transformationJson.remove("stepAssociations");
    List<Future<ProcessedHarvesterResponse>> stepFutures = new ArrayList<>();
    for (Object arrayObject : stepsIdsJson) {
      JsonObject step = (JsonObject) arrayObject;
      String stepId = step.containsKey("step")
          ? step.getJsonObject("step").getString("id")
          : step.getString("stepId");
      String stepName = step.containsKey("step")
          ? step.getJsonObject("step").getString("name")
          : step.getString("stepName");
      stepFutures.add(getConfigRecordByIdOrName(HARVESTER_STEPS_PATH, stepId, stepName));
    }
    Promise<ProcessedHarvesterResponsePost> promise = Promise.promise();
    GenericCompositeFuture.all(stepFutures).onComplete(steps -> {
      if (steps.succeeded()) {
        boolean allStepsFound = true;
        for (int h = 0; h < steps.result().size(); h++) {
          ProcessedHarvesterResponseGetById stepResponse = steps.result().resultAt(h);
          if (stepResponse.statusCode() == NOT_FOUND) {
            logger.info("Step not found: " + stepResponse.errorMessage());
            allStepsFound = false;
            promise.complete(new ProcessedHarvesterResponsePost(422,
                "Referenced step not found, cannot store transformation pipeline: "
                    + stepResponse.errorMessage()));
            break;
          }
        }
        if (allStepsFound) {
          doPostConfigRecord(
              routingContext.request().absoluteURI(),
              HARVESTER_TRANSFORMATIONS_PATH,
              transformationJson).onComplete(
                  transformationPost -> {
                    if (transformationPost.succeeded()
                        && transformationPost.result().statusCode() == CREATED) {
                      JsonObject createdTransformation = transformationPost.result().jsonObject();
                      createdTransformation.put("stepAssociations", new JsonArray());
                      for (int i = 0; i < steps.result().size(); i++) {
                        ProcessedHarvesterResponseGetById stepResponse = steps.result().resultAt(i);
                        final JsonObject stepJson = stepResponse.jsonObject();
                        JsonObject tsaJson = new JsonObject();
                        tsaJson.put("id", getRandomFifteenDigitString());
                        tsaJson.put("position", Integer.toString(i + 1));
                        tsaJson.put("step", new JsonObject());
                        tsaJson.getJsonObject("step")
                            .put("entityType",
                                typeToEmbeddedTypeMap.get(stepJson.getString("type")));
                        tsaJson.getJsonObject("step").put("id", stepJson.getString("id"));
                        tsaJson.put("transformation", createdTransformation.getString("id"));
                        createdTransformation.getJsonArray("stepAssociations").add(tsaJson);
                      }
                      putConfigRecord(
                          routingContext,
                          HARVESTER_TRANSFORMATIONS_PATH,
                          createdTransformation,
                          createdTransformation.getString("id")).onComplete(putResponse -> {
                            if (putResponse.succeeded()) {
                              getConfigRecordById(HARVESTER_TRANSFORMATIONS_PATH,
                                  createdTransformation.getString("id"))
                                  .onComplete(lookup -> {
                                    if (lookup.succeeded()) {
                                      promise.complete(
                                          transformationPost.result()
                                              .setConfirmationResponse(lookup.result().jsonObject())
                                      );
                                    } else {
                                      promise.complete(
                                          new ProcessedHarvesterResponsePost(500,
                                          " Failed to POST (with subsequent PUT and GET) of "
                                              + "Transformation."
                                              + " Retrieval failed with  "
                                              + lookup.result().errorMessage()));
                                    }
                                  });
                            } else {
                              promise.complete(
                                  new ProcessedHarvesterResponsePost(500,
                                  " Failed to POST (with subsequent PUT and GET) "
                                      + "of Transformation."
                                      + " PUT failed with "
                                      + putResponse.result().errorMessage()));
                            }
                          });
                    } else {
                      logger.error(transformationPost.cause().getMessage());
                    }
                  });
        }
      } else {
        promise.complete(
            new ProcessedHarvesterResponsePost(500,
                "There was a problem looking up referenced transformation steps,"
            + " could not store the transformation pipeline " + steps.cause().getMessage()));
      }
    });
    return promise.future();
  }

  private Future<ProcessedHarvesterResponsePost> doPostTsaPutTransformation(
      RoutingContext routingContext) {
    JsonObject incomingTsa = routingContext.body().asJsonObject();
    String transId = incomingTsa.getString("transformation");
    String transName = incomingTsa.getString("transformationName");
    String stepId = incomingTsa.getJsonObject("step").getString("id");
    String stepName = incomingTsa.getJsonObject("step").getString("name");
    Promise<ProcessedHarvesterResponsePost> promise = Promise.promise();
    getConfigRecordByIdOrName(HARVESTER_TRANSFORMATIONS_PATH, transId, transName).onComplete(
        theTransformation -> {
          if (theTransformation.failed()) {
            promise.complete(
                new ProcessedHarvesterResponsePost(
                    422,
                    theTransformation.cause().getMessage()));
          } else if (!theTransformation.result().found()) {
            promise.complete(
                new ProcessedHarvesterResponsePost(
                    422,
                    "Could not create transformation-step association. Transformation "
                    + transId + " not found."));
          } else {
            JsonObject transformationFound = theTransformation.result().jsonObject();
            getConfigRecordByIdOrName(HARVESTER_STEPS_PATH, stepId, stepName).onComplete(
                theStep -> {
                  if (!theStep.result().found()) {
                    promise.complete(
                        new ProcessedHarvesterResponsePost(
                            422,
                            "Could not create transformation-step association. Step " + stepId
                                + " not found."
                        )
                    );
                  } else {
                    incomingTsa.put("type","transformationStepAssociation");
                    JsonObject stepFound = theStep.result().jsonObject();
                    String entityType = typeToEmbeddedTypeMap.get(stepFound.getString("type"));
                    incomingTsa.getJsonObject("step").put("entityType", entityType);
                    incomingTsa.getJsonObject("step").put("id", stepFound.getString("id"));
                    incomingTsa.put("transformation", transformationFound.getString("id"));
                    doPostConfigRecord(
                        routingContext.request().absoluteURI(),
                        HARVESTER_TSAS_PATH,
                        incomingTsa).onComplete(postedTsa -> {
                          if (postedTsa.succeeded() && postedTsa.result() != null) {
                            JsonObject transformationStepAssociation
                                = postedTsa.result().jsonObject();
                            // Insert the tsa in the transformation JSON
                            JsonObject transformation = theTransformation.result().jsonObject();
                            transformation.put("stepAssociations",
                                insertStepIntoPipeline(
                                    transformation.getJsonArray("stepAssociations"),
                                    transformationStepAssociation));
                            try {
                              // PUT the transformation
                              String xml =
                                  JsonToHarvesterXml.convertToHarvesterRecord(
                                      transformation,
                                      EntityRootNames.TRANSFORMATION_ROOT_PROPERTY,
                                      tenant);
                              harvesterPutRequest(
                                  HARVESTER_TRANSFORMATIONS_PATH + "/"
                                      + transformationFound.getString("id"))
                                  .sendBuffer(Buffer.buffer(xml), ar -> {
                                    if (ar.succeeded()) {
                                      if (ar.result().statusCode() == NO_CONTENT) {
                                        promise.complete(postedTsa.result());
                                      } else {
                                        promise.complete(
                                            new ProcessedHarvesterResponsePost(
                                                ar.result().statusCode(),
                                                "There was a problem PUTting " + "to "
                                                    + HARVESTER_TSAS_PATH
                                                    + "/" + ": "
                                                    + ar.result().statusMessage()
                                            )
                                        );
                                      }
                                    } else {
                                      promise.complete(
                                          new ProcessedHarvesterResponsePost(
                                              INTERNAL_SERVER_ERROR,
                                              "There was an error PUTting to "
                                                  + HARVESTER_TRANSFORMATIONS_PATH + "/"
                                                  + transId + ": "
                                                  + ar.cause().getMessage()
                                          )
                                      );
                                    }
                                  });
                            } catch (TransformerException | ParserConfigurationException xe) {
                              logger.error("Error parsing json " + transformation);
                              promise.complete(
                                  new ProcessedHarvesterResponsePost(
                                      INTERNAL_SERVER_ERROR,
                                      "Error parsing json " + transformation
                                  )
                              );
                            }
                          } else {
                            promise.complete(
                                new ProcessedHarvesterResponsePost(
                                    422,
                                    "Problem POSTing to " + HARVESTER_TSAS_PATH + "/"
                                )
                            );
                          }
                        });
                  }
                });
          }
        });
    return promise.future();
  }

  /**
   * Gets the logs for a job.
   */
  public Future<HttpResponse<Buffer>> getJobLog(RoutingContext routingContext) {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    String id = routingContext.request().getParam("id");
    String fromParameter = routingContext.request().getParam("from");
    getConfigRecordById(HARVESTER_HARVESTABLES_PATH, id).onComplete(idLookup -> {
      if (idLookup.succeeded()) {
        ProcessedHarvesterResponseGetById idLookUpResponse = idLookup.result();
        if (idLookup.result().wasNotFound()) {
          promise.fail("Could not find harvestable with ID " + id);
        } else if (idLookup.result().wasOK()) {
          String lastStarted = idLookup.result().jsonObject().getString("lastHarvestStarted");
          if (lastStarted == null || lastStarted.isEmpty()) {
            promise.fail(
                "Could not find logs. This harvest configuration doesn't appear to have been run. "
                    + "Cannot retrieve logs "
                + HARVESTER_HARVESTABLES_PATH + "/" + id);
          } else {
            String from = fromParameter != null && !fromParameter.isEmpty()
                ? fromParameter
                : lastStarted.substring(0, 19);
            getJobLog(id, from).onComplete(ar -> promise.complete(ar.result()));
          }
        } else {
          promise.fail("There was an error (" + idLookUpResponse.statusCode() + ") looking up "
              + HARVESTER_HARVESTABLES_PATH + "/" + id
              + " to get logs: " + idLookUpResponse.errorMessage());
        }
      } else {
        promise.fail("Could not look up harvest job " + HARVESTER_HARVESTABLES_PATH
            + "/" + id + " to get logs : " + idLookup.cause().getMessage());
      }
    });
    return promise.future();
  }

  /**
   * Gets a job log.
   */
  public Future<HttpResponse<Buffer>> getJobLog(String harvestableId, String fromDate) {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    harvesterGetRequest(HARVESTER_HARVESTABLES_PATH + "/" + harvestableId + "/log?from="
        + fromDate.substring(0,Math.min(fromDate.length(),19)))
        .send(ar -> promise.complete(ar.result()));
    return promise.future();
  }

  /**
   * Gets failed records.
   */
  public Future<ProcessedHarvesterResponseGet> getFailedRecords(RoutingContext routingContext) {
    Promise<ProcessedHarvesterResponseGet> promise = Promise.promise();
    String id = routingContext.request().getParam("id");
    int offset = getIntOrDefault(routingContext.request().getParam("offset"),0);
    int limit = getIntOrDefault(routingContext.request().getParam("limit"),1000);
    getConfigRecordById(HARVESTER_HARVESTABLES_PATH, id).onComplete(idLookup -> {
      if (idLookup.succeeded()) {
        ProcessedHarvesterResponseGetById idLookUpResponse = idLookup.result();
        if (idLookup.result().wasNotFound()) {
          promise.complete(
              new ProcessedHarvesterResponseGet(
                  new JsonObject(), 404, "No harvestable found with ID " + id));
        } else if (idLookup.result().wasOK()) {
          getFailedRecords(id, offset, limit)
              .onComplete(result -> promise.complete(result.result()));
        } else {
          promise.fail("There was an error (" + idLookUpResponse.statusCode() + ") looking up "
              + HARVESTER_HARVESTABLES_PATH + "/" + id
              + " to get logs: " + idLookUpResponse.errorMessage());
        }
      } else {
        promise.fail("Could not look up harvest job " + HARVESTER_HARVESTABLES_PATH
            + "/" + id + " to get logs : " + idLookup.cause().getMessage());
      }
    });
    return promise.future();
  }

  /**
   * Gets failed records.
   */
  public Future<ProcessedHarvesterResponseGet> getFailedRecords(
      String harvestableId, int offset, int limit) {
    Promise<ProcessedHarvesterResponseGet> promise = Promise.promise();
    harvesterGetRequest(HARVESTER_HARVESTABLES_PATH + "/" + harvestableId + "/failed-records")
        .send(ar -> {
          ProcessedHarvesterResponseGet listResponse =
              new ProcessedHarvesterResponseGet(ar,
                  HARVESTER_HARVESTABLES_PATH + "/" + harvestableId + "/failed-records",null);
          JsonObject fileList = listResponse.jsonObject();
          JsonArray fileArray = fileList.getJsonArray("failedRecords");
          List<Future<ProcessedHarvesterResponseGetById>> failedRecordFutures = new ArrayList<>();
          for (int i = offset; i < Math.min(offset + limit, fileArray.size()); i++)  {
            failedRecordFutures.add(getFailedRecord(fileArray.getJsonObject(i)));
          }
          GenericCompositeFuture.all(failedRecordFutures).onComplete(results -> {
            JsonObject response = new JsonObject();
            JsonArray failedRecords = new JsonArray();
            response.put("failedRecords", failedRecords);
            for (int i = 0; i < results.result().list().size(); i++) {
              if (results.result().resultAt(i) != null) {
                JsonObject record = ((ProcessedHarvesterResponseGetById) results
                    .result().resultAt(i)).jsonObject();
                record.put("harvestableId", harvestableId);
                failedRecords.add(record);
              }
            }
            response.put("totalRecords", failedRecords.size());
            promise.complete(new ProcessedHarvesterResponseGet(response, 200, null));
          });
        });
    return promise.future();
  }

  /**
   * Gets a failed record by harvestable ID and record number.
   */
  public Future<ProcessedHarvesterResponseGetById> getFailedRecord(
      RoutingContext routingContext) {
    String id = routingContext.request().getParam("id");
    String num = routingContext.request().getParam("num");
    return getFailedRecord(id, num);
  }

  /**
   * Gets a failed record by harvestable ID and record number.
   */
  public Future<ProcessedHarvesterResponseGetById> getFailedRecord(
      String harvestableId, String recordNum) {
    Promise<ProcessedHarvesterResponseGetById> promise = Promise.promise();
    String failedRecordUri =
        HARVESTER_HARVESTABLES_PATH + "/" + harvestableId + "/failed-records/" + recordNum + ".xml";
    getConfigRecordById(HARVESTER_HARVESTABLES_PATH, harvestableId).onComplete(idLookup -> {
      if (idLookup.succeeded()) {
        ProcessedHarvesterResponseGetById idLookUpResponse = idLookup.result();
        if (idLookup.result().wasNotFound()) {
          promise.complete(idLookUpResponse);
        } else if (idLookup.result().wasOK()) {
          harvesterGetRequest(failedRecordUri)
              .send(ar ->  promise.complete(
                  new ProcessedHarvesterResponseGetById(ar,failedRecordUri,harvestableId,"")));
        } else {
          promise.fail("There was an error (" + idLookUpResponse.statusCode() + ") looking up "
              + HARVESTER_HARVESTABLES_PATH + "/" + harvestableId
              + " to get logs: " + idLookUpResponse.errorMessage());
        }
      } else {
        promise.fail("Could not look up harvest job " + HARVESTER_HARVESTABLES_PATH
            + "/" + harvestableId + " to get logs : " + idLookup.cause().getMessage());
      }
    });
    return promise.future();
  }

  private Future<ProcessedHarvesterResponseGetById> getFailedRecord(JsonObject entry) {
    Promise<ProcessedHarvesterResponseGetById> promise = Promise.promise();
    String uri = entry.getString("url");
    harvesterGetRequest(uri)
        .send(ar ->  {
          if (ar.result().bodyAsString() != null) {
            ProcessedHarvesterResponseGetById response =
                new ProcessedHarvesterResponseGetById(ar, uri, "", "");
            response.jsonObject()
                .put("timeStamp", entry.getJsonObject("file").getString("date"));
            response.jsonObject()
                .put("recordNumber", entry.getJsonObject("file").getString("name"));
            promise.complete(response);
          } else {
            promise.complete(null);
          }

        });
    return promise.future();
  }

  /**
   * Gets a script.
   */
  public Future<String> getScript(RoutingContext routingContext) {
    Promise<String> promise = Promise.promise();
    String id = routingContext.request().getParam("id");
    getConfigRecordById(ApiPaths.HARVESTER_STEPS_PATH, id).onComplete(getStep -> {
      if (getStep.result().found()) {
        String script = getStep.result().jsonObject().getString(STEP_SCRIPT_KEY);
        script = script.replaceAll("\\r[\\n]?", System.lineSeparator());
        promise.complete(script);
      } else {
        if (getStep.result().wasNotFound()) {
          promise.fail("Did not find step with ID " + id + " to GET script from");
        } else {
          promise.fail(getStep.result().errorMessage());
        }
      }
    });
    return promise.future();
  }

  /**
   * PUTs a transformation script.
   */
  public Future<ProcessedHarvesterResponsePut> putScript(RoutingContext routingContext) {
    Promise<ProcessedHarvesterResponsePut> promise = Promise.promise();
    String id = routingContext.request().getParam("id");
    String name = routingContext.request().getParam("name");
    if (name == null || name.isEmpty()) {
      responseText(routingContext, 400).end(
          "Parameter 'name' is mandatory when putting a script to the step. The value should "
              + "match the name of the step to PUT to.");
    } else {
      String script = routingContext.body().asString().replaceAll(System.lineSeparator(), "\n");
      getConfigRecordById(HARVESTER_STEPS_PATH, id).onComplete(getStep -> {
        if (getStep.result().found()) {
          JsonObject step = getStep.result().jsonObject();
          String stepType = step.getString(STEP_TYPE_KEY);
          String stepName = step.getString(STEP_NAME_KEY);
          if (!stepType.equals(VALID_STEP_TYPE)) {
            promise.complete(
                new ProcessedHarvesterResponsePut(
                    400,
                    "Can only PUT scripts to steps of type 'XmlTransformStep', step "
                        + id + ", '" + stepName + "' is '" + stepType + "'"));
          } else if (stepName.matches(name.replaceAll("\\*", ".*"))) {
            String validationResponse = validateScriptAsXml(script);
            if (validationResponse.equals("OK")) {
              step.put(STEP_SCRIPT_KEY, script);

              putConfigRecord(routingContext, ApiPaths.HARVESTER_STEPS_PATH, step, id)
                  .onComplete(putStep -> {
                    if (putStep.succeeded()) {
                      promise.complete(new ProcessedHarvesterResponsePut(
                          putStep.result().harvesterResponse, routingContext.request().path(), ""));
                    } else {
                      promise.complete(
                          new ProcessedHarvesterResponsePut(
                              putStep.result().statusCode(),putStep.cause().getMessage()));
                    }
                  });
            } else {
              promise.complete(
                  new ProcessedHarvesterResponsePut(
                      422,
                      "Validation of the script as XML failed, error message was: "
                      + validationResponse));
            }
          } else {
            promise.complete(
                new ProcessedHarvesterResponsePut(
                    400,
                    "A script with ID " + id + " was found, but it's name [" + stepName
                    + "] did not match the name provided [" + name + "]."));
          }
        } else if (getStep.result().wasNotFound()) {
          promise.complete(
              new ProcessedHarvesterResponsePut(
                  404, "Did not find step with ID " + id + " to PUT script to"));
        } else {
          promise.complete(new ProcessedHarvesterResponsePut(
              getStep.result().statusCode(), getStep.result().errorMessage()));
        }
      });
    }
    return promise.future();
  }

  /**
   * Checks for valid XML.
   */
  private static String validateScriptAsXml(String script) {
    try {
      DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      parser.parse(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)));
    } catch (ParserConfigurationException | IOException | SAXException pe) {
      return "Validation failed for script [ " + script + "]: " + pe.getMessage();
    }
    return "OK";
  }


  private static JsonArray insertStepIntoPipeline(
      JsonArray existingSteps,
      JsonObject updatingStep) {
    logger.debug("Inserting/moving step: " + updatingStep.encodePrettily());
    JsonArray updatedListOfSteps = new JsonArray();
    if (existingSteps == null) {
      updatedListOfSteps.add(updatingStep);
    } else {
      int positionOfUpdatingStep = Integer.parseInt(updatingStep.getString("position"));
      List<JsonObject> existingList = existingSteps.copy().getList();
      for (JsonObject existingStep : existingList) {
        if (existingStep.getString("id").equals(updatingStep.getString("id"))) {
          existingList.remove(existingStep);
          break;
        }
      }
      existingList.add(Math.min(positionOfUpdatingStep - 1, existingList.size()),
          updatingStep.copy());
      for (int i = 0; i < existingList.size(); i++) {
        JsonObject step = existingList.get(i);
        step.put("position", Integer.toString(i + 1));
        updatedListOfSteps.add(step);
      }
    }
    logger.debug("Constructed new pipeline " + updatedListOfSteps.encodePrettily());
    return updatedListOfSteps;
  }

  private static String buildQueryString(Map<String, String> parameterMap) {
    StringBuilder queryString = new StringBuilder();
    if (parameterMap != null) {
      Object[] keys = parameterMap.keySet().toArray();
      for (int i = 0; i < keys.length; i++) {
        if (i == 0) {
          queryString.append("?");
        } else {
          queryString.append("&");
        }
        String key = (String) keys[i];
        queryString.append(crosswalkRequestParameterNames.get(key));
        queryString.append("=");
        if (key.equals("query")) {
          queryString.append(
              URLEncoder.encode(
                  crosswalkCqlFieldNames(parameterMap.get(key)),
                  StandardCharsets.UTF_8));
        } else {
          queryString.append(
              URLEncoder.encode(parameterMap.get(key), StandardCharsets.UTF_8));
        }
      }
    }
    return queryString.toString();
  }

  private String mapToHarvesterPath(RoutingContext routingContext) {
    return harvesterPathByRequestPath.get(
        routingContext.request().path().replaceAll("/" + routingContext.pathParam("id") + "$",
            ""));
  }

  /**
   * Gets supported get request parameters.
   */
  public static Map<String, String> getSupportedGetRequestParameters(HttpServerRequest request) {
    Map<String, String> requestParameterMap = new HashMap<>();
    for (String param : supportedGetRequestParameters) {
      String paramValue = request.getParam(param);
      if (paramValue != null && !paramValue.isEmpty()) {
        requestParameterMap.put(param, paramValue);
      }
    }
    return requestParameterMap;
  }

  private String aclFilter(String tenant) {
    return LegacyServiceConfig.filterByTenant ? "?acl=" + tenant : "";
  }

  private String andAclFilter(String tenant) {
    return LegacyServiceConfig.filterByTenant ? "&acl=" + tenant : "";
  }

  /**
   * Creates harvester get request.
   */
  public HttpRequest<Buffer> harvesterGetRequest(String pathAndQuery) {
    HttpRequest<Buffer> request = restClient.get(LegacyServiceConfig.harvesterHost, pathAndQuery);
    if (LegacyServiceConfig.hasHarvesterPort()) {
      request.port(LegacyServiceConfig.harvesterPort);
    }
    if (LegacyServiceConfig.harvesterRequiresSsl()) {
      request.ssl(true);
    }
    if (LegacyServiceConfig.hasBasicAuthForHarvester()) {
      request.basicAuthentication(
          LegacyServiceConfig.basicAuthUsername, LegacyServiceConfig.basicAuthPassword);
    }
    return request;
  }

  /**
   * Creates harvester post request.
   */
  public HttpRequest<Buffer> harvesterPostRequest(String path) {
    HttpRequest<Buffer> request = restClient.post(LegacyServiceConfig.harvesterHost, path);
    if (LegacyServiceConfig.hasHarvesterPort()) {
      request.port(LegacyServiceConfig.harvesterPort);
    }
    if (LegacyServiceConfig.harvesterRequiresSsl()) {
      request.ssl(true);
    }
    if (LegacyServiceConfig.hasBasicAuthForHarvester()) {
      request.basicAuthentication(
          LegacyServiceConfig.basicAuthUsername, LegacyServiceConfig.basicAuthPassword);
    }
    request.putHeader(HEADER_CONTENT_TYPE, "application/xml");
    return request;
  }

  /**
   * Creates harvester delete request.
   */
  public HttpRequest<Buffer> harvesterDeleteRequest(String path) {
    HttpRequest<Buffer> request = restClient.delete(LegacyServiceConfig.harvesterHost, path);
    if (LegacyServiceConfig.hasHarvesterPort()) {
      request.port(LegacyServiceConfig.harvesterPort);
    }
    if (LegacyServiceConfig.harvesterRequiresSsl()) {
      request.ssl(true);
    }
    if (LegacyServiceConfig.hasBasicAuthForHarvester()) {
      request.basicAuthentication(
          LegacyServiceConfig.basicAuthUsername, LegacyServiceConfig.basicAuthPassword);
    }
    return request;
  }

  /**
   * Creates harvester put request.
   */
  public HttpRequest<Buffer> harvesterPutRequest(String path) {
    HttpRequest<Buffer> request = restClient.put(LegacyServiceConfig.harvesterHost, path);
    if (LegacyServiceConfig.hasHarvesterPort()) {
      request.port(LegacyServiceConfig.harvesterPort);
    }
    if (LegacyServiceConfig.harvesterRequiresSsl()) {
      request.ssl(true);
    }
    if (LegacyServiceConfig.hasBasicAuthForHarvester()) {
      request.basicAuthentication(
          LegacyServiceConfig.basicAuthUsername, LegacyServiceConfig.basicAuthPassword);
    }
    request.putHeader(HEADER_CONTENT_TYPE, "application/xml");
    return request;
  }

  /**
   * Parses string to int, returns default if parsing fails.
   */
  public static int getIntOrDefault(String number, int defaultInt) {
    try {
      return Integer.parseInt(number);
    } catch (NumberFormatException | NullPointerException e) {
      return defaultInt;
    }
  }

  /**
   * Applies offset and limit to lines of text.
   */
  public static String pagingPlainText(String linesOfText, int offset, int limit) {
    if (linesOfText != null) {
      BufferedReader bufReader = new BufferedReader(new StringReader(linesOfText));
      try {
        StringBuilder page = new StringBuilder();
        String line;
        for (int i = 0; i < offset + limit && ((line = bufReader.readLine()) != null); i++)  {
          if (i >= offset) {
            page.append(line).append(System.lineSeparator());
          }
        }
        return page.toString();
      } catch (IOException e) {
        logger.error("Error reading lines of plain text: " + e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

}
