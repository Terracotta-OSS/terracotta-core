/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

public class MapOfCollectionsTestApp extends AbstractTransparentApp {

  private final ConcurrentHashMap<Key, Map<Key, Value>> chm        = new ConcurrentHashMap<Key, Map<Key, Value>>();
  private final CyclicBarrier                           barrier;
  private final static int                              RANGE      = 10000;
  private final static int                              SIZE       = 50;
  private final static int                              LOOP_COUNT = 4000;

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MapOfCollectionsTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);
    spec.addRoot("chm", "chm");
    spec.addRoot("barrier", "barrier");

    config.addIncludePattern(Key.class.getName() + "$*", false, false, true);
    config.addIncludePattern(Value.class.getName() + "$*", false, false, true);
    new SynchronizedIntSpec().visit(visitor, config);
    new CyclicBarrierSpec().visit(visitor, config);
  }

  public MapOfCollectionsTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
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
    System.out.println("started client " + index);

    Random rand = new Random(index * 10000);
    for (int i = 0; i < LOOP_COUNT; i++) {
      int next = rand.nextInt(RANGE);
      if (rand.nextBoolean()) {
        Map<Key, Value> val = createMap(index);
        chm.put(new Key(next), val);
      } else {
        Map<Key, Value> ret = chm.remove(new Key(next));
        if (ret != null) {
          verifyMap(ret);
        }
      }

      if (i % 1000 == 0) {
        System.out.println("completed loop " + i);
      }
      ThreadUtil.reallySleep(10);
    }
  }

  private Map<Key, Value> createMap(int index) {
    Map<Key, Value> val = new HashMap<Key, Value>();
    val.put(new Key(Integer.MAX_VALUE), new Value(index));

    for (int i = 0; i < SIZE; i++) {
      val.put(new Key(i * index), new Value(i * index));
    }

    return val;
  }

  private void verifyMap(Map<Key, Value> map) {
    int index = map.get(new Key(Integer.MAX_VALUE)).getVal();

    Assert.assertEquals(createMap(index), map);
  }

  private static class Value {

    private final int val;

    public int getVal() {
      return val;
    }

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
}
