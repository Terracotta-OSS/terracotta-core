/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import gnu.trove.THashMap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CyclicBarrier;

/**
 * Test to make sure local object state is preserved when TC throws UnlockedSharedObjectException
 * and ReadOnlyException - INT-186
 * 
 * @author hhuynh
 */
public class LocalObjectStateTestApp extends AbstractErrorCatchingTransparentApp {
  private List<MapWrapper> root       = new ArrayList<MapWrapper>();
  private CyclicBarrier    barrier;
  private Class[]          mapClasses = new Class[] { HashMap.class, TreeMap.class, Hashtable.class,
      LinkedHashMap.class, THashMap.class /*, ConcurrentHashMap.class, FastHashMap.class */};

  public LocalObjectStateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(cfg.getGlobalParticipantCount());
  }

  protected void runTest() throws Throwable {
    if (await() == 0) {
      createMaps();
    }
    await();

    for (Boolean withReadLock : new Boolean[] { Boolean.FALSE, Boolean.TRUE }) {
      for (MapWrapper mw : root) {
        testMutate(mw, withReadLock, new PutMutator());
        testMutate(mw, withReadLock, new PutAllMutator());
        testMutate(mw, withReadLock, new RemoveMutator());
        testMutate(mw, withReadLock, new ClearMutator());
        testMutate(mw, withReadLock, new KeySetClearMutator());
        testMutate(mw, withReadLock, new RemoveValueMutator());
        testMutate(mw, withReadLock, new EntrySetClearMutator());
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
        MapWrapper mw = new MapWrapper(k);
        mw.getMap().putAll(data);
        root.add(mw);
      }
    }
  }

  private void testMutate(MapWrapper m, boolean testWithReadLock, Mutator mutator) throws Exception {
    int currentSize = m.getMap().size();
    boolean currentReadLock = m.getHandler().getReadLock();

    if (await() == 0) {
      m.getHandler().setReadLock(testWithReadLock);
      try {
        mutator.doMutate(m.getMapProxy());
      } catch (Exception e) {
        System.out.println("Expected: " + e.getClass());
      }
    }

    await();
    m.getHandler().setReadLock(currentReadLock);
    Assert.assertEquals("Map type: " + m.getMap().getClass() + ", readLock: " + testWithReadLock, currentSize, m
        .getMap().size());
  }

  private int await() {
    try {
      return barrier.await();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.addNewModule("clustered-commons-collections-3.1", "1.0.0");

    String testClass = LocalObjectStateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.createMaps()";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClass + "*.runTest()";
    config.addReadAutolock(methodExpression);

    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");

    config.addReadAutolock("* " + Handler.class.getName() + "*.invokeWithReadLock(..)");
    config.addWriteAutolock("* " + Handler.class.getName() + "*.setReadLock(..)");
  }

  static class Handler implements InvocationHandler {
    private final Object o;
    private boolean      readLock = false;

    public Handler(Object o) {
      this.o = o;
    }

    public void setReadLock(boolean wrl) {
      synchronized (this) {
        this.readLock = wrl;
      }
    }

    public boolean getReadLock() {
      return readLock;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (readLock) { return invokeWithReadLock(method, args); }
      return method.invoke(o, args);
    }

    private Object invokeWithReadLock(Method method, Object[] args) throws Throwable {
      synchronized (o) {
        return method.invoke(o, args);
      }
    }
  }

  private static class MapWrapper {
    private Map     map;
    private Map     proxy;
    private Handler handler;

    public MapWrapper(Class mapType) throws Exception {
      map = (Map) mapType.newInstance();
      handler = new Handler(map);
      proxy = (Map) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Map.class }, handler);
    }

    public Map getMap() {
      return map;
    }

    public Map getMapProxy() {
      return proxy;
    }

    public Handler getHandler() {
      return handler;
    }
  }

  private static interface Mutator {
    public void doMutate(Object o);
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

  private static class KeySetClearMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set keys = map.keySet();
      keys.clear();
    }
  }

  private static class KeySetRemoveMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Set keys = map.keySet();
      keys.remove("k1");
    }
  }
}
