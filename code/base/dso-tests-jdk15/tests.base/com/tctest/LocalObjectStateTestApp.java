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
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import gnu.trove.THashMap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

/**
 * Test to make sure local object state is preserved when TC throws UnlockedSharedObjectException and ReadOnlyException -
 * INT-186
 * 
 * @author hhuynh
 */
public class LocalObjectStateTestApp extends AbstractErrorCatchingTransparentApp {
  private List<MapWrapper> root       = new ArrayList<MapWrapper>();
  private CyclicBarrier    barrier;
  private Class[]          mapClasses = new Class[] { HashMap.class, TreeMap.class, Hashtable.class,
      LinkedHashMap.class, THashMap.class, ConcurrentHashMap.class, FastHashMap.class };

  public LocalObjectStateTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(cfg.getGlobalParticipantCount());
  }

  protected void runTest() throws Throwable {
    if (await() == 0) {
      createMaps();
    }
    await();

    for (LOCK_MODE lockMode : LOCK_MODE.values()) {
      for (MapWrapper mw : root) {
        testMutate(mw, lockMode, new PutMutator());
        testMutate(mw, lockMode, new PutAllMutator());
        testMutate(mw, lockMode, new RemoveMutator());
        testMutate(mw, lockMode, new ClearMutator());
        testMutate(mw, lockMode, new KeySetClearMutator());
        testMutate(mw, lockMode, new RemoveValueMutator());
        testMutate(mw, lockMode, new EntrySetClearMutator());
        testMutate(mw, lockMode, new NonPortableAddMutator());
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

  private void testMutate(MapWrapper m, LOCK_MODE lockMode, Mutator mutator) throws Throwable {
    int currentSize = m.getMap().size();
    LOCK_MODE curr_lockMode = m.getHandler().getLockMode();
    boolean gotExpectedException = false;

    if (await() == 0) {
      m.getHandler().setLockMode(lockMode);
      try {
        mutator.doMutate(m.getMapProxy());
      } catch (UndeclaredThrowableException ute) {
        System.out.println("UndeclaredThrowableException: " + ute.getClass());
      } catch (Exception e) {
        gotExpectedException = true;
      }
    }

    await();
    m.getHandler().setLockMode(curr_lockMode);

    if (gotExpectedException) {
      Assert.assertEquals("Map type: " + m.getMap().getClass() + ", lock: " + lockMode, currentSize, m
          .getMap().size());
    }
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
    config.addWriteAutolock("* " + Handler.class.getName() + "*.invokeWithWriteLock(..)");
    config.addWriteAutolock("* " + Handler.class.getName() + "*.setLockMode(..)");
  }

  private static enum LOCK_MODE {
    NONE, READ, WRITE
  };

  private static class Handler implements InvocationHandler {
    private final Object o;
    private LOCK_MODE    lockMode = LOCK_MODE.NONE;

    public Handler(Object o) {
      this.o = o;
    }

    public LOCK_MODE getLockMode() {
      return lockMode;
    }

    public void setLockMode(LOCK_MODE mode) {
      synchronized (this) {
        lockMode = mode;
      }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      switch (lockMode) {
        case NONE:
          return method.invoke(o, args);
        case READ:
          return invokeWithReadLock(method, args);
        case WRITE:
          return invokeWithWriteLock(method, args);
        default:
          throw new RuntimeException("Should not happen");
      }
    }

    private Object invokeWithReadLock(Method method, Object[] args) throws Throwable {
      synchronized (o) {
        return method.invoke(o, args);
      }
    }

    private Object invokeWithWriteLock(Method method, Object[] args) throws Throwable {
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
  
  private static class NonPortableAddMutator implements Mutator {
    public void doMutate(Object o) {
      Map map = (Map) o;
      Map anotherMap = new LinkedHashMap();
      anotherMap.put("k4", "v4");
      anotherMap.put("socket", new Socket());
      anotherMap.put("k5", "v5");
      map.putAll(anotherMap);
    }
  }
}
