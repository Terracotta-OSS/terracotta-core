/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import com.tc.config.schema.MockXmlObject;

public class StringXPathBasedConfigItemTest extends XPathBasedConfigItemTestBase {

  private class SubBean extends MockXmlObject {
    public String getStringValue() {
      return currentValue;
    }
  }

  private class DefaultBean extends MockXmlObject {
    private final String value;

    public DefaultBean(String value) {
      this.value = value;
    }

    public String getStringValue() {
      return value;
    }
  }

  private String currentValue;

  protected MockXmlObject createSubBean() throws Exception {
    return new SubBean();
  }

  public void setUp() throws Exception {
    super.setUp();

    this.currentValue = "hi there";
  }

  public void testNoDefault() throws Exception {
    StringXPathBasedConfigItem item = new StringXPathBasedConfigItem(context, xpath);

    assertEquals("hi there", item.getString());
    assertEquals("hi there", item.getObject());

    currentValue = null;
    item = new StringXPathBasedConfigItem(context, xpath);

    assertNull(item.getString());
    assertNull(item.getObject());
  }

  public void testDefault() throws Exception {
    context.setReturnedHasDefaultFor(true);
    context.setReturnedDefaultFor(new DefaultBean("the default"));
    StringXPathBasedConfigItem item = new StringXPathBasedConfigItem(context, xpath);

    assertEquals("hi there", item.getString());
    assertEquals("hi there", item.getObject());

    currentValue = null;
    item = new StringXPathBasedConfigItem(context, xpath);

    assertEquals("the default", item.getString());
    assertEquals("the default", item.getObject());
  }

}
