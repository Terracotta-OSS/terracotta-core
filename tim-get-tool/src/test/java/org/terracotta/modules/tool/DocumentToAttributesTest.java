/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.io.IOUtils;
import org.terracotta.modules.tool.DocumentToAttributes.DependencyType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

public final class DocumentToAttributesTest extends TestCase {

  private Document document = null;

  @Override
  public void setUp() throws Exception {
    InputStream data = null;
    try {
      data = getClass().getResourceAsStream("/testData01.xml");
      document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(data);
    } finally {
      IOUtils.closeQuietly(data);
    }
  }

  public void testParse() {
    NodeList children = document.getDocumentElement().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element elem = (Element) node;
        Map<String, Object> attributes = DocumentToAttributes.transform(elem);

        assertTrue(attributes.containsKey("groupId"));
        assertTrue(attributes.containsKey("artifactId"));
        assertTrue(attributes.containsKey("version"));
        assertTrue(attributes.containsKey("tc-version"));
        assertTrue(attributes.containsKey("website"));
        assertTrue(attributes.containsKey("vendor"));
        assertTrue(attributes.containsKey("copyright"));
        assertTrue(attributes.containsKey("category"));
        assertTrue(attributes.containsKey("description"));
        assertTrue(attributes.containsKey("repoURL"));
        assertTrue(attributes.containsKey("installPath"));
        assertTrue(attributes.containsKey("filename"));
        assertTrue(attributes.containsKey("dependencies"));

        List<Map<String, Object>> dependencies = (List<Map<String, Object>>) attributes.get("dependencies");

        for (Map<String, Object> dependency : dependencies) {
          assertTrue(dependency.containsKey("_dependencyType"));

          if (DependencyType.INSTANCE.equals(dependency.get("_dependencyType"))) {
            assertTrue(dependency.containsKey("groupId"));
            assertTrue(dependency.containsKey("artifactId"));
            assertTrue(dependency.containsKey("version"));
            assertTrue(dependency.containsKey("repoURL"));
            assertTrue(dependency.containsKey("installPath"));
            assertTrue(dependency.containsKey("filename"));
            continue;
          }

          if (DependencyType.REFERENCE.equals(dependency.get("_dependencyType"))) {
            assertTrue(dependency.containsKey("_dependencyType"));
            assertTrue(dependency.containsKey("groupId"));
            assertTrue(dependency.containsKey("artifactId"));
            assertTrue(dependency.containsKey("version"));
            continue;
          }

          fail("DependencyType is neither REFERENCE or INSTANCE");
        }
      }
    }
  }
}
