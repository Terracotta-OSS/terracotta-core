/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class TreeSetSimplifiedGCTestApp extends AbstractErrorCatchingTransparentApp {
  // roots
  private final CyclicBarrier   barrier     = new CyclicBarrier(getParticipantCount());
  private final Map             root        = new HashMap();

  public TreeSetSimplifiedGCTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    final int index = barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        TreeSet set = new TreeSet();
        set.add(new FooObject(0));
        set.add(new FooObject(1));
        set.add(new FooObject(2));
        set.add(new FooObject(3));
        root.put("key0", set);
      }
      synchronized(root) {
        TreeSet set = (TreeSet)root.get("key0");
        set.remove(new FooObject(3));
      }
      synchronized(root) {
        TreeSet set = (TreeSet)root.get("key0");
        Assert.assertEquals("Mutator see HashSet size", 3, set.size());
      }
    }
    barrier.barrier();
    if (index != 0) {
      synchronized(root) {
        TreeSet set = (TreeSet)root.get("key0");
        Assert.assertEquals("Reader see HashSet size", 3, set.size());
      }
    }
    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    new CyclicBarrierSpec().visit(visitor, config);
    config.getOrCreateSpec(FooObject.class.getName());

    String testClassName = TreeSetSimplifiedGCTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
    String methodExpression = "* " + testClassName + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");
  }


  private static final class FooObject implements Comparable {
    private final int id;

    public FooObject(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }

    public boolean equals(Object foo) {
      if (foo == null) { return false; }
      return ((FooObject) foo).getId() == id;
    }

    public int hashCode() {
      return id;
    }

    public int compareTo(Object o) {
      int othersId = ((FooObject) o).getId();
      if (id < othersId) {
        return -1;
      } else if (id == othersId) {
        return 0;
      } else {
        return 1;
      }
    }
  }
}
