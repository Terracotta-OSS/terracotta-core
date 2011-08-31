/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Comparator;
import java.util.TreeMap;

public class TreeMapBuggyComparatorTestApp extends AbstractTransparentApp {

  private final TreeMap       map = new TreeMap(new BuggyComparator());
  private final CyclicBarrier barrier;

  public TreeMapBuggyComparatorTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      run0();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  public void run0() throws Exception {
    BuggyComparator cmp = (BuggyComparator) map.comparator();
    cmp.setBuggy(true);

    final boolean first;
    synchronized (map) {
      first = map.isEmpty();
      if (first) {
        map.put("key", "value");
      }
    }

    Assert.assertEquals(1, map.size());
    // Since our comparator is buggy, the map will not think it has the given key
    Assert.assertFalse(map.containsKey("key"));
    Assert.assertNull(map.remove("key"));

    barrier.barrier();

    cmp.setBuggy(false);
    Assert.assertTrue(map.containsKey("key"));
    Assert.assertEquals("value", map.get("key"));

    barrier.barrier();

    synchronized (map) {
      if (!map.isEmpty()) {
        Object removed = map.remove("key");
        Assert.assertEquals("value", removed);
        Assert.assertEquals(0, map.size());
      }
    }
  }

  private static class BuggyComparator implements Comparator {
    private boolean buggy;

    synchronized void setBuggy(boolean b) {
      this.buggy = b;
    }

    public int compare(Object o1, Object o2) {
      if (buggy) {
        return 1;
      } else {
        return ((Comparable) o1).compareTo(o2);
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = TreeMapBuggyComparatorTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("map", "map");
    spec.addRoot("barrier", "barrier");
    
    config.addIncludePattern(BuggyComparator.class.getName());
  }

}
