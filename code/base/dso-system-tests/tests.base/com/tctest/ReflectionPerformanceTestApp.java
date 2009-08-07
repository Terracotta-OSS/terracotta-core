/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.awt.Color;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

/*
 * The test cases defined in this class is for measuring the overhead of the instrumented Field class. For correctness
 * tests for instrumented Field class, refer to the ReflectionFieldTestApp.
 */
public class ReflectionPerformanceTestApp extends AbstractTransparentApp {

  private final static int                COUNT                 = 1000000;

  private final DataRoot                  dataRoot              = new DataRoot();
  private final DataRoot                  nonSharedObject       = new DataRoot();
  private final NonInstrumentedTestObject nonInstrumentedObject = new NonInstrumentedTestObject();

  public ReflectionPerformanceTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    reflectionFieldPerformanceTest();
    reflectionArrayPerformanceTest();
  }

  private void reflectionArrayPerformanceTest() {
    System.out.println("==========Performance Tests for java.lang.reflect.Array begin==========");
    System.out.println("==========Performance Tests on Non-Instrumented Array begin==========");
    modifyNonInstrumentedPrimitiveArrayTest();
    System.gc();
    modifyNonInstrumentedReferenceArrayTest();
    System.out.println("==========Performance Tests on Non-Instrumented Array end==========");
    System.out.println();
    System.gc();

    System.out.println("==========Performance Tests on Non-Shared Array begin==========");
    modifyNonSharedPrimitiveTest();
    System.gc();
    modifyNonSharedReferenceArrayTest();
    System.out.println("==========Performance Tests on Non-Shared Array end==========");
    System.out.println();
    System.gc();

    System.out.println("==========Performance Tests on Shared Array begin==========");
    modifySharedPrimitiveTest();
    System.gc();
    modifySharedReferenceArrayTest();
    System.out.println("==========Performance Tests on Shared Array end==========");
    System.out.println("==========Performance Tests for java.lang.reflect.Array end==========");
  }

  private void modifySharedPrimitiveTest() {
    long[] longArray = dataRoot.getLongArray();

    synchronized (dataRoot) {
      long start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        Array.setLong(longArray, 0, Long.MAX_VALUE);
      }
      long end = System.currentTimeMillis();
      long elapsed = end - start;
      System.out.println("Elapsed time for modifying shared primitive array: " + elapsed + "msec.");
    }
  }

  private void modifySharedReferenceArrayTest() {
    Object[] objectArray = dataRoot.getObjectArray();
    Object newValue = new Object();

    synchronized (dataRoot) {
      long start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        Array.set(objectArray, 0, newValue);
      }
      long end = System.currentTimeMillis();
      long elapsed = end - start;
      System.out.println("Elapsed time for modifying shared reference array: " + elapsed + "msec.");
    }
  }

  private void modifyNonSharedPrimitiveTest() {
    long[] longArray = nonSharedObject.getLongArray();

    long start = System.currentTimeMillis();
    for (int i = 0; i < COUNT; i++) {
      Array.setLong(longArray, 0, Long.MAX_VALUE);
    }
    long end = System.currentTimeMillis();
    long elapsed = end - start;
    System.out.println("Elapsed time for modifying non shared primitive array: " + elapsed + "msec.");
  }

  private void modifyNonSharedReferenceArrayTest() {
    Object[] objectArray = nonSharedObject.getObjectArray();
    Object newValue = new Object();

    long start = System.currentTimeMillis();
    for (int i = 0; i < COUNT; i++) {
      Array.set(objectArray, 0, newValue);
    }
    long end = System.currentTimeMillis();
    long elapsed = end - start;
    System.out.println("Elapsed time for modifying non shared reference array: " + elapsed + "msec.");
  }

  private void modifyNonInstrumentedPrimitiveArrayTest() {
    long[] longArray = nonInstrumentedObject.getLongArray();
    long start = System.currentTimeMillis();
    for (int i = 0; i < COUNT; i++) {
      Array.setLong(longArray, 0, Long.MAX_VALUE);
    }
    long end = System.currentTimeMillis();
    long elapsed = end - start;
    System.out.println("Elapsed time for modifying non instrumented primitive array: " + elapsed + "msec.");
  }

  private void modifyNonInstrumentedReferenceArrayTest() {
    Object[] objectArray = nonInstrumentedObject.getObjectArray();
    Object newObject = new Object();
    long start = System.currentTimeMillis();
    for (int i = 0; i < COUNT; i++) {
      Array.set(objectArray, 0, newObject);
    }
    long end = System.currentTimeMillis();
    long elapsed = end - start;
    System.out.println("Elapsed time for modifying non instrumented reference array: " + elapsed + "msec.");
  }

  private void reflectionFieldPerformanceTest() {
    System.out.println("==========Performance Tests for java.lang.reflect.Field begin==========");
    System.out.println("==========Performance Tests on Non-Instrumented Objects begin==========");
    modifyNonInstrumentedObjectTest();
    System.gc();
    modifyNonInstrumentedObjectReferenceTest();
    System.gc();
    retrieveNonInstrumentedObjectTest();
    System.out.println("==========Performance Tests on Non-Instrumented Objects end==========");
    System.out.println();
    System.gc();

    System.out.println("==========Performance Tests on Non-Shared Objects begin==========");
    modifyNonSharedObjectTest();
    System.gc();
    modifyNonSharedObjectReferenceTest();
    System.gc();
    retrieveNonSharedObjectTest();
    System.out.println("==========Performance Tests on Non-Shared Objects end==========");
    System.out.println();
    System.gc();

    System.out.println("==========Performance Tests on Shared Objects begin==========");
    modifySharedObjectTest();
    System.gc();
    modifySharedObjectReferenceTest();
    System.gc();
    retrieveSharedObjectTest();
    System.out.println("==========Performance Tests on Shared Objects end==========");
    System.out.println("==========Performance Tests for java.lang.reflect.Field end==========");
    System.out.println();
  }

  private void modifyNonInstrumentedObjectTest() {
    try {
      Field longValueField = nonInstrumentedObject.getClass().getDeclaredField("longValue");
      longValueField.setAccessible(true);
      long start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        longValueField.setLong(nonInstrumentedObject, Long.MAX_VALUE);
      }
      long end = System.currentTimeMillis();
      long elapsed = end - start;
      System.out.println("Elapsed time for modifying non instrumented objects: " + elapsed + "msec.");
    } catch (IllegalAccessException iae) {
      // ignore IllegalAccessException in test.
    } catch (NoSuchFieldException e) {
      // ignore NoSuchFieldException in test.
    }

  }

  private void modifyNonInstrumentedObjectReferenceTest() {
    Color newColor = new Color(200, true);

    try {
      Field colorField = nonInstrumentedObject.getClass().getDeclaredField("color");
      colorField.setAccessible(true);
      long start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        colorField.set(nonInstrumentedObject, newColor);
      }
      long end = System.currentTimeMillis();
      long elapsed = end - start;
      System.out.println("Elapsed time for modifying non instrumented object reference: " + elapsed + "msec.");
    } catch (IllegalAccessException iae) {
      // ignore IllegalAccessException in test.
    } catch (NoSuchFieldException e) {
      // ignore NoSuchFieldException in test.
    }

  }

  private void retrieveNonInstrumentedObjectTest() {
    try {
      Field colorField = nonInstrumentedObject.getClass().getDeclaredField("color");
      colorField.setAccessible(true);
      long start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        colorField.get(nonInstrumentedObject);
      }
      long end = System.currentTimeMillis();
      long elapsed = end - start;
      System.out.println("Elapsed time for retrieving non instrumented objects: " + elapsed + "msec.");
    } catch (IllegalAccessException iae) {
      // ignore IllegalAccessException in test.
    } catch (NoSuchFieldException e) {
      // ignore NoSuchFieldException in test.
    }

  }

  private void modifyNonSharedObjectTest() {
    try {
      Field longValueField = nonSharedObject.getClass().getDeclaredField("longValue");
      longValueField.setAccessible(true);
      long start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        longValueField.setLong(nonSharedObject, Long.MAX_VALUE);
      }
      long end = System.currentTimeMillis();
      long elapsed = end - start;
      System.out.println("Elapsed time for modifying non shared objects: " + elapsed + "msec.");
    } catch (IllegalAccessException iae) {
      // ignore IllegalAccessException in test.
    } catch (NoSuchFieldException e) {
      // ignore NoSuchFieldException in test.
    }

  }

  private void modifyNonSharedObjectReferenceTest() {
    Color newColor = new Color(200, true);

    try {
      Field colorField = nonSharedObject.getClass().getDeclaredField("color");
      colorField.setAccessible(true);
      long start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        colorField.set(nonSharedObject, newColor);
      }
      long end = System.currentTimeMillis();
      long elapsed = end - start;
      System.out.println("Elapsed time for modifying non shared object reference: " + elapsed + "msec.");
    } catch (IllegalAccessException iae) {
      // ignore IllegalAccessException in test.
    } catch (NoSuchFieldException e) {
      // ignore NoSuchFieldException in test.
    }

  }

  private void retrieveNonSharedObjectTest() {
    try {
      Field colorField = nonSharedObject.getClass().getDeclaredField("color");
      colorField.setAccessible(true);
      long start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        colorField.get(nonSharedObject);
      }
      long end = System.currentTimeMillis();
      long elapsed = end - start;
      System.out.println("Elapsed time for retrieving non shared objects: " + elapsed + "msec.");
    } catch (IllegalAccessException iae) {
      // ignore IllegalAccessException in test.
    } catch (NoSuchFieldException e) {
      // ignore NoSuchFieldException in test.
    }

  }

  private void modifySharedObjectTest() {
    synchronized (dataRoot) {

      try {
        Field longValueField = dataRoot.getClass().getDeclaredField("longValue");
        longValueField.setAccessible(true);
        long start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
          longValueField.setLong(dataRoot, Long.MAX_VALUE);
        }
        long end = System.currentTimeMillis();
        long elapsed = end - start;
        System.out.println("Elapsed time for modifying shared objects: " + elapsed + "msec.");
      } catch (IllegalAccessException iae) {
        // ignore IllegalAccessException in test.
      } catch (NoSuchFieldException e) {
        // ignore NoSuchFieldException in test.
      }

    }
  }

  private void modifySharedObjectReferenceTest() {
    Color newColor = new Color(200, true);

    synchronized (dataRoot) {

      try {
        Field colorField = dataRoot.getClass().getDeclaredField("color");
        colorField.setAccessible(true);
        long start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
          colorField.set(dataRoot, newColor);
        }
        long end = System.currentTimeMillis();
        long elapsed = end - start;
        System.out.println("Elapsed time for modifying shared object reference: " + elapsed + "msec.");
      } catch (IllegalAccessException iae) {
        // ignore IllegalAccessException in test.
      } catch (NoSuchFieldException e) {
        // ignore NoSuchFieldException in test.
      }

    }
  }

  private void retrieveSharedObjectTest() {
    try {
      Field colorField = dataRoot.getClass().getDeclaredField("color");
      colorField.setAccessible(true);
      long start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        colorField.get(dataRoot);
      }
      long end = System.currentTimeMillis();
      long elapsed = end - start;
      System.out.println("Elapsed time for retrieving shared objects: " + elapsed + "msec.");
    } catch (IllegalAccessException iae) {
      // ignore IllegalAccessException in test.
    } catch (NoSuchFieldException e) {
      // ignore NoSuchFieldException in test.
    }

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ReflectionPerformanceTestApp.class.getName();
    String methodExpression = "* " + testClass + ".*(..)";
    config.addWriteAutolock(methodExpression);
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("dataRoot", "dataRoot");
    config.addIncludePattern(DataRoot.class.getName());
  }

  @SuppressWarnings("unused")
  private static class DataRoot {
    private long[]   longArray   = new long[2];
    private Object[] objectArray = new Object[2];
    private Color    color       = new Color(100, true);
    private long     longValue   = Long.MIN_VALUE;

    public DataRoot() {
      super();
    }

    protected Color getColor() {
      return color;
    }

    protected void setColor(Color color) {
      this.color = color;
    }

    protected long getLongValue() {
      return longValue;
    }

    protected void setLongValue(long longValue) {
      this.longValue = longValue;
    }

    public long[] getLongArray() {
      return longArray;
    }

    public void setLongArray(long[] longArray) {
      this.longArray = longArray;
    }

    public Object[] getObjectArray() {
      return objectArray;
    }

    public void setObjectArray(Object[] objectArray) {
      this.objectArray = objectArray;
    }

  }

}
