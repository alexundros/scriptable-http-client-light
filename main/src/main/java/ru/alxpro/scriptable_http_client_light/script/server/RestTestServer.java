package ru.alxpro.scriptable_http_client_light.script.server;

import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestTestServer {

  private static final Logger log = LoggerFactory.getLogger(RestTestServer.class);
  private HttpServer server;

  public void start(int port) {
    try {
      URI baseUri = UriBuilder.fromUri("http://localhost/").port(port).build();
      ResourceConfig config = new ResourceConfig();
      config.register(MockRestService.class);
      // false = не запускать сразу (хотя factory обычно запускает)
      server = JdkHttpServerFactory.createHttpServer(baseUri, config, false);
      server.start();
      log.info("RestTestServer started on port {}", port);
    } catch (Exception e) {
      log.error("Failed to start RestTestServer: {}", e.getMessage());
    }
  }

  public void stop() {
    if (server != null) {
      server.stop(0);
      log.info("RestTestServer stopped");
      server = null;
    }
  }
}