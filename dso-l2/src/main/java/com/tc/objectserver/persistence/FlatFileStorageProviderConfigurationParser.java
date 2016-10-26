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
 *
 */
package com.tc.objectserver.persistence;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.terracotta.config.service.ServiceConfigParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tc.util.Assert;


public class FlatFileStorageProviderConfigurationParser implements ServiceConfigParser {
  private static final URI NAMESPACE = URI.create("http://www.terracotta.org/config/restartable-platform-persistence");
  private static final URL XML_SCHEMA = ServiceConfigParser.class.getResource("/restartable-platform-persistence.xsd");

  @Override
  public Source getXmlSchema() throws IOException {
    return new StreamSource(XML_SCHEMA.openStream());
  }

  @Override
  public URI getNamespace() {
    return NAMESPACE;
  }

  @Override
  public FlatFileStorageProviderConfiguration parse(Element fragment, String source) {
    NodeList list = fragment.getChildNodes();
    String path = null;
    boolean shouldBlockOnLock = false;
    for (int i = 0; i < list.getLength(); ++i) {
      Node node = list.item(i);
      String nodeName = node.getNodeName();
      if ("restartable-platform-persistence:path".equals(nodeName)) {
        path = node.getTextContent();
      } else if ("restartable-platform-persistence:should-block-on-lock".equals(nodeName)) {
        shouldBlockOnLock = Boolean.parseBoolean(node.getTextContent());
      } else {
        // This is probably a text element for whitespace, etc, so just skip it.
      }
    }
    Assert.assertNotNull(path);
    return new FlatFileStorageProviderConfiguration(new File(path), shouldBlockOnLock);
  }
}
