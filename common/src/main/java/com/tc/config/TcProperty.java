/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
