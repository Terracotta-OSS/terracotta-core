/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;

public class ArrayOfGarbageMapTestApp extends AbstractTransparentApp {

  private final static int            NUM_OF_LOOPS        = 200;
  private final static int            ENTRIES_IN_EACH_MAP = 1000;
  private final List<Map<Key, Value>> list                = new ArrayList<Map<Key, Value>>();
  private final CyclicBarrier         barrier;

  public ArrayOfGarbageMapTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    int index;
    try {
      index = this.barrier.await();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    Random rand = new Random(index * 1000000);
    for (int i = 0; i < NUM_OF_LOOPS; i++) {
      Map<Key, Value> val = createMap();
      synchronized (list) {
        if (rand.nextBoolean()) {
          list.add(val);
        } else {
          list.clear();
        }
      }
      if (i % 10 == 0) {
        System.out.println("XXXXX loop " + i);
      }
    }
  }

  private Map<Key, Value> createMap() {
    Map<Key, Value> val = new HashMap<Key, Value>();

    for (int i = 0; i < ENTRIES_IN_EACH_MAP; i++) {
      val.put(new Key(i), new Value(i));
    }

    return val;
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = ArrayOfGarbageMapTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);

    new SynchronizedIntSpec().visit(visitor, config);

    spec.addRoot("list", "list");
    spec.addRoot("barrier", "barrier");

    String verifyExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(verifyExpression);
  }

  private static class Key {

    private final int key;

    public Key(int val) {
      this.key = val;
    }

    @Override
    public int hashCode() {
      return key;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Key other = (Key) obj;
      if (key != other.key) return false;
      return true;
    }

  }

  private static class Value {

    private final int val;

    public Value(int val) {
      this.val = val;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Value other = (Value) obj;
      if (val != other.val) return false;
      return true;
    }

  }
}
