/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class HashSetSubclassTestApp extends AbstractTransparentApp {

  private final CyclicBarrier barrier;

  public HashSetSubclassTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      barrier.barrier();

      List hellos = new ArrayList();

      synchronized (hellos) {
        Set abc = new HashSet();
        abc.add("A");
        abc.add("B");
        abc.add("C");
        hellos.add(abc);
        new MySet("xxx", abc);
      }

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = HashSetSubclassTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");

    spec.addRoot("barrier", "barrier");
  }

  class MySet extends HashSet {

    protected String something;

    public MySet(String something, Collection c) {
      super(c.size());
      Iterator iter = c.iterator();
      while (iter.hasNext()) {
        super.add(iter.next());
      }
      this.something = something;
    }
  }

}
