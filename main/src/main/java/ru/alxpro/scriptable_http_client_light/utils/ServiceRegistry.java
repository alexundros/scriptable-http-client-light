package ru.alxpro.scriptable_http_client_light.utils;

import java.util.HashMap;
import java.util.Map;

public class ServiceRegistry {

  private final Map<String, Object> services = new HashMap<>();

  public void register(String name, Object service) {
    services.put(name, service);
  }

  @SuppressWarnings("unchecked")
  public <T> T getService(String key) {
    return (T) services.get(key);
  }

  public Map<String, Object> getServices() {
    return services;
  }
}