package ru.alxpro.scriptable_http_client_light.script.client;

import static java.lang.Integer.parseInt;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import ru.alxpro.scriptable_http_client_light.script.AppConfig;
import ru.alxpro.scriptable_http_client_light.utils.HttpUtils;
import ru.alxpro.scriptable_http_client_light.utils.Slf4jLogFilter;

abstract class BaseClient {

  protected final AppConfig config;
  protected final Client httpClient;

  protected BaseClient(AppConfig config) {
    this.config = config;
    try {
      ClientBuilder builder = ClientBuilder.newBuilder();

      httpClient = builder
          .sslContext(HttpUtils.getTrustAllCertsSslCtx())
          .hostnameVerifier((hostname, session) -> true)
          //.property(ClientProperties.CONNECT_TIMEOUT, 10_000)
          //.property(ClientProperties.READ_TIMEOUT, 60_000)
          .register(new Slf4jLogFilter(parseInt(config.get(AppConfig.MAX_BODY_LOG_SIZE))))
          .build();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
