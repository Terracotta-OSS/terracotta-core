/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.MockXmlObject;

public class BooleanXPathBasedConfigItemTest extends XPathBasedConfigItemTestBase {

  private class SubBean extends MockXmlObject {
    public boolean getBooleanValue() {
      return currentValue;
    }
  }

  private boolean currentValue;

  protected MockXmlObject createSubBean() throws Exception {
    return new SubBean();
  }

  public void setUp() throws Exception {
    super.setUp();

    this.currentValue = false;
  }

  public void testAll() throws Exception {
    BooleanXPathBasedConfigItem withoutDefault = new BooleanXPathBasedConfigItem(context, xpath);
    BooleanXPathBasedConfigItem withTrueDefault = new BooleanXPathBasedConfigItem(context, xpath, true);
    BooleanXPathBasedConfigItem withFalseDefault = new BooleanXPathBasedConfigItem(context, xpath, false);

    this.currentValue = false;
    assertFalse(withoutDefault.getBoolean());
    assertEquals(Boolean.FALSE, withoutDefault.getObject());

    this.currentValue = true;
    withoutDefault = new BooleanXPathBasedConfigItem(context, xpath);
    assertTrue(withoutDefault.getBoolean());
    assertEquals(Boolean.TRUE, withoutDefault.getObject());

    this.currentValue = false;
    assertFalse(withTrueDefault.getBoolean());
    assertEquals(Boolean.FALSE, withTrueDefault.getObject());

    withTrueDefault = new BooleanXPathBasedConfigItem(context, xpath, true);
    this.bean.setReturnedSelectPath(new XmlObject[0]);
    assertTrue(withTrueDefault.getBoolean());
    assertEquals(Boolean.TRUE, withTrueDefault.getObject());
    this.bean.setReturnedSelectPath(new XmlObject[] { this.subBean });

    this.currentValue = true;
    assertTrue(withFalseDefault.getBoolean());
    assertEquals(Boolean.TRUE, withFalseDefault.getObject());

    withFalseDefault = new BooleanXPathBasedConfigItem(context, xpath, false);
    this.bean.setReturnedSelectPath(new XmlObject[0]);
    assertFalse(withFalseDefault.getBoolean());
    assertEquals(Boolean.FALSE, withFalseDefault.getObject());
  }

}
