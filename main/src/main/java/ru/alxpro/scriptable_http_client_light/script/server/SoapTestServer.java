package ru.alxpro.scriptable_http_client_light.script.server;

import jakarta.xml.ws.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoapTestServer {

  private static final Logger log = LoggerFactory.getLogger(SoapTestServer.class);
  private Endpoint endpoint;

  public void start(String url) {
    try {
      endpoint = Endpoint.publish(url, new CalcSoapService());
      log.info("SoapTestServer started ({}?wsdl)", url);
    } catch (Exception e) {
      log.error("Failed to start SoapTestServer: {}", e.getMessage());
    }
  }

  public void stop() {
    if (endpoint != null && endpoint.isPublished()) {
      endpoint.stop();
      log.info("SoapTestServer stopped");
      endpoint = null;
    }
  }
}