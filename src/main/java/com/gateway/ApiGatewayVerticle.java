package com.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import com.gateway.config.AppConfig;
import com.gateway.handler.AggregateHandler;
import com.gateway.handler.GlobalExceptionHandler;

public class ApiGatewayVerticle extends AbstractVerticle {

  private AppConfig config;

  @Override
  public void start(Promise<Void> startPromise) {
    config = new AppConfig(vertx);

    GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    AggregateHandler aggregateHandler = new AggregateHandler(vertx, config);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.get("/aggregate").handler(aggregateHandler::handle);
    router.route().failureHandler(exceptionHandler::handle);

    HttpServer server = vertx.createHttpServer();

    server.requestHandler(router)
      .listen(config.getServerPort())
      .onSuccess(http -> {
        System.out.println("API Gateway started on port " + config.getServerPort());
        startPromise.complete();
      })
      .onFailure(err -> {
        System.err.println("Failed to start server: " + err.getMessage());
        startPromise.fail(err);
      });
  }

  public static void main(String[] args) {

    VertxOptions options = new VertxOptions()
      .setWorkerPoolSize(20)
      .setEventLoopPoolSize(4)
      .setMaxWorkerExecuteTime(60_000_000_000L); // 60 seconds

    Vertx vertx = Vertx.vertx(options);

    vertx.deployVerticle(new ApiGatewayVerticle())
      .onSuccess(id -> System.out.println("Verticle deployed successfully with ID: " + id))
      .onFailure(err -> {
        System.err.println("Failed to deploy verticle: " + err.getMessage());
        vertx.close();
      });
  }
}
