/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.collections.FastHashMap;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tcclient.ehcache.TimeExpiryMap;

import gnu.trove.THashMap;
import gnu.trove.TObjectFunction;
import gnu.trove.TObjectHash;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

public class GenericMapTestApp extends GenericTestApp {

  private final Map nonSharedArrayMap = new HashMap();

  public GenericMapTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, Map.class);
  }

  protected Object getTestObject(String test) {
    List maps = (List) sharedMap.get("maps");

    // This is just to make sure all the expected maps are here.
    // As new map classes get added to this test, you'll have to adjust this number obviously
    Assert.assertEquals(25, maps.size());

    return maps.iterator();
  }

  protected void setupTestObject(String test) {
    List maps = new ArrayList();

    maps.add(new HashMap());
    maps.add(new Hashtable());
    maps.add(new TreeMap(new NullTolerantComparator()));
    maps.add(new LinkedHashMap());
    maps.add(new THashMap());
    maps.add(new FastHashMap());
    FastHashMap fm = new FastHashMap();
    fm.setFast(true);
    maps.add(fm);
    maps.add(new Properties());
    maps.add(new MyHashMap(11));
    maps.add(new MyHashMap(new HashMap()));
    maps.add(new MyHashMap2());
    maps.add(new MyHashMap3(0));
    maps.add(new MyTreeMap(new NullTolerantComparator()));
    maps.add(new MyTreeMap2(new NullTolerantComparator()));
    maps.add(new MyHashtable());
    maps.add(new MyHashtable2());
    maps.add(new MyLinkedHashMap());
    maps.add(new MyLinkedHashMap2());
    maps.add(new MyLinkedHashMap3(true));
    maps.add(new MyTHashMap());
    maps.add(new MyFastHashMap());
    maps.add(new MyProperties());
    maps.add(new MyProperties2());
    maps.add(new MyProperties3());
    maps.add(new TimeExpiryMap(1, 100, 200, "testMap")); // no invalidator is running

    // maps.add(new IdentityHashMap());
    // maps.add(new WeakHashMap());

    /*
     * sharedMap.put("maps", maps); sharedMap.put("arrayforHashMap", new Object[4]); sharedMap.put("arrayforHashtable",
     * new Object[4]); sharedMap.put("arrayforTreeMap", new Object[4]); sharedMap.put("arrayforTreeMap2", new
     * Object[4]); sharedMap.put("arrayforTHashMap", new Object[4]); sharedMap.put("arrayforLinkedHashMap", new
     * Object[4]); sharedMap.put("arrayforFastHashMap", new Object[4]); sharedMap.put("arrayforProperties", new
     * Object[4]); sharedMap.put("arrayforFastHashMapWithFast", new Object[4]); sharedMap.put("arrayforMyHashMap", new
     * Object[4]); sharedMap.put("arrayforMyHashMap2", new Object[4]); sharedMap.put("arrayforMyHashMap3", new
     * Object[4]); sharedMap.put("arrayforMyTreeMap", new Object[4]); sharedMap.put("arrayforMyHashtable", new
     * Object[4]); sharedMap.put("arrayforMyHashtable2", new Object[4]); sharedMap.put("arrayforMyLinkedHashMap", new
     * Object[4]); sharedMap.put("arrayforMyLinkedHashMap2", new Object[4]); sharedMap.put("arrayforMyTHashMap", new
     * Object[4]); sharedMap.put("arrayforMyFastHashMap", new Object[4]); sharedMap.put("arrayforMyProperties", new
     * Object[4]); sharedMap.put("arrayforMyProperties2", new Object[4]);
     */

    sharedMap.put("maps", maps);
    nonSharedArrayMap.put("arrayforHashMap", new Object[4]);
    nonSharedArrayMap.put("arrayforHashtable", new Object[4]);
    sharedMap.put("arrayforTreeMap", new Object[4]);
    sharedMap.put("arrayforTreeMap2", new Object[4]);
    sharedMap.put("arrayforTHashMap", new Object[4]);
    nonSharedArrayMap.put("arrayforLinkedHashMap", new Object[4]);
    nonSharedArrayMap.put("arrayforFastHashMap", new Object[4]);
    nonSharedArrayMap.put("arrayforProperties", new Object[4]);
    nonSharedArrayMap.put("arrayforFastHashMapWithFast", new Object[4]);
    nonSharedArrayMap.put("arrayforMyHashMap", new Object[4]);
    nonSharedArrayMap.put("arrayforMyHashMap2", new Object[4]);
    sharedMap.put("arrayforMyTreeMap", new Object[4]);
    nonSharedArrayMap.put("arrayforMyHashtable", new Object[4]);
    nonSharedArrayMap.put("arrayforMyHashtable2", new Object[4]);
    nonSharedArrayMap.put("arrayforMyLinkedHashMap", new Object[4]);
    nonSharedArrayMap.put("arrayforMyLinkedHashMap2", new Object[4]);
    nonSharedArrayMap.put("arrayforMyLinkedHashMap3", new Object[4]);
    sharedMap.put("arrayforMyTHashMap", new Object[4]);
    nonSharedArrayMap.put("arrayforMyFastHashMap", new Object[4]);
    nonSharedArrayMap.put("arrayforMyProperties", new Object[4]);
    nonSharedArrayMap.put("arrayforMyProperties2", new Object[4]);
    nonSharedArrayMap.put("arrayforMyProperties3", new Object[4]);
    nonSharedArrayMap.put("arrayforTimeExpiryMap", new Object[4]);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = GenericMapTestApp.class.getName();
    config.addNewModule("clustered-commons-collections-3.1", "1.0.0");
    config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);
    config.addIncludePattern(testClass + "$*");
    config.addIncludePattern(Key.class.getName());
    config.addIncludePattern(NullTolerantComparator.class.getName());
    config.addIncludePattern(SimpleEntry.class.getName());
    config.addExcludePattern(MyNonPortableObject.class.getName());
    
    config.addNewModule("clustered-ehcache-1.2.4", "1.0.0"); // this is just a quick way to add TimeExpiryMap to the instrumentation list
  }

  void testBasicUnSynchronizedPut(Map map, boolean validate) {
    if (map instanceof Hashtable) { return; }
    if (map instanceof FastHashMap) { return; }
    if (map instanceof TimeExpiryMap) { return; }

    if (validate) {
      assertEmptyMap(map);
    } else {
      try {
        map.put("January", "Jan");
        throw new AssertionError("Should have thrown a UnlockedSharedObjectException");
      } catch (UnlockedSharedObjectException use) {
        // this is expected.
      }
    }
  }

  void testBasicPut(Map map, boolean validate) {
    if (validate) {
      assertSingleMapping(map, "timmy", "teck");
      if (map instanceof MyHashMap) {
        Assert.assertEquals("timmy", ((MyHashMap) map).getKey());
        Assert.assertEquals("teck", ((MyHashMap) map).getValue());
      } else if (map instanceof MyTreeMap2) {
        Assert.assertEquals("timmy", ((MyTreeMap2) map).getKey());
        Assert.assertEquals("teck", ((MyTreeMap2) map).getValue());
      } else if (map instanceof MyLinkedHashMap2) {
        Assert.assertEquals("timmy", ((MyLinkedHashMap2) map).getKey());
        Assert.assertEquals("teck", ((MyLinkedHashMap2) map).getValue());
      } else if (map instanceof MyProperties) {
        Assert.assertEquals("timmy", ((MyProperties) map).getKey());
        Assert.assertEquals("teck", ((MyProperties) map).getValue());
      }
    } else {
      synchronized (map) {
        Object prev = map.put("timmy", "teck");
        Assert.assertNull(prev);
      }
    }
  }

  void testBasicPutAll(Map map, boolean validate) {
    Map toAdd = getOrderSensitiveMappings();

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
    }
  }

  void testPutNullKey(Map map, boolean validate) {
    if (!allowsNull(map)) { return; }
    if (validate) {
      assertSingleMapping(map, null, "value");
    } else {
      synchronized (map) {
        Object prev = map.put(null, "value");
        Assert.assertNull(prev);
      }
    }
  }

  void testReplaceNullKey(Map map, boolean validate) {
    if (!allowsNull(map)) { return; }
    if (validate) {
      assertSingleMapping(map, null, "value2");
    } else {
      synchronized (map) {
        Object prev = map.put(null, "value");
        Assert.assertNull(prev);
      }

      synchronized (map) {
        Object prev = map.put(null, "value2");
        Assert.assertEquals("value", prev);
      }
    }
  }

  void testBasicReplace(Map map, boolean validate) {
    if (validate) {
      assertSingleMapping(map, "key", "value2");
    } else {
      synchronized (map) {
        Object prev = map.put("key", "value");
        Assert.assertNull(prev);
      }

      synchronized (map) {
        Object prev = map.put("key", "value2");
        Assert.assertEquals("value", prev);
      }
    }
  }

  void testPutNullValue(Map map, boolean validate) {
    if (!allowsNull(map)) { return; }
    if (validate) {
      assertSingleMapping(map, "key", null);
    } else {
      synchronized (map) {
        Object prev = map.put("key", null);
        Assert.assertNull(prev);
      }
    }
  }

  void testReplaceKeyRetention(Map map, boolean validate) {
    if (validate) {
      assertSingleMapping(map, new Key("** doesn't matter **", "key"), "value2");

      Assert.assertEquals(1, map.keySet().size());
      Key retainedKey = (Key) map.keySet().iterator().next();

      String expect = (map instanceof THashMap) ? "id2" : "id1";
      Assert.assertEquals(expect, retainedKey.id);
    } else {
      synchronized (map) {
        Object prev = map.put(new Key("id1", "key"), "value");
        Assert.assertNull(prev);
      }
      synchronized (map) {
        Object prev = map.put(new Key("id2", "key"), "value2");
        Assert.assertEquals("value", prev);
      }
    }
  }

  void testBasicSetProperty(Map map, boolean validate) {
    if (!(map instanceof Properties)) { return; }

    if (validate) {
      assertSingleMapping(map, "timmy", "teck");
    } else {
      synchronized (map) {
        Object previous = ((Properties) map).setProperty("timmy", "teck");
        Assert.assertNull(previous);
      }
    }
  }

  void testBasicGetProperty(Map map, boolean validate) {
    if (!(map instanceof Properties)) { return; }

    if (validate) {
      Assert.assertEquals("value", ((Properties) map).getProperty("key"));
      Assert.assertEquals("defaultValue", ((Properties) map).getProperty("nonsense", "defaultValue"));
      Assert.assertEquals("value", ((Properties) map).getProperty("key", "defaultValue"));
    } else {
      synchronized (map) {
        ((Properties) map).setProperty("key", "value");
      }
    }
  }

  void testBasicLoad(Map map, boolean validate) {
    if (!(map instanceof Properties)) { return; }

    if (validate) {
      Map expectedMap = new Properties();
      expectedMap.put("key1", "val1");
      expectedMap.put("key2", "val2");
      expectedMap.put("key3", "val3");
      assertMappings(expectedMap, map);
    } else {
      synchronized (map) {
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
  }

  /*
   * Java 1.5 specific API used void testBasicLoadFromXML(Map map, boolean validate) { if (!(map instanceof Properties)) {
   * return; } if(validate) { Map expectedMap = new Properties(); expectedMap.put("key1", "val1");
   * expectedMap.put("key2", "val2"); expectedMap.put("key3", "val3"); assertMappings(expectedMap, map); } else {
   * synchronized (map) { Properties data = new Properties(); data.setProperty("key1", "val1"); data.setProperty("key2",
   * "val2"); data.setProperty("key3", "val3"); ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); try {
   * data.storeToXML(outputStream, null); } catch (IOException ioe) { Assert.fail(); } ByteArrayInputStream inputStream =
   * new ByteArrayInputStream(outputStream.toByteArray()); try { ((Properties)map).loadFromXML(inputStream); } catch
   * (IOException ioe) { Assert.fail(); } } } }
   */

  void testKeySetClear(Map map, boolean validate) {
    if (validate) {
      assertEmptyMap(map);
    } else {
      addMappings(map, 42);
      synchronized (map) {
        map.keySet().clear();
      }
    }
  }

  void testKeySetIteratorRemove(Map map, boolean validate) {
    if (validate) {
      Map expect = getOrderSensitiveMappings();
      expect.remove("February");
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      synchronized (map) {
        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
          Object key = i.next();
          if ("February".equals(key) || "March".equals(key)) {
            i.remove();
          }
        }
      }
    }
  }

  void testKeySetIteratorRemoveNull(Map map, boolean validate) {
    if (!allowsNull(map)) { return; }

    if (validate) {
      assertSingleMapping(map, "key1", "value1");
    } else {
      synchronized (map) {
        map.put("key1", "value1");
        map.put(null, "value for null key");
      }

      synchronized (map) {
        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
          Object key = i.next();
          if (!"key1".equals(key)) {
            i.remove();
          }
        }
      }
    }
  }

  void testKeySetRemove(Map map, boolean validate) {
    if (validate) {
      Map expect = getOrderSensitiveMappings();
      expect.remove("February");
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      synchronized (map) {
        Set keys = map.keySet();
        boolean removed;
        removed = keys.remove("February");
        Assert.assertTrue(removed);
        removed = keys.remove("March");
        Assert.assertTrue(removed);
        removed = keys.remove("key4");
        Assert.assertFalse(removed);
      }
    }
  }

  void testBasicGet(Map map, boolean validate) {
    if (validate) {
      try {
        if (isAccessOrderedLinkedHashMap(map)) {
          synchronized (map) {
            Assert.assertEquals("value", map.get("key"));
          }
        } else {
          Assert.assertEquals("value", map.get("key"));
        }
        if (map instanceof MyHashMap) {
          Assert.assertEquals("value", ((MyHashMap) map).getObject("key"));
        }
      } catch (Throwable t) {
        System.err.println("*******" + Thread.currentThread().getName() + ", map: " + map + "********");
        throw new RuntimeException(t);
      }
    } else {
      synchronized (map) {
        map.put("key", "value");
      }
    }
  }

  void testBasicRemove(Map map, boolean validate) {
    if (validate) {
      assertEmptyMap(map);
    } else {
      synchronized (map) {
        Object prev = map.put("key", "value");
        Assert.assertNull(prev);
      }
      synchronized (map) {
        Object prev = map.remove("key");
        Assert.assertEquals("value", prev);
      }
      synchronized (map) {
        Object prev = map.remove("key");
        Assert.assertNull(prev);
      }
    }
  }

  void testRemoveNullKey(Map map, boolean validate) {
    if (!allowsNull(map)) { return; }
    if (validate) {
      assertEmptyMap(map);
    } else {
      synchronized (map) {
        map.put(null, "value");
      }
      synchronized (map) {
        Object removed = map.remove(null);
        Assert.assertEquals("value", removed);
      }
    }
  }

  void testClearNonEmpty(Map map, boolean validate) {
    if (validate) {
      assertEmptyMap(map);
    } else {
      addMappings(map, 1);

      Assert.assertFalse(map.isEmpty());

      synchronized (map) {
        map.clear();
      }
    }
  }

  void testClearEmpty(Map map, boolean validate) {
    if (validate) {
      assertEmptyMap(map);
    } else {
      Assert.assertTrue(map.isEmpty());
      synchronized (map) {
        map.clear();
      }
    }
  }

  void testValuesClear(Map map, boolean validate) {
    if (validate) {
      assertEmptyMap(map);
    } else {
      addMappings(map, 23);

      synchronized (map) {
        map.values().clear();
      }
    }
  }

  void testValuesRemove(Map map, boolean validate) {
    if (validate) {
      Map expect = getOrderSensitiveMappings();
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      synchronized (map) {
        map.values().remove("Mar");
      }
    }
  }

  void testValuesDuplicateRemove(Map map, boolean validate) {
    if (validate) {
      if (map instanceof THashMap) {
        // values().remove(Object) on THashMap will remove all mappings for the given value, not just the first
        assertEmptyMap(map);
      } else {
        Object expect = sharedMap.get("expect" + map.getClass().getName());
        assertSingleMapping(map, expect, "value");
      }
    } else {
      synchronized (map) {
        map.put("key1", "value");
        map.put("key2", "value");
      }

      synchronized (map) {
        boolean removed = map.values().remove("value");
        Assert.assertTrue(removed);
        String expectedKey = map.containsKey("key1") ? "key1" : "key2";
        sharedMap.put("expect" + map.getClass().getName(), expectedKey);
      }
    }
  }

  void testValuesRemoveNull(Map map, boolean validate) {
    if (!allowsNull(map)) { return; }
    if (validate) {
      assertSingleMapping(map, "key1", "value1");
    } else {
      synchronized (map) {
        map.put("key1", "value1");
        map.put("key for null value", null);
        map.put(null, "value for null key");
      }

      synchronized (map) {
        map.values().remove(null);
        map.values().remove("value for null key");
      }
    }
  }

  void testValuesRemoveAll(Map map, boolean validate) {
    if (validate) {
      Map expect = getOrderSensitiveMappings();
      expect.remove("February");
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      Set toRemove = new HashSet();
      toRemove.add("Feb");
      toRemove.add("Mar");

      synchronized (map) {
        map.values().removeAll(toRemove);
      }
    }
  }

  void testValuesRetainAll(Map map, boolean validate) {
    if (validate) {
      assertSingleMapping(map, "March", "Mar");
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      Set toRetain = new HashSet();
      toRetain.add("Mar");

      synchronized (map) {
        map.values().retainAll(toRetain);
      }
    }
  }

  void testValuesIteratorRemove(Map map, boolean validate) {
    if (validate) {
      Map expect = getOrderSensitiveMappings();
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      synchronized (map) {
        for (Iterator i = map.values().iterator(); i.hasNext();) {
          Object value = i.next();
          if ("Mar".equals(value)) {
            i.remove();
          }
        }
      }
    }
  }

  void testValuesIteratorRemoveNull(Map map, boolean validate) {
    if (!allowsNull(map)) { return; }
    if (validate) {
      assertSingleMapping(map, "key1", "value1");
    } else {
      synchronized (map) {
        map.put("key1", "value1");
        map.put(null, "value for null key");
        map.put("key for null value", null);
      }

      synchronized (map) {
        for (Iterator i = map.values().iterator(); i.hasNext();) {
          Object value = i.next();
          if (!"value1".equals(value)) {
            i.remove();
          }
        }
      }
    }
  }

  void testValuesIterator(Map map, boolean validate) {
    if (validate) {
      int count = 0;
      for (Iterator i = map.values().iterator(); i.hasNext();) {
        count++;
        Assert.assertEquals("teck", i.next());
      }
      Assert.assertEquals(1, count);
    } else {
      synchronized (map) {
        map.put("timmy", "teck");
      }
    }
  }

  void testKeySetIterator(Map map, boolean validate) {
    if (validate) {
      int count = 0;
      for (Iterator i = map.keySet().iterator(); i.hasNext();) {
        count++;
        Assert.assertEquals("timmy", i.next());
      }
      Assert.assertEquals(1, count);
    } else {
      synchronized (map) {
        map.put("timmy", "teck");
      }
    }
  }

  void testEntrySetIterator(Map map, boolean validate) {
    if (validate) {
      int count = 0;
      for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
        count++;
        Map.Entry entry = (Entry) i.next();
        Assert.assertEquals("timmy", entry.getKey());
        Assert.assertEquals("teck", entry.getValue());
      }
      Assert.assertEquals(1, count);
    } else {
      synchronized (map) {
        map.put("timmy", "teck");
      }
    }
  }

  void testEntrySetAdd(Map map, boolean validate) {
    // no test for entrySet().add() as HashMap, Hashtable, TreeMap, and THashMap throw
    // UnsupportedOperationException.
  }

  void testEntrySetValue(Map map, boolean validate) {
    if (validate) {
      assertSingleMapping(map, "key", "value2");
    } else {
      synchronized (map) {
        map.put("key", "value1");
      }

      synchronized (map) {
        Set set = map.entrySet();
        for (Iterator i = set.iterator(); i.hasNext();) {
          Entry entry = (Entry) i.next();
          Object prev = entry.setValue("value2");
          Assert.assertEquals("value1", prev);
        }
      }
    }
  }

  void testEntrySetValueNull(Map map, boolean validate) {
    if (!allowsNull(map)) return;

    if (validate) {
      assertSingleMapping(map, "key", null);
    } else {
      synchronized (map) {
        map.put("key", "value1");
      }

      synchronized (map) {
        Set set = map.entrySet();
        for (Iterator i = set.iterator(); i.hasNext();) {
          Entry entry = (Entry) i.next();
          Object prev = entry.setValue(null);
          Assert.assertEquals("value1", prev);
        }
      }
    }
  }

  void testEntrySetRemoveNull(Map map, boolean validate) {
    if (!allowsNull(map)) return;

    if (validate) {
      assertEmptyMap(map);
    } else {
      synchronized (map) {
        map.put("key", null);
        map.put(null, "value");
      }

      addMappings(map, 3);

      synchronized (map) {
        Set set = map.entrySet();
        for (Iterator i = set.iterator(); i.hasNext();) {
          i.next();
          i.remove();
        }
      }
    }
  }

  void testEntrySetRemove(Map map, boolean validate) {
    if (validate) {
      Map expect = getOrderSensitiveMappings();
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      synchronized (map) {
        map.entrySet().remove(new SimpleEntry("March", "Mar"));
      }
    }
  }

  void testEntrySetRemoveAll(Map map, boolean validate) {
    if (validate) {
      Map expect = new HashMap();
      expect.put("die, die", "die, my darling");
      expect.put("die, die, die", "no die");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.put("die", "another day");
        map.put("die, die", "die, my darling");
        map.put("die, die, die", "no die");
        map.put("on the last day of your life, don't forget to", "die");
      }

      Set removeSet = new HashSet(2);
      removeSet.add(new SimpleEntry("die", "another day"));
      removeSet.add(new SimpleEntry("on the last day of your life, don't forget to", "die"));

      synchronized (map) {
        map.entrySet().removeAll(removeSet);
      }
    }
  }

  void testKeySetRemoveAll(Map map, boolean validate) {
    if (validate) {
      Map expect = getOrderSensitiveMappings();
      expect.remove("February");
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      Set toRemove = new HashSet(2);
      toRemove.add("February");
      toRemove.add("March");

      synchronized (map) {
        map.keySet().removeAll(toRemove);
      }
    }
  }

  void testKeySetRetainAll(Map map, boolean validate) {
    if (validate) {
      Map expect = getOrderSensitiveMappings();
      expect.remove("January");
      expect.remove("April");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      Set toRetain = new HashSet(2);
      toRetain.add("March");
      toRetain.add("February");

      synchronized (map) {
        map.keySet().retainAll(toRetain);
      }
    }
  }

  void testKeySetRemoveNull(Map map, boolean validate) {
    if (!allowsNull(map)) { return; }

    if (validate) {
      assertSingleMapping(map, "key1", "value1");
    } else {
      synchronized (map) {
        map.put("key1", "value1");
        map.put(null, "value for null key");
        map.put("key for null value", null);
      }

      synchronized (map) {
        map.keySet().remove(null);
        map.keySet().remove("key for null value");
      }
    }

  }

  void testEntrySetRetainAll(Map map, boolean validate) {
    if (validate) {
      // NOTE: this test will fail for THashMap if using releases of trove before 1.1b5. See trove bug 1382196
      // http://sourceforge.net/tracker/index.php?func=detail&aid=1382196&group_id=39235&atid=424682
      assertSingleMapping(map, "key1", "value1");
    } else {
      synchronized (map) {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
      }

      Set retainSet = new HashSet();
      retainSet.add(new SimpleEntry("key1", "value1"));

      synchronized (map) {
        map.entrySet().retainAll(retainSet);
      }
    }
  }

  void testEntrySetRetainAll2(Map map, boolean validate) {
    if (validate) {
      Map expect = getOrderSensitiveMappings();
      expect.remove("January");
      expect.remove("April");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      Set retainSet = new HashSet();
      retainSet.add(new SimpleEntry("February", "Feb"));
      retainSet.add(new SimpleEntry("March", "Mar"));

      synchronized (map) {
        map.entrySet().retainAll(retainSet);
      }
    }
  }

  void testEntrySetClear(Map map, boolean validate) {
    if (validate) {
      assertEmptyMap(map);
    } else {
      addMappings(map, 3);

      synchronized (map) {
        map.entrySet().clear();
      }
    }
  }

  void testEntrySetIteratorRemove(Map map, boolean validate) {
    if (validate) {
      Map expect = getOrderSensitiveMappings();
      expect.remove("February");
      expect.remove("March");
      assertMappings(expect, map);
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }

      synchronized (map) {
        Set set = map.entrySet();
        for (Iterator i = set.iterator(); i.hasNext();) {
          Entry entry = (Entry) i.next();
          Object key = entry.getKey();
          if ("February".equals(key) || "March".equals(key)) {
            i.remove();
          }
        }
      }
    }
  }

  void testEntrySetIteratorRemoveNull(Map map, boolean validate) {
    if (!allowsNull(map)) { return; }
    if (validate) {
      assertSingleMapping(map, "key1", "value1");
    } else {
      synchronized (map) {
        map.put("key1", "value1");
        map.put(null, "value for null key");
        map.put("key for null value", null);
      }

      synchronized (map) {
        Set set = map.entrySet();
        for (Iterator i = set.iterator(); i.hasNext();) {
          Entry entry = (Entry) i.next();
          Object key = entry.getKey();
          if (!"key1".equals(key)) {
            i.remove();
          }
        }
      }
    }
  }

  void testEntrySetToArray(Map map, boolean validate) {
    Object[] array = getArray(map);

    if (validate) {
      if (canTestSharedArray(map)) {
        assertMappingsEqual(array, map);
      }
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }
      synchronized (array) {
        Object[] returnArray = map.entrySet().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
      assertMappingsEqual(array, map);
    }
  }

  void testKeySetToArray(Map map, boolean validate) {
    Object[] array = getArray(map);

    if (validate) {
      if (canTestSharedArray(map)) {
        assertMappingsKeysEqual(array, map.keySet());
      }
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }
      synchronized (array) {
        Object[] returnArray = map.keySet().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
      assertMappingsKeysEqual(array, map.keySet());
    }
  }

  void testValuesToArray(Map map, boolean validate) {
    Object[] array = getArray(map);

    if (validate) {
      if (canTestSharedArray(map)) {
        assertMappingsKeysEqual(array, map.values());
      }
    } else {
      synchronized (map) {
        map.putAll(getOrderSensitiveMappings());
      }
      synchronized (array) {
        Object[] returnArray = map.values().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
      assertMappingsKeysEqual(array, map.values());
    }
  }

  void testFastHashMapSetFast(Map map, boolean validate) {
    if (!(map instanceof FastHashMap)) { return; }

    FastHashMap fastHashMap = (FastHashMap) map;
    if (validate) {
      Assert.assertTrue(fastHashMap.getFast());
    } else {
      synchronized (fastHashMap) {
        fastHashMap.setFast(true);
      }
    }
  }

  // ReadOnly testing methods.
  void testReadOnlyPut(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    if (validate) {
      assertEmptyMap(map);
    } else {
      synchronized (map) {
        try {
          Object prev = map.put("timmy", "teck");
          Assert.assertNull(prev);
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException re) {
          // expected
        }
      }
    }
  }

  void testReadOnlyPutAll(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    if (validate) {
      assertEmptyMap(map);
    } else {
      synchronized (map) {
        try {
          Map toAdd = new HashMap();
          addMappings(toAdd, 3);
          map.putAll(toAdd);
          throw new AssertionError("Should have thrown a ReadOnlyException");
        } catch (ReadOnlyException t) {
          // expected
        }
      }
    }
  }

  // Setting up for the ReadOnly test for remove.
  void testSetUpRemove(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyRemove(map);
    }

  }

  // tryReadOnlyRemove() goes hand in hand with testSetUpRemove().
  private void tryReadOnlyRemove(Map map) {
    synchronized (map) {
      try {
        map.remove("key2");
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // expected
      }
    }
  }

  // Setting up for the ReadOnly test for clear.
  void testSetUpClear(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyClear(map);
    }

  }

  // tryReadOnlyClear() goes hand in hand with testSetUpClear().
  private void tryReadOnlyClear(Map map) {
    synchronized (map) {
      try {
        map.clear();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // expected
      }
    }
  }

  // Setting up for the ReadOnly test for entry set clear.
  void testSetUpEntrySetClear(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyEntrySetClear(map);
    }

  }

  // tryReadOnlyEntrySetClear() goes hand in hand with testSetUpEntrySetClear().
  private void tryReadOnlyEntrySetClear(Map map) {
    synchronized (map) {
      Set entrySet = map.entrySet();
      try {
        entrySet.clear();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // expected
      }
    }
  }

  // Setting up for the ReadOnly test for entry set remove.
  void testSetUpEntrySetRemove(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyEntrySetRemove(map);
    }

  }

  // tryReadOnlyEntrySetRemove() goes hand in hand with testSetUpEntrySetRemove().
  private void tryReadOnlyEntrySetRemove(Map map) {
    synchronized (map) {
      Set entrySet = map.entrySet();
      Iterator iterator = entrySet.iterator();
      Object o = iterator.next();
      try {
        entrySet.remove(o);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // expected
      }
    }
  }

  // Setting up for the ReadOnly test for entry set retainAll.
  void testSetUpEntrySetRetainAll(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyEntrySetRetainAll(map);
    }

  }

  // tryReadOnlyEntrySetRetainAll() goes hand in hand with testSetUpEntrySetRetainAll().
  private void tryReadOnlyEntrySetRetainAll(Map map) {
    synchronized (map) {
      Set entrySet = map.entrySet();
      Object o = entrySet.iterator().next();
      Set retainAll = new HashSet();
      retainAll.add(o);
      try {
        entrySet.retainAll(retainAll);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // ignore the ReadOnlyException in test
      }
    }
  }

  // Setting up for the ReadOnly test for entry set removeAll.
  void testSetUpEntrySetRemoveAll(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyEntrySetRemoveAll(map);
    }

  }

  // tryReadOnlyEntrySetRemoveAll() goes hand in hand with testSetUpEntrySetRemoveAll().
  private void tryReadOnlyEntrySetRemoveAll(Map map) {
    synchronized (map) {
      Set entrySet = map.entrySet();
      Object o = entrySet.iterator().next();
      Set removeAll = new HashSet();
      removeAll.add(o);
      try {
        entrySet.removeAll(removeAll);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for entry set iterator remove.
  void testSetUpEntrySetIteratorRemove(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyEntrySetIteratorRemove(map);
    }

  }

  // tryReadOnlyEntrySetIteratorRemove() goes hand in hand with testSetUpEntrySetIteratorRemove().
  private void tryReadOnlyEntrySetIteratorRemove(Map map) {
    synchronized (map) {
      Iterator iterator = map.entrySet().iterator();
      try {
        iterator.next();
        iterator.remove();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for entry set setValue.
  void testSetUpEntrySet(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyEntrySet(map);
    }

  }

  // tryReadOnlyEntrySet() goes hand in hand with testSetUpEntrySet().
  private void tryReadOnlyEntrySet(Map map) {
    synchronized (map) {
      Iterator iterator = map.entrySet().iterator();
      try {
        Entry entry = (Entry) iterator.next();
        entry.setValue("modified value");
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for key set clear.
  void testSetUpKeySetClear(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyKeySetClear(map);
    }

  }

  // tryReadOnlyKeySetClear() goes hand in hand with testSetUpKeySetClear().
  private void tryReadOnlyKeySetClear(Map map) {
    synchronized (map) {
      Set keySet = map.keySet();
      try {
        keySet.clear();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for key set retainAll.
  void testSetUpKeySetRetainAll(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyKeySetRetainAll(map);
    }

  }

  // tryReadOnlyKeySetRetainAll() goes hand in hand with testSetUpKeySetRetainAll().
  private void tryReadOnlyKeySetRetainAll(Map map) {
    synchronized (map) {
      Set keySet = map.keySet();
      Object o = keySet.iterator().next();
      Set retainAll = new HashSet();
      retainAll.add(o);
      try {
        keySet.retainAll(retainAll);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for key set removeAll.
  void testSetUpKeySetRemoveAll(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyKeySetRemoveAll(map);
    }

  }

  // tryReadOnlyKeySetRemoveAll() goes hand in hand with testSetUpKeySetRemoveAll().
  private void tryReadOnlyKeySetRemoveAll(Map map) {
    synchronized (map) {
      Set keySet = map.keySet();
      Object o = keySet.iterator().next();
      Set removeAll = new HashSet();
      removeAll.add(o);
      try {
        keySet.removeAll(removeAll);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for key set iterator remove.
  void testSetUpKeySetIteratorRemove(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyKeySetIteratorRemove(map);
    }
  }

  // tryReadOnlyKeySetIteratorRemove() goes hand in hand with testSetUpKeySetIteratorRemove().
  private void tryReadOnlyKeySetIteratorRemove(Map map) {
    synchronized (map) {
      Iterator iterator = map.keySet().iterator();
      try {
        iterator.next();
        iterator.remove();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for values clear.
  void testSetUpValuesClear(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyValuesClear(map);
    }

  }

  // tryReadOnlyValuesClear() goes hand in hand with testSetUpValuesClear().
  private void tryReadOnlyValuesClear(Map map) {
    synchronized (map) {
      Collection values = map.values();
      try {
        values.clear();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for values retainAll.
  void testSetUpValuesRetainAll(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyValuesRetainAll(map);
    }

  }

  // tryReadOnlyKeySetRetainAll() goes hand in hand with testSetUpValuesRetainAll().
  private void tryReadOnlyValuesRetainAll(Map map) {
    synchronized (map) {
      Collection values = map.values();
      Object o = values.iterator().next();
      Set retainAll = new HashSet();
      retainAll.add(o);
      try {
        values.retainAll(retainAll);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for values removeAll.
  void testSetUpValuesRemoveAll(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyValuesRemoveAll(map);
    }

  }

  // tryReadOnlyValuesRemoveAll() goes hand in hand with testSetUpValuesRemoveAll().
  private void tryReadOnlyValuesRemoveAll(Map map) {
    synchronized (map) {
      Collection values = map.values();
      Object o = values.iterator().next();
      Set removeAll = new HashSet();
      removeAll.add(o);
      try {
        values.removeAll(removeAll);
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // Setting up for the ReadOnly test for values iterator remove.
  void testSetUpValuesIteratorRemove(Map map, boolean validate) {
    if (! canTestReadOnly(map)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 3);

    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
      }
      tryReadOnlyValuesIteratorRemove(map);
    }

  }

  // tryReadOnlyValuesIteratorRemove() goes hand in hand with testSetUpValuesIteratorRemove().
  private void tryReadOnlyValuesIteratorRemove(Map map) {
    synchronized (map) {
      Iterator iterator = map.values().iterator();
      try {
        iterator.next();
        iterator.remove();
        throw new AssertionError("Should have thrown a ReadOnlyException");
      } catch (ReadOnlyException t) {
        // Expected
      }
    }
  }

  // THashMap specific testing methods.
  void testTHashMapRemoveAt(Map map, boolean validate) {
    if (!(map instanceof THashMap)) { return; }

    Map toAdd = new HashMap();
    addMappings(toAdd, 2);
    if (validate) {
      assertMappings(toAdd, map);
    } else {
      synchronized (map) {
        map.putAll(toAdd);
        map.put("key2", "value2");
      }
      Class mapClass = TObjectHash.class;
      Class[] parameterType = new Class[1];

      try {
        parameterType[0] = Object.class;
        Method m = mapClass.getDeclaredMethod("index", parameterType);
        m.setAccessible(true); // suppressing java access checking since removeRange is
        // a protected method.
        Object indexObj = m.invoke(map, new Object[] { "key2" });

        synchronized (map) {
          parameterType[0] = Integer.TYPE;
          m = mapClass.getDeclaredMethod("removeAt", parameterType);
          m.setAccessible(true); // suppressing java access checking since removeRange is
          // a protected method.

          m.invoke(map, new Object[] { indexObj });
        }
      } catch (NoSuchMethodException e) {
        // ignore NoSuchmethodExcpetion in test.
      } catch (IllegalArgumentException e) {
        // ignore IllegalArgumentException in test.
      } catch (IllegalAccessException e) {
        // ignore IllegalAccessException in test.
      } catch (InvocationTargetException e) {
        // ignore InvocationTargetException in test.
      }
    }
  }

  void testTHashMapTransformValues(Map map, boolean validate) {
    if (!(map instanceof THashMap)) { return; }
    THashMap tMap = (THashMap) map;
    if (validate) {
      for (int i = 0; i < 10; i++) {
        Assert.assertEquals(new Integer(i + 1), tMap.get(String.valueOf(i)));
      }
    } else {
      synchronized (tMap) {
        for (int i = 0; i < 10; i++) {
          tMap.put(String.valueOf(i), new Integer(i));
        }
      }
      synchronized (tMap) {
        tMap.transformValues(new MyObjectFunction());
      }
    }
  }

  void testPutNonPortableObject(Map map, boolean validate) {
    if (!canTestNonPortableObject(map)) { return; }

    Map toBeAdded = new HashMap();
    toBeAdded.put("First", "First Value");
    toBeAdded.put("Second", "Second Value");
    if (validate) {
      assertMappings(toBeAdded, map);
    } else {
      synchronized (map) {
        map.putAll(toBeAdded);
      }
      synchronized (map) {
        try {
          map.put("Non-portable", new MyNonPortableObject());
          throw new AssertionError("Should have thrown a TCNonPortableObjectError.");
        } catch (TCNonPortableObjectError e) {
          //
        }
      }
    }
  }

  void testLinkedHashMapAccessOrderGet(Map map, boolean validate) {
    if (!isAccessOrderedLinkedHashMap(map)) { return; }

    if (validate) {
      Map toBeExpected = new LinkedHashMap();
      toBeExpected.put("First", "First Value");
      toBeExpected.put("Third", "Third Value");
      toBeExpected.put("Second", "Second Value"); // access order maps put recently accessed items at the end
      assertMappings(toBeExpected, map);
    } else {
      Map toBeAdded = new LinkedHashMap();
      toBeAdded.put("First", "First Value");
      toBeAdded.put("Second", "Second Value");
      toBeAdded.put("Third", "Third Value");
      synchronized (map) {
        map.putAll(toBeAdded);
      }
      synchronized (map) {
        Assert.assertEquals("Second Value", map.get("Second"));
      }
    }
  }

  void testLinkedHashMapInsertionOrderPut(Map map, boolean validate) {
    if (!(map instanceof LinkedHashMap)) { return; }

    // we only want insertion ordered maps in this test
    if (isAccessOrderedLinkedHashMap(map)) { return; }

    if (validate) {
      Map toBeExpected = new LinkedHashMap();
      toBeExpected.put("First", "First Value");
      toBeExpected.put("Second", "New Second Value");
      toBeExpected.put("Third", "Third Value");
      assertMappings(toBeExpected, map);
    } else {
      Map toBeAdded = new LinkedHashMap();
      toBeAdded.put("First", "First Value");
      toBeAdded.put("Second", "Second Value");
      toBeAdded.put("Third", "Third Value");
      synchronized (map) {
        map.putAll(toBeAdded);
      }
      synchronized (map) {
        // replacing mapping should not affect order (for insertion order maps)
        map.put("Second", "New Second Value");
      }
    }
  }

  void testLinkedHashMapInsertionOrderRemovePut(Map map, boolean validate) {
    if (!(map instanceof LinkedHashMap)) { return; }

    // we only want insertion ordered maps in this test
    if (isAccessOrderedLinkedHashMap(map)) { return; }

    if (validate) {
      Map toBeExpected = new LinkedHashMap();
      toBeExpected.put("First", "First Value");
      toBeExpected.put("Third", "Third Value");
      toBeExpected.put("Second", "New Second Value");
      assertMappings(toBeExpected, map);
    } else {
      Map toBeAdded = new LinkedHashMap();
      toBeAdded.put("First", "First Value");
      toBeAdded.put("Second", "Second Value");
      toBeAdded.put("Third", "Third Value");
      synchronized (map) {
        map.putAll(toBeAdded);
      }
      synchronized (map) {
        map.remove("Second");
        map.put("Second", "New Second Value");
      }
    }
  }

  void testLinkedHashMapAccessOrderPut(Map map, boolean validate) {
    if (!isAccessOrderedLinkedHashMap(map)) { return; }

    if (validate) {
      Map toBeExpected = new LinkedHashMap();
      toBeExpected.put("First", "First Value");
      toBeExpected.put("Third", "Third Value");
      toBeExpected.put("Second", "New Second Value"); // access order maps put recently accessed items at the end
      assertMappings(toBeExpected, map);
    } else {
      Map toBeAdded = new LinkedHashMap();
      toBeAdded.put("First", "First Value");
      toBeAdded.put("Second", "Second Value");
      toBeAdded.put("Third", "Third Value");
      synchronized (map) {
        map.putAll(toBeAdded);
      }
      synchronized (map) {
        // puts count as access on access order linked hash maps
        map.put("Second", "New Second Value");
      }
    }
  }

  private boolean canTestSharedArray(Map map) {
    return (!(map instanceof HashMap) && !(map instanceof LinkedHashMap) && !(map instanceof Hashtable) && !(map instanceof TimeExpiryMap));
  }

  private boolean canTestNonPortableObject(Map map) {
    return ((map instanceof HashMap) || (map instanceof LinkedHashMap) || (map instanceof Hashtable));
  }
  
  private boolean canTestReadOnly(Map map) {
    return (!(map instanceof Hashtable) && !(map instanceof FastHashMap) && !(map instanceof TimeExpiryMap));
  }

  /**
   * Getting the array for HashMap and LinkedHashMap from a non-shared map temporarily until we fixed HashMap and
   * LinkedHashMap.
   */
  private Object getMySubclassArray(Map map) {
    if (map instanceof TimeExpiryMap) { return nonSharedArrayMap.get("arrayforTimeExpiryMap"); }
    if (map instanceof MyLinkedHashMap3) { return nonSharedArrayMap.get("arrayforMyLinkedHashMap3"); }
    if (map instanceof MyLinkedHashMap2) { return nonSharedArrayMap.get("arrayforMyLinkedHashMap2"); }
    if (map instanceof MyLinkedHashMap) { return nonSharedArrayMap.get("arrayforMyLinkedHashMap"); }
    if (map instanceof MyHashMap2) { return nonSharedArrayMap.get("arrayforMyHashMap2"); }
    if (map instanceof MyHashMap) { return nonSharedArrayMap.get("arrayforMyHashMap"); }
    if (map instanceof MyHashMap3) { return sharedMap.get("arrayforMyHashMap3"); }
    if (map instanceof MyTreeMap) { return sharedMap.get("arrayforMyTreeMap"); }
    if (map instanceof MyTreeMap2) { return sharedMap.get("arrayforMyTreeMap"); }
    if (map instanceof MyHashtable2) { return nonSharedArrayMap.get("arrayforMyHashtable2"); }
    if (map instanceof MyHashtable) { return nonSharedArrayMap.get("arrayforMyHashtable"); }
    if (map instanceof MyTHashMap) { return sharedMap.get("arrayforMyTHashMap"); }
    if (map instanceof MyFastHashMap) { return nonSharedArrayMap.get("arrayforMyFastHashMap"); }
    if (map instanceof MyProperties2) { return nonSharedArrayMap.get("arrayforMyProperties2"); }
    if (map instanceof MyProperties3) { return nonSharedArrayMap.get("arrayforMyProperties3"); }
    if (map instanceof MyProperties) { return nonSharedArrayMap.get("arrayforMyProperties"); }
    return null;
  }

  /**
   * Getting the array for HashMap and LinkedHashMap from a non-shared map temporarily until we fixed HashMap and
   * LinkedHashMap.
   */
  private Object[] getArray(Map map) {
    Object o = getMySubclassArray(map);
    if (o != null) { return (Object[]) o; }

    if (map instanceof Properties) { return (Object[]) nonSharedArrayMap.get("arrayforProperties"); }
    if (map instanceof FastHashMap) {
      if (((FastHashMap) map).getFast()) {
        return (Object[]) nonSharedArrayMap.get("arrayforFastHashMapWithFast");
      } else {
        return (Object[]) nonSharedArrayMap.get("arrayforFastHashMap");
      }
    } else if (map instanceof LinkedHashMap) {
      return (Object[]) nonSharedArrayMap.get("arrayforLinkedHashMap");
    } else if (map instanceof HashMap) {
      return (Object[]) nonSharedArrayMap.get("arrayforHashMap");
    } else if (map instanceof Hashtable) {
      return (Object[]) nonSharedArrayMap.get("arrayforHashtable");
    } else if (map instanceof TreeMap) {
      return (Object[]) sharedMap.get("arrayforTreeMap");
    } else if (map instanceof THashMap) { return (Object[]) sharedMap.get("arrayforTHashMap"); }
    return null;
  }

  void assertMappingsKeysEqual(Object[] expect, Collection collection) {
    Assert.assertEquals(expect.length, collection.size());
    for (int i = 0; i < expect.length; i++) {
      Assert.assertTrue(collection.contains(expect[i]));
    }
  }

  void assertMappings(Map expect, Map actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Set expectEntries = expect.entrySet();
    Set actualEntries = actual.entrySet();
    if (actual instanceof LinkedHashMap) {
      for (Iterator iExpect = expectEntries.iterator(), iActual = actualEntries.iterator(); iExpect.hasNext();) {
        Assert.assertEquals(iExpect.next(), iActual.next());
      }
    }

    for (Iterator i = actualEntries.iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Assert.assertEquals(entry.getValue(), expect.get(entry.getKey()));
    }
  }

  void assertMappingsEqual(Object[] expect, Map map) {
    Assert.assertEquals(expect.length, map.size());

    if (map instanceof LinkedHashMap) {
      Set entries = map.entrySet();
      int i = 0;
      for (Iterator iActual = entries.iterator(); iActual.hasNext();) {
        Assert.assertEquals(expect[i++], iActual.next());
      }
    }

    for (int i = 0; i < expect.length; i++) {
      Entry entry = (Entry) expect[i];
      if (isAccessOrderedLinkedHashMap(map)) {
        synchronized (map) {
          Assert.assertEquals(entry.getValue(), map.get(entry.getKey()));
        }
      } else {
        Assert.assertEquals(entry.getValue(), map.get(entry.getKey()));
      }
    }
  }

  void assertSingleMappingInternal(Map map, final Object key, final Object value) {
    try {
      Assert.assertFalse(map.isEmpty());
      Assert.assertEquals(1, map.size());
      Assert.assertEquals(1, map.entrySet().size());
      Assert.assertEquals(1, map.values().size());
      Assert.assertEquals(1, map.keySet().size());

      Assert.assertEquals(value, map.get(key));
      Assert.assertTrue(map.containsKey(key));
      Assert.assertTrue(map.containsValue(value));

      Set entries = map.entrySet();
      for (Iterator i = entries.iterator(); i.hasNext();) {
        Entry entry = (Entry) i.next();
        Assert.assertEquals(key, entry.getKey());
        Assert.assertEquals(value, entry.getValue());
      }

      for (Iterator i = map.values().iterator(); i.hasNext();) {
        Object o = i.next();
        Assert.assertEquals(value, o);
      }

      for (Iterator i = map.keySet().iterator(); i.hasNext();) {
        Object o = i.next();
        Assert.assertEquals(key, o);
      }
    } catch (Throwable t) {
      notifyError(new ErrorContext(map.getClass().getName(), t));
    }
  }

  void assertSingleMapping(Map map, final Object key, final Object value) {
    if (isAccessOrderedLinkedHashMap(map)) {
      // MyLinkedHashMap3 has accessOrder set to true; therefore, get() method
      // will mutate internal state and thus, require synchronized.
      synchronized (map) {
        assertSingleMappingInternal(map, key, value);
      }
    } else {
      assertSingleMappingInternal(map, key, value);
    }
  }

  void assertEmptyMap(Map map) {
    try {
      Assert.assertTrue(map.isEmpty());
      Assert.assertEquals(0, map.size());
      Assert.assertEquals(0, map.entrySet().size());
      Assert.assertEquals(0, map.values().size());
      Assert.assertEquals(0, map.keySet().size());
      Assert.assertEquals(Collections.EMPTY_MAP, map);
    } catch (Throwable t) {
      notifyError(new ErrorContext(map.getClass().getName(), t));
    }
  }

  private void addMappings(Map map, int count) {
    synchronized (map) {
      for (int i = 0; i < count; i++) {
        map.put("key" + i, "value" + i);
      }
    }
  }

  private Map getOrderSensitiveMappings() {
    Map map = null;
    map = new HashMap();
    map.put("January", "Jan");
    map.put("February", "Feb");
    map.put("March", "Mar");
    map.put("April", "Apr");
    return map;
  }

  private boolean isAccessOrderedLinkedHashMap(Map map) {
    if (map instanceof LinkedHashMap) {
      try {
        Field f = LinkedHashMap.class.getDeclaredField("accessOrder");
        f.setAccessible(true);
        return ((Boolean) f.get(map)).booleanValue();
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }

    return false;
  }

  private static boolean allowsNull(Map map) {
    return !(map instanceof Hashtable) && !(map instanceof TimeExpiryMap);
  }

  // Used to determine if a specific key instance is retained or not
  private static class Key implements Comparable {
    private final String id;
    private final String equals;

    Key(String id, String equals) {
      this.id = id;
      this.equals = equals;
    }

    public int hashCode() {
      return equals == null ? 0 : equals.hashCode();
    }

    public boolean equals(Object o) {
      if (!(o instanceof Key)) { return false; }
      Key other = (Key) o;
      if (this.equals == null && other.equals == null) { return true; }
      if (this.equals == null && other.equals != null) { return false; }
      if (other.equals == null && this.equals != null) { return false; }
      return this.equals.equals(other.equals);
    }

    public int compareTo(Object o) {
      Key cmp = (Key) o;
      return equals.compareTo(cmp.equals);
    }

  }

  private static class NullTolerantComparator implements Comparator {

    public int compare(Object o1, Object o2) {
      if (o1 == null && o2 == null) { return 0; }
      if (o1 == null && o2 != null) { return -1; }
      if (o1 != null && o2 == null) { return 1; }
      return ((Comparable) o1).compareTo(o2);
    }
  }

  private static class SimpleEntry implements Map.Entry {
    // NOTE: this class more or less copied from Sun's source to java/util/AbstractMap

    private final Object key;
    private Object       value;

    public SimpleEntry(Object key, Object value) {
      this.key = key;
      this.value = value;
    }

    public SimpleEntry(Map.Entry e) {
      this.key = e.getKey();
      this.value = e.getValue();
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

  private static class MyHashMap extends HashMap {
    protected Object key;
    protected Object value;

    public MyHashMap(int k) {
      super(11);
    }

    public MyHashMap(Map map) {
      putAll(map);
    }

    public Object put(Object arg0, Object arg1) {
      this.key = arg0;
      this.value = arg1;
      return super.put(arg0, arg1);
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public Object getObject(Object arg) {
      return super.get(arg);
    }

    protected void testProtected() {
      // do nothing
    }

    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
      s.defaultReadObject();
    }

    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
      s.defaultWriteObject();
    }
  }

  private static class MyHashMap2 extends MyHashMap {
    public MyHashMap2() {
      super(12);
    }
  }

  private static class MyTreeMap extends TreeMap {
    public MyTreeMap() {
      super();
    }

    public MyTreeMap(Comparator comparator) {
      super(comparator);
    }
  }

  private static class MyHashMap3 extends HashMap {
    private int i;

    public MyHashMap3(int i) {
      super();
      this.i = i;
    }

    public int getI() {
      // this method here to silence compiler warning, no other reason
      return i;
    }
  }

  private static class MyTreeMap2 extends MyTreeMap {
    private Object key;
    private Object value;

    public MyTreeMap2(Comparator comparator) {
      super(comparator);
    }

    public Object put(Object arg0, Object arg1) {
      this.key = arg0;
      this.value = arg1;
      return super.put(arg0, arg1);
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
  }

  private static class MyHashtable extends Hashtable {
    public MyHashtable() {
      super();
    }

  }

  private static class MyHashtable2 extends MyHashtable {
    public MyHashtable2() {
      super();
    }
  }

  private static class MyLinkedHashMap extends LinkedHashMap {
    public MyLinkedHashMap() {
      super();
    }
  }

  private static class MyLinkedHashMap2 extends MyLinkedHashMap {
    private Object key;
    private Object value;

    public MyLinkedHashMap2() {
      super();
    }

    public Object put(Object arg0, Object arg1) {
      this.key = arg0;
      this.value = arg1;
      return super.put(arg0, arg1);
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
  }

  private static class MyLinkedHashMap3 extends LinkedHashMap {
    public MyLinkedHashMap3(boolean accessOrder) {
      super(10, 0.75f, accessOrder);
    }
  }

  private static class MyTHashMap extends THashMap {
    public MyTHashMap() {
      super();
    }
  }

  private static class MyFastHashMap extends FastHashMap {
    public MyFastHashMap() {
      super();
    }
  }

  private static class MyProperties extends Properties {
    private Object key;
    private Object value;

    public MyProperties() {
      super();
    }

    public Object put(Object arg0, Object arg1) {
      this.key = arg0;
      this.value = arg1;
      return super.put(arg0, arg1);
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
  }

  private static class MyProperties2 extends MyProperties {
    private Object lastGetKey;

    public MyProperties2() {
      super();
    }

    public synchronized Object get(Object key) {
      this.lastGetKey = key;
      return super.get(key);
    }

    public Object getLastGetKey() {
      return lastGetKey;
    }
  }

  private static class MyProperties3 extends Properties {
    public MyProperties3() {
      super();
    }

    public void setDefault(Properties newDefaults) {
      defaults = newDefaults;
    }

    public Object put(Object arg0, Object arg1) {
      return super.put(arg0, arg1);
    }
  }

  private static class MyObjectFunction implements TObjectFunction {

    public Object execute(Object value) {
      return new Integer(((Integer) value).intValue() + 1);
    }

  }

  private static class MyNonPortableObject {
    public MyNonPortableObject() {
      super();
    }
  }

}
