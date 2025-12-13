package ru.alxpro.scriptable_http_client_light.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLogFilter implements ClientRequestFilter, ClientResponseFilter {

  private static final Logger log = LoggerFactory.getLogger("HTTP");
  private final int maxBodyLogSize;

  public Slf4jLogFilter(int maxBodyLogSize) {
    this.maxBodyLogSize = maxBodyLogSize;
  }

  @Override
  public void filter(ClientRequestContext reqCtx) {
    log.info("> {} {}", reqCtx.getMethod(), reqCtx.getUri());
    // Log Headers
    reqCtx.getStringHeaders().forEach((key, val) -> {
      if ("Authorization".equalsIgnoreCase(key)) {
        val = Collections.singletonList("***********");
      }
      log.info("> {}: {}", key, val);
    });
    // Log Body (Outgoing)
    if (reqCtx.hasEntity()) {
      String body = String.valueOf(reqCtx.getEntity());
      if (body.length() > maxBodyLogSize) {
        body = body.substring(0, maxBodyLogSize) + "...";
      }
      log.info("> Body: {}", body);
    }
  }

  @Override
  public void filter(ClientRequestContext reqCtx, ClientResponseContext rspCtx)
      throws IOException {
    log.info("< {} {}", rspCtx.getStatus(), rspCtx.getStatusInfo());
    // Log Headers
    rspCtx.getHeaders().forEach((key, val) -> log.info("< {}: {}", key, val));
    // Log Body (Incoming)
    if (rspCtx.hasEntity()) {
      InputStream stream = rspCtx.getEntityStream();
      if (stream != null) {
        byte[] bytes = stream.readAllBytes();
        String body;
        if (bytes.length > maxBodyLogSize) {
          body = new String(bytes, 0, maxBodyLogSize, UTF_8) + "...";
        } else {
          body = new String(bytes, UTF_8);
        }
        log.info("< Body: {}", body);
        rspCtx.setEntityStream(new ByteArrayInputStream(bytes));
      }
    }
  }
}
