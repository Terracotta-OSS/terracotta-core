/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.MockXmlObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.context.MockConfigContext;
import com.tc.test.TCTestCase;
import com.tc.util.TCAssertionError;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link XPathBasedConfigItem}.
 */
public class XPathBasedConfigItemTest extends TCTestCase {

  private static class TestXPathBasedConfigItem extends XPathBasedConfigItem {
    private int      numFetchDataFromXmlObjects;
    private List     lastXmlObjects;
    private Object[] returnedFetchDataFromXmlObject;
    private int      returnedFetchDataFromXmlObjectPos;

    public TestXPathBasedConfigItem(ConfigContext context, String xpath, Object defaultValue) {
      super(context, xpath, defaultValue);
      this.lastXmlObjects = new ArrayList();
      this.returnedFetchDataFromXmlObject = null;
      reset();
    }

    public TestXPathBasedConfigItem(ConfigContext context, String xpath) {
      super(context, xpath);
      this.lastXmlObjects = new ArrayList();
      this.returnedFetchDataFromXmlObject = null;
      reset();
    }

    public void reset() {
      this.numFetchDataFromXmlObjects = 0;
      this.lastXmlObjects.clear();
      this.returnedFetchDataFromXmlObjectPos = 0;
    }

    protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
      ++this.numFetchDataFromXmlObjects;
      this.lastXmlObjects.add(xmlObject);
      int numReturned = this.returnedFetchDataFromXmlObject == null ? 0 : this.returnedFetchDataFromXmlObject.length;
      if (this.returnedFetchDataFromXmlObjectPos >= numReturned) this.returnedFetchDataFromXmlObjectPos = 0;
      if (this.returnedFetchDataFromXmlObject == null) return null;
      else return this.returnedFetchDataFromXmlObject[this.returnedFetchDataFromXmlObjectPos++];
    }

    public XmlObject getLastXmlObject() {
      return (XmlObject) this.lastXmlObjects.get(this.lastXmlObjects.size() - 1);
    }

    public int getNumFetchDataFromXmlObjects() {
      return numFetchDataFromXmlObjects;
    }

    public XmlObject[] getLastXmlObjects() {
      return (XmlObject[]) this.lastXmlObjects.toArray(new XmlObject[this.lastXmlObjects.size()]);
    }

    public void setReturnedFetchDataFromXmlObject(Object[] returnedFetchDataFromXmlObject) {
      this.returnedFetchDataFromXmlObject = returnedFetchDataFromXmlObject;
    }
  }

  private MockConfigContext        context;
  private String                   xpath;

  private TestXPathBasedConfigItem item;

  private MockXmlObject            bean;
  private MockXmlObject            subBean;
  private Object                   convertedValue;

  private MockConfigItemListener   listener1;
  private MockConfigItemListener   listener2;

  public void setUp() throws Exception {
    this.context = new MockConfigContext();
    this.xpath = "foobar/baz";

    this.item = new TestXPathBasedConfigItem(this.context, this.xpath);

    this.bean = new MockXmlObject();
    this.context.setReturnedBean(this.bean);

    this.subBean = new MockXmlObject();
    this.bean.setReturnedSelectPath(new XmlObject[] { this.subBean });

    this.convertedValue = new Object();
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { this.convertedValue });

    this.listener1 = new MockConfigItemListener();
    this.listener2 = new MockConfigItemListener();
    
    this.item.defaultValue(); // to trigger default mechanism
  }

  public void testConstruction() throws Exception {
    try {
      new TestXPathBasedConfigItem(null, this.xpath);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new TestXPathBasedConfigItem(this.context, null);
      fail("Didn't get NPE on no XPath");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new TestXPathBasedConfigItem(this.context, "");
      fail("Didn't get IAE on empty context");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      new TestXPathBasedConfigItem(this.context, "   ");
      fail("Didn't get TCAE on blank context");
    } catch (IllegalArgumentException iae) {
      // ok
    }
  }

  public void testInitialSetup() throws Exception {
    assertEquals(1, this.context.getNumItemCreateds());
    assertSame(this.item, this.context.getLastItemCreated());

    assertEquals(1, this.context.getNumHasDefaultFors());
    assertEquals(this.xpath, this.context.getLastHasDefaultForXPath());

    assertEquals(0, this.context.getNumBeans());
    assertEquals(0, this.item.getNumFetchDataFromXmlObjects());
  }

  public void testComponents() throws Exception {
    assertSame(this.context, this.item.context());
    assertSame(this.xpath, this.item.xpath());
    assertNull(this.item.defaultValue());
  }

  public void testFetchObject() throws Exception {
    assertEquals(0, this.context.getNumBeans());
    assertEquals(0, this.item.getNumFetchDataFromXmlObjects());
    assertEquals(0, this.bean.getNumSelectPaths());

    this.context.reset();

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());

    assertSame(this.convertedValue, this.item.getObject());

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());
    assertEquals(1, this.context.getNumBeans());
    assertEquals(1, this.bean.getNumSelectPaths());
    assertEquals(this.xpath, this.bean.getLastSelectPath());
    assertEquals(1, this.item.getNumFetchDataFromXmlObjects());
    assertSame(this.subBean, this.item.getLastXmlObject());

    // Fetch again -- make sure it caches
    this.context.reset();
    this.bean.reset();
    this.item.reset();

    assertSame(this.convertedValue, this.item.getObject());
    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());
    assertEquals(0, this.context.getNumBeans());
    assertEquals(0, this.bean.getNumSelectPaths());
    assertEquals(0, this.item.getNumFetchDataFromXmlObjects());
  }

  public void testFetchObjectWithDefaultsWithValue() throws Exception {
    Object defaultValue = new Object();

    this.item = new TestXPathBasedConfigItem(this.context, this.xpath, defaultValue);
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { this.convertedValue });

    assertEquals(0, this.context.getNumBeans());
    assertEquals(0, this.item.getNumFetchDataFromXmlObjects());
    assertEquals(0, this.bean.getNumSelectPaths());

    this.context.reset();

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());

    assertSame(this.convertedValue, this.item.getObject());

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());
    assertEquals(1, this.context.getNumBeans());
    assertEquals(1, this.bean.getNumSelectPaths());
    assertEquals(this.xpath, this.bean.getLastSelectPath());
    assertEquals(1, this.item.getNumFetchDataFromXmlObjects());
    assertSame(this.subBean, this.item.getLastXmlObject());

    // Fetch again -- make sure it caches
    this.context.reset();
    this.bean.reset();
    this.item.reset();

    assertSame(this.convertedValue, this.item.getObject());
    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());
    assertEquals(0, this.context.getNumBeans());
    assertEquals(0, this.bean.getNumSelectPaths());
    assertEquals(0, this.item.getNumFetchDataFromXmlObjects());
  }

  public void testFetchObjectWithDefaultsWithNoValueInXPath() throws Exception {
    Object defaultValue = new Object();

    this.item = new TestXPathBasedConfigItem(this.context, this.xpath, defaultValue);
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { this.convertedValue });

    assertEquals(0, this.context.getNumBeans());
    assertEquals(0, this.item.getNumFetchDataFromXmlObjects());
    assertEquals(0, this.bean.getNumSelectPaths());

    this.context.reset();

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());

    this.bean.setReturnedSelectPath(null);
    this.item = new TestXPathBasedConfigItem(this.context, this.xpath, defaultValue);
    assertSame(defaultValue, this.item.getObject());

    assertEquals(1, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());
    assertEquals(1, this.context.getNumBeans());
    assertEquals(1, this.bean.getNumSelectPaths());
    assertEquals(this.xpath, this.bean.getLastSelectPath());

    // Fetch again -- make sure it caches
    this.context.reset();
    this.bean.reset();
    this.item.reset();

    assertSame(defaultValue, this.item.getObject());
    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());
    assertEquals(0, this.context.getNumBeans());
    assertEquals(0, this.bean.getNumSelectPaths());
    assertEquals(0, this.item.getNumFetchDataFromXmlObjects());
  }

  public void testFetchObjectWithDefaultsWithNoConvertedValue() throws Exception {
    Object defaultValue = new Object();

    this.item = new TestXPathBasedConfigItem(this.context, this.xpath, defaultValue);
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { null });

    assertEquals(0, this.context.getNumBeans());
    assertEquals(0, this.item.getNumFetchDataFromXmlObjects());
    assertEquals(0, this.bean.getNumSelectPaths());

    this.context.reset();

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());

    assertSame(defaultValue, this.item.getObject());

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());
    assertEquals(1, this.context.getNumBeans());
    assertEquals(1, this.bean.getNumSelectPaths());
    assertEquals(this.xpath, this.bean.getLastSelectPath());
    assertEquals(1, this.item.getNumFetchDataFromXmlObjects());
    assertSame(this.subBean, this.item.getLastXmlObject());

    // Fetch again -- make sure it caches
    this.context.reset();
    this.bean.reset();
    this.item.reset();

    assertSame(defaultValue, this.item.getObject());
    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());
    assertEquals(0, this.context.getNumBeans());
    assertEquals(0, this.bean.getNumSelectPaths());
    assertEquals(0, this.item.getNumFetchDataFromXmlObjects());
  }

  public void testListenerDefaultBeforehandNoConvertedValue() throws Exception {
    Object defaultValue = new Object();

    this.item = new TestXPathBasedConfigItem(this.context, this.xpath, defaultValue);
    Object newValue = new Object();
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { null, newValue });

    this.context.reset();

    MockXmlObject newBean = new MockXmlObject();
    MockXmlObject newSubBean = new MockXmlObject();
    newBean.setReturnedSelectPath(new XmlObject[] { newSubBean });
    this.item.addListener(this.listener1);

    this.item.configurationChanged(this.bean, newBean);

    assertEquals(1, this.listener1.getNumValueChangeds());
    assertSame(defaultValue, this.listener1.getLastOldValue());
    assertSame(newValue, this.listener1.getLastNewValue());

    assertSame(newValue, this.item.getObject());
  }

  public void testListenerDefaultBeforehandNoXPath() throws Exception {
    Object defaultValue = new Object();

    this.item = new TestXPathBasedConfigItem(this.context, this.xpath, defaultValue);
    Object newValue = new Object();
    this.bean.setReturnedSelectPath(null);
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { null, newValue });

    this.context.reset();

    MockXmlObject newBean = new MockXmlObject();
    MockXmlObject newSubBean = new MockXmlObject();
    newBean.setReturnedSelectPath(new XmlObject[] { newSubBean });
    this.item.addListener(this.listener1);

    this.item.configurationChanged(this.bean, newBean);

    assertEquals(1, this.listener1.getNumValueChangeds());
    assertSame(defaultValue, this.listener1.getLastOldValue());
    assertSame(newValue, this.listener1.getLastNewValue());

    assertSame(newValue, this.item.getObject());
  }

  public void testListenerDefaultAfterwardsNoConvertedValue() throws Exception {
    Object defaultValue = new Object();

    this.item = new TestXPathBasedConfigItem(this.context, this.xpath, defaultValue);
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { this.convertedValue, null });

    this.context.reset();

    MockXmlObject newBean = new MockXmlObject();
    MockXmlObject newSubBean = new MockXmlObject();
    newBean.setReturnedSelectPath(new XmlObject[] { newSubBean });
    this.item.addListener(this.listener1);

    this.item.configurationChanged(this.bean, newBean);

    assertEquals(1, this.listener1.getNumValueChangeds());
    assertSame(this.convertedValue, this.listener1.getLastOldValue());
    assertSame(defaultValue, this.listener1.getLastNewValue());

    assertSame(defaultValue, this.item.getObject());
  }

  public void testListenerDefaultAfterwardsNoXPath() throws Exception {
    Object defaultValue = new Object();

    this.item = new TestXPathBasedConfigItem(this.context, this.xpath, defaultValue);
    Object newValue = new Object();
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { this.convertedValue, null, newValue });

    this.context.reset();

    MockXmlObject newBean = new MockXmlObject();
    newBean.setReturnedSelectPath(null);
    this.item.addListener(this.listener1);

    this.item.configurationChanged(this.bean, newBean);

    assertEquals(1, this.listener1.getNumValueChangeds());
    assertSame(this.convertedValue, this.listener1.getLastOldValue());
    assertSame(defaultValue, this.listener1.getLastNewValue());

    assertSame(defaultValue, this.item.getObject());
  }

  public void testListenerDefaultBeforeAndAfterNoConvertedValue() throws Exception {
    Object defaultValue = new Object();

    this.item = new TestXPathBasedConfigItem(this.context, this.xpath, defaultValue);
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { null, null });

    this.context.reset();

    MockXmlObject newBean = new MockXmlObject();
    MockXmlObject newSubBean = new MockXmlObject();
    newBean.setReturnedSelectPath(new XmlObject[] { newSubBean });
    this.item.addListener(this.listener1);

    this.item.configurationChanged(this.bean, newBean);

    assertEquals(0, this.listener1.getNumValueChangeds());

    assertSame(defaultValue, this.item.getObject());
  }

  public void testPathReturnsRObjects() throws Exception {
    this.context.reset();

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());

    this.bean.setReturnedSelectPath(null);

    assertSame(this.convertedValue, this.item.getObject());

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());
    assertEquals(1, this.context.getNumBeans());
    assertEquals(1, this.bean.getNumSelectPaths());
    assertEquals(this.xpath, this.bean.getLastSelectPath());
    assertEquals(1, this.item.getNumFetchDataFromXmlObjects());
  }

  public void testPathReturnsZeroLengthArray() throws Exception {
    this.context.reset();

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());

    this.bean.setReturnedSelectPath(new XmlObject[0]);

    assertSame(this.convertedValue, this.item.getObject());

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());
    assertEquals(1, this.context.getNumBeans());
    assertEquals(1, this.bean.getNumSelectPaths());
    assertEquals(this.xpath, this.bean.getLastSelectPath());
    assertEquals(1, this.item.getNumFetchDataFromXmlObjects());
  }

  public void testPathReturnsMultipleObjects() throws Exception {
    this.context.reset();

    assertEquals(0, this.context.getNumItemCreateds());
    assertEquals(0, this.context.getNumHasDefaultFors());

    this.bean.setReturnedSelectPath(new XmlObject[] { new MockXmlObject(), new MockXmlObject() });

    try {
      this.item.getObject();
      fail("Didn't get TCAE on multiple return values");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

  public void testListeners() throws Exception {
    checkListeners(false, false, false, null);

    resetItem();
    this.item.addListener(this.listener1);
    checkListeners(true, false, false, null);

    resetItem();
    this.item.addListener(this.listener1);
    this.item.addListener(this.listener2);
    checkListeners(true, true, false, null);

    resetItem();
    this.item.addListener(this.listener1);
    this.item.addListener(this.listener2);
    this.item.removeListener(this.listener1);
    checkListeners(false, true, false, null);

    resetItem();
    this.item.addListener(this.listener1);
    this.item.addListener(this.listener2);
    this.item.removeListener(this.listener1);
    this.item.removeListener(this.listener2);
    this.item.removeListener(this.listener2);
    this.item.removeListener(this.listener1);
    checkListeners(false, false, false, null);
  }

  public void testListenersWithDataAlready() throws Exception {
    Object curVal = this.convertedValue;

    assertSame(this.convertedValue, this.item.getObject());
    curVal = checkListeners(false, false, true, curVal);

    this.item.addListener(this.listener1);
    curVal = checkListeners(true, false, true, curVal);

    this.item.addListener(this.listener2);
    curVal = checkListeners(true, true, true, curVal);

    this.item.removeListener(this.listener2);
    curVal = checkListeners(true, false, true, curVal);

    this.item.removeListener(this.listener1);
    curVal = checkListeners(false, false, true, curVal);

    this.item.removeListener(this.listener1);
    curVal = checkListeners(false, false, true, curVal);
  }

  public void testNoListenerTriggerOnEqualObjects() throws Exception {
    Integer one = new Integer(24);
    Integer two = new Integer(24);

    assertNotSame(one, two);

    this.item.addListener(this.listener1);
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { one, two });

    MockXmlObject newBean = new MockXmlObject();
    newBean.setReturnedSelectPath(new XmlObject[] { new MockXmlObject() });

    this.item.configurationChanged(this.bean, newBean);

    assertEquals(0, this.listener1.getNumValueChangeds());
  }

  public void testNoListenerTriggerOnBothNull() throws Exception {
    this.item.addListener(this.listener1);
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { null, null });

    MockXmlObject newBean = new MockXmlObject();
    newBean.setReturnedSelectPath(new XmlObject[] { new MockXmlObject() });

    this.item.configurationChanged(this.bean, newBean);

    assertEquals(0, this.listener1.getNumValueChangeds());
  }

  public void testListenerTriggerOnNullToSomething() throws Exception {
    Integer two = new Integer(24);

    this.item.addListener(this.listener1);
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { null, two });

    MockXmlObject newBean = new MockXmlObject();
    newBean.setReturnedSelectPath(new XmlObject[] { new MockXmlObject() });

    this.item.configurationChanged(this.bean, newBean);

    assertEquals(1, this.listener1.getNumValueChangeds());
    assertNull(this.listener1.getLastOldValue());
    assertSame(two, this.listener1.getLastNewValue());
  }

  public void testListenerTriggerOnSomethingToNull() throws Exception {
    Integer one = new Integer(24);

    this.item.addListener(this.listener1);
    this.item.setReturnedFetchDataFromXmlObject(new Object[] { one, null });

    MockXmlObject newBean = new MockXmlObject();
    newBean.setReturnedSelectPath(new XmlObject[] { new MockXmlObject() });

    this.item.configurationChanged(this.bean, newBean);

    assertEquals(1, this.listener1.getNumValueChangeds());
    assertSame(one, this.listener1.getLastOldValue());
    assertNull(this.listener1.getLastNewValue());
  }

  private void resetItem() throws Exception {
    this.item = new TestXPathBasedConfigItem(this.context, this.xpath);
  }

  private Object checkListeners(boolean expectedOne, boolean expectedTwo, boolean hasDataAlready, Object oldData) {
    this.listener1.reset();
    this.listener2.reset();
    this.item.reset();

    MockXmlObject oldBean = new MockXmlObject();
    MockXmlObject newBean = new MockXmlObject();

    MockXmlObject oldSubBean = new MockXmlObject();
    MockXmlObject newSubBean = new MockXmlObject();

    oldBean.setReturnedSelectPath(new XmlObject[] { oldSubBean });
    newBean.setReturnedSelectPath(new XmlObject[] { newSubBean });

    Object oldObject = new Object();
    Object newObject = new Object();

    if (!hasDataAlready) this.item.setReturnedFetchDataFromXmlObject(new Object[] { oldObject, newObject });
    else this.item.setReturnedFetchDataFromXmlObject(new Object[] { newObject });

    this.item.configurationChanged(oldBean, newBean);

    if (!hasDataAlready) {
      assertEquals(1, oldBean.getNumSelectPaths());
      assertEquals(this.xpath, oldBean.getLastSelectPath());

      assertEquals(1, newBean.getNumSelectPaths());
      assertEquals(this.xpath, newBean.getLastSelectPath());

      assertEquals(2, this.item.getNumFetchDataFromXmlObjects());
      assertSame(oldSubBean, this.item.getLastXmlObjects()[0]);
      assertSame(newSubBean, this.item.getLastXmlObjects()[1]);
    } else {
      assertEquals(1, newBean.getNumSelectPaths());
      assertEquals(this.xpath, newBean.getLastSelectPath());

      assertEquals(1, this.item.getNumFetchDataFromXmlObjects());
      assertSame(newSubBean, this.item.getLastXmlObjects()[0]);
    }

    if (expectedOne) {
      assertEquals(1, this.listener1.getNumValueChangeds());
      if (hasDataAlready) assertSame(oldData, this.listener1.getLastOldValue());
      else assertSame(oldObject, this.listener1.getLastOldValue());
      assertSame(newObject, this.listener1.getLastNewValue());
    } else {
      assertEquals(0, this.listener1.getNumValueChangeds());
    }

    if (expectedTwo) {
      assertEquals(1, this.listener2.getNumValueChangeds());
      if (hasDataAlready) assertSame(oldData, this.listener2.getLastOldValue());
      else assertSame(oldObject, this.listener2.getLastOldValue());
      assertSame(newObject, this.listener2.getLastNewValue());
    } else {
      assertEquals(0, this.listener2.getNumValueChangeds());
    }

    return newObject;
  }

  public void testToString() throws Exception {
    this.item.toString();
  }

}
