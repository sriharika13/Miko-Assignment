package com.gateway;

import com.gateway.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionTest {

  @Test
  void testGetStatusCode() {
    ApiException ex = new ApiException("Test message", 404, "NOT_FOUND");
    assertEquals(404, ex.getStatusCode());
  }

  @Test
  void testGetErrorCode() {
    ApiException ex = new ApiException("Test message", 500, "INTERNAL_ERROR");
    assertEquals("INTERNAL_ERROR", ex.getErrorCode());
  }

  @Test
  void testGetMessage() {
    ApiException ex = new ApiException("Custom error message", 400, "BAD_REQUEST");
    assertEquals("Custom error message", ex.getMessage());
  }

  @Test
  void testConstructorWithCause() {
    Throwable cause = new RuntimeException("Root cause");
    ApiException ex = new ApiException("Wrapper message", cause, 502, "BAD_GATEWAY");

    assertEquals("Wrapper message", ex.getMessage());
    assertEquals(502, ex.getStatusCode());
    assertEquals("BAD_GATEWAY", ex.getErrorCode());
    assertEquals(cause, ex.getCause());
  }

  @Test
  void testExceptionIsRuntimeException() {
    ApiException ex = new ApiException("Test", 500, "ERROR");
    assertInstanceOf(RuntimeException.class, ex);
  }
}
