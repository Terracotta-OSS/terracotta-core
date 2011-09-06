/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.Root;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.lang.reflect.Array;

public class ReflectionArrayTestApp extends GenericTransparentApp {
  public ReflectionArrayTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, DataRoot.class);
  }

  protected Object getTestObject(String testName) {
    DataRoot dataRoot = (DataRoot) sharedMap.get("dataRoot");
    return dataRoot;
  }

  protected void setupTestObject(String testName) {
    sharedMap.put("dataRoot", new DataRoot());
  }

  void testCreateLongArray(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(20, Array.getLength(root.getLongArray()));
    } else {
      synchronized (root) {
        root.setLongArray((long[]) Array.newInstance(Long.TYPE, 20));
      }
    }
  }

  void testModifyLongArray(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Long.MAX_VALUE, Array.getLong(root.getLongArray(), 0));
    } else {
      synchronized (root) {
        Array.setLong(root.getLongArray(), 0, Long.MAX_VALUE);
      }
    }
  }

  void testModifyIntArray(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Integer.MAX_VALUE, Array.getInt(root.getIntArray(), 0));
    } else {
      synchronized (root) {
        Array.setInt(root.getIntArray(), 0, Integer.MAX_VALUE);
      }
    }
  }

  void testModiyShortArray(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Short.MAX_VALUE, Array.getShort(root.getShortArray(), 0));
    } else {
      synchronized (root) {
        Array.setShort(root.getShortArray(), 0, Short.MAX_VALUE);
      }
    }
  }

  void testModiyByteArray(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Byte.MAX_VALUE, Array.getByte(root.getByteArray(), 0));
    } else {
      synchronized (root) {
        Array.setByte(root.getByteArray(), 0, Byte.MAX_VALUE);
      }
    }
  }

  void testModiyBooleanArray(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertTrue(Boolean.TRUE.booleanValue() == Array.getBoolean(root.getBooleanArray(), 0));
    } else {
      synchronized (root) {
        Array.setBoolean(root.getBooleanArray(), 0, Boolean.TRUE.booleanValue());
      }
    }
  }

  void testModifyDoubleArray(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Double.MAX_VALUE, Array.getDouble(root.getDoubleArray(), 0));
    } else {
      synchronized (root) {
        Array.setDouble(root.getDoubleArray(), 0, Double.MAX_VALUE);
      }
    }
  }

  void testModifyFloatArray(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Float.MAX_VALUE, Array.getFloat(root.getFloatArray(), 0));
    } else {
      synchronized (root) {
        Array.setFloat(root.getFloatArray(), 0, Float.MAX_VALUE);
      }
    }
  }

  void testModifyCharArray(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Character.MAX_VALUE, Array.getChar(root.getCharArray(), 0));
    } else {
      synchronized (root) {
        Array.setChar(root.getCharArray(), 0, Character.MAX_VALUE);
      }
    }
  }

  void testSetReferenceArrayElementToNull(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertNull(root.getInstanceArray()[0]);
    } else {
      synchronized (root) {
        Array.set(root.getInstanceArray(), 0, null);
      }

      // test on a non-shared array too (for good measure)
      Object[] nonSharedArray = new Object[] { this };
      Array.set(nonSharedArray, 0, null);
    }
  }

  void testWideningModifyByteArray1(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Byte.MAX_VALUE, Array.getShort(root.getShortArray(), 0));
    } else {
      synchronized (root) {
        Array.setByte(root.getShortArray(), 0, Byte.MAX_VALUE);
      }
    }
  }

  void testWideningModifyByteArray2(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Byte.MAX_VALUE, Array.getInt(root.getIntArray(), 0));
    } else {
      synchronized (root) {
        Array.setByte(root.getIntArray(), 0, Byte.MAX_VALUE);
      }
    }
  }

  void testWideningModifyByteArray3(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Byte.MAX_VALUE, Array.getLong(root.getLongArray(), 0));
    } else {
      synchronized (root) {
        Array.setByte(root.getLongArray(), 0, Byte.MAX_VALUE);
      }
    }
  }

  void testWideningModifyByteArray4(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Byte.MAX_VALUE, Array.getFloat(root.getFloatArray(), 0));
    } else {
      synchronized (root) {
        Array.setByte(root.getFloatArray(), 0, Byte.MAX_VALUE);
      }
    }
  }

  void testWideningModifyByteArray5(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Byte.MAX_VALUE, Array.getDouble(root.getDoubleArray(), 0));
    } else {
      synchronized (root) {
        Array.setByte(root.getDoubleArray(), 0, Byte.MAX_VALUE);
      }
    }
  }

  void testWideningModifyCharArray1(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Character.MAX_VALUE, Array.getInt(root.getIntArray(), 0));
    } else {
      synchronized (root) {
        Array.setChar(root.getIntArray(), 0, Character.MAX_VALUE);
      }
    }
  }

  void testWideningModifyCharArray2(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Character.MAX_VALUE, Array.getLong(root.getLongArray(), 0));
    } else {
      synchronized (root) {
        Array.setChar(root.getLongArray(), 0, Character.MAX_VALUE);
      }
    }
  }

  void testWideningModifyCharArray3(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Character.MAX_VALUE, Array.getFloat(root.getFloatArray(), 0));
    } else {
      synchronized (root) {
        Array.setChar(root.getFloatArray(), 0, Character.MAX_VALUE);
      }
    }
  }

  void testWideningModifyCharArray4(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(Character.MAX_VALUE, Array.getDouble(root.getDoubleArray(), 0));
    } else {
      synchronized (root) {
        Array.setChar(root.getDoubleArray(), 0, Character.MAX_VALUE);
      }
    }
  }

  void testInstanceResolve(DataRoot root, boolean validate) {
    if (validate) {
      Instance[] array = root.getInstanceArray();
      Assert.assertEquals(2, Array.getLength(array));
      Assert.assertNotNull(Array.get(array, 0));
      Assert.assertNotNull(Array.get(array, 1));
    } else {
      // nothing
    }
  }

  /*
   * // Comment out JDK 1.5 Generic specific test. // JDK 1.5 Generic construct specific test. void
   * testJDK15GenericLongArray(DataRoot root, boolean validate) { if (validate) { Assert.assertEquals(20,
   * Array.getLength(root.getGenericArray())); } else { synchronized (root) { GenericTestObject<Integer>[] genericArray =
   * (GenericTestObject<Integer>[]) Array .newInstance(GenericTestObject.class, 20);
   * root.setGenericArray(genericArray); } } }
   */

  // ReadOnly tests.
  void testReadOnlyModifyLongArray(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(0, root.getLongArray()[0]);
    } else {
      synchronized (root) {
        try {
          Array.setLong(root.getLongArray(), 0, Long.MAX_VALUE);
        } catch (ReadOnlyException roe) {
          // ignore ReadOnlyException in test.
        }
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ReflectionArrayTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);

    config.addRoot(new Root(testClass, "dataRoot", "dataRoot"), true);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    config.addIncludePattern(DataRoot.class.getName());
    config.addIncludePattern(Instance.class.getName());
    new CyclicBarrierSpec().visit(visitor, config);
  }

  private static class Instance {
    //
  }

  private static class DataRoot {
    private final double[]   doubleArray   = new double[2];
    private final float[]    floatArray    = new float[2];
    private long[]           longArray     = new long[2];
    private final int[]      intArray      = new int[2];
    private final short[]    shortArray    = new short[2];
    private final byte[]     byteArray     = new byte[2];
    private final boolean[]  booleanArray  = new boolean[2];
    private final char[]     charArray     = new char[2];
    private final Instance[] instanceArray = new Instance[] { new Instance(), new Instance() };

    // private GenericTestObject<Integer>[] genericArray;

    public DataRoot() {
      super();
    }

    public Instance[] getInstanceArray() {
      return instanceArray;
    }

    public void setLongArray(long[] ls) {
      this.longArray = ls;
    }

    public double[] getDoubleArray() {
      return doubleArray;
    }

    public float[] getFloatArray() {
      return floatArray;
    }

    public long[] getLongArray() {
      return longArray;
    }

    public int[] getIntArray() {
      return intArray;
    }

    public short[] getShortArray() {
      return shortArray;
    }

    public byte[] getByteArray() {
      return byteArray;
    }

    public boolean[] getBooleanArray() {
      return booleanArray;
    }

    public char[] getCharArray() {
      return charArray;
    }

    /*
     * public GenericTestObject<Integer>[] getGenericArray() { return genericArray; } public void
     * setGenericArray(GenericTestObject<Integer>[] genericArray) { this.genericArray = genericArray; }
     */

  }

  /*
   * private static class GenericTestObject<T> { T obj; public GenericTestObject(T o) { this.obj = o; } public T
   * getObj() { return obj; } public String toString() { return obj.toString(); } }
   */
}
