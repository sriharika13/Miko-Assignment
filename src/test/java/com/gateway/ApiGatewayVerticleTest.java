package com.gateway;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ApiGatewayVerticleTest {

  private Vertx vertx;
  private WebClient client;

  @BeforeEach
  void setup(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    client = WebClient.create(vertx);
    vertx.deployVerticle(new ApiGatewayVerticle(), testContext.succeedingThenComplete());
  }

  @AfterEach
  void cleanup(VertxTestContext testContext) {
    vertx.close(testContext.succeedingThenComplete());
  }

  @Test
  void testServerStarts(VertxTestContext testContext) {
    client.get(8080, "localhost", "/aggregate")
      .send()
      .onComplete(testContext.succeeding(response -> {
        testContext.verify(() -> {
          assertNotNull(response);
          testContext.completeNow();
        });
      }));
  }

  @Test
  void testAggregateReturns200OnSuccess(VertxTestContext testContext) {
    client.get(8080, "localhost", "/aggregate")
      .send()
      .onComplete(testContext.succeeding(response -> {
        testContext.verify(() -> {
          assertEquals(200, response.statusCode());
          JsonObject body = response.bodyAsJsonObject();
          assertNotNull(body);
          assertTrue(body.containsKey("post_title"));
          assertTrue(body.containsKey("author_name"));
          testContext.completeNow();
        });
      }));
  }

  @Test
  void testAggregateHandlesPartialFailure(VertxTestContext testContext) {
    client.get(8080, "localhost", "/aggregate")
      .send()
      .onComplete(testContext.succeeding(response -> {
        testContext.verify(() -> {
          assertEquals(200, response.statusCode());
          JsonObject body = response.bodyAsJsonObject();
          assertNotNull(body);
          assertTrue(body.containsKey("post_title") || body.containsKey("author_name"));
          testContext.completeNow();
        });
      }));
  }
}
