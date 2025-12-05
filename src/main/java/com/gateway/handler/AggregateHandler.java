package com.gateway.handler;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import com.gateway.config.AppConfig;
import com.gateway.exception.ApiException;
import com.gateway.service.ApiService;

public class AggregateHandler {

  private final AppConfig config;
  private final ApiService apiService;
  private final CircuitBreaker postsBreaker;
  private final CircuitBreaker usersBreaker;

  public AggregateHandler(Vertx vertx, AppConfig config) {
    this.config = config;
    this.apiService = new ApiService(config.getWebClient());

    CircuitBreakerOptions breakerOptions = new CircuitBreakerOptions()
      .setMaxFailures(5)
      .setTimeout(config.getHttpTimeout())
      .setResetTimeout(30000)
      .setFallbackOnFailure(true);

    this.postsBreaker = CircuitBreaker.create("posts-breaker", vertx, breakerOptions);
    this.usersBreaker = CircuitBreaker.create("users-breaker", vertx, breakerOptions);
  }

  public void handle(RoutingContext context) {
    Future<JsonObject> postsFuture = postsBreaker.execute(promise ->
      fetchPostData()
        .onSuccess(promise::complete)
        .onFailure(promise::fail)
    );

    Future<JsonObject> usersFuture = usersBreaker.execute(promise ->
      fetchUserData()
        .onSuccess(promise::complete)
        .onFailure(promise::fail)
    );

    CompositeFuture.all(postsFuture, usersFuture)
      .onSuccess(result -> {
        JsonObject postData = postsFuture.result();
        JsonObject userData = usersFuture.result();

        JsonObject response = aggregateResponse(postData, userData);
        sendSuccessResponse(context, response);
      })
      .onFailure(error -> {
        handleAggregateFailure(context, error, postsFuture, usersFuture);
      });
  }

  private Future<JsonObject> fetchPostData() {
    return apiService.fetch(config.getPostsApiUrl())
      .compose(response -> {
        if (response.statusCode() == 200) {
          return Future.succeededFuture(response.bodyAsJsonObject());
        } else {
          return Future.failedFuture(
            new ApiException(
              "Failed to fetch post data",
              response.statusCode(),
              "POST_API_ERROR"
            )
          );
        }
      })
      .recover(error -> {
        System.err.println("Error fetching post data: " + error.getMessage());
        return Future.failedFuture(error);
      });
  }


  private Future<JsonObject> fetchUserData() {
    return apiService.fetch(config.getUsersApiUrl())
      .compose(response -> {
        if (response.statusCode() == 200) {
          return Future.succeededFuture(response.bodyAsJsonObject());
        } else {
          return Future.failedFuture(
            new ApiException(
              "Failed to fetch user data",
              response.statusCode(),
              "USER_API_ERROR"
            )
          );
        }
      })
      .recover(error -> {
        System.err.println("Error fetching user data: " + error.getMessage());
        return Future.failedFuture(error);
      });
  }

  private JsonObject aggregateResponse(JsonObject postData, JsonObject userData) {
    String postTitle = postData.getString("title", "N/A");
    String authorName = userData.getString("name", "N/A");

    return new JsonObject()
      .put("post_title", postTitle)
      .put("author_name", authorName);
  }

  private void handleAggregateFailure(
    RoutingContext context,
    Throwable error,
    Future<JsonObject> postsFuture,
    Future<JsonObject> usersFuture
  ) {
    if (postsFuture.succeeded() && usersFuture.failed()) {
      JsonObject fallbackResponse = new JsonObject()
        .put("post_title", postsFuture.result().getString("title", "N/A"))
        .put("author_name", "N/A")
        .put("warning", "User data unavailable");
      sendSuccessResponse(context, fallbackResponse);
    } else if (postsFuture.failed() && usersFuture.succeeded()) {
      JsonObject fallbackResponse = new JsonObject()
        .put("post_title", "N/A")
        .put("author_name", usersFuture.result().getString("name", "N/A"))
        .put("warning", "Post data unavailable");
      sendSuccessResponse(context, fallbackResponse);
    } else {
      context.fail(error);
    }
  }

  private void sendSuccessResponse(RoutingContext context, JsonObject response) {
    context.response()
      .setStatusCode(200)
      .putHeader("content-type", "application/json")
      .end(response.encodePrettily());
  }
}
