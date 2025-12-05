package com.gateway.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import com.gateway.exception.ApiException;

public class GlobalExceptionHandler {
  public void handle(RoutingContext context) {
    Throwable failure = context.failure();
    int statusCode = context.statusCode();

    if (failure != null) {
      handleException(context, failure);
    } else {
      handleUnknownError(context);
    }
  }

  private void handleException(RoutingContext context, Throwable throwable) {
    JsonObject errorResponse;
    int statusCode;

    if (throwable instanceof ApiException) {
      ApiException apiEx = (ApiException) throwable;
      statusCode = apiEx.getStatusCode();
      errorResponse = createErrorResponse(
        apiEx.getMessage(),
        apiEx.getErrorCode(),
        statusCode
      );
    } else {
      statusCode = 500;
      errorResponse = createErrorResponse(
        "Internal server error: " + throwable.getMessage(),
        "INTERNAL_ERROR",
        statusCode
      );
    }

    logError(throwable, statusCode);
    sendErrorResponse(context, statusCode, errorResponse);
  }

  private void handleUnknownError(RoutingContext context) {
    JsonObject errorResponse = createErrorResponse(
      "An unexpected error occurred",
      "UNKNOWN_ERROR",
      500
    );
    sendErrorResponse(context, 500, errorResponse);
  }

  private JsonObject createErrorResponse(String message, String errorCode, int statusCode) {
    return new JsonObject()
      .put("error", true)
      .put("message", message)
      .put("errorCode", errorCode)
      .put("statusCode", statusCode)
      .put("timestamp", System.currentTimeMillis());
  }

  private void sendErrorResponse(RoutingContext context, int statusCode, JsonObject response) {
    context.response()
      .setStatusCode(statusCode)
      .putHeader("content-type", "application/json")
      .end(response.encodePrettily());
  }

  private void logError(Throwable throwable, int statusCode) {
    System.err.println("Error occurred [" + statusCode + "]: " + throwable.getMessage());
    if (statusCode >= 500) {
      throwable.printStackTrace();
    }
  }
}
