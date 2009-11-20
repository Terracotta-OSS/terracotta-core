/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

public class ConcurrentHashMapLoadTestApp extends AbstractTransparentApp {
  private static final int        NUM_OF_PUT = 1000;

  private final DataKey[]         keyRoots   = new DataKey[] { new DataKey(1), new DataKey(2), new DataKey(3),
      new DataKey(4)                        };
  private final DataValue[]       valueRoots = new DataValue[] { new DataValue(10), new DataValue(20),
      new DataValue(30), new DataValue(40)  };

  private final CyclicBarrier     barrier;
  private final ConcurrentHashMap mapRoot    = new ConcurrentHashMap();
  private final SharedObject      sharedRoot = new SharedObject();

  public ConcurrentHashMapLoadTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

      testUnsharedToShared1(index);
      testUnsharedToShared2(index);
      testUnsharedToShared3(index);
      testUnsharedToShared4(index);

      testContainsKey1(index);
      testContainsKey2(index);
      testGet(index);
      testRemove(index);
      testReplace(index);

      testPutMany(index);
      testPutAndRemoveMany(index);
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testUnsharedToShared1(int index) throws Exception {
    if (index == 0) {
      ConcurrentHashMap newMap = new ConcurrentHashMap();
      newMap.put(keyRoots[0], valueRoots[0]);
      newMap.put(keyRoots[1], valueRoots[1]);
      newMap.put(keyRoots[2], valueRoots[2]);
      newMap.put(keyRoots[3], valueRoots[3]);

      synchronized (sharedRoot) {
        sharedRoot.setMap(newMap);
      }
    }

    barrier.await();

    Map newMap = new HashMap();
    newMap.put(keyRoots[0], valueRoots[0]);
    newMap.put(keyRoots[1], valueRoots[1]);
    newMap.put(keyRoots[2], valueRoots[2]);
    newMap.put(keyRoots[3], valueRoots[3]);

    Map sharedMap = sharedRoot.getMap();
    assertMappingsEqual(newMap, sharedMap);

    barrier.await();
  }

  private void testUnsharedToShared2(int index) throws Exception {
    if (index == 0) {
      ConcurrentHashMap newMap = new ConcurrentHashMap();
      newMap.put(keyRoots[0], valueRoots[0]);
      newMap.put(keyRoots[1], valueRoots[1]);
      newMap.put(keyRoots[2], valueRoots[2]);

      mapRoot.put("newMap", newMap);
      mapRoot.put(keyRoots[0], keyRoots[1]);
    }

    barrier.await();

    Map newMap = new HashMap();
    newMap.put(keyRoots[0], valueRoots[0]);
    newMap.put(keyRoots[1], valueRoots[1]);
    newMap.put(keyRoots[2], valueRoots[2]);

    Map sharedMap = (Map) mapRoot.get("newMap");
    assertMappingsEqual(newMap, sharedMap);

    barrier.await();

    if (index == 1) {
      Map m = (Map) mapRoot.get("newMap");
      m.put(keyRoots[3], valueRoots[3]);
    }

    barrier.await();

    newMap.put(keyRoots[3], valueRoots[3]);

    sharedMap = (Map) mapRoot.get("newMap");
    assertMappingsEqual(newMap, sharedMap);

    barrier.await();
  }

  private void testUnsharedToShared3(int index) throws Exception {
    if (index == 0) {
      ConcurrentHashMap newMap = new ConcurrentHashMap();
      DataKey key1 = new DataKey(1);
      DataKey key2 = new DataKey(2);
      DataValue val1 = new DataValue(10);
      DataValue val2 = new DataValue(20);
      newMap.put(key1, val1);
      newMap.put(key2, val2);

      Assert.assertNull(((Manageable) key1).__tc_managed());

      mapRoot.put("newMap", newMap);

      Assert.assertNotNull(((Manageable) key1).__tc_managed());
    }

    barrier.await();
  }

  private void testUnsharedToShared4(int index) throws Exception {
    clearMapRoot(index);
    if (index == 0) {
      ConcurrentHashMap newMap = new ConcurrentHashMap();
      HashKey key1 = new HashKey(1);
      HashKey key2 = new HashKey(2);
      HashValue val1 = new HashValue(10);
      HashValue val2 = new HashValue(20);
      newMap.put(key1, val1);
      newMap.put(key2, val2);

      mapRoot.put(newMap, "newMap");
    }

    barrier.await();

    Assert.assertEquals(1, mapRoot.size());

    Set keys = mapRoot.keySet();
    Iterator keyIterator = keys.iterator();
    Map map = (Map) keyIterator.next();
    map.containsKey(new HashKey(1));
    map.containsKey(new HashKey(2));
    map.containsValue(new HashValue(10));
    map.containsValue(new HashValue(20));

    Object o = mapRoot.get(map);
    Assert.assertEquals("newMap", o);

    barrier.await();

    if (index == 1) {
      map.put(new HashKey(3), new HashValue(30));
    }

    barrier.await();

    Assert.assertEquals(new HashValue(30), map.get(new HashKey(3)));

    barrier.await();
  }

  private void testContainsKey1(int index) throws Exception {
    if (index == 0) {
      DataKey key1 = new DataKey(1);
      DataKey key2 = new DataKey(1);

      DataValue val1 = new DataValue(10);
      DataValue val2 = new DataValue(10);

      Assert.assertNull(((Manageable) key1).__tc_managed());
      Assert.assertNull(((Manageable) key2).__tc_managed());
      Assert.assertNull(((Manageable) val1).__tc_managed());
      Assert.assertNull(((Manageable) val2).__tc_managed());

      mapRoot.put(share(key1), val1);
      Assert.assertNotNull(((Manageable) key1).__tc_managed());
      Assert.assertNotNull(((Manageable) val1).__tc_managed());

      Assert.assertTrue(mapRoot.containsKey(key1));
      Assert.assertFalse(mapRoot.containsKey(key2));

      Assert.assertNull(((Manageable) key2).__tc_managed());

      Assert.assertTrue(mapRoot.contains(val1));
      Assert.assertFalse(mapRoot.contains(val2));

      Assert.assertNull(((Manageable) val2).__tc_managed());
    }

    barrier.await();
  }

  private void testContainsKey2(int index) throws Exception {
    if (index == 0) {
      HashKey key1 = new HashKey(1);
      HashKey key2 = new HashKey(1);

      HashValue val1 = new HashValue(10);
      HashValue val2 = new HashValue(10);

      Assert.assertNull(((Manageable) key1).__tc_managed());
      Assert.assertNull(((Manageable) key2).__tc_managed());
      Assert.assertNull(((Manageable) val1).__tc_managed());
      Assert.assertNull(((Manageable) val2).__tc_managed());

      mapRoot.put(key1, val1);

      Assert.assertNotNull(((Manageable) key1).__tc_managed());
      Assert.assertNotNull(((Manageable) val1).__tc_managed());

      Assert.assertTrue(mapRoot.containsKey(key1));
      Assert.assertTrue(mapRoot.containsKey(key2));

      Assert.assertNull(((Manageable) key2).__tc_managed());

      Assert.assertTrue(mapRoot.contains(val1));
      Assert.assertTrue(mapRoot.contains(val2));

      Assert.assertNull(((Manageable) val2).__tc_managed());
    }

    barrier.await();
  }

  private void testGet(int index) throws Exception {
    if (index == 0) {
      DataKey key1 = new DataKey(1);
      DataKey key2 = new DataKey(1);

      DataValue val1 = new DataValue(10);
      DataValue val2 = new DataValue(10);

      Assert.assertNull(((Manageable) key1).__tc_managed());
      Assert.assertNull(((Manageable) key2).__tc_managed());
      Assert.assertNull(((Manageable) val1).__tc_managed());
      Assert.assertNull(((Manageable) val2).__tc_managed());

      mapRoot.put(share(key1), val1);

      Assert.assertNotNull(((Manageable) key1).__tc_managed());
      Assert.assertNotNull(((Manageable) val1).__tc_managed());

      Assert.assertNotNull(mapRoot.get(key1));
      Assert.assertNull(mapRoot.get(key2));

      Assert.assertNull(((Manageable) key2).__tc_managed());
    }

    barrier.await();
  }

  private DataKey share(DataKey key) {
    // This wierdness is to make this test work with the new restriction that
    // keys w/o hashCode() override must be shared before they are used as keys
    mapRoot.put("__SHARE KEY__", key);
    return key;
  }

  private void testRemove(int index) throws Exception {
    if (index == 0) {
      DataKey key1 = new DataKey(1);
      DataKey key2 = new DataKey(1);

      DataValue val1 = new DataValue(10);
      DataValue val2 = new DataValue(10);

      Assert.assertNull(((Manageable) key1).__tc_managed());
      Assert.assertNull(((Manageable) key2).__tc_managed());
      Assert.assertNull(((Manageable) val1).__tc_managed());
      Assert.assertNull(((Manageable) val2).__tc_managed());

      mapRoot.put(share(key1), val1);

      Assert.assertNotNull(((Manageable) key1).__tc_managed());
      Assert.assertNotNull(((Manageable) val1).__tc_managed());

      Assert.assertNull(mapRoot.remove(key2));

      Assert.assertNull(((Manageable) key2).__tc_managed());
    }

    barrier.await();
  }

  private void testReplace(int index) throws Exception {
    if (index == 0) {
      DataKey key1 = new DataKey(1);
      DataKey key2 = new DataKey(1);

      DataValue val1 = new DataValue(10);
      DataValue val2 = new DataValue(10);

      Assert.assertNull(((Manageable) key1).__tc_managed());
      Assert.assertNull(((Manageable) key2).__tc_managed());
      Assert.assertNull(((Manageable) val1).__tc_managed());
      Assert.assertNull(((Manageable) val2).__tc_managed());

      mapRoot.put(share(key1), val1);

      Assert.assertNotNull(((Manageable) key1).__tc_managed());
      Assert.assertNotNull(((Manageable) val1).__tc_managed());

      Assert.assertNull(mapRoot.replace(key2, val2));

      Assert.assertNull(((Manageable) key2).__tc_managed());
    }

    barrier.await();
  }

  private void testPutMany(int index) throws Exception {
    if (index == 0) {
      for (int i = 0; i < NUM_OF_PUT; i++) {
        mapRoot.put(new HashKey(i), new HashValue(i));
      }
    }

    barrier.await();

    for (int i = 0; i < NUM_OF_PUT; i++) {
      Assert.assertEquals(new HashValue(i), mapRoot.get(new HashKey(i)));
    }

    barrier.await();
  }

  private void testPutAndRemoveMany(int index) throws Exception {
    clearMapRoot(index);

    if (index == 0) {
      for (int i = 0; i < NUM_OF_PUT; i++) {
        System.out.println("Put: " + i);
        mapRoot.put(new HashKey(i), new HashValue(i));
      }
    } else if (index == 1) {
      for (int i = 0; i < NUM_OF_PUT; i++) {
        Object o = null;
        while (o == null) {
          o = mapRoot.remove(new HashKey(i));
        }

        Assert.assertEquals(o, new HashValue(i));
        System.out.println("Remove: " + i);
      }
    }

    barrier.await();

    Assert.assertTrue(mapRoot.isEmpty());

    barrier.await();
  }

  private void clearMapRoot(int index) throws Exception {
    if (index == 0) {
      System.err.println("In clearMapRoot");

      mapRoot.clear();
    }
    barrier.await();
  }

  void assertMappingsEqual(Map expect, Map actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Set expectEntries = expect.entrySet();
    Set actualEntries = actual.entrySet();

    for (Iterator i = expectEntries.iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Assert.assertEquals(((DataValue) entry.getValue()).getInt(), ((DataValue) actual.get(entry.getKey())).getInt());
    }

    for (Iterator i = actualEntries.iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Assert.assertEquals(((DataValue) entry.getValue()).getInt(), ((DataValue) expect.get(entry.getKey())).getInt());
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ConcurrentHashMapLoadTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("mapRoot", "mapRoot");
    spec.addRoot("sharedRoot", "sharedRoot");
    spec.addRoot("keyRoots", "keyRoots");
    spec.addRoot("valueRoots", "valueRoots");
  }

  private static class DataKey {
    private final int i;

    public DataKey(int i) {
      super();
      this.i = i;
    }

    @Override
    public String toString() {
      return super.toString() + ", i: " + i;
    }
  }

  private static class DataValue {
    private final int i;

    public DataValue(int i) {
      super();
      this.i = i;
    }

    public int getInt() {
      return this.i;
    }

    @Override
    public String toString() {
      return super.toString() + ", i: " + i;
    }
  }

  private static class HashKey {
    private final int i;

    public HashKey(int i) {
      super();
      this.i = i;
    }

    @Override
    public int hashCode() {
      return i;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (!(obj instanceof HashKey)) return false;
      return ((HashKey) obj).i == i;
    }

    @Override
    public String toString() {
      return super.toString() + ", i: " + i;
    }
  }

  private static class HashValue {
    private final int i;

    public HashValue(int i) {
      super();
      this.i = i;
    }

    @Override
    public int hashCode() {
      return i;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (!(obj instanceof HashValue)) return false;
      return ((HashValue) obj).i == i;
    }

    @Override
    public String toString() {
      return super.toString() + ", i: " + i;
    }
  }

  private static class SharedObject {
    private ConcurrentHashMap map;

    public SharedObject() {
      super();
    }

    public ConcurrentHashMap getMap() {
      return map;
    }

    public void setMap(ConcurrentHashMap map) {
      this.map = map;
    }

  }

}
