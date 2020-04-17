/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
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
