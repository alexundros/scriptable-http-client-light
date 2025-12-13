package ru.alxpro.scriptable_http_client_light.script.client;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import ru.alxpro.scriptable_http_client_light.script.AppConfig;
import ru.alxpro.scriptable_http_client_light.script.JResponse;

public class RestClient extends BaseClient {

  public RestClient(AppConfig config) {
    super(config);
  }

  public JResponse get(String url) {
    return request("GET", url, null, null, null);
  }

  public JResponse post(String url, String body) {
    return request("POST", url, body, null, null);
  }

  public JResponse getWithToken(String url, String token) {
    return request("GET", url, null, null, token);
  }

  public JResponse postWithToken(String url, String body, String token) {
    return request("POST", url, body, null, token);
  }

  public JResponse request(
      String method, String url, String body, Map<String, String> headers, String token
  ) {
    try {
      Invocation.Builder builder = httpClient.target(url).request(APPLICATION_JSON_TYPE);
      if (token != null) {
        builder.header("Authorization", "Bearer " + token);
      }
      if (headers != null) {
        headers.forEach(builder::header);
      }
      Entity<String> entity = null;
      if (body != null) {
        entity = Entity.json(body);
      }
      try (Response response = builder.build(method, entity).invoke()) {
        return new JResponse(response.getStatus(), response.readEntity(String.class));
      }
    } catch (Exception e) {
      throw new RuntimeException("HTTP Request failed: " + e.getMessage(), e);
    }
  }
}