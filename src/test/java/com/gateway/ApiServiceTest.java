package com.gateway;

import com.gateway.service.ApiService;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ApiServiceTest {

  @Mock
  private WebClient webClient;

  @Mock
  private io.vertx.ext.web.client.HttpRequest<Buffer> request;

  @Mock
  private HttpResponse<Buffer> response;

  private ApiService apiService;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    apiService = new ApiService(webClient);
  }

  @Test
  void testFetchCallsGetAbs(VertxTestContext testContext) {
    String url = "https://example.com/api";

    when(webClient.getAbs(url)).thenReturn(request);
    when(request.send()).thenReturn(Future.succeededFuture(response));
    when(response.statusCode()).thenReturn(200);

    apiService.fetch(url)
      .onComplete(testContext.succeeding(result -> {
        testContext.verify(() -> {
          verify(webClient).getAbs(url);
          verify(request).send();
          assertEquals(200, result.statusCode());
          testContext.completeNow();
        });
      }));
  }

  @Test
  void testFetchFutureCompletes(VertxTestContext testContext) {
    String url = "https://example.com/api";

    when(webClient.getAbs(url)).thenReturn(request);
    when(request.send()).thenReturn(Future.succeededFuture(response));

    Future<HttpResponse<Buffer>> future = apiService.fetch(url);

    future.onComplete(testContext.succeeding(result -> {
      testContext.verify(() -> {
        assertNotNull(result);
        testContext.completeNow();
      });
    }));
  }

  @Test
  void testFetchFutureFails(VertxTestContext testContext) {
    String url = "https://example.com/api";
    RuntimeException error = new RuntimeException("Connection failed");

    when(webClient.getAbs(url)).thenReturn(request);
    when(request.send()).thenReturn(Future.failedFuture(error));

    apiService.fetch(url)
      .onComplete(testContext.failing(err -> {
        testContext.verify(() -> {
          assertEquals("Connection failed", err.getMessage());
          testContext.completeNow();
        });
      }));
  }
}
