/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import com.tc.config.schema.MockXmlObject;
import com.tc.util.TCAssertionError;

import java.math.BigInteger;

public class IntXPathBasedConfigItemTest extends XPathBasedConfigItemTestBase {

  private class SubBean extends MockXmlObject {
    public BigInteger getBigIntegerValue() {
      return currentValue;
    }
  }

  private static class DefaultBean extends MockXmlObject {
    private final BigInteger value;

    public DefaultBean(BigInteger value) {
      this.value = value;
    }

    public BigInteger getBigIntegerValue() {
      return this.value;
    }
  }

  private BigInteger currentValue;

  protected MockXmlObject createSubBean() throws Exception {
    return new SubBean();
  }

  public void setUp() throws Exception {
    super.setUp();

    this.currentValue = new BigInteger("147");
    context.setReturnedHasDefaultFor(false);
    context.setReturnedIsOptional(false);
  }

  public void testConstruction() throws Exception {
    context.setReturnedIsOptional(true);

    try {
      new IntXPathBasedConfigItem(context, xpath);
      fail("Didn't get TCAE on item that's optional with no default");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

  public void testNoDefault() throws Exception {
    IntXPathBasedConfigItem item = new IntXPathBasedConfigItem(context, xpath);

    assertEquals(147, item.getInt());
    assertEquals(new Integer(147), item.getObject());

    this.currentValue = new BigInteger("-123854");

    item = new IntXPathBasedConfigItem(context, xpath);
    assertEquals(-123854, item.getInt());
    assertEquals(new Integer(-123854), item.getObject());

    this.currentValue = new BigInteger("43289058203953495739457489020092370897586768975042532");
    item = new IntXPathBasedConfigItem(context, xpath);

    try {
      item.getInt();
      fail("Didn't get TCAE on too-big value");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      item.getObject();
      fail("Didn't get TCAE on too-big value");
    } catch (TCAssertionError tcae) {
      // ok
    }

    this.currentValue = new BigInteger("-43289058203953495739457489020092370897586768975042532");
    item = new IntXPathBasedConfigItem(context, xpath);

    try {
      item.getInt();
      fail("Didn't get TCAE on too-small value");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      item.getObject();
      fail("Didn't get TCAE on too-small value");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

  public void testDefault() throws Exception {
    context.setReturnedHasDefaultFor(true);
    context.setReturnedDefaultFor(new DefaultBean(new BigInteger("42")));

    this.currentValue = null;
    IntXPathBasedConfigItem item = new IntXPathBasedConfigItem(context, xpath);

    assertEquals(42, item.getInt());
    assertEquals(new Integer(42), item.getObject());

    this.currentValue = new BigInteger("483290");
    item = new IntXPathBasedConfigItem(context, xpath);

    assertEquals(483290, item.getInt());
    assertEquals(new Integer(483290), item.getObject());
  }

  public void testTooBigDefault() throws Exception {
    context.setReturnedHasDefaultFor(true);
    context.setReturnedDefaultFor(new DefaultBean(new BigInteger("424328904823905829058025739572230498074078")));

    try {
      new IntXPathBasedConfigItem(context, xpath).defaultValue();
      fail("Didn't get TCAE on too-big default");
    } catch (TCAssertionError tcae) {
      // ok
    }

    context.setReturnedDefaultFor(new DefaultBean(new BigInteger("-424328904823905829058025739572230498074078")));

    try {
      new IntXPathBasedConfigItem(context, xpath).defaultValue();
      fail("Didn't get TCAE on too-small default");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

}
