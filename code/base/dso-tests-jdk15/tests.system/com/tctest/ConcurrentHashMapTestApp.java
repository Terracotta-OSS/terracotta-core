/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapTestApp extends GenericTestApp {

  private final DataKey[]           keyRoots   = new DataKey[]{ new DataKey(1), new DataKey(2), new DataKey(3), new DataKey(4)};
  private final DataValue[]         valueRoots = new DataValue[]{ new DataValue(10), new DataValue(20), new DataValue(30), new DataValue(40) };
  
  private final HashKey[]           hashKeys   = new HashKey[]{ new HashKey(1), new HashKey(2), new HashKey(3), new HashKey(4)};
  private final HashValue[]         hashValues = new HashValue[]{ new HashValue(10), new HashValue(20), new HashValue(30), new HashValue(40) };
  
  public ConcurrentHashMapTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, ConcurrentHashMap.class);
  }

  protected Object getTestObject(String test) {
    return sharedMap.get("map");
  }

  protected void setupTestObject(String test) {
    List listOfMaps = new ArrayList();
    listOfMaps.add(new ConcurrentHashMap());
    sharedMap.put("maps", listOfMaps);
    sharedMap.put("map", new ConcurrentHashMap());
    sharedMap.put("arrayforConcurrentHashMap", new Object[4]);
    sharedMap.put("arrayforConcurrentHashMapWithHashKeys", new Object[4]);
  }
  
  void testPut1(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertFalse(map.isEmpty());
      Assert.assertEquals(1, map.size());
      Assert.assertEquals(20, ((DataValue) (map.get(keyRoots[0]))).getInt());
    } else {
      DataValue value1 = new DataValue(10);
      DataValue value2 = new DataValue(20);
      Object o = map.put(keyRoots[0], value1);
      Assert.assertNull(o);

      o = map.put(keyRoots[0], value2);
      Assert.assertTrue(o == value1);
    }
  }
  
  void testPut2(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertFalse(map.isEmpty());
      Assert.assertEquals(1, map.size());
      Assert.assertEquals(hashValues[1], map.get(hashKeys[0]));
    } else {
      Object o = map.put(hashKeys[0], hashValues[0]);
      Assert.assertNull(o);

      o = map.put(hashKeys[0], hashValues[1]);
      Assert.assertTrue(o == hashValues[0]);
    }
  }
  
  void testPutWithClassKey(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertFalse(map.isEmpty());
      Assert.assertEquals(1, map.size());
      Assert.assertEquals(hashValues[0], map.get(HashKey.class));
    } else {
      Object o = map.put(HashKey.class, hashValues[0]);
      Assert.assertNull(o);
    }
  }

  void testPutIfAbsent(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertFalse(map.isEmpty());
      Assert.assertEquals(1, map.size());
      Assert.assertEquals(10, ((DataValue) (map.get(keyRoots[0]))).getInt());
    } else {
      DataValue value1 = new DataValue(10);
      DataValue value2 = new DataValue(20);
      Object o = map.put(keyRoots[0], value1);

      o = map.putIfAbsent(keyRoots[0], value2);
      Assert.assertTrue(o == value1);
    }
  }

  void testPutIfAbsent2(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertFalse(map.isEmpty());
      Assert.assertEquals(1, map.size());
      Assert.assertEquals(hashValues[0], map.get(hashKeys[0]));
    } else {
      Object o = map.put(hashKeys[0], hashValues[0]);

      o = map.putIfAbsent(hashKeys[0], hashValues[1]);
      Assert.assertTrue(o == hashValues[0]);
    }
  }
  
  void testPutAll1(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      assertMappingsEqual(toPut, map);
    } else {
      map.putAll(toPut);
    }
  }
  
  void testPutAll2(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      assertMappingsHashEqual(toPut, map);
    } else {
      map.putAll(toPut);
    }
  }

  void testRemove1(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      toPut.remove(keyRoots[1]);
      assertMappingsEqual(toPut, map);
    } else {
      map.putAll(toPut);
      map.remove(keyRoots[1]);
    }
  }

  void testHashRemove1(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      toPut.remove(hashKeys[1]);
      assertMappingsHashEqual(toPut, map);
    } else {
      map.putAll(toPut);
      map.remove(hashKeys[1]);
    }
  }

  void testRemove2(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      assertMappingsEqual(toPut, map);
    } else {
      map.putAll(toPut);
      map.remove(keyRoots[1], new DataValue(30));
    }
  }
  
  void testHashRemove2(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      assertMappingsHashEqual(toPut, map);
    } else {
      map.putAll(toPut);
      map.remove(hashKeys[1], new HashValue(30));
    }
  }
  
  void testRemove3(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      toPut.remove(keyRoots[3]);
      assertMappingsEqual(toPut, map);
    } else {
      map.putAll(toPut);
      map.remove(keyRoots[3], valueRoots[3]);
    }
  }
  
  void testHashRemove3(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      toPut.remove(hashKeys[3]);
      assertMappingsHashEqual(toPut, map);
    } else {
      map.putAll(toPut);
      //map.remove(hashKeys[3], new HashValue(40));
      map.remove(hashKeys[3], hashValues[3]);
    }
  }
  
  void testReplace1(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(10, ((DataValue) map.get(keyRoots[0])).getInt());
    } else {
      DataValue value1 = new DataValue(10);
      Object o = map.put(keyRoots[0], value1);
      Assert.assertNull(o);
      o = map.replace(new DataKey(1), new DataValue(20));
      Assert.assertNull(o);
    }
  }
  
  void testHashReplace1(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      assertSingleHashMapping(hashKeys[0], hashValues[0], map);
    } else {
      Object o = map.put(hashKeys[0], hashValues[0]);
      Assert.assertNull(o);
      o = map.replace(hashKeys[1], hashValues[1]);
      Assert.assertNull(o);
    }
  }

  void testReplace2(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(20, ((DataValue) map.get(keyRoots[0])).getInt());
    } else {
      DataValue value1 = new DataValue(10);
      Object o = map.put(keyRoots[0], value1);
      Assert.assertNull(o);
      o = map.replace(keyRoots[0], new DataValue(20));
      Assert.assertEquals(10, ((DataValue) o).getInt());
    }
  } 

  void testHashReplace2(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      assertSingleHashMapping(hashKeys[0], hashValues[1], map);
    } else {
      Object o = map.put(hashKeys[0], hashValues[0]);
      Assert.assertNull(o);
      Object o2 = new HashKey(1);
      o = map.replace(o2, new HashValue(20));
      Assert.assertEquals(o, hashValues[0]);
    }
  }
  
  void testReplaceIfValueEqual1(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(valueRoots[0], map.get(keyRoots[0]));
    } else {
      Object o = map.put(keyRoots[0], valueRoots[0]);
      Assert.assertNull(o);
      boolean returnValue = map.replace(keyRoots[0], new DataValue(10), new DataValue(20));
      Assert.assertFalse(returnValue);
    }
  }
  
  void testHashReplaceIfValueEqual1(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(hashValues[0], map.get(hashKeys[0]));
    } else {
      Object o = map.put(hashKeys[0], hashValues[0]);
      Assert.assertNull(o);
      boolean returnValue = map.replace(new HashKey(1), new HashValue(15), new DataValue(20));
      Assert.assertFalse(returnValue);
    }
  }
  
  void testReplaceIfValueEqual2(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(20, ((DataValue) map.get(keyRoots[0])).getInt());
    } else {
      Object o = map.put(keyRoots[0], valueRoots[0]);
      Assert.assertNull(o);
      boolean returnValue = map.replace(keyRoots[0], valueRoots[0], new DataValue(20));
      Assert.assertTrue(returnValue);
    }
  }
  
  void testHashReplaceIfValueEqual2(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(hashValues[1], map.get(hashKeys[0]));
    } else {
      Object o = map.put(hashKeys[0], hashValues[0]);
      Assert.assertNull(o);
      boolean returnValue = map.replace(new HashKey(1), new HashValue(10), new HashValue(20));
      Assert.assertTrue(returnValue);
    }
  }

  void testContains1(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertTrue(map.containsKey(keyRoots[0]));
      Assert.assertFalse(map.containsKey(new DataKey(1)));

      Assert.assertTrue(map.containsValue(valueRoots[0]));
      Assert.assertFalse(map.containsValue(new DataValue(10)));

      Assert.assertTrue(map.contains(valueRoots[0]));
      Assert.assertFalse(map.contains(new DataValue(10)));
    } else {
      map.put(keyRoots[0], valueRoots[0]);
    }
  }
  
  void testContains2(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertTrue(map.containsKey(hashKeys[0]));
      Assert.assertTrue(map.containsKey(new HashKey(1)));

      Assert.assertTrue(map.containsValue(hashValues[0]));
      Assert.assertTrue(map.containsValue(new HashValue(10)));

      Assert.assertTrue(map.contains(hashValues[0]));
      Assert.assertTrue(map.contains(new HashValue(10)));
    } else {
      map.put(hashKeys[0], hashValues[0]);
    }
  }
  
  void testEntrySetClear(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    DataKey key1 = new DataKey(1);
    DataKey key2 = new DataKey(2);
    DataKey key3 = new DataKey(3);

    DataValue value1 = new DataValue(10);
    DataValue value2 = new DataValue(20);
    DataValue value3 = new DataValue(30);
    toPut.put(key1, value1);
    toPut.put(key2, value2);
    toPut.put(key3, value3);
    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      map.putAll(toPut);
      
      map.entrySet().clear();
    }
  }
  
  void testEntrySetContains1(ConcurrentHashMap map, boolean validate) throws Exception {
    SimpleEntry entry = new SimpleEntry(keyRoots[0], valueRoots[0]); 
    if (validate) {
      Assert.assertTrue(map.entrySet().contains(entry));
    } else {
      map.put(keyRoots[0], valueRoots[0]);
    }
  }
  
  void testEntrySetContains2(ConcurrentHashMap map, boolean validate) throws Exception {
    SimpleEntry entry = new SimpleEntry(new HashKey(1), new HashValue(10)); 
    if (validate) {
      Assert.assertTrue(map.entrySet().contains(entry));
    } else {
      map.put(hashKeys[0], hashValues[0]);
    }
  }
  
  void testEntrySetContainsAll1(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);
 
    if (validate) {
      SimpleEntry entry1 = new SimpleEntry(keyRoots[1], valueRoots[1]);
      SimpleEntry entry2 = new SimpleEntry(keyRoots[2], valueRoots[2]);
      List containsList = new ArrayList(2);
      containsList.add(entry1);
      containsList.add(entry2);
      Assert.assertTrue(map.entrySet().containsAll(containsList));
    } else {
      map.putAll(toPut);
    }
  }
  
  void testEntrySetContainsAll2(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);
 
    if (validate) {
      SimpleEntry entry1 = new SimpleEntry(hashKeys[1], hashValues[1]);
      SimpleEntry entry2 = new SimpleEntry(new HashKey(3), new HashValue(30));
      List containsList = new ArrayList(2);
      containsList.add(entry1);
      containsList.add(entry2);
      Assert.assertTrue(map.entrySet().containsAll(containsList));
    } else {
      map.putAll(toPut);
    }
  }
  
  void testEntrySetRetainAll1(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);
    if (validate) {
      toPut.remove(keyRoots[0]);
      toPut.remove(keyRoots[3]);
      assertMappingsEqual(toPut, map);
    } else {
      map.putAll(toPut);
      SimpleEntry entry1 = new SimpleEntry(keyRoots[1], valueRoots[1]);
      SimpleEntry entry2 = new SimpleEntry(keyRoots[2], valueRoots[2]);
      List containsList = new ArrayList(2);
      containsList.add(entry1);
      containsList.add(entry2);
      map.entrySet().retainAll(containsList);
    }
  }
  
  void testEntrySetRetainAll2(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);
    
    if (validate) {
      toPut.remove(hashKeys[0]);
      toPut.remove(hashKeys[3]);
      assertMappingsHashEqual(toPut, map);
    } else {
      map.putAll(toPut);
      SimpleEntry entry1 = new SimpleEntry(hashKeys[1], hashValues[1]);
      SimpleEntry entry2 = new SimpleEntry(new HashKey(3), new HashValue(30));
      List containsList = new ArrayList(2);
      containsList.add(entry1);
      containsList.add(entry2);
      map.entrySet().retainAll(containsList);
    }
  }
  
  void testEntrySetRemove1(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      map.put(keyRoots[0], valueRoots[0]);
      SimpleEntry entry = new SimpleEntry(keyRoots[0], valueRoots[0]);
      map.entrySet().remove(entry);
    }
  }
  
  void testEntrySetRemove2(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      map.put(hashKeys[0], hashValues[0]);
      SimpleEntry entry = new SimpleEntry(hashKeys[0], hashValues[0]);
      map.entrySet().remove(entry);
    }
  }
  
  void testEntrySetRemoveAll1(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);
    if (validate) {
      toPut.remove(keyRoots[1]);
      toPut.remove(keyRoots[2]);
      assertMappingsEqual(toPut, map);
    } else {
      map.putAll(toPut);
      
      SimpleEntry entry1 = new SimpleEntry(keyRoots[1], valueRoots[1]); 
      SimpleEntry entry2 = new SimpleEntry(keyRoots[2], valueRoots[2]);
      List toRemove = new ArrayList(2);
      toRemove.add(entry1);
      toRemove.add(entry2);
      map.entrySet().removeAll(toRemove);
    }
  }
  
  void testEntrySetRemoveAll2(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);
    if (validate) {
      toPut.remove(hashKeys[1]);
      toPut.remove(hashKeys[2]);
      assertMappingsHashEqual(toPut, map);
    } else {
      map.putAll(toPut);
      
      SimpleEntry entry1 = new SimpleEntry(hashKeys[1], hashValues[1]); 
      SimpleEntry entry2 = new SimpleEntry(new HashKey(3), new HashValue(30));
      List toRemove = new ArrayList(2);
      toRemove.add(entry1);
      toRemove.add(entry2);
      map.entrySet().removeAll(toRemove);
    }
  }
  
  void testEntrySetSize1(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(1, map.entrySet().size());
    } else {
      map.put(keyRoots[0], valueRoots[0]);
    }
  }
  
  void testEntrySetSize2(ConcurrentHashMap map, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(1, map.entrySet().size());
    } else {
      map.put(hashKeys[0], hashValues[0]);
    }
  }
  
  void testEntrySetSetValue1(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);
    if (validate) {
      Assert.assertEquals(15, ((DataValue)map.get(keyRoots[1])).getInt());
    } else {
      map.putAll(toPut);
      for (Iterator i=map.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry = (Map.Entry)i.next();
        if (((DataKey)entry.getKey()).getInt() == 2) {
          entry.setValue(new DataValue(15));
        }
      }
    }
  }
  
  void testEntrySetSetValue2(ConcurrentHashMap map, boolean validate) throws Exception {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);
    if (validate) {
      Assert.assertEquals(new HashValue(15), map.get(hashKeys[1]));
    } else {
      map.putAll(toPut);
      for (Iterator i=map.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry = (Map.Entry)i.next();
        if (((HashKey)entry.getKey()).getInt() == 2) {
          entry.setValue(new HashValue(15));
        }
      }
    }
  }
  
  void testEntrySetIteratorRemove1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);
    if (validate) {
      toPut.remove(keyRoots[1]);
      assertMappingsEqual(toPut, map);
    } else {
      map.putAll(toPut);
      assertMappingsEqual(toPut, map);
      
      for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry e = (Map.Entry) i.next();
        if (e.getKey().equals(keyRoots[1])) {
          i.remove();
          break;
        }
      }
    }
  }
  
  void testEntrySetIteratorRemove2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);
    if (validate) {
      toPut.remove(hashKeys[1]);
      assertMappingsHashEqual(toPut, map);
    } else {
      map.putAll(toPut);
      assertMappingsHashEqual(toPut, map);
      
      for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry e = (Map.Entry) i.next();
        if (e.getKey().equals(hashKeys[1])) {
          i.remove();
          break;
        }
      }
    }
  }
  
  void testEntrySetToArray1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);
    Object[] array = getArray(map, false);

    if (validate) {
      assertMappingsEqual(array, map);
    } else {
      map.putAll(toPut);
      synchronized (array) {
        Object[] returnArray = map.entrySet().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }
  
  void testEntrySetToArray2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);
    Object[] array = getArray(map, true);

    if (validate) {
      assertMappingsEqual(array, map);
    } else {
      map.putAll(toPut);
      synchronized (array) {
        Object[] returnArray = map.entrySet().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }
  
  void testValuesClear1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      map.putAll(toPut);
      map.values().clear();
    }
  }
  
  void testValuesClear2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      map.putAll(toPut);
      map.values().clear();
    }
  }
  
  void testValuesContains1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      Assert.assertTrue(map.values().contains(valueRoots[2]));
    } else {
      map.putAll(toPut);
    }
  }
  
  void testValuesContains2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      Assert.assertTrue(map.values().contains(new HashValue(20)));
    } else {
      map.putAll(toPut);
    }
  }
  
  void testValuesContainsAll1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      Assert.assertTrue(map.values().containsAll(toPut.values()));
    } else {
      map.putAll(toPut);
    }
  }
  
  void testValuesContainsAll2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      Assert.assertTrue(map.values().containsAll(toPut.values()));
    } else {
      map.putAll(toPut);
    }
  }
  
  void testValuesRemove1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      toPut.remove(keyRoots[1]);
      assertMappingsEqual(toPut, map);
    } else {
      map.putAll(toPut);
      map.values().remove(valueRoots[1]);
    }
  }
  
  void testValuesRemove2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      toPut.remove(hashKeys[1]);
      assertMappingsHashEqual(toPut, map);
    } else {
      map.putAll(toPut);
      map.values().remove(new HashValue(20));
    }
  }
  
  void testValuesRemoveAll1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(valueRoots[0]);
      expect.add(valueRoots[2]);
      assertCollectionsEqual(expect, map.values());
    } else {
      map.putAll(toPut);
      List toRemove = new ArrayList(2);
      toRemove.add(valueRoots[1]);
      toRemove.add(valueRoots[3]);
      map.values().removeAll(toRemove);
    }
  }
  
  void testValuesRemoveAll2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(hashValues[0]);
      expect.add(hashValues[2]);
      assertCollectionsEqual(expect, map.values());
    } else {
      map.putAll(toPut);
      List toRemove = new ArrayList(2);
      toRemove.add(new HashValue(20));
      toRemove.add(new HashValue(40));
      map.values().removeAll(toRemove);
    }
  }
  
  void testValuesRetainAll1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(valueRoots[1]);
      expect.add(valueRoots[3]);
      assertCollectionsEqual(expect, map.values());
    } else {
      map.putAll(toPut);
      List toRetain = new ArrayList(2);
      toRetain.add(valueRoots[1]);
      toRetain.add(valueRoots[3]);
      map.values().retainAll(toRetain);
    }
  }
  
  void testValuesRetainAll2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(hashValues[1]);
      expect.add(hashValues[3]);
      assertCollectionsEqual(expect, map.values());
    } else {
      map.putAll(toPut);
      List toRetain = new ArrayList(2);
      toRetain.add(new HashValue(20));
      toRetain.add(new HashValue(40));
      map.values().retainAll(toRetain);
    }
  }
  
  void testValuesToArray1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);
    Object[] array = getArray(map, false);

    if (validate) {
      assertCollectionsEqual(array, map.values());
    } else {
      map.putAll(toPut);
      synchronized (array) {
        Object[] returnArray = map.values().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }
  
  void testValuesToArray2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);
    Object[] array = getArray(map, false);

    if (validate) {
      assertCollectionsEqual(array, map.values());
    } else {
      map.putAll(toPut);
      synchronized (array) {
        Object[] returnArray = map.values().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }
  
  void testValuesIteratorRemove1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(valueRoots[0]);
      expect.add(valueRoots[2]);
      expect.add(valueRoots[3]);
      assertCollectionsEqual(expect, map.values());
    } else {
      map.putAll(toPut);
      for (Iterator i=map.values().iterator(); i.hasNext(); ) {
        DataValue value = (DataValue)i.next();
        if (value.getInt() == 20) {
          i.remove();
        }
      }
    }
  }
  
  void testValuesIteratorRemove2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(hashValues[0]);
      expect.add(hashValues[2]);
      expect.add(hashValues[3]);
      assertCollectionsEqual(expect, map.values());
    } else {
      map.putAll(toPut);
      for (Iterator i=map.values().iterator(); i.hasNext(); ) {
        Object value = i.next();
        if (value.equals(new HashValue(20))) {
          i.remove();
        }
      }
    }
  }
  
  void testKeySetClear1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      map.putAll(toPut);
      map.keySet().clear();
    }
  }
  
  void testKeySetClear2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      Assert.assertEquals(0, map.size());
    } else {
      map.putAll(toPut);
      map.keySet().clear();
    }
  }
  
  void testKeySetContains1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      Assert.assertTrue(map.keySet().contains(keyRoots[2]));
    } else {
      map.putAll(toPut);
    }
  }
  
  void testKeySetContains2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      Assert.assertTrue(map.keySet().contains(new HashKey(2)));
    } else {
      map.putAll(toPut);
    }
  }
  
  void testKeySetContainsAll1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      Assert.assertTrue(map.keySet().containsAll(toPut.keySet()));
    } else {
      map.putAll(toPut);
    }
  }
  
  void testKeySetContainsAll2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      Assert.assertTrue(map.keySet().containsAll(toPut.keySet()));
    } else {
      map.putAll(toPut);
    }
  }
  
  void testKeySetRemove1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      toPut.remove(keyRoots[1]);
      assertMappingsEqual(toPut, map);
    } else {
      map.putAll(toPut);
      map.keySet().remove(keyRoots[1]);
    }
  }
  
  void testKeySetRemove2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      toPut.remove(hashKeys[1]);
      assertMappingsHashEqual(toPut, map);
    } else {
      map.putAll(toPut);
      map.keySet().remove(new HashKey(2));
    }
  }
  
  void testKeySetRemoveAll1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(keyRoots[0]);
      expect.add(keyRoots[2]);
      assertCollectionsEqual(expect, map.keySet());
    } else {
      map.putAll(toPut);
      List toRemove = new ArrayList(2);
      toRemove.add(keyRoots[1]);
      toRemove.add(keyRoots[3]);
      map.keySet().removeAll(toRemove);
    }
  }
  
  void testKeySetRemoveAll2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(hashKeys[0]);
      expect.add(hashKeys[2]);
      assertCollectionsEqual(expect, map.keySet());
    } else {
      map.putAll(toPut);
      List toRemove = new ArrayList(2);
      toRemove.add(new HashKey(2));
      toRemove.add(new HashKey(4));
      map.keySet().removeAll(toRemove);
    }
  }
  
  void testKeySetRetainAll1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(keyRoots[1]);
      expect.add(keyRoots[3]);
      assertCollectionsEqual(expect, map.keySet());
    } else {
      map.putAll(toPut);
      List toRetain = new ArrayList(2);
      toRetain.add(keyRoots[1]);
      toRetain.add(keyRoots[3]);
      map.keySet().retainAll(toRetain);
    }
  }
  
  void testKeySetRetainAll2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(hashKeys[1]);
      expect.add(hashKeys[3]);
      assertCollectionsEqual(expect, map.keySet());
    } else {
      map.putAll(toPut);
      List toRetain = new ArrayList(2);
      toRetain.add(hashKeys[1]);
      toRetain.add(hashKeys[3]);
      map.keySet().retainAll(toRetain);
    }
  }
  
  void testKeySetToArray1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);
    Object[] array = getArray(map, false);

    if (validate) {
      assertCollectionsEqual(array, map.keySet());
    } else {
      map.putAll(toPut);
      synchronized (array) {
        Object[] returnArray = map.keySet().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }
  
  void testKeySetToArray2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);
    Object[] array = getArray(map, false);

    if (validate) {
      assertCollectionsEqual(array, map.keySet());
    } else {
      map.putAll(toPut);
      synchronized (array) {
        Object[] returnArray = map.keySet().toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }
  
  void testKeySetIteratorRemove1(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(keyRoots[0], valueRoots[0]);
    toPut.put(keyRoots[1], valueRoots[1]);
    toPut.put(keyRoots[2], valueRoots[2]);
    toPut.put(keyRoots[3], valueRoots[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(keyRoots[0]);
      expect.add(keyRoots[2]);
      expect.add(keyRoots[3]);
      assertCollectionsEqual(expect, map.keySet());
    } else {
      map.putAll(toPut);
      for (Iterator i=map.keySet().iterator(); i.hasNext(); ) {
        DataKey key = (DataKey)i.next();
        if (key.getInt() == 2) {
          i.remove();
        }
      }
    }
  }
  
  void testKeySetIteratorRemove2(ConcurrentHashMap map, boolean validate) {
    Map toPut = new HashMap();
    toPut.put(hashKeys[0], hashValues[0]);
    toPut.put(hashKeys[1], hashValues[1]);
    toPut.put(hashKeys[2], hashValues[2]);
    toPut.put(hashKeys[3], hashValues[3]);

    if (validate) {
      List expect = new ArrayList();
      expect.add(hashKeys[0]);
      expect.add(hashKeys[2]);
      expect.add(hashKeys[3]);
      assertCollectionsEqual(expect, map.keySet());
    } else {
      map.putAll(toPut);
      for (Iterator i=map.keySet().iterator(); i.hasNext(); ) {
        Object key = i.next();
        if (key.equals(new HashKey(2))) {
          i.remove();
        }
      }
    }
  }

  void assertSingleHashMapping(Object expectedKey, Object expectedValue, Map map) {
    Assert.assertEquals(1, map.size());
    Assert.assertEquals(expectedValue, map.get(expectedKey));
  }
  
  void assertMappingsEqual(Object[] expect, Map map) {
    Assert.assertEquals(expect.length, map.size());
    for (int i = 0; i < expect.length; i++) {
      Entry entry = (Entry) expect[i];
      Object val = map.get(entry.getKey());
      Assert.assertEquals(entry.getValue(), val);
    }
  }
  
  void assertCollectionsEqual(Object[] expect, Collection collection) {
    Assert.assertEquals(expect.length, collection.size());
    for (int i = 0; i < expect.length; i++) {
      Assert.assertTrue(collection.contains(expect[i]));
    }
  }
  
  void assertCollectionsEqual(Collection expect, Collection collection) {
    Assert.assertEquals(expect.size(), collection.size());
    for (Iterator i=expect.iterator(); i.hasNext(); ) {
      Assert.assertTrue(collection.contains(i.next()));
    }
  }
  
  void assertMappingsHashEqual(Map expect, Map actual) {
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
  
  void assertMappingsEqual(Map expect, Map actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Set expectEntries = expect.entrySet();
    Set actualEntries = actual.entrySet();

    for (Iterator i = expectEntries.iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Assert.assertEquals(((DataValue)entry.getValue()).getInt(), ((DataValue)actual.get(entry.getKey())).getInt());
    }

    for (Iterator i = actualEntries.iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Assert.assertEquals(((DataValue)entry.getValue()).getInt(), ((DataValue)expect.get(entry.getKey())).getInt());
    }
  }
  
  private Object[] getArray(Map map, boolean hashKey) {
    if (!hashKey) {
      return (Object[]) sharedMap.get("arrayforConcurrentHashMap");
    } else {
      return (Object[]) sharedMap.get("arrayforConcurrentHashMapWithHashKeys");
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ConcurrentHashMapTestApp.class.getName();
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");
    spec.addRoot("keyRoots", "keyRoots");
    spec.addRoot("valueRoots", "valueRoots");
    spec.addRoot("hashKeys", "hashKeys");
    spec.addRoot("hashValues", "hashValues");
  }
  
  private static class SimpleEntry implements Map.Entry {

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

  private static class DataKey {
    private int i;

    public DataKey(int i) {
      super();
      this.i = i;
    }

    public int getInt() {
      return this.i;
    }
  }

  private static class DataValue {
    private int i;

    public DataValue(int i) {
      super();
      this.i = i;
    }

    public int getInt() {
      return this.i;
    }
  }
  
  private static class HashKey {
    private int i;

    public HashKey(int i) {
      super();
      this.i = i;
    }

    public int getInt() {
      return this.i;
    }
    
    public int hashCode() {
      return i;
    }
    
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (! (obj instanceof HashKey)) return false;
      return ((HashKey)obj).i == i;
    }
  }

  private static class HashValue {
    private int i;

    public HashValue(int i) {
      super();
      this.i = i;
    }

    public int getInt() {
      return this.i;
    }
    
    public int hashCode() {
      return i;
    }
    
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (! (obj instanceof HashValue)) return false;
      return ((HashValue)obj).i == i;
    }
    
    public String toString() {
      return super.toString() + ", i: " + i;
    }
  }
  
}
