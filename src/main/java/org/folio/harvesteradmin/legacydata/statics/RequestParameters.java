package org.folio.harvesteradmin.legacydata.statics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RequestParameters {
  private static final String REQUEST_PARAMETER_QUERY = "query";
  private static final String REQUEST_PARAMETER_LIMIT = "limit";
  private static final String REQUEST_PARAMETER_OFFSET = "offset";
  private static final String REQUEST_PARAMETER_ORDER_BY = "orderBy";

  public static final Set<String> supportedGetRequestParameters = new HashSet<>();

  static {
    supportedGetRequestParameters.add(REQUEST_PARAMETER_QUERY);
    supportedGetRequestParameters.add(REQUEST_PARAMETER_LIMIT);
    supportedGetRequestParameters.add(REQUEST_PARAMETER_OFFSET);
    supportedGetRequestParameters.add(REQUEST_PARAMETER_ORDER_BY);
  }

  public static final Map<String, String> crosswalkRequestParameterNames = new HashMap<>();

  static {
    crosswalkRequestParameterNames.put(REQUEST_PARAMETER_QUERY, "query");
    crosswalkRequestParameterNames.put(REQUEST_PARAMETER_LIMIT, "max");
    crosswalkRequestParameterNames.put(REQUEST_PARAMETER_OFFSET, "start");
    crosswalkRequestParameterNames.put(REQUEST_PARAMETER_ORDER_BY, "sort");
  }

  private static final Map<String, String> crosswalkCqlFieldNames = new HashMap<>();

  static {
    crosswalkCqlFieldNames.put("transformationId", "transformation.id");
    crosswalkCqlFieldNames.put("storageId", "storage.id");
  }

  /**
   * Translate transformationId to transformation.id.
   * And storageId to storage.id.
   */
  public static String crosswalkCqlFieldNames(String cqlQuery) {
    String query = cqlQuery;
    for (String key : crosswalkCqlFieldNames.keySet()) {
      query = query.replaceAll(key, crosswalkCqlFieldNames.get(key));
    }
    return query;
  }
}
