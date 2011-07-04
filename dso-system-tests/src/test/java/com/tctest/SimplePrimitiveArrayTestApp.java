/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.exception.TCRuntimeException;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SimplePrimitiveArrayTestApp extends AbstractTransparentApp {

  private ArrayRoot           root;
  private final CyclicBarrier barrier;

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = SimplePrimitiveArrayTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "the-data-root-yo");
    spec.addRoot("barrier", "barrier");
    config.addIncludePattern(ArrayRoot.class.getName());
    new CyclicBarrierSpec().visit(visitor, config);
  }

  public SimplePrimitiveArrayTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int length = new Random().nextInt(100);
      root = new ArrayRoot(getParticipantCount(), length);
      Double[] original = root.get();

      ArrayRoot.validateWithEquals(1, original);
      ArrayRoot.validate(1, original);
      barrier.barrier();

      ArrayRoot.modify(2, original);
      barrier.barrier();

      root.validateWithEquals(2);
      root.validate(2);
      barrier.barrier();

      Double[] toCopyFrom = new Double[original.length];
      for (int i = 0; i < toCopyFrom.length; i++) {
        toCopyFrom[i] = new Double(3);
      }

      synchronized (original) {
        System.arraycopy(toCopyFrom, 0, original, 0, toCopyFrom.length);
      }

      barrier.barrier();
      root.validateWithEquals(3);
      root.validate(3);

    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }

  }

  private static final class ArrayRoot {
    private final List arrays;
    private int        index;

    public ArrayRoot(int count, int length) {
      arrays = new ArrayList();
      for (int i = 0; i < count; i++) {
        Double[] sub = new Double[length];
        arrays.add(sub);
        for (int j = 0; j < sub.length; j++) {
          sub[j] = new Double(1);
        }
      }
    }

    public synchronized Double[] get() {
      return (Double[]) arrays.get(index++);
    }

    public static void modify(double newValue, Double[] array) {
      synchronized (array) {
        for (int i = 0; i < array.length; i++) {
          array[i] = new Double(newValue);
        }
      }
    }

    public synchronized void validate(double expectedValue) {
      for (Iterator i = arrays.iterator(); i.hasNext();) {
        validate(expectedValue, (Double[]) i.next());
      }
    }

    public synchronized void validateWithEquals(double expectedValue) {
      for (Iterator i = arrays.iterator(); i.hasNext();) {
        validateWithEquals(expectedValue, (Double[]) i.next());
      }
    }

    public static void validate(double expectedValue, Double[] array) {
      synchronized (array) {
        for (int i = 0; i < array.length; i++) {
          double value = array[i].doubleValue();
          if (expectedValue != value) { throw new RuntimeException("Expected " + expectedValue + " but was " + value); }
        }
      }
    }

    private static void validateWithEquals(double expectedValue, Double[] array) {
      Double[] compare = new Double[array.length];
      for (int i = 0; i < compare.length; i++) {
        compare[i] = new Double(expectedValue);
      }
      if (!Arrays.equals(compare, array)) throw new RuntimeException("Arrays aren't equal!");
    }

  }

}
