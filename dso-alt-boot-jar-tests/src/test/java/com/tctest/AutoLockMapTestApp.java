/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

public class AutoLockMapTestApp extends GenericTransparentApp {

  public AutoLockMapTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, Map.class);
  }

  protected Object getTestObject(String test) {
    List maps = (List) sharedMap.get("maps");

    return maps.iterator();
  }

  protected void setupTestObject(String test) {
    List maps = new ArrayList();
    maps.add(new HashMap());
    maps.add(new Hashtable());
    maps.add(new Properties());

    sharedMap.put("maps", maps);
    sharedMap.put("arrayforHashtable", new Object[4]);
    sharedMap.put("arrayforProperties", new Object[4]);
  }

  private void initialize(Map map) {
    map.putAll(getInitialData());
  }

  private Hashtable getInitialData() {
    Hashtable table = new Hashtable();
    table.put("January", "Jan");
    table.put("February", "Feb");
    table.put("March", "Mar");
    table.put("April", "Apr");
    return table;
  }

  void testDisableAutoLocks(Map map, boolean validate) throws Exception {
    if (!(map instanceof Hashtable) || (map instanceof HashMap)) { return; }

    if (validate) { return; }

    Hashtable ht = (Hashtable) map;
    ((Manageable) ht).__tc_managed().disableAutoLocking();
    try {
      ht.put("saravan", "smells");
      throw new AssertionError("put() did not fail");
    } catch (UnlockedSharedObjectException use) {
      // expected
    }
  }

  void testPut(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      assertMappings(getInitialData(), map);
    } else {
      initialize(map);
    }
  }

  void testEntrySetRemove(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("February");
      assertMappings(expect, map);
    } else {
      initialize(map);

      Set entrySet = map.entrySet();
      entrySet.remove(new SimpleEntry("February", "Feb"));
    }
  }

  void testEntrySetClear(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      initialize(map);
      Set entrySet = map.entrySet();
      entrySet.clear();
    }
  }

  void testEntrySetRetainAll(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("January");
      expect.remove("April");
      assertMappings(expect, map);
    } else {
      initialize(map);
      Set entrySet = map.entrySet();

      Collection toRetain = new ArrayList();
      toRetain.add(new SimpleEntry("February", "Feb"));
      toRetain.add(new SimpleEntry("March", "Mar"));

      entrySet.retainAll(toRetain);
    }
  }

  void testEntrySetRemoveAll(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("February");
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      initialize(map);
      Set entrySet = map.entrySet();

      Collection toRemove = new ArrayList();
      toRemove.add(new SimpleEntry("February", "Feb"));
      toRemove.add(new SimpleEntry("March", "Mar"));

      entrySet.removeAll(toRemove);
    }
  }

  /**
   * EntryWrapper is not portable yet, so the putting the entries to a shared array by calling the entrySet().toArray()
   * method will not work and will throw a NonPortableException at this point.
   */
  /*
   * void testEntrySetToArray(Map map, boolean validate) { Object[] array = getArray(map); if (validate) {
   * assertArray(array, map); } else { initialize(map); synchronized (array) { Object[] returnArray =
   * map.entrySet().toArray(array); Assert.assertTrue(returnArray == array); } } }
   */

  void testEntrySetIterator(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("February");

      assertMappings(expect, map);
    } else {
      initialize(map);
      Set entrySet = map.entrySet();
      for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
        Entry entry = (Entry) iterator.next();
        if ("February".equals(entry.getKey())) {
          iterator.remove();
          break;
        }
      }
    }
  }

  void testKeySetRemove(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("February");
      assertMappings(expect, map);
    } else {
      initialize(map);
      Set keySet = map.keySet();
      keySet.remove("February");
    }
  }

  void testKeySetClear(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      initialize(map);
      Set keySet = map.keySet();
      keySet.clear();
    }
  }

  void testKeySetIterator(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("February");
      assertMappings(expect, map);
    } else {
      initialize(map);
      Set keySet = map.keySet();
      for (Iterator iterator = keySet.iterator(); iterator.hasNext();) {
        String key = (String) iterator.next();
        if ("February".equals(key)) {
          iterator.remove();
          break;
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  void testKeySetRetainAll(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("January");
      expect.remove("April");
      assertMappings(expect, map);
    } else {
      initialize(map);
      Set keySet = map.keySet();

      Collection toRetain = new ArrayList();
      toRetain.add("February");
      toRetain.add("March");

      keySet.retainAll(toRetain);
    }
  }

  void testKeySetRemoveAll(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("February");
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      initialize(map);
      Set keySet = map.keySet();

      Collection toRemove = new ArrayList();
      toRemove.add("February");
      toRemove.add("March");

      keySet.removeAll(toRemove);
    }
  }

  void testKeySetToArray(Map map, boolean validate) {
    if (map instanceof HashMap) { return; }

    Object[] array = getArray(map);

    if (validate) {
      assertArray(array, map.keySet());
    } else {
      initialize(map);

      synchronized (array) {
        Object[] returnArray = map.keySet().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }

  void testValuesRemove(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("February");
      assertMappings(expect, map);
    } else {
      initialize(map);
      Collection values = map.values();
      values.remove("Feb");
    }
  }

  void testValuesClear(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      initialize(map);
      Collection values = map.values();
      values.clear();
    }
  }

  void testValuesIterator(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("February");
      assertMappings(expect, map);
    } else {
      initialize(map);

      Collection values = map.values();
      for (Iterator iterator = values.iterator(); iterator.hasNext();) {
        String value = (String) iterator.next();
        if ("Feb".equals(value)) {
          iterator.remove();
          break;
        }
      }
    }
  }

  void testValuesRetainAll(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("January");
      expect.remove("April");
      assertMappings(expect, map);
    } else {
      initialize(map);
      Collection values = map.values();

      Collection toRetain = new ArrayList();
      toRetain.add("Feb");
      toRetain.add("Mar");

      values.retainAll(toRetain);
    }
  }

  void testValuesRemoveAll(Map map, boolean validate) throws Exception {
    if (map instanceof HashMap) { return; }

    if (validate) {
      Hashtable expect = getInitialData();
      expect.remove("February");
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      initialize(map);
      Collection values = map.values();

      Collection toRemove = new ArrayList();
      toRemove.add("Feb");
      toRemove.add("Mar");

      values.removeAll(toRemove);
    }
  }

  void testValuesToArray(Map map, boolean validate) {
    if (map instanceof HashMap) { return; }

    Object[] array = getArray(map);

    if (validate) {
      assertArray(array, map.values());
    } else {
      initialize(map);

      synchronized (array) {
        Object[] returnArray = map.values().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }

  void testBasicSetProperty(Map map, boolean validate) {
    if (map instanceof HashMap) { return; }
    if (!(map instanceof Properties)) { return; }

    if (validate) {
      assertMappings(getInitialData(), map);
    } else {
      ((Properties) map).setProperty("January", "Jan");
      ((Properties) map).setProperty("February", "Feb");
      ((Properties) map).setProperty("March", "Mar");
      ((Properties) map).setProperty("April", "Apr");
    }
  }

  void testBasicGetProperty(Map map, boolean validate) {
    if (map instanceof HashMap) { return; }
    if (!(map instanceof Properties)) { return; }

    if (validate) {
      Assert.assertEquals("value", ((Properties) map).getProperty("key"));
      Assert.assertEquals("defaultValue", ((Properties) map).getProperty("nonsense", "defaultValue"));
      Assert.assertEquals("value", ((Properties) map).getProperty("key", "defaultValue"));
    } else {
      ((Properties) map).setProperty("key", "value");
    }
  }

  void testBasicLoad(Map map, boolean validate) {
    if (map instanceof HashMap) { return; }
    if (!(map instanceof Properties)) { return; }

    if (validate) {
      Map expectedMap = new Properties();
      expectedMap.put("key1", "val1");
      expectedMap.put("key2", "val2");
      expectedMap.put("key3", "val3");
      assertMappings(expectedMap, map);
    } else {
      Properties data = new Properties();
      data.setProperty("key1", "val1");
      data.setProperty("key2", "val2");
      data.setProperty("key3", "val3");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try {
        data.store(outputStream, null);
      } catch (IOException ioe) {
        Assert.fail();
      }
      ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
      try {
        ((Properties) map).load(inputStream);
      } catch (IOException ioe) {
        Assert.fail();
      }
    }
  }

  void testHashMapPut(Map map, boolean validate) {
    if (!(map instanceof HashMap)) return;
    
    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      try {
        map.put("key1", "value1");
        throw new AssertionError("Should have thrown an UnlockedSharedObjectException.");
      } catch (UnlockedSharedObjectException e) {
        // ignore
      }
    }
  }

  void assertArray(Object[] expect, Collection collection) {
    Assert.assertEquals(expect.length, collection.size());
    for (int i = 0; i < expect.length; i++) {
      String val = (String) expect[i];
      Assert.assertTrue(collection.contains(val));
    }
  }

  void assertArray(Object[] expect, Map map) {
    Assert.assertEquals(expect.length, map.size());
    for (int i = 0; i < expect.length; i++) {
      Entry entry = (Entry) expect[i];
      Object val = map.get(entry.getKey());
      Assert.assertEquals(entry.getValue(), val);
    }
  }

  void assertMappings(Map expect, Map actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Set expectEntries = expect.entrySet();
    Set actualEntries = actual.entrySet();

    for (Iterator i = expectEntries.iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Assert.assertEquals(entry.getValue(), actual.get(entry.getKey()));
    }

    for (Iterator i = actualEntries.iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Assert.assertEquals(entry.getValue(), expect.get(entry.getKey()));
    }
  }

  private Object[] getArray(Map map) {
    if (map instanceof Properties) { return (Object[]) sharedMap.get("arrayforProperties"); }
    if (map instanceof Hashtable) { return (Object[]) sharedMap.get("arrayforHashtable"); }

    return null;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = AutoLockMapTestApp.class.getName();
    config.getOrCreateSpec(testClass);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
  }

  private static class SimpleEntry implements Map.Entry {

    private final Object key;
    private Object       value;

    public SimpleEntry(Object key, Object value) {
      this.key = key;
      this.value = value;
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public Object setValue(Object value) {
      Object oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) return false;
      Map.Entry e = (Map.Entry) o;
      return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    public int hashCode() {
      return ((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode());
    }

    public String toString() {
      return key + "=" + value;
    }

    private static boolean eq(Object o1, Object o2) {
      return (o1 == null ? o2 == null : o1.equals(o2));
    }
  }
}
