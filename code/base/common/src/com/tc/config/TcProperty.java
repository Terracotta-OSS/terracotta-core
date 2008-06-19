/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config;

public class TcProperty {
  private String name;
  private String value;

  public TcProperty(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getPropertyName() {
    return name;
  }

  public String getPropertyValue() {
    return value;
  }
}
