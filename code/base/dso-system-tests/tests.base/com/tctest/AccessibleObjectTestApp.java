/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AccessibleObjectTestApp extends AbstractTransparentApp {
  private final CyclicBarrier barrier;
  private final DataRoot      root = new DataRoot();

  public AccessibleObjectTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();

      testField(index);
      testConstructor(index);
      testMethod(index);

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testField(int index) throws Exception {
    basicFieldTest(index);
    superFieldTest(index);
    fieldSetAccessibleTest(index);
  }

  private void basicFieldTest(int index) throws Exception {
    if (index == 0) {
      Field f = Superclass.class.getDeclaredField("superString");
      f.setAccessible(true);
      root.setF1(f);
    }

    barrier.barrier();

    if (index == 1) {
      Superclass sc = new Superclass();
      Field f = root.getF1();
      f.set(sc, "Basic Field Test");
      Assert.assertEquals("Basic Field Test", sc.getSuperString());
      root.setSharedObject(sc);
    }

    barrier.barrier();

    Field f = root.getF1();
    Object o = f.get(root.getSharedObject());
    Assert.assertEquals("Basic Field Test", o);

    barrier.barrier();
  }

  private void superFieldTest(int index) throws Exception {
    if (index == 0) {
      Field f = Subclass.class.getField("subString");
      f.setAccessible(true);
      root.setF1(f);
    }

    barrier.barrier();

    if (index == 1) {
      Superclass sc = new Subclass();
      Field f = root.getF1();
      f.set(sc, "Super Field Test");
      Assert.assertEquals("Super Field Test", sc.getSubString());
      root.setSharedObject(sc);
    }

    barrier.barrier();

    Field f = root.getF1();
    Object o = f.get(root.getSharedObject());
    Assert.assertEquals("Super Field Test", o);

    barrier.barrier();
  }

  private void fieldSetAccessibleTest(int index) throws Exception {
    if (index == 0) {
      Field f = Superclass.class.getDeclaredField("superString");
      root.setF1(f);
    }

    barrier.barrier();

    if (index == 1) {
      Field f = root.getF1();

      // Do this many times to potentially get this action folded
      for (int i = 0; i < 1000; i++) {
        synchronized (root) {
          f.setAccessible(true);
        }
      }
    }

    barrier.barrier();

    Assert.assertTrue(root.getF1().isAccessible());

    barrier.barrier();

    if (index == 0) {
      Field f = root.getF1();
      synchronized (root) {
        f.setAccessible(false);
      }
    }

    barrier.barrier();

    Assert.assertFalse(root.getF1().isAccessible());

    barrier.barrier();

    if (index == 1) {
      synchronized (root) {
        AccessibleObject.setAccessible(new AccessibleObject[] { root.getF1() }, true);
      }
    }

    barrier.barrier();

    Assert.assertTrue(root.getF1().isAccessible());

    barrier.barrier();
  }

  private void testConstructor(int index) throws Exception {
    basicConstructorTest(index);
    constructorSetAccessibleTest(index);
  }

  private void basicConstructorTest(int index) throws Exception {
    if (index == 0) {
      Constructor c = Superclass.class.getDeclaredConstructor(new Class[] { Integer.TYPE, Long.TYPE, String.class });
      root.setC1(c);
    }

    barrier.barrier();

    if (index == 1) {
      Constructor c = root.getC1();
      Superclass o = (Superclass) c
          .newInstance(new Object[] { new Integer(1), new Long(10), "Basic Constructor Test" });
      Assert.assertEquals(1, o.getI());
      Assert.assertEquals(10, o.getL());
      Assert.assertEquals("Basic Constructor Test", o.getSuperString());
    }

    barrier.barrier();
  }

  private void constructorSetAccessibleTest(int index) throws Exception {
    if (index == 0) {
      Constructor c = Superclass.class.getDeclaredConstructor(new Class[] { Integer.TYPE, Long.TYPE, String.class });
      root.setC1(c);
    }

    barrier.barrier();

    if (index == 1) {
      Constructor c = root.getC1();
      synchronized (root) {
        c.setAccessible(true);
      }
    }

    barrier.barrier();

    Assert.assertTrue(root.getC1().isAccessible());

    barrier.barrier();

    if (index == 0) {
      Constructor c = root.getC1();
      synchronized (root) {
        c.setAccessible(false);
      }
    }

    barrier.barrier();

    Assert.assertFalse(root.getC1().isAccessible());

    barrier.barrier();

    if (index == 1) {
      synchronized (root) {
        AccessibleObject.setAccessible(new AccessibleObject[] { root.getC1() }, true);
      }
    }

    barrier.barrier();

    Assert.assertTrue(root.getC1().isAccessible());

    barrier.barrier();
  }

  private void testMethod(int index) throws Exception {
    basicSetMethodTest(index);
    subclassSetMethodTest1(index);
    subclassSetMethodTest2(index);
    subclassSetMethodTest3(index);
    subclassSetMethodTest4(index);
    subclassSetMethodTestStaticArray(index);
    subclassSetMethodTestUnlocked(index);
    basicGetMethodTest(index);
    subclassGetMethodTest1(index);
    subclassGetMethodTest2(index);
    subclassGetMethodTest3(index);
    subclassGetMethodTest4(index);
  }

  private void subclassSetMethodTestStaticArray(int index) throws Exception {
    if (index == 0) {
      Method m1 = Superclass.class.getDeclaredMethod("setSuperString", new Class[] { String.class });
      root.setM1(m1);
      Method m2 = Superclass.class.getDeclaredMethod("setL", new Class[] { Long.TYPE });
      root.setM2(m2);
    }

    barrier.barrier();

    if (index == 1) {
      Method m1 = root.getM1();
      Assert.assertFalse(m1.isAccessible());
      Method m2 = root.getM2();
      Assert.assertFalse(m2.isAccessible());
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        Method m1 = root.getM1();
        Method m2 = root.getM2();
        AccessibleObject.setAccessible(new AccessibleObject[] { m1, m2 }, true);
      }
    }

    barrier.barrier();

    if (index == 1) {
      Method m1 = root.getM1();
      Assert.assertTrue(m1.isAccessible());
      Method m2 = root.getM2();
      Assert.assertTrue(m2.isAccessible());

      Subclass sub = new Subclass();
      m1.invoke(sub, new Object[] { "sample super string" });
      m2.invoke(sub, new Object[] { new Long(773648L) });
      Assert.assertEquals("sample super string", sub.getSuperString());
      Assert.assertEquals(773648L, sub.getL());
    }

    barrier.barrier();
  }

  private void subclassSetMethodTestUnlocked(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("setSuperString", new Class[] { String.class });
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Assert.assertFalse(m.isAccessible());
    }

    barrier.barrier();

    if (index == 0) {
      Method m = root.getM1();
      try {
        m.setAccessible(true);
        Assert.fail();
      } catch (UnlockedSharedObjectException e) {
        Assert.assertFalse(m.isAccessible());
      }
    }

    barrier.barrier();
  }

  private void subclassSetMethodTest4(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("setSuperString", new Class[] { String.class });
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Assert.assertFalse(m.isAccessible());
    }

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        Method m = root.getM1();
        m.setAccessible(true);
      }
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Assert.assertTrue(m.isAccessible());
      Subclass sub = new Subclass();
      m.invoke(sub, new Object[] { "sample super string" });
      Assert.assertEquals("sample super string", sub.getSuperString());
    }

    barrier.barrier();
  }

  private void subclassSetMethodTest3(int index) throws Exception {
    if (index == 0) {
      Method m = Subclass.class.getMethod("setS", new Class[] { String.class });
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Subclass sub = new Subclass();
      m.invoke(sub, new Object[] { "sample string" });
      Assert.assertEquals("sample string", sub.getS());
    }

    barrier.barrier();
  }

  private void subclassSetMethodTest2(int index) throws Exception {
    if (index == 0) {
      Method m = Subclass.class.getMethod("setI", new Class[] { Integer.TYPE });
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Subclass sub = new Subclass();
      m.invoke(sub, new Object[] { new Integer(10) });
      Assert.assertEquals(10, sub.getI());
    }

    barrier.barrier();
  }

  private void subclassSetMethodTest1(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("setI", new Class[] { Integer.TYPE });
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Subclass sub = new Subclass();
      m.invoke(sub, new Object[] { new Integer(10) });
      Assert.assertEquals(10, sub.getI());
    }

    barrier.barrier();
  }

  private void subclassGetMethodTest4(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("getSuperString", new Class[] {});
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      synchronized (root) {
        m.setAccessible(true);
      }
      Subclass sub = new Subclass();
      sub.setSuperString("sample super string");
      String value = (String) m.invoke(sub, new Object[] {});
      Assert.assertEquals("sample super string", value);
    }

    barrier.barrier();
  }

  private void subclassGetMethodTest3(int index) throws Exception {
    if (index == 0) {
      Method m = Subclass.class.getMethod("getS", new Class[] {});
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Subclass sub = new Subclass();
      sub.setS("sample string");
      String value = (String) m.invoke(sub, new Object[] {});
      Assert.assertEquals("sample string", value);
    }

    barrier.barrier();
  }

  private void subclassGetMethodTest2(int index) throws Exception {
    if (index == 0) {
      Method m = Subclass.class.getMethod("getI", new Class[] {});
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Subclass sub = new Subclass();
      sub.setI(20);
      Integer value = (Integer) m.invoke(sub, new Object[] {});
      Assert.assertEquals(20, value.intValue());
    }

    barrier.barrier();
  }

  private void subclassGetMethodTest1(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("getI", new Class[] {});
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Subclass sub = new Subclass();
      sub.setI(20);
      Integer value = (Integer) m.invoke(sub, new Object[] {});
      Assert.assertEquals(20, value.intValue());
    }

    barrier.barrier();
  }

  private void basicGetMethodTest(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("getI", new Class[] {});
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Superclass sc = new Superclass();
      sc.setI(20);
      Integer value = (Integer) m.invoke(sc, new Object[] {});
      Assert.assertEquals(20, value.intValue());
    }

    barrier.barrier();
  }

  private void basicSetMethodTest(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("setI", new Class[] { Integer.TYPE });
      root.setM1(m);
    }

    barrier.barrier();

    if (index == 1) {
      Method m = root.getM1();
      Superclass sc = new Superclass();
      m.invoke(sc, new Object[] { new Integer(10) });
      Assert.assertEquals(10, sc.getI());
    }

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");
    String testClass = AccessibleObjectTestApp.class.getName();
    config.getOrCreateSpec(testClass).addRoot("barrier", "barrier").addRoot("root", "root");
    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);
    config.addIncludePattern(testClass + "$*");
  }

  private static class Superclass {
    private int    i;
    private long   l;
    private String superString;
    public String  subString;

    public Superclass() {
      super();
    }

    // this cstr is accessed reflectively
    @SuppressWarnings("unused")
    public Superclass(int i, long l, String superString) {
      this.i = i;
      this.l = l;
      this.superString = superString;
    }

    public int getI() {
      return i;
    }

    public void setI(int i) {
      this.i = i;
    }

    protected String getSuperString() {
      return superString;
    }

    protected void setSuperString(String superString) {
      this.superString = superString;
    }

    public synchronized String getSubString() {
      return subString;
    }

    // method is accessed reflectively
    @SuppressWarnings("unused")
    public synchronized void setSubString(String subString) {
      this.subString = subString;
    }

    protected long getL() {
      return l;
    }

    // method is accessed reflectively
    @SuppressWarnings("unused")
    protected void setL(long l) {
      this.l = l;
    }
  }

  private static class Subclass extends Superclass {
    private String s;

    public String getS() {
      return s;
    }

    public void setS(String s) {
      this.s = s;
    }
  }

  private static class DataRoot {
    private Method      m1;
    private Method      m2;
    private Constructor c1;
    private Field       f1;
    private Superclass  sharedObject;

    public synchronized Method getM1() {
      return m1;
    }

    public synchronized void setM1(Method m) {
      this.m1 = m;
    }

    public synchronized Method getM2() {
      return m2;
    }

    public synchronized void setM2(Method m) {
      this.m2 = m;
    }

    public synchronized Superclass getSharedObject() {
      return sharedObject;
    }

    public synchronized void setSharedObject(Superclass sharedObject) {
      this.sharedObject = sharedObject;
    }

    public synchronized Constructor getC1() {
      return c1;
    }

    public synchronized void setC1(Constructor c1) {
      this.c1 = c1;
    }

    public synchronized Field getF1() {
      return f1;
    }

    public synchronized void setF1(Field f1) {
      this.f1 = f1;
    }
  }
}
