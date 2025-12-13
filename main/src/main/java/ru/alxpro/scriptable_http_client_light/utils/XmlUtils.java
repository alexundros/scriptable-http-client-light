package ru.alxpro.scriptable_http_client_light.utils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.soap.SOAPMessage;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.glassfish.jaxb.core.marshaller.CharacterEscapeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlUtils {

  private static final Logger log = LoggerFactory.getLogger(XmlUtils.class);

  private static final String XSLT_STRIP_SPACE =
      "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
          "<xsl:strip-space elements=\"*\"/>" +
          "<xsl:template match=\"@*|node()\">" +
          "<xsl:copy><xsl:apply-templates select=\"@*|node()\"/></xsl:copy>" +
          "</xsl:template>" +
          "</xsl:stylesheet>";

  public static Node wrapNode(Node node) {
    if (node == null) {
      return null;
    }
    Set<Class<?>> classSet = new HashSet<>();
    Class<?> clazz = node.getClass();
    while (clazz != null) {
      Collections.addAll(classSet, clazz.getInterfaces());
      clazz = clazz.getSuperclass();
    }
    return (Node) Proxy.newProxyInstance(
        node.getClass().getClassLoader(),
        classSet.toArray(new Class[0]), (p, method, args) -> {
          if ("toString".equals(method.getName())
              && (args == null || args.length == 0)) {
            return nodeToString(node, true, false, null);
          }
          return method.invoke(node, args);
        }
    );
  }

  public static String xpathString(Object obj, String query) {
    Node node = xpathNode(obj, query);
    return node != null ? node.getTextContent() : null;
  }

  public static Node xpathNode(Object obj, String query) {
    return (Node) evaluateXPath(obj, query, XPathConstants.NODE);
  }

  public static List<String> xpathListString(Object obj, String query) {
    List<Node> nodes = xpathListNode(obj, query);
    return nodes.stream().map(Node::getTextContent).collect(Collectors.toList());
  }

  public static List<Node> xpathListNode(Object obj, String query) {
    NodeList list = (NodeList) evaluateXPath(obj, query, XPathConstants.NODESET);
    List<Node> result = new ArrayList<>();
    if (list != null) {
      for (int i = 0; i < list.getLength(); i++) {
        result.add(list.item(i));
      }
    }
    return result;
  }

  private static Object evaluateXPath(Object obj, String query, QName type) {
    try {
      Node node = resolveXmlSource(obj);
      XPath xPath = XPathFactory.newInstance().newXPath();
      return xPath.evaluate(query, node, type);
    } catch (Exception e) {
      log.error("XPath Evaluation Failed: {}", e.getMessage());
      throw new RuntimeException("Error: " + e.getMessage(), e);
    }
  }

  private static Node resolveXmlSource(Object obj) throws Exception {
    if (obj == null) {
      return null;
    }
    if (obj instanceof Node) {
      return (Node) obj;
    }
    if (obj instanceof SOAPMessage) {
      return ((SOAPMessage) obj).getSOAPPart();
    }
    if (obj instanceof String) {
      return parseXml((String) obj);
    }
    throw new IllegalArgumentException("Not an XML Node object");
  }

  public static Document parseXml(String xmlStr) throws Exception {
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    f.setNamespaceAware(true);
    InputSource is = new InputSource(new StringReader(xmlStr));
    return f.newDocumentBuilder().parse(is);
  }

  public static String xmlToString(Object obj) {
    return xmlToString(obj, false, false, null);
  }

  public static String xmlToString(Object obj, Boolean pretty) {
    return xmlToString(obj, false, pretty, null);
  }

  public static String xmlToString(Object obj, Boolean omit, Boolean pretty, Integer indent) {
    try {
      Node node = null;
      try {
        node = resolveXmlSource(obj);
      } catch (IllegalArgumentException ignored) {
      }
      if (node != null) {
        return nodeToString(node, omit, pretty, indent);
      } else {
        return jaxbToString(obj, pretty);
      }
    } catch (Exception e) {
      log.error("Error converting Object to string: {}", e.getMessage());
      throw new RuntimeException("Error: " + e.getMessage(), e);
    }
  }

  public static String nodeToString(Node node, Boolean omit, Boolean pretty, Integer indent) {
    try {
      Transformer tf;
      if (Boolean.TRUE.equals(pretty)) {
        tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        String pn = "{http://xml.apache.org/xslt}indent-amount";
        tf.setOutputProperty(pn, indent != null ? indent.toString() : "2");
      } else {
        StreamSource ss = new StreamSource(new StringReader(XSLT_STRIP_SPACE));
        tf = TransformerFactory.newInstance().newTransformer(ss);
      }
      tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      tf.setOutputProperty(OutputKeys.METHOD, "xml");
      if (Boolean.TRUE.equals(omit)) {
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      }
      StringWriter sw = new StringWriter();
      tf.transform(new DOMSource(node), new StreamResult(sw));
      return sw.toString();
    } catch (Exception e) {
      log.error("Error converting Node to string: {}", e.getMessage());
      throw new RuntimeException("Error: " + e.getMessage(), e);
    }
  }

  private static String jaxbToString(Object obj, Boolean pretty) throws Exception {
    JAXBContext context = JAXBContext.newInstance(obj.getClass());
    Marshaller marshaller = context.createMarshaller();
    CharacterEscapeHandler hnd = (a, i, j, f, w) -> w.write(a, i, j);
    marshaller.setProperty(CharacterEscapeHandler.class.getName(), hnd);
    marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
    if (pretty) {
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    }
    StringWriter sw = new StringWriter();
    marshaller.marshal(obj, sw);
    return sw.toString();
  }

  public static Map<String, Object> xmlToMap(Object obj) {
    try {
      Node node = resolveXmlSource(obj);
      Object value = nodeContentToObject(node);
      return Collections.singletonMap(node.getNodeName(), value);
    } catch (Exception e) {
      log.error("Error converting XML to Map: {}", e.getMessage());
      throw new RuntimeException("Error: " + e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  private static Object nodeContentToObject(Node node) {
    if (!node.hasAttributes() && isTextOnly(node)) {
      return node.getTextContent();
    }
    Map<String, Object> map = new LinkedHashMap<>();
    if (node.hasAttributes()) {
      for (int i = 0; i < node.getAttributes().getLength(); i++) {
        Node attr = node.getAttributes().item(i);
        map.put("@" + attr.getNodeName(), attr.getNodeValue());
      }
    }
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node child = childNodes.item(i);
      if (child.getNodeType() == Node.TEXT_NODE
          && child.getTextContent().trim().isEmpty()) {
        continue;
      }
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        String childName = child.getNodeName();
        Object childValue = nodeContentToObject(child);
        if (map.containsKey(childName)) {
          Object existing = map.get(childName);
          if (existing instanceof List) {
            ((List<Object>) existing).add(childValue);
          } else {
            List<Object> list = new ArrayList<>();
            list.add(existing);
            list.add(childValue);
            map.put(childName, list);
          }
        } else {
          map.put(childName, childValue);
        }
      } else if (child.getNodeType() == Node.TEXT_NODE
          || child.getNodeType() == Node.CDATA_SECTION_NODE) {
        String text = child.getTextContent();
        if (text != null && !text.isEmpty()) {
          map.put(child.getNodeName(), text);
        }
      }
    }
    return map;
  }

  private static boolean isTextOnly(Node node) {
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node child = childNodes.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        return false;
      }
    }
    return true;
  }

  public static String mapToXml(Map<String, Object> map) {
    return mapToXml(map, null);
  }

  @SuppressWarnings("unchecked")
  public static String mapToXml(Map<String, Object> map, Map<String, String> namespaces) {
    try {
      if (map.size() != 1) {
        throw new IllegalArgumentException("Map must have exactly one root key.");
      }

      Map.Entry<String, Object> entry = map.entrySet().iterator().next();
      String rootName = entry.getKey();
      Object rootValue = entry.getValue();

      DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
      df.setNamespaceAware(true);
      Document doc = df.newDocumentBuilder().newDocument();

      Element rootElement;
      if (namespaces != null) {
        String rootUri;
        int colonIdx = rootName.indexOf(':');
        if (colonIdx > 0) {
          String prefix = rootName.substring(0, colonIdx);
          rootUri = namespaces.get(prefix);
        } else {
          rootUri = namespaces.get("xmlns");
        }
        rootElement = doc.createElementNS(rootUri, rootName);
      } else {
        rootElement = doc.createElement(rootName);
      }
      doc.appendChild(rootElement);

      if (rootValue instanceof Map) {
        mapToXml(rootElement, (Map<String, Object>) rootValue, namespaces);
      } else if (rootValue instanceof List) {
        throw new IllegalArgumentException("Root element cannot be a List.");
      } else if (rootValue != null) {
        if (namespaces != null) {
          applyNamespaces(rootElement, namespaces);
        }
        rootElement.setTextContent(rootValue.toString());
      }

      return xmlToString(doc);
    } catch (Exception e) {
      log.error("Error converting Map to XML: {}", e.getMessage());
      throw new RuntimeException("Error converting Map to XML", e);
    }
  }

  public static void mapToXml(Element parent, Map<String, Object> map) {
    mapToXml(parent, map, null);
  }

  public static void mapToXml(
      Element parent, Map<String, Object> map, Map<String, String> namespaces
  ) {
    if (namespaces != null) {
      applyNamespaces(parent, namespaces);
    }
    Document doc = parent.getOwnerDocument();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof List) {
        for (Object item : (List<?>) value) {
          appendElement(doc, parent, key, item);
        }
      } else {
        appendElement(doc, parent, key, value);
      }
    }
  }

  private static void applyNamespaces(Element element, Map<String, String> namespaces) {
    for (Map.Entry<String, String> entry : namespaces.entrySet()) {
      String prefix = entry.getKey();
      String uri = entry.getValue();
      String attr = "xmlns".equals(prefix) ? "xmlns" : "xmlns:" + prefix;
      if (!element.hasAttribute(attr)) {
        element.setAttribute(attr, uri);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void appendElement(
      Document doc, Element parent, String tag, Object value
  ) {
    if (tag.startsWith("@")) {
      parent.setAttribute(tag.substring(1), value != null ? value.toString() : "");
      return;
    }
    Element element;
    int colonIdx = tag.indexOf(':');
    if (colonIdx > 0) {
      String prefix = tag.substring(0, colonIdx);
      // Ищем URI в контексте родителя (там уже проставлены xmlns атрибуты)
      String nsUri = parent.lookupNamespaceURI(prefix);
      if (nsUri != null) {
        element = doc.createElementNS(nsUri, tag);
      } else {
        element = doc.createElement(tag);
      }
    } else {
      String defaultNs = parent.lookupNamespaceURI(null);
      if (defaultNs != null) {
        element = doc.createElementNS(defaultNs, tag);
      } else {
        element = doc.createElement(tag);
      }
    }
    parent.appendChild(element);
    if (value instanceof Map) {
      // namespaces передавать не нужно, они уже в контексте DOM
      mapToXml(element, (Map<String, Object>) value, null);
    } else if (value != null) {
      element.setTextContent(value.toString());
    }
  }

  public static Node parseInnerXmlNode(Object obj) {
    Document doc = parseInnerXmlDoc(obj);
    return doc != null ? doc.getDocumentElement() : null;
  }

  public static Document parseInnerXmlDoc(Object obj) {
    try {
      Node node = resolveXmlSource(obj);
      if (node == null) {
        return null;
      }
      String content = node.getTextContent();
      if (content == null || content.isBlank()) {
        return null;
      }
      Document doc = parseXml(content);
      doc.getDocumentElement().normalize();
      return doc;
    } catch (Exception e) {
      log.error("Error parsing inner XML: {}", e.getMessage());
      throw new RuntimeException("Error: " + e.getMessage(), e);
    }
  }
}
