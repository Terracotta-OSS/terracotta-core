/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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

import java.util.Arrays;

public class ArrayCopyTestApp extends AbstractTransparentApp {
  private final static int    ARRAY_LENGTH        = 20;
  private final static int    BIG_ARRAY_LENGTH    = 10000;

  private final int[]         unsharedIntArray    = new int[ARRAY_LENGTH];
  private final Object[]      unsharedRefArray    = new Object[ARRAY_LENGTH];

  private final int[]         bigUnsharedIntArray = new int[BIG_ARRAY_LENGTH];
  private final Object[]      bigUnsharedRefArray = new Object[BIG_ARRAY_LENGTH];

  private final DataRoot      root                = new DataRoot(ARRAY_LENGTH);
  private final DataRoot      bigRoot             = new DataRoot(BIG_ARRAY_LENGTH);

  private final Object[]      sharedArray         = makeArrayData();

  private final CyclicBarrier barrier;

  public ArrayCopyTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  private static Object[] makeArrayData() {
    return new Object[] { "dig", "this", "yo", new Thingy("!") };
  }

  public void run() {
    try {
      long startTime = System.currentTimeMillis();

      basicCopyTest();
      basicRefCopyTest();
      copyRefToUnsharedTest();
      copyToSameArrayTest();
      copyRefToSameArrayTest();
      copyToDifferentPrimitiveTypeArrayTest();
      NullSrcTest();
      NullDestTest();
      SrcNotArrayTest();
      DestNotArrayTest();
      SrcAndDestNotCompatibleTest();
      negativeLengthCopyTest();
      copyNonCompatibleRefObject();
      IndexOutOfBoundsCopyTest();
      bigTimedBasicCopyTest();
      bigTimedCopyToSameArrayTest();
      multipleTimedBasicCopyTest();
      multipleTimedCopyToSameArrayTest();

      copyThenChangeTest();
      overlapWitBiggerArrayTest();

      System.err.println("%%%% Total Duration: " + (System.currentTimeMillis() - startTime));

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void overlapWitBiggerArrayTest() throws Exception {
    clear();
    initialize(bigUnsharedIntArray);
    barrier.barrier();

    int index = -1;
    synchronized (bigRoot) {
      index = bigRoot.getIndex();
      bigRoot.setIndex(index + 1);
    }
    barrier.barrier();

    int destPos = 5;
    int destSize = 100;

    if (index == 0) {
      synchronized (bigRoot) {
        int[] destIntArray = bigRoot.destIntArray;
        // elements 10..20 from bigUnsharedIntArray
        System.arraycopy(bigUnsharedIntArray, 10, destIntArray, destPos, 10);
        // elements 0..100 from bigUnsharedIntArray
        System.arraycopy(bigUnsharedIntArray, 0, destIntArray, destPos, destSize);
      }
    }
    barrier.barrier();

    try {
      if (index != 0) {
        synchronized (bigRoot) {
          int[] destIntArray = bigRoot.destIntArray;
          assertEqualIntArray(bigUnsharedIntArray, destIntArray, 0, destPos, destSize);
        }
      }
    } finally {
      barrier.barrier();
    }
  }

  private void copyThenChangeTest() throws Exception {
    clear();
    initialize(bigUnsharedIntArray);
    barrier.barrier();

    int index = -1;
    synchronized (bigRoot) {
      index = bigRoot.getIndex();
      bigRoot.setIndex(index + 1);
    }
    barrier.barrier();

    int destPos = 5;
    int destSize = 100;

    if (index == 0) {
      synchronized (bigRoot) {
        int[] destIntArray = bigRoot.destIntArray;
        destIntArray[destPos + 3] = -10;
        destIntArray[destPos + 4] = -11;
        // elements 0..100 from bigUnsharedIntArray
        System.arraycopy(bigUnsharedIntArray, 0, destIntArray, destPos, destSize);
        // elements 10..20 from bigUnsharedIntArray
        System.arraycopy(bigUnsharedIntArray, 10, destIntArray, destPos, 10);
        destIntArray[destPos + 1] = -5;
        destIntArray[destPos + 2] = -6;
      }
    }
    barrier.barrier();

    try {
      if (index != 0) {
        synchronized (bigRoot) {
          int[] destIntArray = bigRoot.destIntArray;
          assertEqualIntArray(bigUnsharedIntArray, destIntArray, 13, destPos + 3, 10 - 3);
          assertEqualIntArray(bigUnsharedIntArray, destIntArray, 13, destPos + 3 + 10, destSize - 3 - 10);
          Assert.assertEquals(10, destIntArray[destPos]);
          Assert.assertEquals(-5, destIntArray[destPos + 1]);
          Assert.assertEquals(-6, destIntArray[destPos + 2]);
        }
      }
    } finally {
      barrier.barrier();
    }
  }

  private void bigTimedBasicCopyTest() throws Exception {
    long startTime = 0;
    clear();
    initialize(bigUnsharedIntArray);

    barrier.barrier();

    int index = -1;
    synchronized (bigRoot) {
      index = bigRoot.getIndex();
      bigRoot.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      startTime = System.currentTimeMillis();
      synchronized (bigRoot) {
        System.arraycopy(bigUnsharedIntArray, 0, bigRoot.getDestIntArray(), 5, (BIG_ARRAY_LENGTH - 5));
        System.err.println("%%%% bigTimedBasicCopyTest(), Duration: " + (System.currentTimeMillis() - startTime));
      }
    }

    barrier.barrier();

    try {
      if (index != 0) {
        assertEqualIntArray(bigUnsharedIntArray, bigRoot.getDestIntArray(), 0, 5, (BIG_ARRAY_LENGTH - 5));
      }
    } finally {
      barrier.barrier();
    }
  }

  private void bigTimedCopyToSameArrayTest() throws Exception {
    long startTime = 0;
    clear();
    initialize(bigUnsharedIntArray);
    barrier.barrier();

    int index = -1;
    synchronized (bigRoot) {
      index = bigRoot.getIndex();
      bigRoot.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (bigRoot) {
        initialize(bigRoot.getSrcIntArray());
      }
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (bigRoot) {
        startTime = System.currentTimeMillis();
        System.arraycopy(bigRoot.getSrcIntArray(), 0, bigRoot.getSrcIntArray(), 5, (BIG_ARRAY_LENGTH - 5));
        System.err.println("%%%% bigTimedCopyToSameArrayTest(), Duration: " + (System.currentTimeMillis() - startTime));
      }
    }

    barrier.barrier();

    try {
      if (index != 0) {
        assertEqualIntArray(bigUnsharedIntArray, bigRoot.getSrcIntArray(), 0, 5, (BIG_ARRAY_LENGTH - 5));
      }
    } finally {
      barrier.barrier();
    }
  }

  private void multipleTimedBasicCopyTest() throws Exception {
    long startTime = 0;
    clear();
    initialize(unsharedIntArray);

    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      startTime = System.currentTimeMillis();
      for (int i = 0; i < 100; i++) {
        synchronized (root) {
          System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), 5, 10);
        }
      }
      System.err.println("%%%% multipleTimedBasicCopyTest(), Duration: " + (System.currentTimeMillis() - startTime));
    }

    barrier.barrier();

    try {
      if (index != 0) {
        assertEqualIntArray(unsharedIntArray, root.getDestIntArray(), 0, 5, 10);
      }
    } finally {
      barrier.barrier();
    }
  }

  private void multipleTimedCopyToSameArrayTest() throws Exception {
    long startTime = 0;
    clear();
    initialize(unsharedIntArray);
    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        initialize(root.getSrcIntArray());
      }
    }

    barrier.barrier();

    if (index == 0) {
      startTime = System.currentTimeMillis();
      for (int i = 0; i < 100; i++) {
        synchronized (root) {
          System.arraycopy(root.getSrcIntArray(), 0, root.getSrcIntArray(), 5, 10);
        }
      }
      System.err.println("%%%% multipleTimedCopyToSameArrayTest(), Duration: "
                         + (System.currentTimeMillis() - startTime));
    }

    barrier.barrier();

    try {
      if (index != 0) {
        for (int i = 0; i < 2; i++) {
          System.arraycopy(unsharedIntArray, 0, unsharedIntArray, 5, 10);
        }
        assertEqualIntArray(unsharedIntArray, root.getSrcIntArray(), 0, 5, 10);
      }
    } finally {
      barrier.barrier();
    }
  }

  private void basicCopyTest() throws Exception {
    clear();
    initialize(unsharedIntArray);

    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), 5, 10);
      }
    }

    barrier.barrier();

    try {
      if (index != 0) {
        assertEqualIntArray(unsharedIntArray, root.getDestIntArray(), 0, 5, 10);
      }
    } finally {
      barrier.barrier();
    }
  }

  private void copyRefToUnsharedTest() throws Exception {
    // There was bug when copying a shared source reference array to an unshared dest.
    // This method covers this case specifically
    int len = sharedArray.length;
    Object[] unsharedDest = new Object[len];
    System.arraycopy(sharedArray, 0, unsharedDest, 0, len);

    try {
      Assert.assertNoNullElements(unsharedDest);
      Assert.assertTrue(Arrays.equals(unsharedDest, makeArrayData()));
    } finally {
      barrier.barrier();
    }
  }

  private void basicRefCopyTest() throws Exception {
    clear();
    initialize(unsharedRefArray);
    root.initializeDestRefArray(unsharedRefArray);

    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        System.arraycopy(unsharedRefArray, 0, root.getDestRefArray(), 5, 10);
      }
    }

    barrier.barrier();

    try {
      if (index != 0) {
        assertEqualRefArray(unsharedRefArray, root.getDestRefArray(), 0, 5, 10);
      }
    } finally {
      barrier.barrier();
    }
  }

  private void copyToSameArrayTest() throws Exception {
    clear();
    initialize(unsharedIntArray);
    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        initialize(root.getSrcIntArray());
      }
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        System.arraycopy(root.getSrcIntArray(), 0, root.getSrcIntArray(), 5, 10);
      }
    }

    barrier.barrier();

    try {
      if (index != 0) {
        assertEqualIntArray(unsharedIntArray, root.getSrcIntArray(), 0, 5, 10);
      }
    } finally {
      barrier.barrier();
    }
  }

  private void copyRefToSameArrayTest() throws Exception {
    clear();
    root.initializeSrcRefArray(unsharedRefArray);
    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        root.initializeSrcRefArray(root.getSrcRefArray());
      }
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        System.arraycopy(root.getSrcRefArray(), 0, root.getSrcRefArray(), 5, 10);
      }
    }

    barrier.barrier();

    try {
      if (index != 0) {
        assertEqualRefArray(unsharedRefArray, root.getSrcRefArray(), 0, 5, 10);
      }
    } finally {
      barrier.barrier();
    }
  }

  private void copyToDifferentPrimitiveTypeArrayTest() throws Exception {
    clear();
    initialize(unsharedIntArray);
    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    try {
      if (index == 0) {
        synchronized (root) {
          try {
            System.arraycopy(unsharedIntArray, 0, root.getDestLongArray(), 5, 10);
            throw new AssertionError("Should have thrown an ArrayStoreException.");
          } catch (ArrayStoreException e) {
            // Expected.
          }
        }
      }
    } finally {
      barrier.barrier();
    }
  }

  private void NullSrcTest() throws Exception {
    clear();
    synchronized (root) {
      initialize(root.getDestIntArray());
    }
    initialize(unsharedIntArray);
    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        try {
          System.arraycopy(null, 0, root.getDestIntArray(), 5, 10);
          throw new AssertionError("Should have thrown an NullPointerException.");
        } catch (NullPointerException e) {
          // Expected
        }
      }
    }

    barrier.barrier();

    try {
      assertEqualIntArray(unsharedIntArray, root.getDestIntArray(), 0, 0, ARRAY_LENGTH);
    } finally {
      barrier.barrier();
    }
  }

  private void NullDestTest() throws Exception {
    clear();
    initialize(unsharedIntArray);
    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    try {
      if (index == 0) {
        synchronized (root) {
          try {
            System.arraycopy(unsharedIntArray, 0, null, 5, 10);
            throw new AssertionError("Should have thrown an NullPointerException.");
          } catch (NullPointerException e) {
            // Expected
          }
        }
      }
    } finally {
      barrier.barrier();
    }
  }

  private void SrcNotArrayTest() throws Exception {
    clear();
    synchronized (root) {
      initialize(root.getDestIntArray());
    }
    initialize(unsharedIntArray);
    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        try {
          System.arraycopy(new Object(), 0, root.getDestIntArray(), 5, 10);
          throw new AssertionError("Should have thrown an ArrayStoreException.");
        } catch (ArrayStoreException e) {
          // Expected
        }
      }
    }

    barrier.barrier();

    try {
      assertEqualIntArray(unsharedIntArray, root.getDestIntArray(), 0, 0, ARRAY_LENGTH);
    } finally {
      barrier.barrier();
    }
  }

  private void DestNotArrayTest() throws Exception {
    clear();
    initialize(unsharedIntArray);
    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    try {
      if (index == 0) {
        synchronized (root) {
          try {
            System.arraycopy(unsharedIntArray, 0, new Object(), 5, 10);
            throw new AssertionError("Should have thrown an ArrayStoreException.");
          } catch (ArrayStoreException e) {
            // Expected
          }
        }
      }
    } finally {
      barrier.barrier();
    }
  }

  private void SrcAndDestNotCompatibleTest() throws Exception {
    clear();
    initialize(unsharedRefArray);
    root.initializeDestRefArray();
    root.initializeDestRefArray(unsharedRefArray);
    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        try {
          System.arraycopy(unsharedIntArray, 0, root.getDestRefArray(), 5, 10);
          throw new AssertionError("Should have thrown an ArrayStoreException.");
        } catch (ArrayStoreException e) {
          // Expected
        }
      }
    }

    barrier.barrier();

    try {
      assertEqualRefArray(unsharedRefArray, root.getDestRefArray(), 0, 0, ARRAY_LENGTH);
    } finally {
      barrier.barrier();
    }
  }

  private void negativeLengthCopyTest() throws Exception {
    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        try {
          System.arraycopy(unsharedIntArray, -1, root.getDestIntArray(), 0, 10);
          throw new AssertionError("Should have thrown an IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
          // Expected.
        }
      }
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        try {
          System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), -1, 10);
          throw new AssertionError("Should have thrown an IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
          // Expected.
        }
      }
    }

    barrier.barrier();

    try {
      if (index == 0) {
        synchronized (root) {
          try {
            System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), 0, -1);
            throw new AssertionError("Should have thrown an IndexOutOfBoundsException.");
          } catch (IndexOutOfBoundsException e) {
            // Expected.
          }
        }
      }
    } finally {
      barrier.barrier();
    }
  }

  private void IndexOutOfBoundsCopyTest() throws Exception {
    clear();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        try {
          System.arraycopy(unsharedIntArray, 15, root.getDestIntArray(), 0, 10);
          throw new AssertionError("Should have thrown an IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
          // Expected.
        }
      }
    }

    barrier.barrier();

    try {
      if (index == 0) {
        synchronized (root) {
          try {
            System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), 15, 10);
            throw new AssertionError("Should have thrown an IndexOutOfBoundsException.");
          } catch (IndexOutOfBoundsException e) {
            // Expected.
          }
        }
      }
    } finally {
      barrier.barrier();
    }
  }

  private void copyNonCompatibleRefObject() throws Exception {
    clear();
    initialize(unsharedRefArray);
    barrier.barrier();

    for (int i = 0; i < 5; i++) {
      unsharedRefArray[i] = new TestObject(i);
    }

    barrier.barrier();

    int index = -1;
    synchronized (root) {
      index = root.getIndex();
      root.setIndex(index + 1);
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        try {
          System.arraycopy(unsharedRefArray, 0, root.getDestRefArray(), 0, 10);
          throw new AssertionError("Should have thrown an ArrayStoreException.");
        } catch (ArrayStoreException e) {
          // Expected.
        }
      }
    }

    barrier.barrier();

    try {
      if (index != 0) {
        assertEqualRefArray(unsharedRefArray, root.getDestRefArray(), 0, 0, 5);
        assertNotEqualRefArray(unsharedRefArray, root.getDestRefArray(), 5, 5, 5);
      }
    } finally {
      barrier.barrier();
    }
  }

  private void assertEqualIntArray(int[] expected, int[] actual, int startExpectedPos, int startActualPos, int length) {
    for (int i = startExpectedPos, j = startActualPos; i < length; i++, j++) {
      Assert.assertEquals(expected[i], actual[j]);
    }
  }

  private void assertEqualRefArray(Object[] expected, Object[] actual, int startExpectedPos, int startActualPos,
                                   int length) {
    for (int i = startExpectedPos, j = startActualPos; i < length; i++, j++) {
      Assert.assertEquals(expected[i], actual[j]);
    }
  }

  private void assertNotEqualRefArray(Object[] expected, Object[] actual, int startExpectedPos, int startActualPos,
                                      int length) {
    for (int i = startExpectedPos, j = startActualPos; i < length; i++, j++) {
      Assert.assertFalse(expected[i].equals(actual[j]));
    }
  }

  private void clear() throws Exception {
    for (int i = 0; i < ARRAY_LENGTH; i++) {
      unsharedIntArray[i] = 0;
      unsharedRefArray[i] = null;
    }
    for (int i = 0; i < BIG_ARRAY_LENGTH; i++) {
      bigUnsharedIntArray[i] = 0;
      bigUnsharedRefArray[i] = null;
    }

    synchronized (root) {
      root.clear();
    }
    synchronized (bigRoot) {
      bigRoot.clear();
    }

    barrier.barrier();
  }

  private void initialize(int[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = i;
    }
  }

  private void initialize(Object[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = new Object();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = ArrayCopyTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("root", "root");
    spec.addRoot("sharedArray", "sharedArray");
    spec.addRoot("bigRoot", "bigRoot");
    spec.addRoot("barrier", "barrier");
  }

  private static class TestObject {
    private final int i;

    public TestObject(int i) {
      this.i = i;
    }

    public int hashCode() {
      return i;
    }

    public boolean equals(Object o) {
      if (!(o instanceof TestObject)) { return false; }
      return this.i == ((TestObject) o).i;
    }

    public String toString() {
      return getClass().getName() + "(" + i + ")";
    }
  }

  private static class DataRoot {
    private final int[]        srcIntArray;
    private final TestObject[] srcRefArray;
    private final int[]        destIntArray;
    private final long[]       destLongArray;
    private final TestObject[] destRefArray;
    private final int          size;

    private int                index = 0;

    public DataRoot(int size) {
      this.index = 0;
      this.size = size;
      srcIntArray = new int[size];
      srcRefArray = new TestObject[size];
      destIntArray = new int[size];
      destLongArray = new long[size];
      destRefArray = new TestObject[size];
    }

    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    public int[] getSrcIntArray() {
      return srcIntArray;
    }

    public TestObject[] getSrcRefArray() {
      return srcRefArray;
    }

    public int[] getDestIntArray() {
      return destIntArray;
    }

    public long[] getDestLongArray() {
      return destLongArray;
    }

    public TestObject[] getDestRefArray() {
      return destRefArray;
    }

    public synchronized void initializeDestRefArray() {
      for (int i = 0; i < size; i++) {
        destRefArray[i] = new TestObject(i);
      }
    }

    public void initializeDestRefArray(Object[] array) {
      for (int i = 0; i < size; i++) {
        array[i] = new TestObject(i);
      }
    }

    public void initializeSrcRefArray(Object[] array) {
      for (int i = 0; i < size; i++) {
        array[i] = new TestObject(i);
      }
    }

    public void clear() {
      this.index = 0;
      for (int i = 0; i < size; i++) {
        srcIntArray[i] = 0;
        srcRefArray[i] = null;
        destIntArray[i] = 0;
        destLongArray[i] = 0;
        destRefArray[i] = null;
      }
    }
  }

  private static class Thingy {
    private final String val;

    Thingy(String val) {
      this.val = val;
    }

    public int hashCode() {
      return val.hashCode();
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof Thingy)) { return false; }
      return val.equals(((Thingy) obj).val);
    }

    public String toString() {
      return getClass().getName() + "(" + val + ")";
    }

  }
}
