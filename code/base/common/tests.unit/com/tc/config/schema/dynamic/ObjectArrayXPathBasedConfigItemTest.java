/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.MockXmlObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.context.MockConfigContext;
import com.tc.test.TCTestCase;

import java.util.Date;

/**
 * Unit test for {@link ObjectArrayXPathBasedConfigItem}.
 */
public class ObjectArrayXPathBasedConfigItemTest extends TCTestCase {

  private class TestObjectArrayXPathBasedConfigItem extends ObjectArrayXPathBasedConfigItem {
    private int       numFetchDataFromXmlObjects;
    private XmlObject lastXmlObject;

    public TestObjectArrayXPathBasedConfigItem(ConfigContext context, String xpath, Object defaultValue) {
      super(context, xpath, defaultValue);

      reset();
    }

    public TestObjectArrayXPathBasedConfigItem(ConfigContext context, String xpath) {
      super(context, xpath);

      reset();
    }

    public void reset() {
      this.numFetchDataFromXmlObjects = 0;
      this.lastXmlObject = null;
    }

    protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
      ++this.numFetchDataFromXmlObjects;
      this.lastXmlObject = xmlObject;
      return currentValue;
    }

    public XmlObject getLastXmlObject() {
      return lastXmlObject;
    }

    public int getNumFetchDataFromXmlObjects() {
      return numFetchDataFromXmlObjects;
    }
  }

  private Object[] currentValue;

  protected void setUp() throws Exception {
    super.setUp();

    this.currentValue = new Object[] { "foo", new Integer(42), new Date() };
  }

  public void testAll() throws Exception {
    MockXmlObject bean = new MockXmlObject();
    MockXmlObject subBean = new MockXmlObject();

    bean.setReturnedSelectPath(new XmlObject[] { subBean });

    MockConfigContext context = new MockConfigContext();

    context.setReturnedBean(bean);

    TestObjectArrayXPathBasedConfigItem withoutDefault = new TestObjectArrayXPathBasedConfigItem(context, "foobar");

    assertSame(this.currentValue, withoutDefault.getObject());
    assertEquals(1, withoutDefault.getNumFetchDataFromXmlObjects());
    assertSame(subBean, withoutDefault.getLastXmlObject());

    withoutDefault.reset();
    withoutDefault = new TestObjectArrayXPathBasedConfigItem(context, "foobar");

    assertSame(this.currentValue, withoutDefault.getObjects());
    assertEquals(1, withoutDefault.getNumFetchDataFromXmlObjects());
    assertSame(subBean, withoutDefault.getLastXmlObject());
  }
}
