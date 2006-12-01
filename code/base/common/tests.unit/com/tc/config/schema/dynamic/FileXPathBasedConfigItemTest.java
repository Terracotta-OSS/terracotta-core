/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import com.tc.config.schema.MockXmlObject;

import java.io.File;

/**
 * Unit test for {@link FileXPathBasedConfigItem}.
 */
public class FileXPathBasedConfigItemTest extends XPathBasedConfigItemTestBase {

  private class SubBean extends MockXmlObject {
    public String getStringValue() {
      return currentValue;
    }
  }
  
  private String currentValue;

  protected MockXmlObject createSubBean() throws Exception {
    return new SubBean();
  }

  protected void setUp() throws Exception {
    super.setUp();
    
    this.currentValue = "foobar";
  }
  
  public void testAll() throws Exception {
    FileXPathBasedConfigItem withoutDefault = new FileXPathBasedConfigItem(context, xpath);
    
    assertEquals(new File("foobar"), withoutDefault.getFile());
    assertEquals(new File("foobar"), withoutDefault.getObject());
    
    this.currentValue = null;
    withoutDefault = new FileXPathBasedConfigItem(context, xpath);
    assertNull(withoutDefault.getFile());
    assertNull(withoutDefault.getObject());
  }
  
}
