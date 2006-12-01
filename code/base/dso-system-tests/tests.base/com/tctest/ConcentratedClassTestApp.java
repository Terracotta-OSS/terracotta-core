/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ConcentratedClassTestApp extends AbstractTransparentApp {
  private static final int THREAD_COUNT = 2;
  private static Map       root         = new HashMap();

  public ConcentratedClassTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    if (getParticipantCount() < 2) {
      // this test needs to have transactions coming in from other nodes (such that reflection based field set()'ing
      // occurs as DNA is applied locally
      throw new RuntimeException("Need at least 2 participants for this test");
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ConcentratedClassTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("root", "lock");
    config.addIncludePattern(ConcentratedClass.class.getName());
  }

  private void threadEntry(String id, int count) {
    ConcentratedClass cc = new ConcentratedClass();

    synchronized (root) {
      final Object prev = root.put(id, cc);
      if (prev != null) { throw new RuntimeException("replaced an entry in the map for id " + id); }

      if (root.size() == count) {
        root.notifyAll();
      } else {
        while (root.size() != count) {
          try {
            root.wait();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }

    Collection values;
    synchronized (root) {
      values = root.values();
    }

    Set all = new HashSet();

    for (int i = 0; i < 10; i++) {
      final ConcentratedClass prevCC;
      final ConcentratedClass newCC = new ConcentratedClass();
      all.add(newCC);

      synchronized (root) {
        prevCC = (ConcentratedClass) root.put(id, newCC);
        values = root.values();
      }

      if (values.size() != count) { throw new RuntimeException("unexpected size: " + values.size()); }

      all.add(prevCC);
      for (Iterator iter = all.iterator(); iter.hasNext();) {
        ConcentratedClass someCC = (ConcentratedClass) iter.next();
        someCC.increment();
      }
    }
  }

  public void run() {
    final SynchronizedRef error = new SynchronizedRef(null);
    final String id = getApplicationId();

    Thread threads[] = new Thread[THREAD_COUNT];
    for (int i = 0; i < threads.length; i++) {
      final int cnt = i;
      threads[i] = new Thread(new Runnable() {
        public void run() {
          try {
            threadEntry(id + cnt, getParticipantCount() * THREAD_COUNT);
          } catch (Throwable t) {
            error.set(t);
          }
        }
      });
    }

    for (int i = 0; i < threads.length; i++) {
      threads[i].start();
    }

    for (int i = 0; i < threads.length; i++) {
      try {
        threads[i].join(5000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    Throwable t = (Throwable) error.get();
    if (t != null) { throw new RuntimeException(t); }
  }

  private static class ConcentratedClass {
    // Leave these fields private! This test is trying to shake out concurrency problems set/get()'ing private
    // fields via reflection in GenericTCField
    private int counter1 = 0;
    private int counter2 = 0;

    synchronized int increment() {
      counter1++;
      counter2++;
      return counter1;
    }
  }

}