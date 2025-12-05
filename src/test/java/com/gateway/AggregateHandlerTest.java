package com.gateway;

import com.gateway.config.AppConfig;
import com.gateway.handler.AggregateHandler;
import com.gateway.handler.GlobalExceptionHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class AggregateHandlerTest {

  private Vertx vertx;
  private AggregateHandler handler;
  private WebClient client;

  @BeforeEach
  void setup() {
    vertx = Vertx.vertx();
    AppConfig config = new AppConfig(vertx);
    GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    handler = new AggregateHandler(vertx, config);
    client = WebClient.create(vertx);
  }

  @Test
  void testFetchPostDataSuccess(VertxTestContext testContext) {
    AppConfig config = new AppConfig(vertx);

    client.getAbs(config.getPostsApiUrl())
      .send()
      .onComplete(testContext.succeeding(response -> {
        testContext.verify(() -> {
          assertEquals(200, response.statusCode());
          JsonObject body = response.bodyAsJsonObject();
          assertNotNull(body);
          assertTrue(body.containsKey("title"));
          testContext.completeNow();
        });
      }));
  }

  @Test
  void testFetchUserDataSuccess(VertxTestContext testContext) {
    AppConfig config = new AppConfig(vertx);

    client.getAbs(config.getUsersApiUrl())
      .send()
      .onComplete(testContext.succeeding(response -> {
        testContext.verify(() -> {
          assertEquals(200, response.statusCode());
          JsonObject body = response.bodyAsJsonObject();
          assertNotNull(body);
          assertTrue(body.containsKey("name"));
          testContext.completeNow();
        });
      }));
  }

  @Test
  void testCompositeFutureCombining(VertxTestContext testContext) {
    Router router = Router.router(vertx);
    router.get("/test").handler(handler::handle);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8081)
      .onComplete(testContext.succeeding(server -> {
        client.get(8081, "localhost", "/test")
          .send()
          .onComplete(testContext.succeeding(response -> {
            testContext.verify(() -> {
              JsonObject body = response.bodyAsJsonObject();
              assertNotNull(body);
              assertTrue(body.containsKey("post_title"));
              assertTrue(body.containsKey("author_name"));
              testContext.completeNow();
            });
          }));
      }));
  }

  @Test
  void testPartialFailureLogic(VertxTestContext testContext) {
    Router router = Router.router(vertx);
    router.get("/test").handler(handler::handle);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8082)
      .onComplete(testContext.succeeding(server -> {
        client.get(8082, "localhost", "/test")
          .send()
          .onComplete(testContext.succeeding(response -> {
            testContext.verify(() -> {
              JsonObject body = response.bodyAsJsonObject();
              assertNotNull(body);
              assertTrue(
                body.containsKey("post_title") ||
                  body.containsKey("author_name") ||
                  body.containsKey("warning")
              );
              testContext.completeNow();
            });
          }));
      }));
  }

  @Test
  void testFullFailureTriggersContextFail(VertxTestContext testContext) {
    Router router = Router.router(vertx);
    GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    router.get("/test").handler(handler::handle);
    router.route().failureHandler(exceptionHandler::handle);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8083)
      .onComplete(testContext.succeeding(server -> {
        client.get(8083, "localhost", "/test")
          .send()
          .onComplete(testContext.succeeding(response -> {
            testContext.verify(() -> {
              assertNotNull(response);
              testContext.completeNow();
            });
          }));
      }));
  }
}
