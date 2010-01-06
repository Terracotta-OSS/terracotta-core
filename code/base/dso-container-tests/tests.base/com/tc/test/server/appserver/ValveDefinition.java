/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test.server.appserver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ValveDefinition {

  private final String              className;
  private final Map<String, String> attributes = new LinkedHashMap<String, String>();

  public ValveDefinition(String className) {
    this.className = className;
  }

  public String getClassName() {
    return className;
  }

  public void setAttribute(String name, String value) {
    attributes.put(name, value);
  }

  @Override
  public String toString() {
    return toXml();
  }

  public String toXml() {
    String xml = "<Valve className=\"" + getClassName() + "\"";
    for (Entry<String, String> attr : attributes.entrySet()) {
      xml += " " + attr.getKey() + "=\"" + attr.getValue() + "\"";
    }

    xml += "/>";

    return xml;
  }
}
