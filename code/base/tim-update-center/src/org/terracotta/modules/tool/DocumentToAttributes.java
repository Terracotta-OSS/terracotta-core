/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.jdom.Attribute;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    for (Iterator i = module.getAttributes().iterator(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      attributes.put(attr.getName(), attr.getValue());
    }

    for (Iterator i = module.getChildren().iterator(); i.hasNext();) {
      Element child = (Element) i.next();
      if ("dependencies".equals(child.getName())) {
        List<Map<String, Object>> dependencies = new ArrayList<Map<String, Object>>();
        for (Iterator j = child.getChildren().iterator(); j.hasNext();) {
          Element element = (Element) j.next();
          Map<String, Object> dependency = DocumentToAttributes.transform(element);
          dependency.put("_dependencyType", DependencyType.computeType(element.getName()));
          dependencies.add(dependency);
        }
        attributes.put(child.getName(), dependencies);
        continue;
      }
      attributes.put(child.getName(), child.getText());
    }

    return attributes;
  }

}
