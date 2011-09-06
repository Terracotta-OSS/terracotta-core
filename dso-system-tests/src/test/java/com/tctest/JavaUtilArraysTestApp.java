/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.lang.ArrayUtils;

import EDU.oswego.cs.dl.util.concurrent.Latch;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Makes sure that the methods in java.util.Arrays work with DSO managed arrays
 */
public class JavaUtilArraysTestApp extends AbstractTransparentApp {

  private final List  data = new ArrayList();
  private final Latch latch;

  public JavaUtilArraysTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    if (getParticipantCount() != 2) { throw new RuntimeException("invalid participant count " + getParticipantCount()); }
    this.latch = new Latch();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = JavaUtilArraysTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("latch", "latch");
    spec.addRoot("data", "data");
    config.addIncludePattern(Data.class.getName());
    String latchClassName = Latch.class.getName();
    config.addIncludePattern(latchClassName);
    // config.addWriteAutolock("* " + latchClassName + "*.*(..)");
  }

  public void run() {
    final boolean firstNode;

    synchronized (data) {
      firstNode = data.size() == 0;
      if (firstNode) {
        data.add("temp");
      }
    }

    if (!firstNode) {
      try {
        synchronized (latch) {
          latch.acquire();
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      validateData();
    } else {
      setupData();
      synchronized (latch) {
        latch.release();
      }
    }
  }

  private String expected(Object theWayItShouldBe, Object theWayItIs) {
    return "expected " + ArrayUtils.toString(theWayItShouldBe) + ", but found: " + ArrayUtils.toString(theWayItIs);
  }

  private void validateData() {
    Data d;

    synchronized (data) {
      d = (Data) data.remove(0);
    }

    synchronized (d) {
      byte[] byteFillCompare = new byte[10];
      Arrays.fill(byteFillCompare, (byte) 1);
      if (!Arrays.equals(byteFillCompare, d.filledByte)) {
        // formatting
        throw new RuntimeException("filled bytes " + expected(byteFillCompare, d.filledByte));
      }
      byte[] sortedByte = new byte[] { Byte.MIN_VALUE, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, Byte.MAX_VALUE };
      if (!Arrays.equals(sortedByte, d.sortedByte)) {
        // formatting
        throw new RuntimeException("sorted bytes: " + expected(sortedByte, d.sortedByte));
      }

      char[] charFillCompare = new char[10];
      Arrays.fill(charFillCompare, 'z');
      if (!Arrays.equals(charFillCompare, d.filledChar)) {
        // formatting
        throw new RuntimeException("filled chars: " + expected(charFillCompare, d.filledChar));
      }
      char[] sortedChar = new char[] { Character.MIN_VALUE, 'c', 'e', 'f', 'k', 'm', 'u', Character.MAX_VALUE };
      if (!Arrays.equals(sortedChar, d.sortedChar)) {
        // formatting
        throw new RuntimeException("sorted chars: " + expected(sortedChar, d.sortedChar));
      }

      double[] doubleFillCompare = new double[10];
      Arrays.fill(doubleFillCompare, Math.PI);
      if (!Arrays.equals(doubleFillCompare, d.filledDouble)) {
        // formatting
        throw new RuntimeException("filled doubles: " + expected(doubleFillCompare, d.filledDouble));
      }
      double[] sortedDouble = new double[] { -Double.MAX_VALUE, -1.43D, -Double.MIN_VALUE, 0D, Double.MIN_VALUE,
          Math.E, Math.PI, Double.MAX_VALUE };
      if (!Arrays.equals(sortedDouble, d.sortedDouble)) {
        // formatting
        throw new RuntimeException("sorted doubles: " + expected(sortedDouble, d.sortedDouble));
      }

      float[] floatFillCompare = new float[10];
      Arrays.fill(floatFillCompare, 8.1F);
      if (!Arrays.equals(floatFillCompare, d.filledFloat)) {
        // formatting
        throw new RuntimeException("filled floats: " + expected(floatFillCompare, d.filledFloat));
      }
      float[] sortedFloat = new float[] { -123F, 0F, Float.MIN_VALUE, 3.14F, Float.MAX_VALUE };
      if (!Arrays.equals(sortedFloat, d.sortedFloat)) {
        // formatting
        throw new RuntimeException("sorted floats: " + expected(sortedFloat, d.sortedFloat));
      }

      int[] intFillCompare = new int[10];
      Arrays.fill(intFillCompare, 1);
      if (!Arrays.equals(intFillCompare, d.filledInt)) {
        // formatting
        throw new RuntimeException("filled ints: " + expected(intFillCompare, d.filledInt));
      }
      int[] sortedInt = new int[] { Integer.MIN_VALUE, 1, 2, 7, 8, Integer.MAX_VALUE };
      if (!Arrays.equals(sortedInt, d.sortedInt)) {
        // formatting
        throw new RuntimeException("sorted ints: " + expected(sortedInt, d.sortedInt));
      }

      long[] longFillCompare = new long[10];
      Arrays.fill(longFillCompare, 42L);
      if (!Arrays.equals(longFillCompare, d.filledLong)) {
        // formatting
        throw new RuntimeException("filled longs: " + expected(longFillCompare, d.filledLong));
      }
      long[] sortedLong = new long[] { Long.MIN_VALUE, 1L, 5L, 1000L, Long.MAX_VALUE };
      if (!Arrays.equals(sortedLong, d.sortedLong)) {
        // formatting
        throw new RuntimeException("sorted longs: " + expected(sortedLong, d.sortedLong));
      }

      short[] shortFillCompare = new short[10];
      Arrays.fill(shortFillCompare, (short) 69);
      if (!Arrays.equals(shortFillCompare, d.filledShort)) {
        // formatting
        throw new RuntimeException("filled shorts: " + expected(shortFillCompare, d.filledShort));
      }
      short[] sortedShort = new short[] { Short.MIN_VALUE, 0, 2, Short.MAX_VALUE };
      if (!Arrays.equals(sortedShort, d.sortedShort)) {
        // formatting
        throw new RuntimeException("sorted shorts: " + expected(sortedShort, d.sortedShort));
      }

      Object[] objectFillCompare = new Object[10];
      Arrays.fill(objectFillCompare, Integer.valueOf(1));
      if (!Arrays.equals(objectFillCompare, d.filledObject)) {
        // formatting
        throw new RuntimeException("filled objects: " + expected(objectFillCompare, d.filledObject));
      }
      Object[] sortedObject = new Object[] { Integer.valueOf(-4), Integer.valueOf(3) };
      if (!Arrays.equals(sortedObject, d.sortedObject)) {
        // formatting
        throw new RuntimeException("sorted objects: " + expected(sortedObject, d.sortedObject));
      }

      boolean[] booleanFillCompare = new boolean[10];
      Arrays.fill(booleanFillCompare, false);
      if (!Arrays.equals(booleanFillCompare, d.filledBoolean)) {
        // formatting
        throw new RuntimeException("filled boolean: " + expected(booleanFillCompare, d.filledBoolean));
      }

      List list = d.asList;
      if (list.size() != 2) { throw new RuntimeException("list wrong size: " + list.size()); }
      if (!list.get(0).equals(Integer.valueOf(-4))) { throw new RuntimeException("wrong data"); }
      if (!list.get(1).equals(Integer.valueOf(3))) { throw new RuntimeException("wrong data"); }

      Object[] array = d.dataBehindAsList2;
      if (array.length != 4) { throw new RuntimeException("array size wrong: " + array.length); }
      Assert.assertEquals("put", array[0]);
      Assert.assertEquals("the", array[1]);
      Assert.assertEquals("rap", array[2]);
      Assert.assertEquals("down", array[3]);
    }
  }

  private void setupData() {
    Data d = new Data();

    synchronized (data) {
      data.clear();
      data.add(d);
    }

    d.sort();
    d.fill();
    d.modifyAsList2();
  }

  // NOTE: There is no such thing as sorted booleans
  private static final byte[]   unsortedByte   = new byte[] { 0, 6, 3, 7, Byte.MAX_VALUE, 4, 2, 9, 8, 1, 5,
      Byte.MIN_VALUE                          };
  private static final char[]   unsortedChar   = new char[] { 'f', Character.MAX_VALUE, 'e', 'u', 'k', 'c', 'm',
      Character.MIN_VALUE                     };
  private static final double[] unsortedDouble = new double[] { Math.PI, Double.MIN_VALUE, -Double.MAX_VALUE, Math.E,
      Double.MAX_VALUE, 0D, -Double.MIN_VALUE, -1.43D };
  private static final float[]  unsortedFloat  = new float[] { Float.MAX_VALUE, 0F, Float.MIN_VALUE, -123F, 3.14F };
  private static final int[]    unsortedInt    = new int[] { 2, Integer.MAX_VALUE, 7, Integer.MIN_VALUE, 8, 1 };
  private static final long[]   unsortedLong   = new long[] { 1000L, 5L, 1L, Long.MIN_VALUE, Long.MAX_VALUE };
  private static final short[]  unsortedShort  = new short[] { Short.MAX_VALUE, 2, Short.MIN_VALUE, 0 };
  private static final Object[] unsortedObject = new Object[] { Integer.valueOf(3), Integer.valueOf(-4) };

  private static class Data {

    private final boolean[] filledBoolean;
    // NOTE: There is no such thing as sorted booleans

    private final byte[]    filledByte;
    private final byte[]    sortedByte;

    private final char[]    filledChar;
    private final char[]    sortedChar;

    private final double[]  filledDouble;
    private final double[]  sortedDouble;

    private final float[]   filledFloat;
    private final float[]   sortedFloat;

    private final int[]     filledInt;
    private final int[]     sortedInt;

    private final long[]    filledLong;
    private final long[]    sortedLong;

    private final short[]   filledShort;
    private final short[]   sortedShort;

    private final Object[]  filledObject;
    private final Object[]  sortedObject;

    private final List      asList;
    private final List      asList2;

    private final Object[]  dataBehindAsList2;

    Data() {
      this.filledBoolean = new boolean[10];
      Arrays.fill(filledBoolean, true);

      this.filledByte = new byte[10];
      this.sortedByte = unsortedByte.clone();

      this.filledChar = new char[10];
      this.sortedChar = unsortedChar.clone();

      this.filledDouble = new double[10];
      this.sortedDouble = unsortedDouble.clone();

      this.filledInt = new int[10];
      this.sortedInt = unsortedInt.clone();

      this.filledFloat = new float[10];
      this.sortedFloat = unsortedFloat.clone();

      this.filledLong = new long[10];
      this.sortedLong = unsortedLong.clone();

      this.filledShort = new short[10];
      this.sortedShort = unsortedShort.clone();

      this.filledObject = new Object[10];
      this.sortedObject = unsortedObject.clone();

      // the underlying array will be sorted(), so we should expect the List to come out sorted on the other side
      this.asList = Arrays.asList(this.sortedObject);

      this.dataBehindAsList2 = new Object[4];
      this.dataBehindAsList2[0] = "put";
      this.dataBehindAsList2[1] = "the";
      this.dataBehindAsList2[2] = "smack";
      this.dataBehindAsList2[3] = "down";

      this.asList2 = Arrays.asList(this.dataBehindAsList2);
    }

    synchronized void fill() {
      Arrays.fill(this.filledBoolean, false);
      Arrays.fill(this.filledByte, (byte) 1);
      Arrays.fill(this.filledChar, 'z');
      Arrays.fill(this.filledDouble, Math.PI);
      Arrays.fill(this.filledFloat, 8.1F);
      Arrays.fill(this.filledInt, 1);
      Arrays.fill(this.filledLong, 42L);
      Arrays.fill(this.filledShort, (short) 69);
      Arrays.fill(this.filledObject, Integer.valueOf(1));
    }

    synchronized void sort() {
      Arrays.sort(this.sortedByte);
      // There is no boolean natural sort
      Arrays.sort(this.sortedChar);
      Arrays.sort(this.sortedDouble);
      Arrays.sort(this.sortedFloat);
      Arrays.sort(this.sortedInt);
      Arrays.sort(this.sortedLong);
      Arrays.sort(this.sortedShort);
      Arrays.sort(this.sortedObject);
    }

    synchronized void modifyAsList2() {
      // test that the modifications made through the asList() makes it into the backend DSO managed array
      this.asList2.set(2, "rap");
    }

  }

  // private static class Latch {
  // private boolean set = false;
  //
  // synchronized void acquire() {
  // while (!set) {
  // try {
  // wait();
  // } catch (InterruptedException e) {
  // throw new RuntimeException(e);
  // }
  // }
  // }
  //
  // synchronized void release() {
  // set = true;
  // notifyAll();
  // }
  // }

}
