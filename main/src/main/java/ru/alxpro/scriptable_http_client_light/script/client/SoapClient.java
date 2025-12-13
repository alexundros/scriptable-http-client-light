package ru.alxpro.scriptable_http_client_light.script.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.Name;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import ru.alxpro.scriptable_http_client_light.script.AppConfig;
import ru.alxpro.scriptable_http_client_light.script.Utils;
import ru.alxpro.scriptable_http_client_light.utils.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public class SoapClient extends BaseClient {

  private static final Logger log = LoggerFactory.getLogger(SoapClient.class);
  private final Utils utils;
  private String username;
  private String password;

  public SoapClient(AppConfig config, Utils utils) {
    super(config);
    this.utils = utils;
  }

  public void clear() {
    this.username = null;
    this.password = null;
  }

  public void setUserPassword(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public Map<String, Object> invokeAsMap(
      String url, String action, String operation, Map<String, Object> params,
      Map<String, String> namespaces
  ) {
    return XmlUtils.xmlToMap(invoke(url, action, operation, params, namespaces));
  }

  public String invokeAsString(
      String url, String action, String operation, Map<String, Object> params,
      Map<String, String> namespaces
  ) {
    return XmlUtils.xmlToString(invoke(url, action, operation, params, namespaces));
  }

  public String invokeAsString(
      String url, String action, String operation, Map<String, Object> params,
      Map<String, String> namespaces, Boolean omit, Boolean pretty, Integer indent
  ) {
    Node node = invoke(url, action, operation, params, namespaces);
    return XmlUtils.xmlToString(node, omit, pretty, indent);
  }

  public Node invoke(
      String url, String action, String operation, Map<String, Object> params,
      Map<String, String> namespaces
  ) {
    try {
      MessageFactory mf = MessageFactory.newInstance();
      SOAPFactory sf = SOAPFactory.newInstance();
      SOAPMessage requestMsg = mf.createMessage();
      SOAPPart soapPart = requestMsg.getSOAPPart();
      SOAPEnvelope envelope = soapPart.getEnvelope();

      envelope.addNamespaceDeclaration("S", envelope.getNamespaceURI());
      envelope.removeNamespaceDeclaration(envelope.getPrefix());
      envelope.setPrefix("S");
      envelope.getHeader().setPrefix("S");
      envelope.getBody().setPrefix("S");

      Name bodyName;
      if (operation.contains(":")) {
        String[] parts = operation.split(":", 2);
        String prefix = parts[0];
        String name = parts[1];
        String uri = namespaces != null ? namespaces.get(prefix) : null;
        if (uri == null) {
          bodyName = sf.createName(name, prefix, null);
        } else {
          bodyName = sf.createName(name, prefix, uri);
        }
      } else {
        if (namespaces != null) {
          bodyName = sf.createName(operation, "", namespaces.get("xmlns"));
        } else {
          bodyName = sf.createName(operation);
        }
      }

      SOAPElement soapBody = envelope.getBody().addBodyElement(bodyName);
      if (namespaces != null) {
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
          String prefix = entry.getKey();
          String uri = entry.getValue();
          if ("xmlns".equals(prefix)) {
            soapBody.addAttribute(sf.createName("xmlns"), uri);
          } else {
            soapBody.addNamespaceDeclaration(prefix, uri);
          }
        }
      }

      XmlUtils.mapToXml(soapBody, params, namespaces);
      requestMsg.saveChanges();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      requestMsg.writeTo(out);
      String requestBody = out.toString(UTF_8);

      Invocation.Builder request = httpClient.target(url).request();
      request.header("Content-Type", "text/xml; charset=utf-8");
      if (action != null) {
        request.header("SOAPAction", action);
      }

      if (username != null && password != null) {
        String auth = username + ":" + password;
        request.header("Authorization", "Basic " + utils.toBase64(auth));
      }

      try (Response response = request.post(Entity.entity(requestBody, "text/xml"))) {
        String rawRespBody = response.readEntity(String.class);
        String cType = response.getHeaderString("Content-Type");
        boolean isXml = cType != null && cType.toLowerCase().contains("text/xml");

        if (!isXml && response.getStatus() != 200) {
          log.error("Invalid Content-Type: {}. Body: {}", cType, rawRespBody);
          throw new RuntimeException("Server Error (" + cType + "): " + rawRespBody);
        }

        try {
          byte[] bytes = rawRespBody.getBytes(UTF_8);
          MimeHeaders headers = new MimeHeaders();
          if (cType != null) {
            headers.addHeader("Content-Type", cType);
          }
          ByteArrayInputStream in = new ByteArrayInputStream(bytes);
          SOAPMessage responseMsg = mf.createMessage(headers, in);
          SOAPBody respSoapBody = responseMsg.getSOAPBody();
          if (respSoapBody.hasFault()) {
            SOAPFault fault = respSoapBody.getFault();
            throw new RuntimeException("SOAP Fault: " + fault.getFaultString());
          }

          Node child = respSoapBody.getFirstChild();
          while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
            child = child.getNextSibling();
          }
          return child;
        } catch (Exception e) {
          log.error("Failed to parse SOAP response: {}", rawRespBody);
          throw new RuntimeException("SOAP Parse Error", e);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("SOAP Failed: " + e.getMessage(), e);
    }
  }
}