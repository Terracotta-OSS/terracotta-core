/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TCObjectExternal;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NonFaultingMapTestApp extends AbstractErrorCatchingTransparentApp {

  private static final String METHOD_PREFIX  = "test";
  private static final String METHOD_PATTERN = "^" + METHOD_PREFIX + ".*$";

  // roots
  private final CyclicBarrier barrier;
  private final Exit          exit           = new Exit();
  private final Map           sharedMap      = new HashMap();

  private final List          tests;

  private final List          localList      = new ArrayList();

  public NonFaultingMapTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    final int count = getParticipantCount();
    if (count != 2) { throw new RuntimeException("wrong number of nodes: " + count); }

    this.barrier = new CyclicBarrier(getParticipantCount());

    this.tests = getTestNames();
  }

  @Override
  public void runTest() throws Throwable {
    Thread.currentThread().setName("CLIENT " + ManagerUtil.getClientID());

    for (Iterator i = tests.iterator(); i.hasNext();) {
      String name = (String) i.next();

      if (barrier.barrier() == 0) {
        setupTestMaps(name);
      }
      barrier.barrier();

      for (Iterator maps = getTestMaps(); maps.hasNext();) {
        synchronized (sharedMap) {
          sharedMap.put("objectIds", new HashSet());
        }

        localList.clear();

        Map map = (Map) maps.next();

        long start = System.currentTimeMillis();
        if (barrier.barrier() == 0) System.err.print("Running test: " + name + " on " + map.getClass() + " ... ");

        try {
          runOp(name, map, false);
        } catch (Throwable t) {
          exit.toggle();
          throw t;
        } finally {
          barrier.barrier();
        }
        if (exit.shouldExit()) return;

        try {
          runOp(name, map, true);
        } catch (Throwable t) {
          exit.toggle();
          throw t;
        } finally {
          if (barrier.barrier() == 0) System.err.println(" took " + (System.currentTimeMillis() - start) + " millis");
        }

        if (exit.shouldExit()) return;
      }
    }
  }

  private void setupTestMaps(String name) {
    List maps = new ArrayList();

    maps.add(new HashMap());
    maps.add(new Hashtable());

    if (Vm.isJDK15Compliant()) {
      maps.add(makeConcurrentHashMap());
    }

    // This is just to make sure all the expected maps are here.
    // As new map classes get added to this test, you'll have to adjust this number obviously
    Assert.assertEquals("Map-count discrepancy", 2 + (Vm.isJDK15Compliant() ? 1 : 0), maps.size());

    synchronized (sharedMap) {
      sharedMap.put("maps", maps);
    }
  }

  private Iterator getTestMaps() {
    List testMaps = (List) sharedMap.get("maps");
    return testMaps.iterator();
  }

  private Object makeConcurrentHashMap() {
    try {
      return Class.forName("java.util.concurrent.ConcurrentHashMap").newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runOp(String op, Map map, boolean validate) throws Throwable {
    Method m = findMethod(op);
    runMethod(m, map, validate);
  }

  private void runMethod(Method m, Map map, boolean validate) throws Throwable {
    final Object[] args = new Object[] { map, Boolean.valueOf(validate) };

    try {
      m.invoke(this, args);
    } catch (InvocationTargetException ite) {
      throw ite.getTargetException();
    }
  }

  private Method findMethod(String name) throws NoSuchMethodException {
    final Class[] sig = new Class[] { Map.class, Boolean.TYPE };

    Method method = getClass().getDeclaredMethod(METHOD_PREFIX + name, sig);
    method.setAccessible(true);
    return method;
  }

  private List getTestNames() {
    List rv = new ArrayList();
    Class klass = getClass();
    Method[] methods = klass.getDeclaredMethods();
    for (Method m : methods) {
      if (m.getName().matches(METHOD_PATTERN)) {
        Class[] args = m.getParameterTypes();

        final boolean ok = (args.length == 2) && args[0].equals(Map.class) && args[1].equals(Boolean.TYPE);

        if (ok) {
          rv.add(m.getName().replaceFirst(METHOD_PREFIX, ""));
        } else {
          throw new RuntimeException("bad method: " + m);
        }
      }
    }

    if (rv.size() <= 0) { throw new RuntimeException("Didn't find any operations"); }

    // make test order predictable (although this is a bad thing to rely on)
    Collections.sort(rv);

    return rv;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = NonFaultingMapTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.getOrCreateSpec(Exit.class.getName());

    spec.addRoot("sharedMap", "sharedMap");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    spec.addRoot("exit", "exit");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    new CyclicBarrierSpec().visit(visitor, config);
  }

  private static class Exit {
    private boolean exit = false;

    synchronized boolean shouldExit() {
      return exit;
    }

    synchronized void toggle() {
      exit = true;
    }
  }

  private void addObjectToLocalSet(Object o) {
    // prevent L1 GC
    localList.add(o);

    // get TCObject (Object should always be managed at this point)
    TCObjectExternal tco = ManagerUtil.lookupExistingOrNull(o);
    Assert.assertNotNull("TCObject for " + o, tco);

    // record object id in clustered collection (as long to prevent munging)
    Set objectIds = (Set) sharedMap.get("objectIds");
    synchronized (objectIds) {
      objectIds.add(new Long(tco.getObjectID().toLong()));
    }
  }

  private int getLocalObjectCount() {
    // XXX: this is kludgey (a less circuitous route would be nicer)
    ClientObjectManager cm = ((TCObject) ManagerUtil.lookupExistingOrNull(sharedMap)).getTCClass().getObjectManager();

    Set objectIds = (Set) sharedMap.get("objectIds");
    int localCount = 0;
    for (Iterator it = objectIds.iterator(); it.hasNext();) {
      Object o = it.next();
      ObjectID oid = new ObjectID(((Long) o).longValue());
      if (cm.lookupIfLocal(oid) != null) localCount++;
    }

    return localCount;
  }

  void testNonFaultingPutAllMethod(Map map, boolean validate) {
    synchronized (sharedMap) {
      Assert.assertTrue(ManagerUtil.isManaged(map));
      if (validate) {
        Assert.assertEquals("Unnecessary L1 faulting occured", 2, getLocalObjectCount());
      } else {
        Object bob = new Object();
        Object alice = new Object();

        Map temp = new HashMap();
        temp.put("bob", bob);
        temp.put("alice", alice);
        map.putAll(temp);

        addObjectToLocalSet(bob);
        addObjectToLocalSet(alice);
      }
    }
  }

  void testNonFaultingKeySetRemoveMethod(Map map, boolean validate) {
    synchronized (map) {
      if (validate) {
        Set keySet = map.keySet();
        Object[] keys = keySet.toArray();

        for (int i = 0; i < keys.length; i++) {
          if (!keys[i].equals(Thread.currentThread().getName())) {
            keySet.remove(keys[i]);
          }
        }
        Assert.assertEquals("Unnecessary L1 faulting occured", 1, getLocalObjectCount());
      } else {
        Object bob = new Object();
        map.put(Thread.currentThread().getName(), bob);
        addObjectToLocalSet(bob);
      }
    }
  }

  void testNonFaultingKeySetRemoveAllMethod(Map map, boolean validate) {
    synchronized (map) {
      if (validate) {
        Set keySet = map.keySet();
        Object[] keys = keySet.toArray();

        Set someKeys = new HashSet();
        for (int i = 0; i < keys.length; i++) {
          if (!((String) keys[i]).startsWith(Thread.currentThread().getName())) {
            someKeys.add(keys[i]);
          }
        }

        keySet.removeAll(someKeys);

        Assert.assertEquals("Unnecessary L1 faulting occured", 2, getLocalObjectCount());
      } else {
        Object bob = new Object();
        Object alice = new Object();

        map.put(Thread.currentThread().getName() + ".bob", bob);
        map.put(Thread.currentThread().getName() + ".alice", alice);

        addObjectToLocalSet(bob);
        addObjectToLocalSet(alice);
      }
    }
  }

  void testNonFaultingKeySetRetainAllMethod(Map map, boolean validate) {
    synchronized (map) {
      if (validate) {

        Set keySet = map.keySet();
        Object[] keys = keySet.toArray();

        Set someKeys = new HashSet();
        for (Object key : keys) {
          if (((String) key).startsWith(Thread.currentThread().getName())) {
            someKeys.add(key);
          }
        }

        keySet.retainAll(someKeys);

        Assert.assertEquals("Unnecessary L1 faulting occured", 2, getLocalObjectCount());
      } else {
        Object bob = new Object();
        Object alice = new Object();

        map.put(Thread.currentThread().getName() + ".bob", bob);
        map.put(Thread.currentThread().getName() + ".alice", alice);

        addObjectToLocalSet(bob);
        addObjectToLocalSet(alice);
      }
    }
  }

  void testNonFaultingKeySetIteratorRemoveMethod(Map map, boolean validate) {
    synchronized (map) {
      if (validate) {
        Set keySet = map.keySet();
        for (Iterator i = keySet.iterator(); i.hasNext();) {
          Object key = i.next();
          if (!key.equals(Thread.currentThread().getName())) {
            i.remove();
          }
        }
        Assert.assertEquals("Unnecessary L1 faulting occured", 1, getLocalObjectCount());
      } else {
        Object bob = new Object();
        map.put(Thread.currentThread().getName(), bob);
        addObjectToLocalSet(bob);
      }
    }
  }
}
