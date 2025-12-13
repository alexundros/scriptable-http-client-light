package ru.alxpro.scriptable_http_client_light.script;

import static ru.alxpro.scriptable_http_client_light.utils.ScriptLoader.SCRIPT_KEY;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import ru.alxpro.scriptable_http_client_light.utils.FileUtils;
import ru.alxpro.scriptable_http_client_light.utils.XmlUtils;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public class Utils {

  private static final Logger log = LoggerFactory.getLogger(Utils.class);
  private final SharedContext context;

  public Utils(SharedContext context) {
    this.context = context;
  }

  public String toBase64(String str) {
    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(bytes);
  }

  public String urlEncode(String str) {
    return URLEncoder.encode(str, StandardCharsets.UTF_8);
  }

  public int getAvailableLocalPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  public String prompt(String text) {
    return prompt(text, null);
  }

  public String prompt(String text, String value) {
    Scanner scanner = new Scanner(System.in);
    while (true) {
      if (value == null) {
        System.out.printf("%s (Type ':e' to Exit): ", text);
      } else {
        String format = "%s [%s] (Type ':d' to use Default or ':e' to Exit): ";
        System.out.printf(format, text, value);
      }
      if (!scanner.hasNextLine()) {
        throw new RuntimeException("Input stream closed");
      }
      String line = scanner.nextLine().trim();
      if (value != null && line.equalsIgnoreCase(":d")) {
        log.info("Using default: {}", value);
        return value;
      }
      if (line.equalsIgnoreCase(":e")) {
        throw new RuntimeException("Prompt terminated.");
      }
      if (line.isEmpty()) {
        if (value == null) {
          log.info("Value is required. Enter a value or type ':e'.");
        } else {
          log.info("Value is required. Enter a value or type ':d'/':e'.");
        }
        continue;
      }
      return line;
    }
  }

  private Object convertFromJs(Object obj) {
    if (obj instanceof ScriptObjectMirror) {
      ScriptObjectMirror mirror = (ScriptObjectMirror) obj;
      if (mirror.isArray()) {
        List<Object> list = new ArrayList<>();
        long length = ((Number) mirror.getMember("length")).longValue();
        for (long i = 0; i < length; i++) {
          list.add(convertFromJs(mirror.getSlot((int) i)));
        }
        return list;
      } else {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : mirror.keySet()) {
          map.put(key, convertFromJs(mirror.get(key)));
        }
        return map;
      }
    }
    return obj;
  }

  public String saveFile(String name, String content) {
    String parent = context.get(SCRIPT_KEY);
    return FileUtils.saveFileDataDir(parent, name, content, ".data");
  }

  public String readFile(String name) {
    return FileUtils.readFileDataDir(name);
  }

  public String saveJsonFile(String name, Object obj) {
    String parent = context.get(SCRIPT_KEY);
    return FileUtils.saveJsonFile(parent, name, convertFromJs(obj));
  }

  public String saveJsonFile(String name, Object obj, Boolean pretty) {
    String parent = context.get(SCRIPT_KEY);
    return FileUtils.saveJsonFile(parent, name, convertFromJs(obj), pretty);
  }

  public Object jsonPath(String json, String query) {
    try {
      return JsonPath.read(json, query);
    } catch (Exception e) {
      log.error("JsonPath Failed: {}", e.getMessage());
      throw new RuntimeException("Error: " + e.getMessage(), e);
    }
  }

  public String saveXmlFile(String name, Object obj) {
    String parent = context.get(SCRIPT_KEY);
    return FileUtils.saveFileDataDir(parent, name, xmlToString(obj), ".xml");
  }

  public String saveXmlFile(String name, Object obj, Boolean pretty) {
    String parent = context.get(SCRIPT_KEY);
    return FileUtils.saveFileDataDir(parent, name, xmlToString(obj, pretty), ".xml");
  }

  public String saveXmlFile(
      String name, Object obj, Boolean omit, Boolean pretty, Integer indent
  ) {
    String parent = context.get(SCRIPT_KEY);
    String content = xmlToString(obj, omit, pretty, indent);
    return FileUtils.saveFileDataDir(parent, name, content, ".xml");
  }

  public Node wrapNode(Node node) {
    return XmlUtils.wrapNode(node);
  }

  public String xpathString(Object obj, String query) {
    return XmlUtils.xpathString(obj, query);
  }

  public Node xpathNode(Object obj, String query) {
    return XmlUtils.xpathNode(obj, query);
  }

  public List<String> xpathListString(Object obj, String query) {
    return XmlUtils.xpathListString(obj, query);
  }

  public List<Node> xpathListNode(Object obj, String query) {
    return XmlUtils.xpathListNode(obj, query);
  }

  public String xmlToString(Object obj) {
    return XmlUtils.xmlToString(obj);
  }

  public String xmlToString(Object obj, Boolean pretty) {
    return XmlUtils.xmlToString(obj, pretty);
  }

  public String xmlToString(Object obj, Boolean omit, Boolean pretty, Integer indent) {
    return XmlUtils.xmlToString(obj, omit, pretty, indent);
  }

  public String nodeToString(Node node, Boolean omit, Boolean pretty, Integer indent) {
    return XmlUtils.nodeToString(node, omit, pretty, indent);
  }

  public Map<String, Object> xmlToMap(Object obj) {
    return XmlUtils.xmlToMap(obj);
  }

  @SuppressWarnings("unchecked")
  public String mapToXml(Object obj) {
    return XmlUtils.mapToXml((Map<String, Object>) convertFromJs(obj));
  }

  @SuppressWarnings("unchecked")
  public String mapToXml(Object obj, Map<String, String> namespaces) {
    return XmlUtils.mapToXml((Map<String, Object>) convertFromJs(obj), namespaces);
  }

  public Node parseInnerXmlNode(Object obj) {
    return XmlUtils.parseInnerXmlNode(obj);
  }

  public Node parseInnerXmlDoc(Object obj) {
    return XmlUtils.parseInnerXmlDoc(obj);
  }
}
