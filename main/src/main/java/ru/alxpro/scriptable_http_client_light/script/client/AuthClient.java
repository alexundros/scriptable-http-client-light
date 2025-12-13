package ru.alxpro.scriptable_http_client_light.script.client;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.alxpro.scriptable_http_client_light.script.AppConfig;
import ru.alxpro.scriptable_http_client_light.script.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthClient extends BaseClient {

  private static final Logger log = LoggerFactory.getLogger(AuthClient.class);
  private final Gson gson = new Gson();
  private String accessToken = null;
  private long tokenExpirationTime = 0;
  // Refresh 10 seconds before actual expiration
  private static final long SAFETY_BUFFER_MS = 10_000;
  private final Utils utils;

  private static class TokenResponse {

    @SerializedName("access_token")
    String accessToken;

    @SerializedName("expires_in")
    long expiresIn;
  }

  public AuthClient(AppConfig config, Utils utils) {
    super(config);
    this.utils = utils;
  }

  public synchronized String getToken(
      String tokenUrl, String clientId, String clientSecret, String scope
  ) {
    long delta = tokenExpirationTime - SAFETY_BUFFER_MS;
    if (accessToken != null && System.currentTimeMillis() < delta) {
      return accessToken;
    }
    if (tokenUrl == null || clientId == null || clientSecret == null) {
      throw new RuntimeException("OAuth credentials missing in config");
    }
    log.info("OAuth token from {}", tokenUrl);
    log.info("Client Scope: {}", scope);
    log.info("Client Id: {}", clientId);
    log.info("Client Secret: {}", clientSecret.substring(0, 20) + "...");
    try {
      // Build Form Data
      Form form = new Form();
      form.param("grant_type", "client_credentials");
      if (scope != null && !scope.isEmpty()) {
        form.param("scope", scope);
      }
      // Execute Request
      String auth = clientId + ":" + clientSecret;
      try (Response response = httpClient.target(tokenUrl)
          .request(MediaType.APPLICATION_JSON_TYPE)
          .header("Authorization", "Basic " + utils.toBase64(auth))
          .post(Entity.form(form))) {
        String jsonBody = response.readEntity(String.class);
        if (response.getStatus() == 200) {
          TokenResponse tokenResp = gson.fromJson(jsonBody, TokenResponse.class);
          if (tokenResp.accessToken == null) {
            throw new RuntimeException("No 'access_token' in response");
          }
          this.accessToken = tokenResp.accessToken;
          // Default to 1 hour if not provided
          long expiresInSec = tokenResp.expiresIn > 0 ? tokenResp.expiresIn : 3600;
          this.tokenExpirationTime = System.currentTimeMillis() + (expiresInSec * 1000);
          log.info("Token refreshed. Valid for {}s", expiresInSec);
          return accessToken;
        }
        log.error("Auth failed. Status: {}, Body: {}", response.getStatus(), jsonBody);
        throw new RuntimeException("Auth failed: " + response.getStatus());
      }
    } catch (Exception e) {
      this.accessToken = null;
      throw new RuntimeException("Auth error: " + e.getMessage(), e);
    }
  }
}