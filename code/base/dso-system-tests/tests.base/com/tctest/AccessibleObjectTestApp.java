/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import java.lang.reflect.Method;

public class AccessibleObjectTestApp extends AbstractTransparentApp {
  private CyclicBarrier  barrier;
  private final DataRoot root = new DataRoot();

  public AccessibleObjectTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();

      testMethod(index);
    } catch (Throwable t) {
      notifyError(t);
    }
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

    protected long getL() {
      return l;
    }

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
    private Method     m1;
    private Method     m2;
    private Superclass sharedObject;

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

    public Superclass getSharedObject() {
      return sharedObject;
    }

    public void setSharedObject(Superclass sharedObject) {
      this.sharedObject = sharedObject;
    }
  }
}
