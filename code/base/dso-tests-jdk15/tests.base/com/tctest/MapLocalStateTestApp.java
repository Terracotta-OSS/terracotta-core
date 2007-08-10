/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.collections.FastHashMap;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import gnu.trove.THashMap;

import java.net.Socket;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

/**
 * Test to make sure local object state is preserved when TC throws:
 * 
 * UnlockedSharedObjectException ReadOnlyException TCNonPortableObjectError
 * 
 * Map version
 * 
 * INT-186
 * 
 * @author hhuynh
 */
public class MapLocalStateTestApp extends GenericLocalStateTestApp {
  private List<Wrapper> root       = new ArrayList<Wrapper>();
  private CyclicBarrier barrier;
  private Class[]       mapClasses = new Class[] { TreeMap.class, THashMap.class, LinkedHashMap.class, Hashtable.class,
      HashMap.class, ConcurrentHashMap.class, FastHashMap.class };

  public MapLocalStateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(cfg.getGlobalParticipantCount());
  }

  protected void runTest() throws Throwable {
    if (await() == 0) {
      createMaps();
    }
    await();

    for (LockMode lockMode : LockMode.values()) {
      for (Wrapper mw : root) {
        testMutate(mw, lockMode, new PutMutator());
        testMutate(mw, lockMode, new PutAllMutator());
        testMutate(mw, lockMode, new RemoveMutator());
        testMutate(mw, lockMode, new ClearMutator());
        testMutate(mw, lockMode, new KeySetClearMutator());
        testMutate(mw, lockMode, new RemoveValueMutator());
        testMutate(mw, lockMode, new EntrySetClearMutator());
        testMutate(mw, lockMode, new EntrySetIteratorRemoveMutator());
        testMutate(mw, lockMode, new KeySetIteratorRemoveMutator());
        testMutate(mw, lockMode, new ValuesIteratorRemoveMutator());
        testMutate(mw, lockMode, new KeySetRemoveMutator());
        testMutate(mw, lockMode, new AddEntryMutator());
        testMutate(mw, lockMode, new AddNonPortableEntryMutator());
        // Failing, disabled for now
        // testMutate(w, LockMode.WRITE, new NonPortableAddMutator());
      }
    }

  }

  private void createMaps() throws Exception {
    Map data = new HashMap();
    data.put("k1", "v1");
    data.put("k2", "v2");
    data.put("k3", "v3");

    synchronized (root) {
      for (Class k : mapClasses) {
        CollectionWrapper mw = new CollectionWrapper(k, Map.class);
        ((Map) mw.getObject()).putAll(data);
        root.add(mw);
      }
    }
  }

  protected void validate(int oldSize, Wrapper wrapper, LockMode lockMode, Mutator mutator) throws Throwable {
    int newSize = wrapper.size();
    switch (lockMode) {
      case NONE:
      case READ:
        Assert.assertEquals("Type: " + wrapper.getObject().getClass() + ", lock: " + lockMode,
                            oldSize, newSize);
        if (mutator instanceof AddEntryMutator) {
          Collection values = ((Map) wrapper.getObject()).values();
          for (Iterator it = values.iterator(); it.hasNext();) {
            String value = (String) it.next();
            Assert.assertFalse("Type: " + wrapper.getObject().getClass() + ", lock: " + lockMode, value.equals("hung"));
          }
        } else if (mutator instanceof NonPortableAddMutator) {
          Collection values = ((Map) wrapper.getObject()).values();
          for (Iterator it = values.iterator(); it.hasNext();) {
            Object value = it.next();
            Assert.assertFalse("Type: " + wrapper.getObject().getClass() + ", lock: " + lockMode,
                               value instanceof Socket);
          }
        }
        break;
      case WRITE:
        System.out.println("Map type: " + wrapper.getObject().getClass().getName());
        System.out.println("Current size: " + newSize);
        System.out.println("New size: " + newSize);
        Assert.assertFalse("Type: " + wrapper.getObject().getClass() + ", socket shouldn't be added", ((Map) wrapper
            .getObject()).containsKey("socket"));
        break;
      default:
        throw new RuntimeException("Shouldn't happen");
    }
  }

  protected int await() {
    try {
      return barrier.await();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.addNewModule("clustered-commons-collections-3.1", "1.0.0");

    String testClass = MapLocalStateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");
    config.addIncludePattern(GenericLocalStateTestApp.class.getName() + "$*");

    config.addWriteAutolock("* " + testClass + "*.createMaps()");
    config.addWriteAutolock("* " + testClass + "*.validate()");
    config.addReadAutolock("* " + testClass + "*.runTest()");

    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");

    config.addReadAutolock("* " + Handler.class.getName() + "*.invokeWithReadLock(..)");
    config.addWriteAutolock("* " + Handler.class.getName() + "*.invokeWithWriteLock(..)");
    config.addWriteAutolock("* " + Handler.class.getName() + "*.setLockMode(..)");
  }

  private static class PutMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      map.put("key", "value");
    }
  }

  private static class PutAllMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Map anotherMap = new HashMap();
      anotherMap.put("k", "v");
      map.putAll(anotherMap);
    }
  }

  private static class RemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      map.remove("k1");
    }
  }

  private static class ClearMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      map.clear();
    }
  }

  private static class RemoveValueMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Collection values = map.values();
      values.remove("v1");
    }
  }

  private static class EntrySetClearMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set entries = map.entrySet();
      entries.clear();
    }
  }

  private static class AddEntryMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set entries = map.entrySet();
      for (Iterator it = entries.iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        entry.setValue("hung");
      }
    }
  }

  private static class AddNonPortableEntryMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set entries = map.entrySet();
      for (Iterator it = entries.iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        entry.setValue(new Socket());
      }
    }
  }

  private static class EntrySetIteratorRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set entries = map.entrySet();
      for (Iterator it = entries.iterator(); it.hasNext();) {
        it.next();
        it.remove();
      }
    }
  }

  private static class KeySetClearMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set keys = map.keySet();
      keys.clear();
    }
  }

  private static class KeySetIteratorRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set keys = map.keySet();
      for (Iterator it = keys.iterator(); it.hasNext();) {
        it.next();
        it.remove();
      }
    }
  }

  private static class ValuesIteratorRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Collection values = map.values();
      for (Iterator it = values.iterator(); it.hasNext();) {
        it.next();
        it.remove();
      }
    }
  }

  private static class KeySetRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set keys = map.keySet();
      keys.remove("k1");
    }
  }

  private static class NonPortableAddMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Map anotherMap = new LinkedHashMap();
      anotherMap.put("k4", "v4");
      anotherMap.put("k5", "v5");
      anotherMap.put("k6", "v6");
      anotherMap.put("k7", "v7");
      anotherMap.put("k8", "v8");
      anotherMap.put("k9", "v9");
      anotherMap.put("socket", new Socket());
      anotherMap.put("k10", "v10");
      map.putAll(anotherMap);
    }
  }
}
