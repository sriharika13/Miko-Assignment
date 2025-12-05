package com.gateway;

import com.gateway.exception.ApiException;
import com.gateway.handler.GlobalExceptionHandler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

  @Mock
  private RoutingContext context;

  @Mock
  private HttpServerResponse response;

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    handler = new GlobalExceptionHandler();
    when(context.response()).thenReturn(response);
    when(response.setStatusCode(anyInt())).thenReturn(response);
    when(response.putHeader(anyString(), anyString())).thenReturn(response);
  }

  @Test
  void testApiExceptionReturnsCorrectStatusAndMessage() {
    ApiException apiEx = new ApiException("API failed", 503, "SERVICE_UNAVAILABLE");
    when(context.failure()).thenReturn(apiEx);
    when(context.statusCode()).thenReturn(-1);

    handler.handle(context);

    verify(response).setStatusCode(503);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(response).end(captor.capture());

    JsonObject result = new JsonObject(captor.getValue());
    assertTrue(result.getBoolean("error"));
    assertEquals("API failed", result.getString("message"));
    assertEquals("SERVICE_UNAVAILABLE", result.getString("errorCode"));
    assertEquals(503, result.getInteger("statusCode"));
  }

  @Test
  void testUnknownExceptionReturns500() {
    RuntimeException ex = new RuntimeException("Something went wrong");
    when(context.failure()).thenReturn(ex);
    when(context.statusCode()).thenReturn(-1);

    handler.handle(context);

    verify(response).setStatusCode(500);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(response).end(captor.capture());

    JsonObject result = new JsonObject(captor.getValue());
    assertTrue(result.getBoolean("error"));
    assertTrue(result.getString("message").contains("Internal server error"));
    assertEquals("INTERNAL_ERROR", result.getString("errorCode"));
    assertEquals(500, result.getInteger("statusCode"));
  }

  @Test
  void testHandlesNullFailure() {
    when(context.failure()).thenReturn(null);
    when(context.statusCode()).thenReturn(-1);

    handler.handle(context);

    verify(response).setStatusCode(500);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(response).end(captor.capture());

    JsonObject result = new JsonObject(captor.getValue());
    assertTrue(result.getBoolean("error"));
    assertEquals("An unexpected error occurred", result.getString("message"));
    assertEquals("UNKNOWN_ERROR", result.getString("errorCode"));
  }

  @Test
  void testResponseIncludesTimestamp() {
    RuntimeException ex = new RuntimeException("Test");
    when(context.failure()).thenReturn(ex);
    when(context.statusCode()).thenReturn(-1);

    handler.handle(context);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(response).end(captor.capture());

    JsonObject result = new JsonObject(captor.getValue());
    assertNotNull(result.getLong("timestamp"));
    assertTrue(result.getLong("timestamp") > 0);
  }
}
