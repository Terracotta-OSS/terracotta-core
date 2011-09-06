/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class DocumentToAttributes {

  public enum DependencyType {
    REFERENCE, INSTANCE, UNKNOWN;

    public static DependencyType computeType(String name) {
      if ("module".equals(name)) {
        return DependencyType.INSTANCE;
      } else if ("moduleRef".equals(name)) {
        return DependencyType.REFERENCE;
      } else {
        return DependencyType.UNKNOWN;
      }
    }
  }

  public static Map<String, Object> transform(Element module) {
    Map<String, Object> attributes = new HashMap<String, Object>();
    NamedNodeMap attrMap = module.getAttributes();

    for (int i = 0; i < attrMap.getLength(); i++) {
      Attr attr = (Attr) attrMap.item(i);
      attributes.put(attr.getName(), attr.getValue());
    }

    NodeList childList = module.getChildNodes();
    for (int i = 0; i < childList.getLength(); i++) {
      Node node = childList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element child = (Element) node;
        if ("dependencies".equals(child.getTagName())) {
          List<Map<String, Object>> dependencies = new ArrayList<Map<String, Object>>();
          NodeList dependencyList = child.getChildNodes();
          for (int j = 0; j < dependencyList.getLength(); j++) {
            node = dependencyList.item(j);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
              Element element = (Element) node;
              Map<String, Object> dependency = DocumentToAttributes.transform(element);
              dependency.put("_dependencyType", DependencyType.computeType(element.getTagName()));
              dependencies.add(dependency);
            }
          }
          attributes.put(child.getTagName(), dependencies);
          continue;
        }
        attributes.put(child.getTagName(), child.getTextContent());
      }
    }

    return attributes;
  }

}
