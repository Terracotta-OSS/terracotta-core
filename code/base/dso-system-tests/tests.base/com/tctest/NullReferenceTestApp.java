/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ITransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class NullReferenceTestApp extends AbstractTransparentApp {

  private final List nodes = new ArrayList();
  private Holder     holder;

  public NullReferenceTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = NullReferenceTestApp.class.getName();
    ITransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("nodes", "nodesLock");
    spec.addRoot("holder", "holderLock");

    String methodExpression;

    methodExpression = "* " + testClass + ".init()";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClass + ".check()";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClass + "$Holder.*(..)";
    config.addWriteAutolock(methodExpression);
    
    config.addIncludePattern(Holder.class.getName());
  }

  public void run() {
    init();
    check();
    finish();
  }

  private void finish() {
    holder.mod();
  }

  private void check() {
    holder.check();

    synchronized (nodes) {
      nodes.remove(0);
      nodes.notifyAll();

      while (nodes.size() > 0) {
        try {
          nodes.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private void init() {
    synchronized (nodes) {
      Holder h = new Holder();
      this.holder = h;

      // Make sure we're actually using the distributed version of the "holder" object.
      // This nonsense wouldn't be necessary if GETFIELD on roots was instrumented
      if (nodes.size() == 0) {
        Assert.assertTrue(this.holder == h);
      } else {
        Assert.assertTrue(this.holder != h);
      }

      nodes.add(new Object());
      nodes.notifyAll();

      while (nodes.size() != getParticipantCount()) {
        try {
          nodes.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

  }

  private static class Holder {
    Object      reference = null;
    Object[]    array     = new Object[] { null, new Object(), null };

    Map         map1      = new HashMap();
    Map         map2      = new IdentityHashMap();

    List        list1     = new LinkedList();
    List        list2     = new ArrayList();
    List        list3     = new Vector();

    Set         set1      = new HashSet();

    private int count     = 0;

    Holder() {
      mod();
    }

    public void check() {
      Assert.assertNull(reference);
      Assert.assertNull(array[0]);
      Assert.assertNotNull(array[1]);
      Assert.assertNull(array[2]);

      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(1, list2.size());
      Assert.assertEquals(1, list3.size());
      Assert.assertTrue(list1.contains(null));
      Assert.assertTrue(list2.contains(null));
      Assert.assertTrue(list3.contains(null));

      Assert.assertEquals(2, map1.size());
      Assert.assertEquals(2, map2.size());
      Assert.assertTrue(map1.containsKey(null));
      Assert.assertTrue(map2.containsKey(null));
      Assert.assertTrue(map1.containsValue(null));
      Assert.assertTrue(map2.containsValue(null));

      Assert.assertEquals(1, set1.size());
      Assert.assertTrue(set1.contains(null));
    }

    synchronized void mod() {
      modSets(new Set[] { set1 });
      modLists(new List[] { list1, list2, list3 });
      modMaps(new Map[] { map1, map2 });
      reference = null;
      array[0] = null;
    }

    void modLists(List[] lists) {
      for (int i = 0; i < lists.length; i++) {
        lists[i].add(null);
      }
    }

    void modSets(Set[] sets) {
      for (int i = 0; i < sets.length; i++) {
        sets[i].add(null);
      }
    }

    void modMaps(Map[] maps) {
      for (int i = 0; i < maps.length; i++) {
        maps[i].put(null, "value for null key");
        maps[i].put("key" + count + " for null value", null);
      }
      count++;
    }

  }

}
