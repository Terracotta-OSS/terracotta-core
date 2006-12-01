/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
