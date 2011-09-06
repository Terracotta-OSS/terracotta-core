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

public class LiteralValueRootTestApp extends AbstractTransparentApp {

  private final SyncRoot      syncRoot                   = new SyncRoot();
  private final SyncRoot      referenceRoot              = new SyncRoot();

  private String              stringRoot                 = null;
  private Integer             integerRoot                = null;
  private Long                longRoot                   = null;
  private Double              doubleRoot                 = null;
  private Float               floatRoot                  = null;
  private Byte                byteRoot                   = null;
  private Boolean             booleanRoot                = null;
  private Character           characterRoot              = null;

  private Class               classRoot                  = null;

  private final Integer       nonSharedIntegerObject     = new Integer(10);

  private int                 nonDefaultPrimitiveIntRoot = 5;
  private Integer             nonDefaultIntegerRoot      = new Integer(5);
  private final Integer       sharedIntegerObject        = new Integer(50);

  private int                 readBeforeSetTestIntRoot;
  private Integer             readBeforeSetTestIntegerRoot;

  private long                primitiveLongRoot;
  private int                 primitiveIntRoot;
  private float               primitiveFloatRoot;
  private double              primitiveDoubleRoot;
  private boolean             primitiveBooleanRoot;
  private byte                primitiveByteRoot;
  private char                primitiveCharRoot;

  private final CyclicBarrier barrier;

  public LiteralValueRootTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      testGetBeforeSetObject();
      testGetBeforeSetPrimitive();

      testDefaultPrimitiveValues();
      testMultipleRootSetting();

      testSharedPrimitiveIntRoot();
      testSharedPrimitiveLongRoot();
      testSharedPrimitiveDoubleRoot();
      testSharedPrimitiveFloatRoot();
      testSharedPrimitiveBooleanRoot();
      testSharedPrimitiveByteRoot();
      testSharedPrimitiveCharRoot();

      testSharedClassRoot();
      testSharedStringRoot();
      testSharedIntegerRoot();
      testSharedLongRoot();
      testSharedFloatRoot();
      testSharedDoubleRoot();
      testSharedByteRoot();
      testSharedBooleanRoot();
      testSharedCharacterRoot();
      testReferenceInequality();

      // testReferenceEquality();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testMultipleRootSetting() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        nonDefaultPrimitiveIntRoot = 10;
      }
    }

    barrier.barrier();

    Assert.assertEquals(10, nonDefaultPrimitiveIntRoot);

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        nonDefaultIntegerRoot = new Integer(10);
      }
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(10), nonDefaultIntegerRoot);

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        nonDefaultIntegerRoot = sharedIntegerObject;
      }
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(50), nonDefaultIntegerRoot);

    barrier.barrier();

  }

  private void testDefaultPrimitiveValues() throws Exception {
    Assert.assertEquals(0, primitiveIntRoot);
    Assert.assertEquals(0L, primitiveLongRoot);
    Assert.assertEquals(0.0D, primitiveDoubleRoot);
    Assert.assertEquals(0.0F, primitiveFloatRoot);
    Assert.assertEquals((byte) 0, primitiveByteRoot);
    Assert.assertFalse(primitiveBooleanRoot);
    barrier.barrier();
  }

  private void testGetBeforeSetObject() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    Assert.assertNull(readBeforeSetTestIntegerRoot);

    barrier.barrier();

    if (index == 0) {
      while (readBeforeSetTestIntegerRoot == null) {
        if (readBeforeSetTestIntegerRoot != null) {
          break;
        }
      }
    } else {
      synchronized (syncRoot) {
        readBeforeSetTestIntegerRoot = new Integer(100);
      }
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(100), readBeforeSetTestIntegerRoot);

    barrier.barrier();
  }

  private void testGetBeforeSetPrimitive() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    Assert.assertEquals(0, readBeforeSetTestIntRoot);

    barrier.barrier();

    if (index == 0) {
      while (readBeforeSetTestIntRoot == 0) {
        if (readBeforeSetTestIntRoot != 0) {
          break;
        }
      }
    } else {
      synchronized (syncRoot) {
        readBeforeSetTestIntRoot = 10;
      }
    }

    barrier.barrier();

    Assert.assertEquals(10, readBeforeSetTestIntRoot);

    barrier.barrier();
  }

  private void testSharedPrimitiveLongRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        primitiveLongRoot = 32L;
      }
    }

    barrier.barrier();

    Assert.assertEquals(32L, primitiveLongRoot);

    barrier.barrier();

  }

  private void testSharedPrimitiveIntRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        primitiveIntRoot = 32;
      }
    }

    barrier.barrier();

    Assert.assertEquals(32, primitiveIntRoot);

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        primitiveIntRoot++;
      }
    }

    barrier.barrier();

    Assert.assertEquals(33, primitiveIntRoot);

    barrier.barrier();
  }

  private void testSharedPrimitiveDoubleRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        primitiveDoubleRoot = 2e4;
      }
    }

    barrier.barrier();

    Assert.assertEquals(2e4, primitiveDoubleRoot);

    barrier.barrier();

  }

  private void testSharedPrimitiveFloatRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        primitiveFloatRoot = 0.3f;
      }
    }

    barrier.barrier();

    Assert.assertEquals(0.3f, primitiveFloatRoot);

    barrier.barrier();

  }

  private void testSharedPrimitiveByteRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        primitiveByteRoot = (byte) 10;
      }
    }

    barrier.barrier();

    Assert.assertEquals((byte) 10, primitiveByteRoot);

    barrier.barrier();

  }

  private void testSharedPrimitiveBooleanRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        primitiveBooleanRoot = true;
      }
    }

    barrier.barrier();

    Assert.assertTrue(primitiveBooleanRoot);

    barrier.barrier();

  }

  private void testSharedPrimitiveCharRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        primitiveCharRoot = 'c';
      }
    }

    barrier.barrier();

    Assert.assertEquals('c', primitiveCharRoot);

    barrier.barrier();

  }

  private void testSharedClassRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        classRoot = Float.class;
      }
    }

    barrier.barrier();

    Assert.assertEquals("java.lang.Float", classRoot.getName());

    barrier.barrier();
  }

  private void clear() throws Exception {
    synchronized (syncRoot) {
      syncRoot.setIndex(0);
      referenceRoot.clear();
    }

    barrier.barrier();
  }

  private void testSharedStringRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        stringRoot = "Shared value";
      }
    }

    barrier.barrier();

    Assert.assertEquals("Shared value", stringRoot);

    barrier.barrier();
  }

  private void testSharedIntegerRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        integerRoot = new Integer(4);
      }
    }

    barrier.barrier();

    Assert.assertEquals(new Integer(4), integerRoot);

    barrier.barrier();
  }

  private void testSharedLongRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        longRoot = new Long(10L);
      }
    }

    barrier.barrier();

    Assert.assertEquals(new Long(10L), longRoot);

    barrier.barrier();
  }

  private void testSharedFloatRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        floatRoot = new Float(3.2D);
      }
    }

    barrier.barrier();

    Assert.assertEquals(new Float(3.2D), floatRoot);

    barrier.barrier();
  }

  private void testSharedDoubleRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        doubleRoot = new Double(3.2e4);
      }
    }

    barrier.barrier();

    Assert.assertEquals(new Double(3.2e4), doubleRoot);

    barrier.barrier();
  }

  private void testSharedByteRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        byteRoot = new Byte((byte) 5);
      }
    }

    barrier.barrier();

    Assert.assertEquals(new Byte((byte) 5), byteRoot);

    barrier.barrier();
  }

  private void testSharedBooleanRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        booleanRoot = Boolean.TRUE;
      }
    }

    barrier.barrier();

    Assert.assertTrue(booleanRoot.booleanValue());

    barrier.barrier();
  }

  private void testSharedCharacterRoot() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        characterRoot = new Character('c');
      }
    }

    barrier.barrier();

    Assert.assertEquals(new Character('c'), characterRoot);

    barrier.barrier();
  }

  private void testReferenceInequality() throws Exception {
    clear();

    int index = -1;
    synchronized (syncRoot) {
      index = syncRoot.getIndex();
      if (index == 0) {
        syncRoot.setIndex(1);
      }
    }

    barrier.barrier();

    synchronized (syncRoot) {
      if (index == 0) {
        referenceRoot.setObj(nonSharedIntegerObject);
      }
    }

    barrier.barrier();

    if (index != 0) {
      Assert.assertEquals(nonSharedIntegerObject, referenceRoot.getObj());
      Assert.assertFalse(referenceRoot.isSameReferencedObject(nonSharedIntegerObject));
    }

    barrier.barrier();
  }

  // TODO: Needs to make this reference equality work
  /*
   * private void testReferenceEquality() throws Exception { clear(); int index = -1; synchronized (syncRoot) { index =
   * syncRoot.getIndex(); if (index == 0) { syncRoot.setIndex(1); } } barrier.barrier(); synchronized (syncRoot) { if
   * (index == 0) { integerRoot = new Integer(20); } } barrier.barrier(); if (index != 0) { Assert.assertEquals(new
   * Integer(20), integerRoot); } barrier.barrier(); synchronized (syncRoot) { if (index == 0) {
   * referenceRoot.setObj(integerRoot); } } barrier.barrier(); if (index != 0) { Assert.assertEquals(integerRoot,
   * referenceRoot.getObj()); Assert.assertTrue(referenceRoot.isSameReferencedObject(integerRoot)); } barrier.barrier();
   * }
   */

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");
    String testClass = LiteralValueRootTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("primitiveIntRoot", "primitiveIntRoot");
    spec.addRoot("primitiveLongRoot", "primitiveLongRoot");
    spec.addRoot("primitiveFloatRoot", "primitiveFloatRoot");
    spec.addRoot("primitiveDoubleRoot", "primitiveDoubleRoot");
    spec.addRoot("primitiveBooleanRoot", "primitiveBooleanRoot");
    spec.addRoot("primitiveByteRoot", "primitiveByteRoot");
    spec.addRoot("primitiveCharRoot", "primitiveCharRoot");
    spec.addRoot("nonDefaultPrimitiveIntRoot", "nonDefaultPrimitiveIntRoot");
    spec.addRoot("nonDefaultIntegerRoot", "nonDefaultIntegerRoot", false);
    spec.addRoot("sharedIntegerObject", "sharedIntegerObject");
    spec.addRoot("readBeforeSetTestIntRoot", "readBeforeSetTestIntRoot");
    spec.addRoot("classRoot", "classRoot");
    spec.addRoot("readBeforeSetTestIntegerRoot", "readBeforeSetTestIntegerRoot");
    spec.addRoot("stringRoot", "stringRoot");
    spec.addRoot("integerRoot", "integerRoot");
    spec.addRoot("longRoot", "longRoot");
    spec.addRoot("doubleRoot", "doubleRoot");
    spec.addRoot("floatRoot", "floatRoot");
    spec.addRoot("byteRoot", "byteRoot");
    spec.addRoot("booleanRoot", "booleanRoot");
    spec.addRoot("characterRoot", "characterRoot");
    spec.addRoot("syncRoot", "syncRoot");
    spec.addRoot("referenceRoot", "referenceRoot");
    spec.addRoot("barrier", "barrier");
  }

  private static class SyncRoot {
    private int    index = 0;
    private Object obj;

    public SyncRoot() {
      super();
    }

    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    public Object getObj() {
      return obj;
    }

    public void setObj(Object obj) {
      this.obj = obj;
    }

    public boolean isSameReferencedObject(Object o) {
      return this.obj == o;
    }

    public void clear() {
      this.index = 0;
      this.obj = null;
    }
  }

}
