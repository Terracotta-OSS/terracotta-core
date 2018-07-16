package org.terracotta.voter;

import com.tc.config.schema.L2ConfigForL1;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class TCConfigParserUtil {

  private static final String SERVER_NODE = "server";
  private static final String HOST_ATTR = "host";
  private static final String TSA_PORT_NODE = "tsa-port";

  private final DocumentBuilder builder;

  public TCConfigParserUtil() {
    try {
      builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  public String[] parseHostPorts(InputStream inputStream) throws IOException, SAXException {
    Document document;
    document = builder.parse(inputStream);

    NodeList serverList = document.getElementsByTagName(SERVER_NODE);
    String[] hostPorts = new String[serverList.getLength()];

    for (int i = 0; i < serverList.getLength(); i++) {
      Node node = serverList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element serverNode = (Element) node;
        if (!serverNode.hasAttribute(HOST_ATTR)) {
          throw new RuntimeException(HOST_ATTR + " attribute must be specified");
        }
        String hostname = serverNode.getAttributes().getNamedItem(HOST_ATTR).getNodeValue();
        NodeList list = serverNode.getElementsByTagName(TSA_PORT_NODE);
        int port = list.getLength() == 0 ? L2ConfigForL1.DEFAULT_PORT : Integer.parseInt(serverNode.getElementsByTagName(TSA_PORT_NODE).item(0).getTextContent());
        hostPorts[i] = hostname + ":" + port;
      }
    }

    return hostPorts;
  }
}
