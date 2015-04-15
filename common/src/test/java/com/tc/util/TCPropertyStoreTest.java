/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
    Assert.assertTrue(propertyStore.containsKey(TCPropertiesConsts.L2_L1RECONNECT_ENABLED));
    Assert.assertTrue(propertyStore.containsKey(TCPropertiesConsts.L2_L1RECONNECT_ENABLED.toUpperCase()));
    Assert.assertTrue(propertyStore.containsKey(TCPropertiesConsts.L2_L1RECONNECT_ENABLED.replace("e", "E")));
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
