/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.util.Arrays;
import java.util.HashMap;

public class ArrayCopyTestApp extends GenericTransparentApp {
  private final static int ARRAY_LENGTH     = 20;
  private final static int BIG_ARRAY_LENGTH = 10000;

  public ArrayCopyTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, Root.class);
  }

  protected Object getTestObject(String testName) {
    return this.sharedMap.get("root");
  }

  protected void setupTestObject(String testName) {
    sharedMap.put("root", new Root(ARRAY_LENGTH, BIG_ARRAY_LENGTH));
  }

  private static int[] makeUnsharedIntArray() {
    return new int[ARRAY_LENGTH];
  }

  private static int[] makeBigUnsharedIntArray() {
    return new int[BIG_ARRAY_LENGTH];
  }

  private static TestObject[] makeUnsharedRefArray() {
    return new TestObject[ARRAY_LENGTH];
  }

  public void testBasicCopy(Root r, boolean validate) {
    int[] unshared = makeUnsharedIntArray();
    initialize(unshared);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualIntArray(unshared, root.getDestIntArray(), 0, 5, 10);
    } else {
      synchronized (r) {
        System.arraycopy(unshared, 0, root.getDestIntArray(), 5, 10);
      }
    }
  }

  public void testOverlapWithBiggerArray(Root r, boolean validate) throws Exception {
    int[] bigUnsharedIntArray = makeBigUnsharedIntArray();
    initialize(bigUnsharedIntArray);

    final int destPos = 5;
    final int destSize = 100;

    DataRoot bigRoot = r.getBigRoot();

    if (validate) {
      int[] destIntArray = bigRoot.destIntArray;
      assertEqualIntArray(bigUnsharedIntArray, destIntArray, 0, destPos, destSize);
    } else {
      synchronized (bigRoot) {
        int[] destIntArray = bigRoot.destIntArray;
        // elements 10..20 from bigUnsharedIntArray
        System.arraycopy(bigUnsharedIntArray, 10, destIntArray, destPos, 10);
        // elements 0..100 from bigUnsharedIntArray
        System.arraycopy(bigUnsharedIntArray, 0, destIntArray, destPos, destSize);
      }
    }

  }

  public void testCopyThenChange(Root r, boolean validate) throws Exception {
    int[] bigUnsharedIntArray = makeBigUnsharedIntArray();
    initialize(bigUnsharedIntArray);

    final int destPos = 5;
    final int destSize = 100;

    DataRoot bigRoot = r.getBigRoot();

    if (validate) {
      int[] destIntArray = bigRoot.destIntArray;
      bigUnsharedIntArray[1] = -5;
      bigUnsharedIntArray[2] = -6;
      assertEqualIntArray(bigUnsharedIntArray, destIntArray, 0, destPos, destSize);
    } else {
      synchronized (bigRoot) {
        int[] destIntArray = bigRoot.destIntArray;
        // elements 0..100 from bigUnsharedIntArray to dest 5..105
        System.arraycopy(bigUnsharedIntArray, 0, destIntArray, destPos, destSize);
        destIntArray[destPos + 1] = -5;
        destIntArray[destPos + 2] = -6;
      }
    }
  }

  public void testBigBasicCopy(Root r, boolean validate) throws Exception {
    int[] bigUnsharedIntArray = makeBigUnsharedIntArray();
    initialize(bigUnsharedIntArray);

    DataRoot bigRoot = r.getBigRoot();

    if (validate) {
      assertEqualIntArray(bigUnsharedIntArray, bigRoot.getDestIntArray(), 0, 5, (BIG_ARRAY_LENGTH - 5));
    } else {
      synchronized (bigRoot) {
        System.arraycopy(bigUnsharedIntArray, 0, bigRoot.getDestIntArray(), 5, (BIG_ARRAY_LENGTH - 5));
      }
    }

  }

  public void testBigCopyToSameArray(Root r, boolean validate) throws Exception {
    int[] bigUnsharedIntArray = makeBigUnsharedIntArray();
    initialize(bigUnsharedIntArray);

    DataRoot bigRoot = r.getBigRoot();

    if (validate) {
      assertEqualIntArray(bigUnsharedIntArray, bigRoot.getSrcIntArray(), 0, 5, (BIG_ARRAY_LENGTH - 5));
    } else {
      synchronized (bigRoot) {
        initialize(bigRoot.getSrcIntArray());
        System.arraycopy(bigRoot.getSrcIntArray(), 0, bigRoot.getSrcIntArray(), 5, (BIG_ARRAY_LENGTH - 5));
      }
    }
  }

  public void testMultipleBasicCopyTest(Root r, boolean validate) throws Exception {
    int[] unsharedIntArray = makeUnsharedIntArray();
    initialize(unsharedIntArray);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualIntArray(unsharedIntArray, root.getDestIntArray(), 0, 5, 10);
    } else {
      for (int i = 0; i < 100; i++) {
        synchronized (root) {
          System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), 5, 10);
        }
      }
    }
  }

  public void testMultipleCopyToSameArray(Root r, boolean validate) throws Exception {
    int[] unsharedIntArray = makeUnsharedIntArray();
    initialize(unsharedIntArray);

    DataRoot root = r.getRoot();

    if (validate) {
      for (int i = 0; i < 2; i++) {
        System.arraycopy(unsharedIntArray, 0, unsharedIntArray, 5, 10);
      }

      assertEqualIntArray(unsharedIntArray, root.getSrcIntArray(), 0, 5, 10);
    } else {
      synchronized (root) {
        initialize(root.getSrcIntArray());
      }

      for (int i = 0; i < 100; i++) {
        synchronized (root) {
          System.arraycopy(root.getSrcIntArray(), 0, root.getSrcIntArray(), 5, 10);
        }
      }
    }
  }

  public void testCopyRefToUnshared(Root r, boolean validate) throws Exception {
    // There was bug when copying a shared source reference array to an unshared dest.
    // This method covers this case specifically

    Object[] sharedArray = r.getSharedArray();

    int len = sharedArray.length;
    Object[] unsharedDest = new Object[len];
    System.arraycopy(sharedArray, 0, unsharedDest, 0, len);

    if (validate) {
      Assert.assertNoNullElements(unsharedDest);
      Assert.assertTrue(Arrays.equals(unsharedDest, makeArrayData()));
    }
  }

  public void testBasicRefCopyTest(Root r, boolean validate) throws Exception {
    TestObject[] unsharedRefArray = makeUnsharedRefArray();
    initialize(unsharedRefArray);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualRefArray(unsharedRefArray, root.getDestRefArray(), 0, 5, 10);
    } else {
      synchronized (root) {
        System.arraycopy(unsharedRefArray, 0, root.getDestRefArray(), 5, 10);
      }
    }
  }

  public void testCopyToSameArray(Root r, boolean validate) throws Exception {
    int[] unsharedIntArray = makeUnsharedIntArray();
    initialize(unsharedIntArray);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualIntArray(unsharedIntArray, root.getSrcIntArray(), 0, 5, 10);
    } else {
      synchronized (root) {
        initialize(root.getSrcIntArray());
      }
      synchronized (root) {
        System.arraycopy(root.getSrcIntArray(), 0, root.getSrcIntArray(), 5, 10);
      }
    }
  }

  public void testCopyRefToSameArray(Root r, boolean validate) throws Exception {
    TestObject[] unsharedRefArray = makeUnsharedRefArray();
    initialize(unsharedRefArray);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualRefArray(unsharedRefArray, root.getSrcRefArray(), 0, 5, 10);
    } else {
      synchronized (root) {
        root.initializeSrcRefArray(root.getSrcRefArray());
      }

      synchronized (root) {
        System.arraycopy(root.getSrcRefArray(), 0, root.getSrcRefArray(), 5, 10);
      }
    }
  }

  public void testCopyToDifferentPrimitiveTypeArray(Root r, boolean validate) throws Exception {
    DataRoot root = r.getRoot();

    if (validate) {
      long[] actual = root.getDestLongArray();
      long[] expected = new long[actual.length];
      Arrays.fill(expected, 42);
      Assert.assertTrue(Arrays.equals(expected, actual));
    } else {
      synchronized (root) {
        long[] l = root.getDestLongArray();
        Arrays.fill(l, 42);
      }

      synchronized (root) {
        try {
          System.arraycopy(makeUnsharedIntArray(), 0, root.getDestLongArray(), 5, 10);
          throw new AssertionError("Should have thrown an ArrayStoreException.");
        } catch (ArrayStoreException e) {
          // Expected. Shared array should not have been disturbed
        }
      }
    }
  }

  public void testNullSrc(Root r, boolean validate) throws Exception {
    int[] unsharedIntArray = makeUnsharedIntArray();
    initialize(unsharedIntArray);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualIntArray(unsharedIntArray, root.getDestIntArray(), 0, 0, ARRAY_LENGTH);
    } else {
      synchronized (root) {
        initialize(root.getDestIntArray());
      }

      synchronized (root) {
        try {
          System.arraycopy(null, 0, root.getDestIntArray(), 5, 10);
          throw new AssertionError("Should have thrown an NullPointerException.");
        } catch (NullPointerException e) {
          // Expected
        }
      }
    }
  }

  public void testNullDest(Root r, boolean validate) throws Exception {
    int[] unsharedIntArray = makeUnsharedIntArray();
    initialize(unsharedIntArray);

    DataRoot root = r.getRoot();

    synchronized (root) {
      try {
        System.arraycopy(unsharedIntArray, 0, null, 5, 10);
        throw new AssertionError("Should have thrown an NullPointerException.");
      } catch (NullPointerException e) {
        // Expected
      }
    }
  }

  public void testSrcNotArray(Root r, boolean validate) throws Exception {
    int[] unsharedIntArray = makeUnsharedIntArray();
    initialize(unsharedIntArray);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualIntArray(unsharedIntArray, root.getDestIntArray(), 0, 0, ARRAY_LENGTH);
    } else {
      synchronized (root) {
        initialize(root.getDestIntArray());
      }

      synchronized (root) {
        try {
          System.arraycopy(new Object(), 0, root.getDestIntArray(), 5, 10);
          throw new AssertionError("Should have thrown an ArrayStoreException.");
        } catch (ArrayStoreException e) {
          // Expected
        }
      }
    }
  }

  public void testDestNotArray(Root r, boolean validate) throws Exception {
    try {
      System.arraycopy(makeUnsharedIntArray(), 0, new Object(), 5, 10);
      throw new AssertionError("Should have thrown an ArrayStoreException.");
    } catch (ArrayStoreException e) {
      // Expected
    }
  }

  public void testSrcAndDestNotCompatibleTest(Root r, boolean validate) throws Exception {
    TestObject[] unsharedRefArray = makeUnsharedRefArray();
    initialize(unsharedRefArray);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualRefArray(unsharedRefArray, root.getDestRefArray(), 0, 0, ARRAY_LENGTH);
    } else {
      root.initializeDestRefArray();

      synchronized (root) {
        try {
          System.arraycopy(makeUnsharedIntArray(), 0, root.getDestRefArray(), 5, 10);
          throw new AssertionError("Should have thrown an ArrayStoreException.");
        } catch (ArrayStoreException e) {
          // Expected
        }
      }
    }

  }

  public void testNegativeLengthCopyTest(Root r, boolean validate) throws Exception {
    int[] unsharedIntArray = makeUnsharedIntArray();
    initialize(unsharedIntArray);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualIntArray(unsharedIntArray, root.getDestIntArray(), 0, 0, ARRAY_LENGTH);
    } else {
      synchronized (root) {
        System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), 0, ARRAY_LENGTH);
      }

      mutate(unsharedIntArray);
      try {
        System.arraycopy(unsharedIntArray, -1, root.getDestIntArray(), 0, 10);
        throw new AssertionError("Should have thrown an IndexOutOfBoundsException.");
      } catch (IndexOutOfBoundsException e) {
        // Expected.
      }

      mutate(unsharedIntArray);
      try {
        System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), -1, 10);
        throw new AssertionError("Should have thrown an IndexOutOfBoundsException.");
      } catch (IndexOutOfBoundsException e) {
        // Expected.
      }

      mutate(unsharedIntArray);
      try {
        System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), 0, -1);
        throw new AssertionError("Should have thrown an IndexOutOfBoundsException.");
      } catch (IndexOutOfBoundsException e) {
        // Expected.
      }
    }

  }

  public void testIndexOutOfBoundsCopy(Root r, boolean validate) throws Exception {
    int[] unsharedIntArray = makeUnsharedIntArray();
    initialize(unsharedIntArray);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualIntArray(unsharedIntArray, root.getDestIntArray(), 0, 0, ARRAY_LENGTH);
    } else {
      synchronized (root) {
        System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), 0, ARRAY_LENGTH);
      }

      mutate(unsharedIntArray);
      try {
        System.arraycopy(unsharedIntArray, 15, root.getDestIntArray(), 0, 10);
        throw new AssertionError("Should have thrown an IndexOutOfBoundsException.");
      } catch (IndexOutOfBoundsException e) {
        // Expected.
      }

      mutate(unsharedIntArray);
      try {
        System.arraycopy(unsharedIntArray, 0, root.getDestIntArray(), 15, 10);
        throw new AssertionError("Should have thrown an IndexOutOfBoundsException.");
      } catch (IndexOutOfBoundsException e) {
        // Expected.
      }
    }
  }

  public void testCopyNonCompatibleRefObject(Root r, boolean validate) throws Exception {
    TestObject[] unsharedRefArray = makeUnsharedRefArray();
    initialize(unsharedRefArray);

    DataRoot root = r.getRoot();

    if (validate) {
      assertEqualRefArray(unsharedRefArray, root.getDestRefArray(), 0, 0, 10);
    } else {
      synchronized (root) {
        System.arraycopy(unsharedRefArray, 0, root.getDestRefArray(), 0, 10);
      }

      Double d[] = new Double[ARRAY_LENGTH];
      for (int i = 0; i < d.length; i++) {
        d[i] = new Double(i);
      }

      synchronized (root) {
        try {
          System.arraycopy(d, 0, root.getDestRefArray(), 0, 10);
          throw new AssertionError("Should have thrown an ArrayStoreException.");
        } catch (ArrayStoreException e) {
          // Expected.
        }
      }
    }
  }

  public void testPartialCopy(Root r, boolean validate) throws Exception {
    TestObject[] unsharedRefArray = makeUnsharedRefArray();
    initialize(unsharedRefArray);

    DataRoot root = r.getRoot();

    TestObject[] dest = root.getDestRefArray();

    if (validate) {
      unsharedRefArray[0] = new TestObject(42);
      unsharedRefArray[1] = null;
      assertEqualRefArray(unsharedRefArray, dest, 0, 0, dest.length);
    } else {
      synchronized (root) {
        System.arraycopy(unsharedRefArray, 0, dest, 0, dest.length);
      }

      Object[] array = new Object[dest.length];
      array[0] = new TestObject(42);
      // element 1 is null
      array[2] = new HashMap();

      synchronized (root) {
        try {
          System.arraycopy(array, 0, dest, 0, dest.length);
          throw new AssertionError();
        } catch (ArrayStoreException ase) {
          // expected, but first 2 elements should have been copied
        }
      }
    }
  }

  private static void mutate(int[] unsharedIntArray) {
    for (int i = 0; i < unsharedIntArray.length; i++) {
      unsharedIntArray[i]++;
    }
  }

  private static void assertEqualIntArray(int[] expected, int[] actual, int startExpectedPos, int startActualPos,
                                          int length) {
    for (int i = startExpectedPos, j = startActualPos; i < length; i++, j++) {
      Assert.assertEquals(expected[i], actual[j]);
    }
  }

  private static void assertEqualRefArray(Object[] expected, Object[] actual, int startExpectedPos, int startActualPos,
                                          int length) {
    for (int i = startExpectedPos, j = startActualPos; i < length; i++, j++) {
      Assert.assertEquals(expected[i], actual[j]);
    }
  }

  private static void initialize(int[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = i;
    }
  }

  private static void initialize(TestObject[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = new TestObject(i);
    }
  }

  private static Object[] makeArrayData() {
    return new Object[] { "dig", "this", "yo", new Thingy("!") };
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = ArrayCopyTestApp.class.getName();
    config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
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

    public DataRoot(int size) {
      this.size = size;
      srcIntArray = new int[size];
      srcRefArray = new TestObject[size];
      destIntArray = new int[size];
      destLongArray = new long[size];
      destRefArray = new TestObject[size];
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
      initialize(destRefArray);
    }

    public void initializeSrcRefArray(Object[] array) {
      for (int i = 0; i < size; i++) {
        array[i] = new TestObject(i);
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

  private static class Root {

    private final DataRoot root;
    private final DataRoot bigRoot;
    private final Object[] sharedArray = makeArrayData();

    Root(int rootSize, int bigRootSize) {
      this.root = new DataRoot(rootSize);
      this.bigRoot = new DataRoot(bigRootSize);
    }

    DataRoot getBigRoot() {
      return bigRoot;
    }

    DataRoot getRoot() {
      return root;
    }

    Object[] getSharedArray() {
      return sharedArray;
    }
  }

}
