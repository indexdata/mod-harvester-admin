package org.folio.harvesteradmin.foliodata;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5)
@WireMockTest(httpPort=9129)
abstract class ClientTestBase {

  @Test
  void success(Vertx vertx) {
    var routingContext = routingContext(vertx);
    assertThat(getStringValue(routingContext), is("val"));
  }

  @Test
  void emptyList(Vertx vertx) {
    var routingContext = routingContext(vertx);
    stubFor(get(anyUrl()).willReturn(ok("""
                    { "items": [], "configs": [] }
                    """)));
    var e = assertThrows(RuntimeException.class, () -> getStringValue(routingContext));
    assertThat(e.getCause().getCause(), is(instanceOf(IndexOutOfBoundsException.class)));
  }

  @Test
  void emptyResponse(Vertx vertx) {
    var routingContext = routingContext(vertx);
    stubFor(get(anyUrl()).willReturn(ok()));
    var e = assertThrows(RuntimeException.class, () -> getStringValue(routingContext));
    assertThat(e.getCause().getCause(), is(instanceOf(NullPointerException.class)));
  }

  @Test
  void notFound(Vertx vertx) {
    var routingContext = routingContext(vertx);
    stubFor(get(anyUrl()).willReturn(WireMock.notFound()));
    var e = assertThrows(RuntimeException.class, () -> getStringValue(routingContext));
    assertThat(e.getMessage(), containsString("404"));
  }

  abstract Future<String> getStringValueFuture(RoutingContext routingContext, String key1, String key2);

  String getStringValue(RoutingContext routingContext) {
    try {
      return getStringValueFuture(routingContext, "mod-x", "lookup")
          .toCompletionStage().toCompletableFuture()
          .get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  RoutingContext routingContext(Vertx vertx) {
    stubFor(get("/settings/entries?query=scope%3D%3D%22mod-x%22+and+key%3D%3D%22lookup%22")
        .willReturn(ok("""
                        { "items": [ { "value": "val" } ] }
                        """)));
    stubFor(get("/configurations/entries?query=module%3D%3D%22mod-x%22+and"
            + "+configName%3D%3D%22lookup%22+and+enabled%3Dtrue")
        .willReturn(ok("""
                        { "configs": [ { "value": "val" } ] }
                        """)));
    var request = mock(HttpServerRequest.class);
    when(request.headers()).thenReturn(HeadersMultiMap.headers()
        .add("X-Okapi-Url", "http://localhost:9129"));
    when(request.getHeader("X-Okapi-Url")).thenReturn("http://localhost:9129");
    var routingContext = mock(RoutingContext.class);
    when(routingContext.vertx()).thenReturn(vertx);
    when(routingContext.request()).thenReturn(request);
    return routingContext;
  }


}
