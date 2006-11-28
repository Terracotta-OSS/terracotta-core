/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.setup;

public class TestStringXmlObject extends TestXmlObject {

  private final String value;
  
  public TestStringXmlObject(String value) {
    this.value = value;
  }
  
  public String getStringValue() {
    return this.value;
  }

}
