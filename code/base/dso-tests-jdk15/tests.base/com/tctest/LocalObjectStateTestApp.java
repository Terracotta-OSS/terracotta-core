/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LocalObjectStateTestApp extends AbstractErrorCatchingTransparentApp {
  private List<MapWrapper> root       = new ArrayList<MapWrapper>();
  private CyclicBarrier    barrier    = new CyclicBarrier(2);
  private Class[]          mapClasses = new Class[] { HashMap.class, TreeMap.class, Hashtable.class,
      LinkedHashMap.class, THashMap.class, /*FastHashMap.class*/};

  public LocalObjectStateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    if (await() == 0) {
      createMaps();
    }
    await();

    for (Boolean withReadLock : new Boolean[] { Boolean.FALSE, Boolean.TRUE }) {
      for (MapWrapper mw : root) {
        testWrapper(mw, withReadLock, new PutMutator());
        testWrapper(mw, withReadLock, new PutAllMutator());
      }
    }
  }

  private void createMaps() throws Exception {
    synchronized (root) {
      for (Class k : mapClasses) {
        root.add(new MapWrapper(k));
      }
    }
  }

  private void testWrapper(MapWrapper m, boolean testWithReadLock, Mutator mutator) throws Exception {
    int currentSize = m.getMap().size();
    boolean currentReadLock = m.getHandler().getReadLock();

    if (await() == 0) {
      m.getHandler().setReadLock(testWithReadLock);
      try {
        mutator.doMutate(m.getMap());
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
      return barrier.barrier();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");
    config.addNewModule("clustered-commons-collections-3.1", "1.0.0");

    String testClass = LocalObjectStateTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

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
      Map map = (Map)o;
      map.put("key", "value");
    }
  }

  private static class PutAllMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map)o;
      Map anotherMap = new HashMap();
      anotherMap.put("k", "v");
      map.putAll(anotherMap);
    }
  }
  
}
