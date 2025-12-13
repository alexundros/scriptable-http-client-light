package ru.alxpro.scriptable_http_client_light.script;

public class JResponse {

  public final int status;
  public final String body;

  public JResponse(int s, String b) {
    this.status = s;
    this.body = b;
  }
}
