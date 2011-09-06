/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TxReorderingApp extends AbstractTransparentApp {
  private final CyclicBarrier          barrier;
  private final ReentrantReadWriteLock rrwl;
  private final TreeSet<SomeData>      set;

  public TxReorderingApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(cfg.getGlobalParticipantCount());
    rrwl = new ReentrantReadWriteLock();
    set = new TreeSet<SomeData>(new SomeDataComparator());
  }

  public void run() {
    int id = waitOnBarrier();
    if (id == 0) {
      System.out.println("Popluating Data");
      rrwl.writeLock().lock();
      set.add(new SomeData(10));
      set.add(new SomeData(2));
      rrwl.writeLock().unlock();
    }

    waitOnBarrier();
    if (id == 1) {
      rrwl.readLock().lock();
      System.out.println("Reading 1st time: " + set);
      rrwl.readLock().unlock();
    }

    waitOnBarrier();
    if (id == 0) {
      rrwl.writeLock().lock();
      SomeData data = set.first();
      set.remove(data);
      data.setSomeData(100);
      set.add(data);
      System.out.println(set.first());
      rrwl.writeLock().unlock();
    }

    waitOnBarrier();
    if (id == 1) {
      rrwl.readLock().lock();
      System.out.println("Reading 2nd time: " + set);
      Assert.assertEquals(2, set.size());
      rrwl.readLock().unlock();
    }
  }

  private int waitOnBarrier() {
    int index = -1;
    try {
      index = barrier.await();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return index;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = TxReorderingApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(TxReorderingApp.class.getName());
    config.addIncludePattern(TxReorderingApp.SomeData.class.getName());
    config.addIncludePattern(TxReorderingApp.SomeDataComparator.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";

    spec.addRoot("barrier", "barrier");
    config.addWriteAutolock(methodExpression);
    spec.addRoot("rrwl", "rrwl");
    spec.addRoot("set", "set");
  }

  class SomeDataComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      SomeData obj1 = (SomeData) o1;
      SomeData obj2 = (SomeData) o2;
      if (obj1.i < obj2.i) return -1;
      else if (obj1.i == obj2.i) return 0;
      else return 1;
    }
  }

  class SomeData {
    private int i;

    public SomeData(int i) {
      this.i = i;
    }

    public void setSomeData(int i) {
      this.i = i;
    }

    public boolean equals(Object obj) {
      return this.i == ((SomeData) obj).i;
    }

    public int hashCode() {
      return i;
    }

    public String toString() {
      return "i = " + i;
    }
  }
}
