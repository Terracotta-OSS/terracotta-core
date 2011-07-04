/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import gnu.trove.THashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Test to make sure local object state is preserved when TC throws: UnlockedSharedObjectException ReadOnlyException
 * TCNonPortableObjectError Map version INT-186
 * 
 * @author hhuynh
 */
public class MapLocalStateTestApp extends GenericLocalStateTestApp {
  private List<Map> root       = new ArrayList<Map>();
  private Class[]   mapClasses = new Class[] { THashMap.class, TreeMap.class, LinkedHashMap.class, Hashtable.class,
      HashMap.class           };

  public MapLocalStateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    initTest();

    for (LockMode lockMode : LockMode.values()) {
      for (Map map : root) {
        testMutate(initMap(map), lockMode, PutMutator.class);
        testMutate(initMap(map), lockMode, PutAllMutator.class);
        testMutate(initMap(map), lockMode, RemoveMutator.class);
        testMutate(initMap(map), lockMode, ClearMutator.class);
        testMutate(initMap(map), lockMode, RemoveValueMutator.class);
        testMutate(initMap(map), lockMode, EntrySetClearMutator.class);
        testMutate(initMap(map), lockMode, EntrySetIteratorRemoveMutator.class);
        testMutate(initMap(map), lockMode, KeySetClearMutator.class);
        testMutate(initMap(map), lockMode, KeySetIteratorRemoveMutator.class);
        testMutate(initMap(map), lockMode, ValuesIteratorRemoveMutator.class);
        testMutate(initMap(map), lockMode, KeySetRemoveMutator.class);
        // failing CDV-163
        // testMutate(initMap(map), lockMode, AddNonPortableEntryMutator.class);
        // testMutate(initMap(map), lockMode, NonPortableAddMutator.class);
      }
    }

    // failing CDV-163
    // testForInternalLockingMap(initMap(new ConcurrentHashMap()), new AddNonPortableEntryMutator());
    // testForInternalLockingMap(initMap(new ConcurrentHashMap()), new NonPortableAddMutator());
    //
    // testForInternalLockingMap(initMap(new FastHashMap()), new AddNonPortableEntryMutator());
    // testForInternalLockingMap(initMap(new FastHashMap()), new NonPortableAddMutator());
  }

  protected void testForInternalLockingMap(Map map, Mutator mutator) throws Exception {
    mutator.doMutate(map);
    assertNoNonportableValue(map);
  }

  private void initTest() throws Exception {
    synchronized (root) {
      for (Class c : mapClasses) {
        root.add((Map) c.newInstance());
      }
    }
  }

  private Map initMap(Map map) throws Exception {
    synchronized (map) {
      map.clear();
      map.put("k1", "v1");
      map.put("k2", "v2");
      map.put("k3", "v3");
      return map;
    }
  }

  protected void validate(int before, int after, Object testTarget, LockMode lockMode, Class mutatorClass)
      throws Exception {
    switch (lockMode) {
      case NONE:
      case READ:
        Assert.assertEquals(testTarget, before, after);
        break;
      case WRITE:
        Assert.assertTrue(before != after);
        break;
      default:
        throw new RuntimeException("Shouldn't happen");
    }

    if (mutatorClass.equals(NonPortableAddMutator.class) || mutatorClass.equals(AddNonPortableEntryMutator.class)) {
      assertNoNonportableValue((Map) testTarget);
    }
  }

  private void assertNoNonportableValue(Map map) {
    for (Iterator it = map.values().iterator(); it.hasNext();) {
      Object o = it.next();
      Assert.assertFalse("Found NonPortable instance: " + map, o instanceof NonPortable);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MapLocalStateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");
    config.addIncludePattern(GenericLocalStateTestApp.class.getName() + "$*");
    config.addExcludePattern(NonPortable.class.getName());

    config.addWriteAutolock("* " + testClass + "*.initMap(..)");
    config.addWriteAutolock("* " + testClass + "*.initTest()");
    config.addWriteAutolock("* " + testClass + "*.validate(..)");
    config.addReadAutolock("* " + testClass + "*.runTest()");

    spec.addRoot("root", "root");

    config.addReadAutolock("* " + Handler.class.getName() + "*.invokeWithReadLock(..)");
    config.addWriteAutolock("* " + Handler.class.getName() + "*.invokeWithWriteLock(..)");
    config.addWriteAutolock("* " + Handler.class.getName() + "*.setLockMode(..)");
  }

  static class PutMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      map.put("key", "value");
    }
  }

  static class PutAllMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Map anotherMap = new HashMap();
      anotherMap.put("k", "v");
      map.putAll(anotherMap);
    }
  }

  static class RemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      map.remove("k1");
    }
  }

  static class ClearMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      map.clear();
    }
  }

  static class RemoveValueMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Collection values = map.values();
      values.remove("v1");
    }
  }

  static class EntrySetClearMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set entries = map.entrySet();
      entries.clear();
    }
  }

  static class AddEntryMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set entries = map.entrySet();
      for (Iterator it = entries.iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        entry.setValue("hung");
      }
    }
  }

  static class AddNonPortableEntryMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set entries = map.entrySet();
      for (Iterator it = entries.iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        entry.setValue(new NonPortable());
      }
    }
  }

  static class EntrySetIteratorRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set entries = map.entrySet();
      for (Iterator it = entries.iterator(); it.hasNext();) {
        it.next();
        it.remove();
      }
    }
  }

  static class KeySetClearMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set keys = map.keySet();
      keys.clear();
    }
  }

  static class KeySetIteratorRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set keys = map.keySet();
      for (Iterator it = keys.iterator(); it.hasNext();) {
        it.next();
        it.remove();
      }
    }
  }

  static class ValuesIteratorRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Collection values = map.values();
      for (Iterator it = values.iterator(); it.hasNext();) {
        it.next();
        it.remove();
      }
    }
  }

  static class KeySetRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set keys = map.keySet();
      keys.remove("k1");
    }
  }

  static class NonPortableAddMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Map anotherMap = new LinkedHashMap();
      anotherMap.put("k4", "v4");
      anotherMap.put("k5", "v5");
      anotherMap.put("k6", "v6");
      anotherMap.put("k7", "v7");
      anotherMap.put("k8", "v8");
      anotherMap.put("k9", "v9");
      anotherMap.put("nonportable", new NonPortable());
      anotherMap.put("k10", "v10");
      map.putAll(anotherMap);
    }
  }
}
