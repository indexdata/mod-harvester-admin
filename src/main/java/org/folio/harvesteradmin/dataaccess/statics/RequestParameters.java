package org.folio.harvesteradmin.dataaccess.statics;

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

  public static final Map<String, String> folioToLegacyParameter = new HashMap<>();

  static {
    folioToLegacyParameter.put(REQUEST_PARAMETER_QUERY, "query");
    folioToLegacyParameter.put(REQUEST_PARAMETER_LIMIT, "max");
    folioToLegacyParameter.put(REQUEST_PARAMETER_OFFSET, "start");
    folioToLegacyParameter.put(REQUEST_PARAMETER_ORDER_BY, "sort");
  }

}
