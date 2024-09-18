package org.folio.harvesteradmin.test;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import org.folio.okapi.common.XOkapiHeaders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Statics {

    static final String TENANT = "mha_test";
    public static final Header CONTENT_TYPE_JSON = new Header("Content-Type", "application/json");
    public static final Header CONTENT_TYPE_XML = new Header("Content-Type", "application/xml");
    public static final Header OKAPI_TOKEN = new Header(XOkapiHeaders.TOKEN,"eyJhbGciOiJIUzUxMiJ9eyJzdWIiOiJhZG1pbiIsInVzZXJfaWQiOiI3OWZmMmE4Yi1kOWMzLTViMzktYWQ0YS0wYTg0MDI1YWIwODUiLCJ0ZW5hbnQiOiJ0ZXN0X3RlbmFudCJ9BShwfHcNClt5ZXJ8ImQTMQtAM1sQEnhsfWNmXGsYVDpuaDN3RVQ9");


    public static int PORT_HARVESTER_ADMIN = 9230;
    public static String BASE_URI_HARVESTER_ADMIN = "http://localhost:" + PORT_HARVESTER_ADMIN;
    public static int PORT_OKAPI = 9031;
    public static String BASE_URI_OKAPI = "http://localhost:" + PORT_OKAPI;

    public static final Header OKAPI_URL = new Header (XOkapiHeaders.URL, BASE_URI_OKAPI);

}
