package ru.alxpro.scriptable_http_client_light.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfig {

  private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
  //public static final String MAX_BODY_LOG_SIZE = "maxBodyLogSize";
  public static final String MAX_BODY_LOG_SIZE = "maxBodyLogSize";
  private final Properties properties = new Properties();

  {
    // Default values
    //properties.put(MAX_BODY_LOG_SIZE, "1024");
    properties.put(MAX_BODY_LOG_SIZE, "1024");
  }

  public boolean load(String filename) {
    File f = new File(filename);
    if (!f.exists()) {
      return false;
    }
    try (var fis = new FileInputStream(f)) {
      properties.load(fis);
      return true;
    } catch (IOException e) {
      log.error("Error reading config: {}", e.getMessage());
      return false;
    }
  }

  public String get(String key) {
    return properties.getProperty(key);
  }

  public String get(String key, String def) {
    return properties.getProperty(key, def);
  }
}
