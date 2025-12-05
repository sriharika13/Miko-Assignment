package com.gateway.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class AppConfig {

  private final Vertx vertx;
  private final WebClient webClient;
  private final int serverPort;
  private final String postsApiUrl;
  private final String usersApiUrl;
  private final int httpTimeout;

  public AppConfig(Vertx vertx) {
    this.vertx = vertx;

    JsonObject config = loadConfiguration();

    this.serverPort = config.getInteger("server.port", 8080);
    this.postsApiUrl = config.getString("api.posts.url",
      "https://jsonplaceholder.typicode.com/posts/1");
    this.usersApiUrl = config.getString("api.users.url",
      "https://jsonplaceholder.typicode.com/users/1");
    this.httpTimeout = config.getInteger("http.timeout", 10000);

    WebClientOptions options = new WebClientOptions()
      .setConnectTimeout(httpTimeout)
      .setIdleTimeout(httpTimeout)
      .setKeepAlive(true)
      .setSsl(true);

    this.webClient = WebClient.create(vertx, options);
  }

  private JsonObject loadConfiguration() {
    return new JsonObject()
      .put("server.port", 8080)
      .put("api.posts.url", "https://jsonplaceholder.typicode.com/posts/1")
      .put("api.users.url", "https://jsonplaceholder.typicode.com/users/1")
      .put("http.timeout", 10000)
      .put("thread.pool.size", 10);
  }

  public Vertx getVertx() {
    return vertx;
  }

  public WebClient getWebClient() {
    return webClient;
  }

  public int getServerPort() {
    return serverPort;
  }

  public String getPostsApiUrl() {
    return postsApiUrl;
  }

  public String getUsersApiUrl() {
    return usersApiUrl;
  }

  public int getHttpTimeout() {
    return httpTimeout;
  }
}
