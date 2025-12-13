package ru.alxpro.scriptable_http_client_light.script;

public class Env {

  public String get(String key) {
    return System.getenv(key);
  }

  public String get(String key, String def) {
    String value = get(key);
    return value != null ? value : def;
  }

  public String getRequired(String key) {
    String value = get(key);
    if (value != null) {
      return value;
    }
    throw new IllegalArgumentException("Env value of " + key + " is Required!");
  }

  public String getNotEmpty(String key) {
    String value = get(key);
    if (value != null && !value.isEmpty()) {
      return value;
    }
    throw new IllegalArgumentException("Env value of " + key + " is Empty!");
  }
}
