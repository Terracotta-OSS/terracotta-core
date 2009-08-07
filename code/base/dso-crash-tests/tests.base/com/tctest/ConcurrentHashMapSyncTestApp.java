/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

// import com.tc.object.bytecode.Manageable;
// import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

public class ConcurrentHashMapSyncTestApp extends AbstractTransparentApp {

  private final DataKey[]           keyRoots   = new DataKey[] { new DataKey(1), new DataKey(2), new DataKey(3),
      new DataKey(4)                          };
  private final DataValue[]         valueRoots = new DataValue[] { new DataValue(10), new DataValue(20),
      new DataValue(30), new DataValue(40)    };

  private final CyclicBarrier       barrier;
  private final ConcurrentHashMap   mapRoot    = new ConcurrentHashMap();

  private final HashKey             myKey      = new HashKey(101);
  private final LinkedList<Integer> sharedList = new LinkedList<Integer>();

  public ConcurrentHashMapSyncTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

      testConcurrentSync(index);
      testConcurrentPingpong(index);

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  /**
   * Multiple threads to change shared at different parts object cocurrently.
   */
  private void testConcurrentSync(int index) throws Exception {

    // make change to mapRoot concurrently
    synchronized (mapRoot) {
      mapRoot.put(keyRoots[index % 4], valueRoots[index % 4]);
    }

    // threads sync here
    barrier.await();

    // verify
    Map newMap = new HashMap();
    newMap.put(keyRoots[0], valueRoots[0]);
    newMap.put(keyRoots[1], valueRoots[1]);
    newMap.put(keyRoots[2], valueRoots[2]);
    newMap.put(keyRoots[3], valueRoots[3]);

    assertMappingsEqual(newMap, mapRoot);

    barrier.await();
  }

  /**
   * Even threads to make number even and odd threads to make number odd. Uses a LinkedList to verify correct
   * synchronization behaving.
   */
  private void testConcurrentPingpong(int index) throws Exception {

    int upbound = 1000;

    if (index == 0) {
      synchronized (sharedList) {
        sharedList.clear();
      }
      synchronized (mapRoot) {
        mapRoot.put(myKey, new DataValue(0));
        sharedList.add(0);
      }
      // int d = ((DataValue)mapRoot.get(myKey)).getInt();
      // System.out.println("*** Init data=["+d+"]");
    }

    barrier.await();

    if ((index % 2) == 0) { // this one makes number even
      boolean done = false;
      int d;
      while (!done) {
        synchronized (mapRoot) {
          if ((d = ((DataValue) mapRoot.get(myKey)).getInt()) < upbound) {
            if ((d % 2) != 0) {
              mapRoot.put(myKey, new DataValue(++d));
              sharedList.add(d);
              // System.out.println("*** Thread["+index+"] value="+d);
              mapRoot.notifyAll();
            }
            if (d < upbound) mapRoot.wait();
          } else {
            done = true;
          }
        }
        Thread.sleep((int) (Math.random() * 10));
      }
    }

    if ((index % 2) != 0) { // this one makes number odd
      boolean done = false;
      int d;
      while (!done) {
        synchronized (mapRoot) {
          if ((d = ((DataValue) mapRoot.get(myKey)).getInt()) < upbound) {
            if ((d % 2) == 0) {
              mapRoot.put(myKey, new DataValue(++d));
              sharedList.add(d);
              // System.out.println("*** Thread["+index+"] value="+d);
              mapRoot.notifyAll();
            }
            if (d < upbound) mapRoot.wait();
          } else {
            done = true;
          }
        }
        Thread.sleep((int) (Math.random() * 10));
      }
    }

    barrier.await();

    // verify
    if (index == 0) {
      for (int i = 0; i < upbound; ++i) {
        // System.out.println("*** Verify["+i+"] value="+sharedList.get(i));
        Assert.assertTrue(i == sharedList.get(i));
      }
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
    String testClass = ConcurrentHashMapSyncTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("mapRoot", "mapRoot");
    spec.addRoot("sharedRoot", "sharedRoot");
    spec.addRoot("keyRoots", "keyRoots");
    spec.addRoot("valueRoots", "valueRoots");
    spec.addRoot("sharedList", "sharedList");
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

}
