/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.context;

import com.tc.config.schema.MockIllegalConfigurationChangeHandler;
import com.tc.config.schema.MockSchemaType;
import com.tc.config.schema.MockXmlObject;
import com.tc.config.schema.defaults.MockDefaultValueProvider;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.MockConfigItem;
import com.tc.config.schema.dynamic.MockListeningConfigItem;
import com.tc.config.schema.dynamic.StringArrayConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;
import com.tc.config.schema.dynamic.XPathBasedConfigItem;
import com.tc.config.schema.repository.MockBeanRepository;
import com.tc.test.TCTestCase;

/**
 * Unit test for {@link StandardConfigContext}.
 */
public class StandardConfigContextTest extends TCTestCase {

  private MockSchemaType                        schemaType;
  private MockBeanRepository                    beanRepository;
  private MockDefaultValueProvider              defaultValueProvider;
  private MockIllegalConfigurationChangeHandler illegalConfigurationChangeHandler;

  private ConfigContext                         context;

  public void setUp() throws Exception {
    this.schemaType = new MockSchemaType();
    this.beanRepository = new MockBeanRepository();
    this.beanRepository.setReturnedRootBeanSchemaType(this.schemaType);
    this.defaultValueProvider = new MockDefaultValueProvider();
    this.illegalConfigurationChangeHandler = new MockIllegalConfigurationChangeHandler();

    this.context = new StandardConfigContext(this.beanRepository, this.defaultValueProvider,
                                             this.illegalConfigurationChangeHandler, null);
  }

  public void testEnsureRepositoryProvides() throws Exception {
    this.beanRepository.setExceptionOnEnsureBeanIsOfClass(null);

    this.context.ensureRepositoryProvides(Number.class);
    assertEquals(1, this.beanRepository.getNumEnsureBeanIsOfClasses());
    assertEquals(Number.class, this.beanRepository.getLastClass());
    this.beanRepository.reset();

    RuntimeException exception = new RuntimeException("foo");
    this.beanRepository.setExceptionOnEnsureBeanIsOfClass(exception);

    try {
      this.context.ensureRepositoryProvides(Object.class);
      fail("Didn't get expected exception");
    } catch (RuntimeException re) {
      assertSame(exception, re);
      assertEquals(1, this.beanRepository.getNumEnsureBeanIsOfClasses());
      assertEquals(Object.class, this.beanRepository.getLastClass());
    }
  }

  public void testConstruction() throws Exception {
    try {
      new StandardConfigContext(null, this.defaultValueProvider, this.illegalConfigurationChangeHandler, null);
      fail("Didn't get NPE on no bean repository");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardConfigContext(this.beanRepository, null, this.illegalConfigurationChangeHandler, null);
      fail("Didn't get NPE on no default value provider");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardConfigContext(this.beanRepository, this.defaultValueProvider, null, null);
      fail("Didn't get NPE on no illegal configuration change handler");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void testHasDefaultFor() throws Exception {
    this.defaultValueProvider.setReturnedPossibleForXPathToHaveDefault(false);
    this.defaultValueProvider.setReturnedHasDefault(false);

    assertFalse(this.context.hasDefaultFor("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumPossibleForXPathToHaveDefaults());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastPossibleForXPathToHaveDefaultsXPath());
    assertEquals(0, this.defaultValueProvider.getNumHasDefaults());

    this.defaultValueProvider.reset();
    this.defaultValueProvider.setReturnedPossibleForXPathToHaveDefault(true);
    this.defaultValueProvider.setReturnedHasDefault(false);

    assertFalse(this.context.hasDefaultFor("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumPossibleForXPathToHaveDefaults());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastPossibleForXPathToHaveDefaultsXPath());
    assertEquals(1, this.defaultValueProvider.getNumHasDefaults());
    assertSame(this.schemaType, this.defaultValueProvider.getLastHasDefaultsSchemaType());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastHasDefaultsXPath());

    this.defaultValueProvider.reset();
    this.defaultValueProvider.setReturnedPossibleForXPathToHaveDefault(false);
    this.defaultValueProvider.setReturnedHasDefault(true);

    assertFalse(this.context.hasDefaultFor("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumPossibleForXPathToHaveDefaults());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastPossibleForXPathToHaveDefaultsXPath());
    assertEquals(0, this.defaultValueProvider.getNumHasDefaults());

    this.defaultValueProvider.reset();
    this.defaultValueProvider.setReturnedPossibleForXPathToHaveDefault(true);
    this.defaultValueProvider.setReturnedHasDefault(true);

    assertTrue(this.context.hasDefaultFor("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumPossibleForXPathToHaveDefaults());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastPossibleForXPathToHaveDefaultsXPath());
    assertEquals(1, this.defaultValueProvider.getNumHasDefaults());
    assertSame(this.schemaType, this.defaultValueProvider.getLastHasDefaultsSchemaType());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastHasDefaultsXPath());
  }

  public void testDefaultFor() throws Exception {
    MockXmlObject object = new MockXmlObject();
    this.defaultValueProvider.setReturnedDefaultFor(object);

    assertSame(object, this.context.defaultFor("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumDefaultFors());
    assertSame(this.schemaType, this.defaultValueProvider.getLastBaseType());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastXPath());
  }

  public void testIsOptional() throws Exception {
    this.defaultValueProvider.setReturnedIsOptional(false);

    assertFalse(this.context.isOptional("foobar/baz"));
    assertEquals(1, this.defaultValueProvider.getNumIsOptionals());
    assertSame(this.schemaType, this.defaultValueProvider.getLastBaseType());
    assertEquals("foobar/baz", this.defaultValueProvider.getLastXPath());
  }

  public void testBean() throws Exception {
    MockXmlObject object = new MockXmlObject();
    this.beanRepository.setReturnedBean(object);

    assertSame(object, this.context.bean());
    assertEquals(1, this.beanRepository.getNumBeans());
  }

  public void testItemCreated() throws Exception {
    MockConfigItem item = new MockConfigItem();

    this.context.itemCreated(item);
    assertEquals(0, this.beanRepository.getNumAddListeners());

    MockListeningConfigItem listeningItem = new MockListeningConfigItem();

    this.context.itemCreated(listeningItem);
    assertEquals(1, this.beanRepository.getNumAddListeners());
    assertSame(listeningItem, this.beanRepository.getLastListener());
  }

  public void testItems() throws Exception {
    checkItem(this.context.intItem("foobar/baz"), "foobar/baz", IntConfigItem.class, null);
    checkItem(this.context.stringItem("foobar/baz"), "foobar/baz", StringConfigItem.class, null);
    checkItem(this.context.stringArrayItem("foobar/baz"), "foobar/baz", StringArrayConfigItem.class, null);
    checkItem(this.context.fileItem("foobar/baz"), "foobar/baz", FileConfigItem.class, null);
    checkItem(this.context.booleanItem("foobar/baz"), "foobar/baz", BooleanConfigItem.class, null);
    checkItem(this.context.booleanItem("foobar/baz", true), "foobar/baz", BooleanConfigItem.class, Boolean.TRUE);
    checkItem(this.context.booleanItem("foobar/baz", false), "foobar/baz", BooleanConfigItem.class, Boolean.FALSE);
  }

  private void checkItem(ConfigItem item, String xpath, Class expectedClass, Object expectedDefaultValue) {
    assertTrue(expectedClass.isInstance(item));
    assertEquals(xpath, ((XPathBasedConfigItem) item).xpath());
    assertSame(this.context, ((XPathBasedConfigItem) item).context());
    assertEquals(expectedDefaultValue, ((XPathBasedConfigItem) item).defaultValue());
  }

}
