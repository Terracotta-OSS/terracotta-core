/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.MockXmlObject;
import com.tc.util.TCAssertionError;

/**
 * Unit test for {@link StringArrayXPathBasedConfigItem}.
 */
public class StringArrayXPathBasedConfigItemTest extends XPathBasedConfigItemTestBase {

  private class NoGetMethod extends MockXmlObject {
    public String[] getFoo() {
      return new String[] { "foo", "bar" };
    }

    public String[] getBarArray(int val) {
      return new String[] { "baz", "bar" };
    }

    protected String[] getBazArray() {
      return new String[] { "foo", "baz" };
    }
  }

  private class OneGetMethod extends MockXmlObject {
    public String[] getFoo() {
      return new String[] { "foo", "bar" };
    }

    public String[] getBarArray(int val) {
      return new String[] { "baz", "bar" };
    }

    protected String[] getBazArray() {
      return new String[] { "foo", "baz" };
    }

    public String[] getZonkArray() {
      return currentValue;
    }
  }

  private class TwoGetMethods extends MockXmlObject {
    public String[] getFoo() {
      return new String[] { "foo", "bar" };
    }

    public String[] getBarArray(int val) {
      return new String[] { "baz", "bar" };
    }

    protected String[] getBazArray() {
      return new String[] { "foo", "baz" };
    }

    public String[] getZonkArray() {
      return new String[] { "baz", "quux" };
    }

    public String[] getAnotherArray() {
      return new String[] { "baz2", "quux2" };
    }
  }

  private String[] currentValue;

  public void setUp() throws Exception {
    super.setUp();

    this.currentValue = new String[] { "real", "data" };
  }

  protected MockXmlObject createSubBean() throws Exception {
    return new OneGetMethod();
  }

  public void testBogusCandidates() throws Exception {
    bean.setReturnedSelectPath(new XmlObject[] { new NoGetMethod() });
    StringArrayXPathBasedConfigItem item = new StringArrayXPathBasedConfigItem(context, xpath);

    try {
      item.getObject();
      fail("Didn't get TCAE on no get method");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      item.getStringArray();
      fail("Didn't get TCAE on no get method");
    } catch (TCAssertionError tcae) {
      // ok
    }

    bean.setReturnedSelectPath(new XmlObject[] { new TwoGetMethods() });
    item = new StringArrayXPathBasedConfigItem(context, xpath);

    try {
      item.getObject();
      fail("Didn't get TCAE on no get method");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      item.getStringArray();
      fail("Didn't get TCAE on no get method");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

  public void testRealData() throws Exception {
    StringArrayXPathBasedConfigItem item = new StringArrayXPathBasedConfigItem(context, xpath);

    assertEqualsOrdered(new String[] { "real", "data" }, item.getStringArray());
    assertEqualsOrdered(new String[] { "real", "data" }, item.getObject());

    this.currentValue = new String[] { "more", "stuff" };

    item = new StringArrayXPathBasedConfigItem(context, xpath);

    assertEqualsOrdered(new String[] { "more", "stuff" }, item.getStringArray());
    assertEqualsOrdered(new String[] { "more", "stuff" }, item.getObject());
  }

}
