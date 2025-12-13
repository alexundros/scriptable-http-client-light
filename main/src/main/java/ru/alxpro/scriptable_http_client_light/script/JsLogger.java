package ru.alxpro.scriptable_http_client_light.script;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsLogger {

  private final Logger log = LoggerFactory.getLogger("JS");
  // Flag to track errors in the current execution
  private final AtomicBoolean errorOccurred = new AtomicBoolean(false);

  public void log(String msg) {
    log.info(msg);
  }

  public void error(String msg) {
    log.error(msg);
    errorOccurred.set(true);
  }

  public boolean hasError() {
    return errorOccurred.get();
  }

  public void reset() {
    errorOccurred.set(false);
  }
}
