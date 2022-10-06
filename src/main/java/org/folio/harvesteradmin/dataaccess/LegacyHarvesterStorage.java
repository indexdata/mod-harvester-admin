package org.folio.harvesteradmin.dataaccess;

import static org.folio.harvesteradmin.dataaccess.HarvesterApiClient.NOT_FOUND;
import static org.folio.harvesteradmin.dataaccess.ScriptHandler.STEP_NAME_KEY;
import static org.folio.harvesteradmin.dataaccess.ScriptHandler.STEP_SCRIPT_KEY;
import static org.folio.harvesteradmin.dataaccess.ScriptHandler.STEP_TYPE_KEY;
import static org.folio.harvesteradmin.dataaccess.ScriptHandler.VALID_STEP_TYPE;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_HARVESTABLES_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_STEPS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_TRANSFORMATIONS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.HARVESTER_TSAS_PATH;
import static org.folio.harvesteradmin.dataaccess.statics.ApiPaths.harvesterPathByRequestPath;
import static org.folio.harvesteradmin.dataaccess.statics.EntityRootNames.mapToNameOfRootOfEntity;
import static org.folio.harvesteradmin.dataaccess.statics.RequestParameters.folioToLegacyParameter;
import static org.folio.harvesteradmin.dataaccess.statics.RequestParameters.supportedGetRequestParameters;
import static org.folio.harvesteradmin.dataaccess.statics.ResultSetRootNames.rootOfResultSetByHarvesterPath;
import static org.folio.okapi.common.HttpResponse.responseText;

import io.vertx.core.CompositeFuture;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponseDelete;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponseGet;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponseGetById;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponsePost;
import org.folio.harvesteradmin.dataaccess.responsehandlers.ProcessedHarvesterResponsePut;
import org.folio.harvesteradmin.dataaccess.statics.ApiPaths;
import org.folio.harvesteradmin.dataaccess.statics.EntityRootNames;
import org.folio.harvesteradmin.dataaccess.statics.LegacyServiceConfig;
import org.folio.tlib.util.TenantUtil;
import org.xml.sax.SAXException;


public class LegacyHarvesterStorage {


  private final String tenant;
  private static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final int BAD_REQUEST = 400;
  public static final int INTERNAL_SERVER_ERROR = 500;
  public static final int NO_CONTENT = 204;
  public static final int CREATED = 201;
  protected final WebClient restClient;
  private static final Logger logger = LogManager.getLogger(LegacyHarvesterStorage.class);

  public LegacyHarvesterStorage(Vertx vertx, String tenant) {
    this.tenant = tenant;
    restClient = WebClient.create(vertx);
  }

  /**
   * Gets config records based on path and tenant.
   */
  public Future<ProcessedHarvesterResponseGet> getConfigRecords(RoutingContext routingContext) {
    String harvesterPath = mapToHarvesterPath(routingContext);
    Map<String, String> queryParameters =
        getSupportedGetRequestParameters(routingContext.request());
    Promise<ProcessedHarvesterResponseGet> promise = Promise.promise();
    String query = buildQueryString(queryParameters);
    String pathAndQuery = harvesterPath
        + (query.isEmpty() ? aclFilter(tenant) : query + andAclFilter(tenant));
    harvesterGetRequest(pathAndQuery).send(ar -> promise.complete(
        new ProcessedHarvesterResponseGet(ar, harvesterPath, query)));
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
    logger.info("GET " + harvesterPath + "/" + id);
    harvesterGetRequest(harvesterPath + "/" + id).send(ar -> promise.complete(
        new ProcessedHarvesterResponseGetById(ar, harvesterPath, id, tenant)));
    return promise.future();

  }

  /**
   * Posts config record, after checking the ID, and retrieves the record for the response.
   */
  public Future<ProcessedHarvesterResponsePost> postConfigRecord(RoutingContext routingContext) {
    Promise<ProcessedHarvesterResponsePost> promise = Promise.promise();
    String harvesterPath = mapToHarvesterPath(routingContext);
    JsonObject jsonToPost = routingContext.body().asJsonObject();
    String idInPostedRecord = jsonToPost.getString("id");
    if (idInPostedRecord == null) {
      doPostConfigRecord(routingContext)
          .onComplete(post -> {
            promise.complete(post.result());
          });
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
                doPostConfigRecord(routingContext).onComplete(post -> {
                      promise.complete(post.result());
                    }
                );
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
    try {
      String xml = JsonToHarvesterXml.convertToHarvesterRecord(json,
          mapToNameOfRootOfEntity(harvesterPath), tenant);
      harvesterPostRequest(harvesterPath).sendBuffer(Buffer.buffer(xml), ar -> {
        if (ar.succeeded()) {
          String location = ar.result().getHeader("Location");
          if (ar.result().statusCode() == CREATED && location != null) {
            String idFromLocation = location.split("/")[location.split("/").length - 1];
            logger.info("Got id from location: " + location);
            getConfigRecordById(harvesterPath, idFromLocation).onComplete(
                // going to return 500, internal server error if not found, 201, Created if found
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
      promise.complete(new ProcessedHarvesterResponsePost(INTERNAL_SERVER_ERROR, e.getMessage()));
    }
    return promise.future();

  }

  protected Future<ProcessedHarvesterResponsePut> putConfigRecord(RoutingContext routingContext,
                                                         JsonObject jsonToPut,
                                                         String generatedId,
                                                         String harvesterPath,
                                                         String tenant) {
    Promise<ProcessedHarvesterResponsePut> promisedResponse = Promise.promise();
    String id = (generatedId == null ? routingContext.request().getParam("id") : generatedId);
    getConfigRecordById(harvesterPath, id).onComplete(
        idLookUp -> {    // going to return 404 if not found
          if (idLookUp.succeeded()) {
            if (idLookUp.result().wasNotFound()) {
              promisedResponse.fail(
                  idLookUp.result().errorMessage() + " Status code: " + idLookUp.result()
                      .statusCode());
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
                  new ProcessedHarvesterResponsePut(INTERNAL_SERVER_ERROR,
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
    String id = routingContext.request().getParam("id");
    String harvesterPath = mapToHarvesterPath(routingContext);
    String requestUri = routingContext.request().absoluteURI();
    return deleteConfigRecord(requestUri, id, harvesterPath);
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


  private Future<ProcessedHarvesterResponsePost> doPostAndPutTransformation(
      RoutingContext routingContext) {
    JsonObject transformationJson = routingContext.body().asJsonObject();
    logger.info("About to POST-then-PUT " + transformationJson.encodePrettily());
    Map<String, String> typeToEmbeddedTypeMap = new HashMap<>();
    typeToEmbeddedTypeMap.put("CustomTransformStep", "customTransformationStep");
    typeToEmbeddedTypeMap.put("XmlTransformStep", "xmlTransformationStep");
    JsonArray stepsIdsJson =
        transformationJson.containsKey("stepAssociations") ? transformationJson.getJsonArray(
            "stepAssociations").copy() : new JsonArray();
    transformationJson.remove("stepAssociations");
    List<Future> stepFutures = new ArrayList<>();
    for (Object arrayObject : stepsIdsJson) {
      JsonObject step = (JsonObject) arrayObject;
      String stepId = step.containsKey("step") ? step.getJsonObject("step").getString("id")
          : step.getString("stepId");
      logger.info("Looking up input step ID: " + stepId);
      stepFutures.add(getConfigRecordById(HARVESTER_STEPS_PATH, stepId));
    }
    Promise<ProcessedHarvesterResponsePost> promise = Promise.promise();
    CompositeFuture.all(stepFutures).onComplete(steps -> {
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
                        logger.info("Looking at step JSON: " + stepJson.encodePrettily());
                        JsonObject tsaJson = new JsonObject();
                        tsaJson.put("id", Integer.toString(getRandomInt()));
                        tsaJson.put("position", Integer.toString(i + 1));
                        tsaJson.put("step", new JsonObject());
                        tsaJson.getJsonObject("step")
                            .put("entityType",
                                typeToEmbeddedTypeMap.get(stepJson.getString("type")));
                        tsaJson.getJsonObject("step").put("id", stepJson.getString("id"));
                        tsaJson.put("transformation", createdTransformation.getString("id"));
                        createdTransformation.getJsonArray("stepAssociations").add(tsaJson);
                      }
                      putConfigRecord(routingContext, createdTransformation,
                          createdTransformation.getString("id"), HARVESTER_TRANSFORMATIONS_PATH,
                          tenant).onComplete(putResponse -> {
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
                                  " Failed to POST (with subsequent PUT and GET) of Transformation."
                                      + " PUT failed with "
                                      + putResponse.result().errorMessage()));
                            }
                          });
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
    String transformationId = incomingTsa.getString("transformation");
    String stepId = incomingTsa.getJsonObject("step").getString("id");
    Promise<ProcessedHarvesterResponsePost> promise = Promise.promise();
    getConfigRecordById(HARVESTER_TRANSFORMATIONS_PATH, transformationId).onComplete(
        checkTransformation -> {
          if (!checkTransformation.result().found()) {
            promise.complete(
                new ProcessedHarvesterResponsePost(
                    422,
                    "Could not create transformation-step association. Transformation "
                    + transformationId + " not found."));
          } else {
            getConfigRecordById(HARVESTER_STEPS_PATH, stepId).onComplete(
                checkStep -> {
                  if (!checkStep.result().found()) {
                    promise.complete(
                        new ProcessedHarvesterResponsePost(
                            422,
                            "Could not create transformation-step association. Step " + stepId
                                + " not found."
                        )
                    );
                  } else {
                    doPostConfigRecord(routingContext).onComplete(postedTsa -> {
                      if (postedTsa.succeeded() && postedTsa.result() != null) {
                        JsonObject transformationStepAssociation =
                            postedTsa.result().jsonObject();
                        logger.debug(
                            "Posted TSA, got: "
                                + transformationStepAssociation.encodePrettily());
                        // Get the transformation
                        getConfigRecordById(HARVESTER_TRANSFORMATIONS_PATH, transformationId)
                            .onComplete(
                              // going to return 422 if not found
                              transformationById -> {
                                if (transformationById.succeeded()
                                    && transformationById.result()
                                    .found()) {
                                  // Insert the tsa in the transformation JSON
                                  JsonObject transformation =
                                      transformationById.result().jsonObject();
                                  logger.debug(
                                      "Got basic transformation "
                                          + transformation.encodePrettily());
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
                                        HARVESTER_TRANSFORMATIONS_PATH
                                        + "/"
                                        + transformationId).sendBuffer(Buffer.buffer(xml),
                                          ar -> {
                                            if (ar.succeeded()) {
                                              if (ar.result().statusCode() == NO_CONTENT) {
                                                promise.complete(
                                                    postedTsa.result()
                                                );
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
                                                          + transformationId + ": "
                                                          + ar.cause().getMessage()
                                                  )
                                              );
                                            }
                                          });
                                    } catch
                                    (TransformerException | ParserConfigurationException xe) {
                                      logger.error("Error parsing json " + transformation);
                                      promise.complete(
                                          new ProcessedHarvesterResponsePost(
                                              INTERNAL_SERVER_ERROR,
                                              "Error parsing json " + transformation
                                          )
                                      );
                                    }
                                  } else {
                                    // If the transformation doesn't exist: respond with 422
                                    promise.complete(
                                        new ProcessedHarvesterResponsePost(
                                            422,
                                            "Problem POSTing "
                                                + HARVESTER_TSAS_PATH + "/"
                                                + ", could not retrieve referenced "
                                                + "transformation with ID " + transformationId
                                                + ": "
                                                + transformationById.cause()
                                        )
                                    );
                                  }
                              });
                      } else {
                        promise.complete(
                            new ProcessedHarvesterResponsePost(
                                422,
                                "Problem POSTing to "
                                    + HARVESTER_TSAS_PATH
                                    + "/"
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
    getConfigRecordById(HARVESTER_HARVESTABLES_PATH, id).onComplete(idLookup -> {
      if (idLookup.succeeded()) {
        ProcessedHarvesterResponseGetById idLookUpResponse = idLookup.result();
        if (idLookup.result().wasNotFound()) {
          promise.fail(idLookUpResponse.errorMessage());
        } else if (idLookup.result().wasOK()) {
          harvesterGetRequest(HARVESTER_HARVESTABLES_PATH + "/" + id + "/log")
              .send(ar -> promise.complete(ar.result()));
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
    String tenant = TenantUtil.tenant(routingContext);
    String id = routingContext.request().getParam("id");
    String name = routingContext.request().getParam("name");
    if (name == null || name.isEmpty()) {
      responseText(routingContext, 400).end(
          "Parameter 'name' is mandatory when putting a script to the step. The value should "
              + "match the name of the step to PUT to.");
    } else {
      String script = routingContext.getBodyAsString().replaceAll(System.lineSeparator(), "\n");
      getConfigRecordById(HARVESTER_STEPS_PATH, id).onComplete(getStep -> {
        if (getStep.result().found()) {
          JsonObject step = getStep.result().jsonObject();
          String stepType = step.getString(STEP_TYPE_KEY);
          String stepName = step.getString(STEP_NAME_KEY);
          if (!stepType.equals(VALID_STEP_TYPE)) {
            promise.fail("400: Can only PUT scripts to steps of type 'XmlTransformStep', step "
                + id + ", '" + stepName + "' is '" + stepType + "'");
          } else if (stepName.matches(name.replaceAll("\\*", ".*"))) {

            String validationResponse = validateScriptAsXml(script);
            if (validationResponse.equals("OK")) {
              step.put(STEP_SCRIPT_KEY, script);

              putConfigRecord(routingContext, step, id, ApiPaths.HARVESTER_STEPS_PATH,
                  tenant).onComplete(putStep -> {
                    if (putStep.succeeded()) {
                      promise.complete(putStep.result());
                    } else {
                      promise.fail(putStep.cause().getMessage());
                    }
                  });
            } else {
              promise.fail("422: Validation of the script as XML failed, error message was: "
                      + validationResponse);
            }
          } else {
            promise.fail("400: A script with ID " + id + " was found, but it's name [" + stepName
                    + "] did not match the name provided [" + name + "].");
          }
        } else if (getStep.result().wasNotFound()) {
          promise.fail("404: Did not find step with ID " + id + " to PUT script to");
        } else {
          promise.fail(getStep.result().statusCode() + ": " + getStep.result().errorMessage());
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
    logger.debug("Constructed new pipeline " + updatedListOfSteps.encodePrettily());
    return updatedListOfSteps;
  }

  private int getRandomInt() {
    Random rand = new Random();
    return rand.nextInt(900000000) + 100000000;
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
        queryString.append(folioToLegacyParameter.get(key));
        queryString.append("=");
        queryString.append(
            URLEncoder.encode(parameterMap.get(key), StandardCharsets.UTF_8));
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
   * Maps to name of root of result set.
   */
  public static String mapToNameOfRootOfResultSet(String harvesterPath) {
    return rootOfResultSetByHarvesterPath.get(harvesterPath);
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

}
