package com.gateway.service;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.core.buffer.Buffer;

public class ApiService {

  private final WebClient webClient;

  public ApiService(WebClient webClient) {
    this.webClient = webClient;
  }

  public Future<HttpResponse<Buffer>> fetch(String url) {
    return webClient
      .getAbs(url)
      .send();
  }
}
