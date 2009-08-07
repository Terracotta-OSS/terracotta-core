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
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.lang.reflect.Field;
import java.util.HashMap;

public class RootReplacementTestApp extends AbstractTransparentApp {

  private final SyncRoot      syncRoot                  = new SyncRoot();

  private static Integer      staticIntegerRoot;
  private static int          staticPrimitiveIntRoot;
  private static SyncRoot     staticSyncRoot;

  private boolean             primitiveBoolean          = true;

  private Integer             integerRoot;
  private int                 primitiveIntRoot;

  private Integer             nonReplaceableIntegerRoot = new Integer(15);
  private int                 nonReplaceableIntRoot     = 15;

  private final int           nonSharedPrimitiveInt     = 45;
  private final int           sharedPrimitiveInt        = 50;

  private final Integer       nonSharedIntegerObject    = new Integer(45);
  private final Integer       sharedIntegerObject       = new Integer(50);

  private SyncRoot            replaceableSyncRoot       = new SyncRoot(5);
  private SyncRoot            nonReplaceableSyncRoot    = new SyncRoot(15);

  private final SyncRoot      nonSharedSyncRoot         = new SyncRoot(45);
  private final SyncRoot      sharedSyncRoot            = new SyncRoot(50);

  private Class               classRoot;

  private final CyclicBarrier barrier;

  public RootReplacementTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      testBooleanChange();

      // testRootCreateOrReplace has been moved to the DsoFinalMethodTest.

      testStaticRootSetting();
      testMultipleClientRootSetting();
      testPrimitiveRootSetting();
      testLiteralRootSetting();
      testClassRootSetting();
      testObjectRootSetting(); // after this test, nonSharedSyncRoot will become shared.
      testRootSettingThroughReflection();
      testNonReplaceableSetting();

      testPrimitiveIntIncrement();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testClassRootSetting() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      syncRoot.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      classRoot = SyncRoot.class;
    }

    barrier.barrier();

    Object o = classRoot.newInstance();

    Assert.eval(o instanceof SyncRoot);

    barrier.barrier();

    if (index == 1) {
      classRoot = HashMap.class;
    }

    barrier.barrier();

    o = classRoot.newInstance();

    Assert.eval(o instanceof HashMap);

    barrier.barrier();
  }

  private void testPrimitiveIntIncrement() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      syncRoot.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      int NUM_OF_COUNT = 5000;
      long startTime = System.currentTimeMillis();
      int i = 0;
      while (i < NUM_OF_COUNT) {
        i++;
      }
      long endTime = System.currentTimeMillis();

      System.err.println("Elapsed time for non-shared primitive int: " + (endTime - startTime) + "ms");

      startTime = System.currentTimeMillis();
      primitiveIntRoot = 0;
      i = 0;
      while (primitiveIntRoot < NUM_OF_COUNT) {
        Assert.assertEquals(i, primitiveIntRoot);
        primitiveIntRoot++;
        i++;
      }
      endTime = System.currentTimeMillis();

      System.err.println("Elapsed time for replaceable int root: " + (endTime - startTime) + "ms");
    }
  }

  private void testBooleanChange() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      syncRoot.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      primitiveBoolean = false;
    }

    barrier.barrier();
    Assert.assertEquals(false, primitiveBoolean);
    barrier.barrier();
  }

  private void testMultipleClientRootSetting() throws Exception {
    clear();
    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      syncRoot.setIndex(index + 1);
    }

    barrier.barrier();

    Assert.assertEquals(0, primitiveIntRoot);

    barrier.barrier();

    if (index == 0) {
      primitiveIntRoot = 10;
    }

    barrier.barrier();

    if (index == 1) {
      primitiveIntRoot = 20;
    }

    barrier.barrier();

    Assert.assertEquals(20, primitiveIntRoot);

    barrier.barrier();
  }

  private void testStaticRootSetting() throws Exception {
    clear();
    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      syncRoot.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      staticPrimitiveIntRoot = 10;
    }

    barrier.barrier();

    Assert.assertEquals(10, staticPrimitiveIntRoot);

    barrier.barrier();

    if (index == 1) {
      staticPrimitiveIntRoot = 20;
    }

    barrier.barrier();

    Assert.assertEquals(20, staticPrimitiveIntRoot);

    barrier.barrier();

    if (index == 0) {
      staticIntegerRoot = new Integer(10);
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(10), staticIntegerRoot);

    barrier.barrier();

    if (index == 1) {
      staticIntegerRoot = new Integer(20);
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(20), staticIntegerRoot);

    barrier.barrier();

    if (index == 0) {
      staticSyncRoot = new SyncRoot(10);
    }

    barrier.barrier();

    Assert.assertEquals(10, staticSyncRoot.getValue());

    barrier.barrier();

    if (index == 1) {
      staticSyncRoot = new SyncRoot(20);
    }

    barrier.barrier();

    Assert.assertEquals(20, staticSyncRoot.getValue());

    barrier.barrier();
  }

  private void testRootSettingThroughReflection() throws Exception {
    clear();
    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      syncRoot.setIndex(index + 1);
    }

    barrier.barrier();

    Field primitiveField = this.getClass().getDeclaredField("primitiveIntRoot");
    primitiveField.setAccessible(true);

    Field integerField = this.getClass().getDeclaredField("integerRoot");
    integerField.setAccessible(true);

    Field objField = this.getClass().getDeclaredField("replaceableSyncRoot");
    objField.setAccessible(true);

    if (index == 0) {
      primitiveField.setInt(this, 11);
    }

    barrier.barrier();

    Assert.assertEquals(11, primitiveIntRoot);

    barrier.barrier();

    if (index == 1) {
      primitiveField.setInt(this, 13);
    }

    barrier.barrier();

    Assert.assertEquals(13, primitiveIntRoot);

    barrier.barrier();

    if (index == 0) {
      integerField.set(this, new Integer(11));
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(11), integerRoot);

    barrier.barrier();

    if (index == 1) {
      integerField.set(this, new Integer(13));
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(13), integerRoot);

    barrier.barrier();

    if (index == 0) {
      objField.set(this, new SyncRoot(11));
    }

    barrier.barrier();

    Assert.assertEquals(11, replaceableSyncRoot.getValue());

    barrier.barrier();

    if (index == 1) {
      objField.set(this, new SyncRoot(13));
    }

    barrier.barrier();

    Assert.assertEquals(13, replaceableSyncRoot.getValue());

    barrier.barrier();

    if (index == 2) {
      objField.set(this, sharedSyncRoot);
    }

    barrier.barrier();

    Assert.assertEquals(50, replaceableSyncRoot.getValue());

    barrier.barrier();

    sharedSyncRoot.setValue(53);

    Assert.assertEquals(53, replaceableSyncRoot.getValue());

    barrier.barrier();
  }

  private void testNonReplaceableSetting() throws Exception {
    clear();
    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      syncRoot.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      nonReplaceableIntRoot = 20;
    }

    barrier.barrier();

    Assert.assertEquals(15, nonReplaceableIntRoot);

    barrier.barrier();

    if (index == 0) {
      nonReplaceableIntegerRoot = new Integer(20);
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(15), nonReplaceableIntegerRoot);

    barrier.barrier();

    if (index == 0) {
      nonReplaceableSyncRoot = new SyncRoot(20);
    }

    barrier.barrier();

    Assert.assertEquals(15, nonReplaceableSyncRoot.getValue());

    barrier.barrier();
  }

  private void testObjectRootSetting() throws Exception {
    clear();
    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      syncRoot.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      replaceableSyncRoot = new SyncRoot(10);
    }

    barrier.barrier();

    Assert.assertEquals(10, replaceableSyncRoot.getValue());

    barrier.barrier();

    if (index == 0) {
      replaceableSyncRoot = sharedSyncRoot;
    }

    barrier.barrier();

    Assert.assertEquals(50, replaceableSyncRoot.getValue());

    barrier.barrier();

    if (index == 0) {
      replaceableSyncRoot.setValue(51);
    }

    barrier.barrier();

    Assert.assertEquals(51, sharedSyncRoot.getValue());

    barrier.barrier();

    if (index == 0) {
      replaceableSyncRoot = nonSharedSyncRoot;
    }

    barrier.barrier();

    if (index == 0) {
      sharedSyncRoot.setValue(52);
    }

    barrier.barrier();

    Assert.assertEquals(52, sharedSyncRoot.getValue());
    Assert.assertEquals(45, replaceableSyncRoot.getValue());

    barrier.barrier();

    if (index == 0) {
      nonSharedSyncRoot.setValue(47);
    }

    barrier.barrier();

    Assert.assertEquals(47, replaceableSyncRoot.getValue());

    barrier.barrier();

    if (index == 0) { // reset the value of sharedSyncRoot and nonSharedSyncRoot.
      sharedSyncRoot.setValue(50);
      nonSharedSyncRoot.setValue(45);
    }

    barrier.barrier();
  }

  private void testLiteralRootSetting() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      syncRoot.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      integerRoot = new Integer(10);
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(10), integerRoot);

    barrier.barrier();

    if (index == 0) {
      integerRoot = sharedIntegerObject;
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(50), integerRoot);

    barrier.barrier();

    if (index == 0) {
      integerRoot = nonSharedIntegerObject;
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(45), integerRoot);

    barrier.barrier();
  }

  private void testPrimitiveRootSetting() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      syncRoot.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      primitiveIntRoot = 10;
    }

    barrier.barrier();

    Assert.assertEquals(10, primitiveIntRoot);

    barrier.barrier();

    if (index == 0) {
      primitiveIntRoot = sharedPrimitiveInt;
    }

    barrier.barrier();

    Assert.assertEquals(50, primitiveIntRoot);

    barrier.barrier();

    if (index == 0) {
      primitiveIntRoot = nonSharedPrimitiveInt;
    }

    barrier.barrier();

    Assert.assertEquals(45, primitiveIntRoot);

    barrier.barrier();

    if (index == 0) {
      primitiveIntRoot++;
    }

    barrier.barrier();

    Assert.assertEquals(46, primitiveIntRoot);

    barrier.barrier();

    if (index == 0) {
      primitiveIntRoot += 3;
    }

    barrier.barrier();

    Assert.assertEquals(49, primitiveIntRoot);

    barrier.barrier();

    if (index == 0) {
      primitiveIntRoot -= 2;
    }

    barrier.barrier();

    Assert.assertEquals(47, primitiveIntRoot);

    barrier.barrier();

    if (index == 0) {
      primitiveIntRoot--;
    }

    barrier.barrier();

    Assert.assertEquals(46, primitiveIntRoot);

    barrier.barrier();
  }

  private void clear() throws Exception {
    synchronized (syncRoot) {
      syncRoot.clear();
    }

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = RootReplacementTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("primitiveBoolean", "primitiveBoolean");

    spec.addRoot("primitiveIntRoot", "primitiveIntRoot");
    spec.addRoot("integerRoot", "integerRoot", false);
    spec.addRoot("nonReplaceableIntRoot", "nonReplaceableIntRoot", true);
    spec.addRoot("nonReplaceableIntegerRoot", "nonReplaceableIntegerRoot");

    spec.addRoot("sharedIntegerObject", "sharedIntegerObject");
    spec.addRoot("sharedPrimitiveInt", "sharedPrimitiveInt", true);

    spec.addRoot("replaceableSyncRoot", "replaceableSyncRoot", false);
    spec.addRoot("nonReplaceableSyncRoot", "nonReplaceableSyncRoot");
    spec.addRoot("sharedSyncRoot", "sharedSyncRoot");

    spec.addRoot("classRoot", "classRoot", false);

    spec.addRoot("staticIntegerRoot", "staticIntegerRoot", false);
    spec.addRoot("staticPrimitiveIntRoot", "staticPrimitiveIntRoot");
    spec.addRoot("staticSyncRoot", "staticSyncRoot", false);

    spec.addRoot("syncRoot", "syncRoot");

    spec.addRoot("barrier", "barrier");
  }

  private static class SyncRoot {
    private int index = 0;
    private int value = 0;

    public SyncRoot() {
      super();
    }

    public SyncRoot(int value) {
      this.value = value;
    }

    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    public synchronized int getValue() {
      return value;
    }

    public synchronized void setValue(int value) {
      this.value = value;
    }

    public void clear() {
      this.index = 0;
      this.value = 0;
    }
  }

}
