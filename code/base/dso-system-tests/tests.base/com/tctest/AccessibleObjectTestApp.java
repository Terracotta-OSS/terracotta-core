/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.lang.reflect.Method;

public class AccessibleObjectTestApp extends AbstractTransparentApp {
  private CyclicBarrier barrier;
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
    basicGetMethodTest(index);
    subclassGetMethodTest1(index);
    subclassGetMethodTest2(index);
    subclassGetMethodTest3(index);
    subclassGetMethodTest4(index);
  }
  
  private void subclassSetMethodTest4(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("setSuperString", new Class[]{String.class});
      root.setM(m);
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      Assert.assertFalse(m.isAccessible());
    }
    
    barrier.barrier();
    
    if (index == 0) {
      synchronized(root) {
        Method m = root.getM();
        m.setAccessible(true);
      }
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      Assert.assertTrue(m.isAccessible());
      Subclass sub = new Subclass();
      m.invoke(sub, new Object[]{"sample super string"});
      Assert.assertEquals("sample super string", sub.getSuperString());
    }
    
    barrier.barrier();
  }
  
  private void subclassSetMethodTest3(int index) throws Exception {
    if (index == 0) {
      Method m = Subclass.class.getMethod("setS", new Class[]{String.class});
      root.setM(m);
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      Subclass sub = new Subclass();
      m.invoke(sub, new Object[]{"sample string"});
      Assert.assertEquals("sample string", sub.getS());
    }
    
    barrier.barrier();
  }
  
  private void subclassSetMethodTest2(int index) throws Exception {
    if (index == 0) {
      Method m = Subclass.class.getMethod("setI", new Class[]{Integer.TYPE});
      root.setM(m);
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      Subclass sub = new Subclass();
      m.invoke(sub, new Object[]{new Integer(10)});
      Assert.assertEquals(10, sub.getI());
    }
    
    barrier.barrier();
  }
  
  private void subclassSetMethodTest1(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("setI", new Class[]{Integer.TYPE});
      root.setM(m);
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      Subclass sub = new Subclass();
      m.invoke(sub, new Object[]{new Integer(10)});
      Assert.assertEquals(10, sub.getI());
    }
    
    barrier.barrier();
  }
  
  private void subclassGetMethodTest4(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("getSuperString", new Class[]{});
      root.setM(m);
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      synchronized(root) {
        m.setAccessible(true);
      }
      Subclass sub = new Subclass();
      sub.setSuperString("sample super string");
      String value = (String)m.invoke(sub, new Object[]{});
      Assert.assertEquals("sample super string", value);
    }
    
    barrier.barrier();
  }
  
  private void subclassGetMethodTest3(int index) throws Exception {
    if (index == 0) {
      Method m = Subclass.class.getMethod("getS", new Class[]{});
      root.setM(m);
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      Subclass sub = new Subclass();
      sub.setS("sample string");
      String value = (String)m.invoke(sub, new Object[]{});
      Assert.assertEquals("sample string", value);
    }
    
    barrier.barrier();
  }
  
  private void subclassGetMethodTest2(int index) throws Exception {
    if (index == 0) {
      Method m = Subclass.class.getMethod("getI", new Class[]{});
      root.setM(m);
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      Subclass sub = new Subclass();
      sub.setI(20);
      Integer value = (Integer)m.invoke(sub, new Object[]{});
      Assert.assertEquals(20, value.intValue());
    }
    
    barrier.barrier();
  }
  
  private void subclassGetMethodTest1(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("getI", new Class[]{});
      root.setM(m);
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      Subclass sub = new Subclass();
      sub.setI(20);
      Integer value = (Integer)m.invoke(sub, new Object[]{});
      Assert.assertEquals(20, value.intValue());
    }
    
    barrier.barrier();
  }
  
  private void basicGetMethodTest(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("getI", new Class[]{});
      root.setM(m);
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      Superclass sc = new Superclass();
      sc.setI(20);
      Integer value = (Integer)m.invoke(sc, new Object[]{});
      Assert.assertEquals(20, value.intValue());
    }
    
    barrier.barrier();
  }

  
  private void basicSetMethodTest(int index) throws Exception {
    if (index == 0) {
      Method m = Superclass.class.getDeclaredMethod("setI", new Class[]{Integer.TYPE});
      root.setM(m);
    }
    
    barrier.barrier();
    
    if (index == 1) {
      Method m = root.getM();
      Superclass sc = new Superclass();
      m.invoke(sc, new Object[]{new Integer(10)});
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
    private int i;
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
    private Method m;
    private Superclass sharedObject;

    public synchronized Method getM() {
      return m;
    }

    public synchronized void setM(Method m) {
      this.m = m;
    }

    public Superclass getSharedObject() {
      return sharedObject;
    }

    public void setSharedObject(Superclass sharedObject) {
      this.sharedObject = sharedObject;
    }
  }
}
