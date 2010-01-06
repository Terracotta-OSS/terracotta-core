/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test.server.appserver;


public class ValveDefinition {

  private final String className;

  public ValveDefinition(String className) {
    this.className = className;
  }

  public String getClassName() {
    return className;
  }

  public String toXml() {
    return "<Valve className=\"" + getClassName() + "\"/>";
  }

}
