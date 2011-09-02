/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class DistributedMethodCallTestApp extends AbstractTransparentApp {

  private final SharedModel   model          = new SharedModel();
  private final CyclicBarrier sharedBarrier  = new CyclicBarrier(getParticipantCount());
  private final CyclicBarrier sharedBarrier2 = new CyclicBarrier(getParticipantCount());

  public DistributedMethodCallTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    try {
      runTest();
      runTestWithNulls();
      runTestNested();
      runTestNonVoid();
      runTestWithParamChange();
      // runTestWithSynchAndNested();
    } catch (Throwable e) {
      notifyError(e);
    }
  }

  private void runTest() throws Throwable {
    final boolean callInitiator = sharedBarrier.await() == 0;

    if (callInitiator) {
      model.callCount.set(0);
      synchronized (model) {
        FooObject[][] foos = makeFooArray();
        int[][][] ints = makeIntArray();
        model.addObject(new FooObject(), 1, 2, foos, ints, true);
      }
    }
    sharedBarrier.await();
    final int actual = model.callCount.get();
    if (actual != getParticipantCount()) {
      notifyError("Unexpected call count: expected=" + getParticipantCount() + ", actual=" + actual);
    }
  }

  private void runTestWithParamChange() throws Throwable {
    final boolean callInitiator = sharedBarrier.await() == 0;

    if (callInitiator) {
      model.callCount.set(0);
      synchronized (model) {
        FooObject[][] foos = makeFooArray();
        int[][][] ints = makeIntArray();
        model.addObjectWithParamChange(new FooObject(), 1, 2, foos, ints, true);
      }
    }
    sharedBarrier.await();
    final int actual = model.callCount.get();
    if (actual != getParticipantCount()) {
      notifyError("Unexpected call count: expected=" + getParticipantCount() + ", actual=" + actual);
    }
  }

  private void runTestNonVoid() throws Throwable {
    final boolean callInitiator = sharedBarrier.await() == 0;

    if (callInitiator) {
      model.callCount.set(0);
      synchronized (model) {
        FooObject[][] foos = makeFooArray();
        int[][][] ints = makeIntArray();
        model.addObjectNonVoid(new FooObject(), 1, 2, foos, ints, true);
      }
    }
    sharedBarrier.await();
    final int actual = model.callCount.get();
    if (actual != getParticipantCount()) {
      notifyError("Unexpected call count: expected=" + getParticipantCount() + ", actual=" + actual);
    }
  }

  private void runTestNested() throws Throwable {
    final boolean callInitiator = sharedBarrier.await() == 0;

    if (callInitiator) {
      model.callCount.set(0);
      synchronized (model) {
        FooObject[][] foos = makeFooArray();
        int[][][] ints = makeIntArray();
        model.addObjectNested(new FooObject(), 1, 2, foos, ints, true);
      }
    }
    sharedBarrier.await();
    final int actual = model.callCount.get();
    if (actual != getParticipantCount()) {
      notifyError("Unexpected call count: expected=" + getParticipantCount() + ", actual=" + actual);
    }
  }

  private void runTestWithNulls() throws Throwable {
    final boolean callInitiator = sharedBarrier.await() == 0;

    if (callInitiator) {
      model.callCount.set(0);
      synchronized (model) {
        model.addObjectWithNulls(null, 1, 2, null, null, true);
      }
    }
    sharedBarrier.await();
    final int actual = model.callCount.get();
    if (actual != getParticipantCount()) {
      notifyError("Unexpected call count: expected=" + getParticipantCount() + ", actual=" + actual);
    }
  }

  private static int[][][] makeIntArray() {
    int[][][] ints = new int[6][8][9];
    int count = 0;
    for (int i = 0; i < ints.length; i++) {
      int[][] array1 = ints[i];
      for (int j = 0; j < array1.length; j++) {
        int[] array2 = array1[j];
        for (int k = 0; k < array2.length; k++) {
          array2[k] = count++;
        }
      }
    }
    return ints;
  }

  private static FooObject[][] makeFooArray() {
    FooObject[][] foos = new FooObject[2][3];
    for (int i = 0; i < foos.length; i++) {
      Arrays.fill(foos[i], new FooObject());
    }
    return foos;
  }

  public class SharedModel {
    public final AtomicInteger callCount = new AtomicInteger(0);

    public synchronized void addObjectSynched(Object obj, int i, double d, FooObject[][] foos, int[][][] ints, boolean b)
        throws Throwable {
      this.addObjectSynched(obj, i, d, foos, ints, b);
    }

    public String addObjectNonVoid(Object obj, int i, double d, FooObject[][] foos, int[][][] ints, boolean b)
        throws Throwable {
      addObject(obj, i, d, foos, ints, b);
      return new String("A-OK");
    }

    public void addObject(Object obj, int i, double d, FooObject[][] foos, int[][][] ints, boolean b) throws Throwable {
      int countCall = callCount.incrementAndGet();

      // The test has been failing since eterninty (MNK-18 was the first)
      // Printing stack trace to know why the distributed method was called
      // more than number of client times.
      System.out.println("XXXXXXX callCount: " + countCall);
      new Exception().printStackTrace();
      // Everything in the "foos" array should be non-null
      for (int index = 0; index < foos.length; index++) {
        FooObject[] array = foos[index];
        for (int j = 0; j < array.length; j++) {
          FooObject foo = array[j];
          if (foo == null) notifyError("foo == null");
        }
      }

      // access all the "ints"
      int count = 0;
      for (int index = 0; index < ints.length; index++) {
        int[][] array1 = ints[index];
        for (int j = 0; j < array1.length; j++) {
          int[] array2 = array1[j];
          for (int k = 0; k < array2.length; k++) {
            int val = array2[k];
            if (count++ != val) notifyError("count ++ != val");
          }
        }
      }

      checkLiteralParams(i, d, b);
      sharedBarrier2.await();
    }

    public void addObjectWithParamChange(Object obj, int i, double d, FooObject[][] foos, int[][][] ints, boolean b)
        throws Throwable {
      callCount.incrementAndGet();
      // Everything in the "foos" array should be non-null
      for (int index = 0; index < foos.length; index++) {
        FooObject[] array = foos[index];
        for (int j = 0; j < array.length; j++) {
          FooObject foo = array[j];
          if (foo == null) notifyError("foo == null");
        }
      }

      // access all the "ints"
      int count = 0;
      for (int index = 0; index < ints.length; index++) {
        int[][] array1 = ints[index];
        for (int j = 0; j < array1.length; j++) {
          int[] array2 = array1[j];
          for (int k = 0; k < array2.length; k++) {
            int val = array2[k];
            if (count++ != val) notifyError("count ++ != val");
          }
        }
      }
      checkLiteralParams(i, d, b);
      // now re-assign all params...
      obj = null;
      i = -777;
      foos = null;
      ints = null;
      b = !b;
      sharedBarrier2.await();
    }

    public void addObjectWithNulls(Object obj, int i, double d, FooObject[][] foos, int[][][] ints, boolean b)
        throws Throwable {
      callCount.incrementAndGet();
      // all params should be nulls
      checkReferenceParams(obj, foos, ints, true);
      checkLiteralParams(i, d, b);
      sharedBarrier2.await();
    }

    public void addObjectNested(Object obj, int i, double d, FooObject[][] foos, int[][][] ints, boolean b)
        throws Throwable {
      addObject(obj, i, d, foos, ints, b);
    }

    private void checkLiteralParams(int i, double d, boolean b) {
      if (i != 1 || d != 2 || !b) {
        System.out.println("Invalid parameters: i:" + i + " d:" + d + " b:" + b);
        notifyError("Invalid parameters: i:" + i + " d:" + d + " b:" + b);
      }
    }

    private void checkReferenceParams(Object o, FooObject[][] foos, int[][][] ints, boolean nullExpected) {
      checkNull(o, nullExpected);
      checkNull(foos, nullExpected);
      checkNull(ints, nullExpected);
    }

    private void checkNull(Object o, boolean nullExpected) {
      final boolean isNull = o == null;
      if (isNull != nullExpected) notifyError("Wrong parameter value: null is expected, actual = " + o);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    try {
      new CyclicBarrierSpec().visit(visitor, config);
      new SynchronizedIntSpec().visit(visitor, config);

      TransparencyClassSpec spec = config.getOrCreateSpec(FooObject.class.getName());
      String testClassName = DistributedMethodCallTestApp.class.getName();
      spec = config.getOrCreateSpec(testClassName);
      spec.addTransient("callInitiator");
      spec.addRoot("model", "model");
      spec.addRoot("sharedStuff", "sharedStuff");
      spec.addRoot("sharedBarrier", "sharedBarrier");
      spec.addRoot("sharedBarrier2", "sharedBarrier2");
      String methodExpression = "* " + testClassName + "*.*(..)";
      config.addWriteAutolock(methodExpression);
      // methodExpression = "* " + DistributedMethodCallTestApp.SharedModel.class.getName() + "*.*(..)";
      // config.addWriteAutolock(methodExpression);

      spec = config.getOrCreateSpec(SharedModel.class.getName());
      spec.addDistributedMethodCall("addObjectNonVoid",
                                    "(Ljava/lang/Object;ID[[Lcom/tctest/FooObject;[[[IZ)java/lang/String;", true);
      spec.addDistributedMethodCall("addObjectWithNulls", "(Ljava/lang/Object;ID[[Lcom/tctest/FooObject;[[[IZ)V", true);
      spec
          .addDistributedMethodCall("addObjectWithParamChange", "(Ljava/lang/Object;ID[[Lcom/tctest/FooObject;[[[IZ)V", true);
      spec.addDistributedMethodCall("addObjectSynched", "(Ljava/lang/Object;ID[[Lcom/tctest/FooObject;[[[IZ)V", true);
      spec.addDistributedMethodCall("addObjectNested", "(Ljava/lang/Object;ID[[Lcom/tctest/FooObject;[[[IZ)V", true);
      spec.addDistributedMethodCall("addObject", "(Ljava/lang/Object;ID[[Lcom/tctest/FooObject;[[[IZ)V", true);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
