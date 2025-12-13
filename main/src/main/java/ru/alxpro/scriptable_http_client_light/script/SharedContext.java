package ru.alxpro.scriptable_http_client_light.script;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedContext {

  private final Map<String, Object> memory = new ConcurrentHashMap<>();

  public <T> void put(String k, T v) {
    memory.put(k, v);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String k) {
    return (T) memory.get(k);
  }

  public Map<String, Object> getAll() {
    return new HashMap<>(memory);
  }
}
