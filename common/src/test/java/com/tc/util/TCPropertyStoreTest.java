/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.properties.TCPropertyStore;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

public class TCPropertyStoreTest extends TestCase {

  // This file resides in src.resource/com/tc/properties directory
  private static final String DEFAULT_TC_PROPERTIES_FILE = "tc.properties";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  private void loadDefaults(String propFile, TCPropertyStore propertyStore) {
    InputStream in = TCPropertiesImpl.class.getResourceAsStream(propFile);
    if (in == null) { throw new AssertionError("TC Property file " + propFile + " not Found"); }
    try {
      propertyStore.load(in);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public void testLoad() {
    TCPropertyStore propertyStore = new TCPropertyStore();
    loadDefaults(DEFAULT_TC_PROPERTIES_FILE, propertyStore);
    Assert.assertTrue(propertyStore.containsKey(TCPropertiesConsts.L2_CACHEMANAGER_ENABLED));
    Assert.assertTrue(propertyStore.containsKey(TCPropertiesConsts.L2_CACHEMANAGER_ENABLED.toUpperCase()));
    Assert.assertTrue(propertyStore.containsKey(TCPropertiesConsts.L2_CACHEMANAGER_ENABLED.replace("e", "E")));
  }

  public void testTrim() {
    TCPropertyStore props = new TCPropertyStore();
    props.setProperty("tim1", "eck");
    props.setProperty("tim2", " eck");
    props.setProperty("tim3", "eck ");
    props.setProperty("tim4", " eck ");

    for (String key : props.keysArray()) {
      assertEquals("eck", props.getProperty(key));
    }
  }

  public void testOrder() {
    TCPropertyStore props = new TCPropertyStore();

    for (int i = 0; i < 1000; i++) {
      props.setProperty(String.valueOf(i), "");
    }

    int count = 0;
    for (String key : props.keysArray()) {
      assertEquals(String.valueOf(count++), key);
    }
  }

  public void testKeyCasePreserve() {
    TCPropertyStore props = new TCPropertyStore();

    props.setProperty("Tim", "eck");
    assertEquals("eck", props.getProperty("tim"));
    assertEquals(1, props.keysArray().length);
    assertEquals("Tim", props.keysArray()[0]);

    props.setProperty("TIM", "eck");
    assertEquals("eck", props.getProperty("tim"));
    assertEquals(1, props.keysArray().length);
    assertEquals("TIM", props.keysArray()[0]);

    props.setProperty("TiM", "eck");
    assertEquals("eck", props.getProperty("tim"));
    assertEquals(1, props.keysArray().length);
    assertEquals("TiM", props.keysArray()[0]);
  }

  public void testSetAndGet() {
    TCPropertyStore propertyStore = new TCPropertyStore();
    propertyStore.setProperty("xyz", "abc");
    Assert.assertEquals("abc", propertyStore.getProperty("XyZ"));
    Assert.assertEquals("abc", propertyStore.getProperty("xYZ"));
    propertyStore.setProperty("abc", "def");
    Assert.assertEquals("def", propertyStore.getProperty("AbC"));
  }

  public void testPutAll() {
    TCPropertyStore propertyStore1 = new TCPropertyStore();
    TCPropertyStore propertyStore2 = new TCPropertyStore();

    propertyStore1.setProperty("abc", "def");
    propertyStore1.setProperty("xyz", "XYZ");

    propertyStore2.putAll(propertyStore1);

    Assert.assertEquals(2, propertyStore2.size());
    Assert.assertEquals("def", propertyStore2.getProperty("AbC"));
    Assert.assertEquals("XYZ", propertyStore2.getProperty("XyZ"));
  }
}
