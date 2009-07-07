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
    Assert.assertEquals(propertyStore.size(), propertyStore.keySize());
    Assert.assertTrue(propertyStore.containsKey(TCPropertiesConsts.L2_CACHEMANAGER_ENABLED));
    Assert.assertTrue(propertyStore.containsKey(TCPropertiesConsts.L2_CACHEMANAGER_ENABLED.toUpperCase()));
    Assert.assertTrue(propertyStore.containsKey(TCPropertiesConsts.L2_CACHEMANAGER_ENABLED.replace("e", "E")));
  }

  public void testSetAndGet() {
    TCPropertyStore propertyStore = new TCPropertyStore();
    propertyStore.setProperty("xyz", "abc");
    Assert.assertEquals("abc", propertyStore.getProperty("XyZ"));
    Assert.assertEquals("abc", propertyStore.get("xYZ"));
    propertyStore.put("abc", "def");
    Assert.assertEquals("def", propertyStore.get("AbC"));
  }

  public void testPutAll() {
    TCPropertyStore propertyStore1 = new TCPropertyStore();
    TCPropertyStore propertyStore2 = new TCPropertyStore();

    propertyStore1.setProperty("abc", "def");
    propertyStore1.put("xyz", "XYZ");

    propertyStore2.putAll(propertyStore1);

    Assert.assertEquals(2, propertyStore2.size());
    Assert.assertEquals("def", propertyStore2.getProperty("AbC"));
    Assert.assertEquals("XYZ", propertyStore2.get("XyZ"));
  }
}
