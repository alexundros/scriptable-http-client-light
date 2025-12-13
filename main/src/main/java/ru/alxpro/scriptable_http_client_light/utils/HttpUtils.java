package ru.alxpro.scriptable_http_client_light.utils;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpUtils {

  public static SSLContext getTrustAllCertsSslCtx() {
    SSLContext sc = null;
    try {
      TrustManager[] trustAllCerts = new TrustManager[]{
          new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String at) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String at) {
            }
          }
      };
      sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new SecureRandom());
    } catch (Exception ignored) {
    }
    return sc;
  }
}
