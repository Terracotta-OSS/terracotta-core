/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test that makes sure "shadowed" roots work correctly with DSO
 * @author hhuynh
 */
public class ShadowRootTestApp extends AbstractErrorCatchingTransparentApp {

  private BaseClass base = new BaseClass();
  private DerivedClass derived = new DerivedClass();

  public ShadowRootTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ShadowRootTestApp.class.getName();
    config.addIncludePattern(testClass);
    config.addIncludePattern(BaseClass.class.getName());
    config.addIncludePattern(DerivedClass.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    String baseClass = BaseClass.class.getName();
    config.addRoot(baseClass+".sum1", baseClass+".sum1");
    config.addRoot(baseClass+".sum2", baseClass+".sum2");
    config.addRoot(baseClass+".sum3", baseClass+".sum3");

    String derivedClass = DerivedClass.class.getName();
    config.addRoot(derivedClass+".sum1", derivedClass+".sum1");
    config.addRoot(derivedClass+".sum2", derivedClass+".sum2");
    config.addRoot(derivedClass+".sum3", derivedClass+".sum3");

  }

  protected void runTest() throws Throwable {
    Assert.assertEquals(-1, base.get1());
    Assert.assertEquals(-1, base.get2());
    Assert.assertEquals(-1, base.get3());

    Assert.assertEquals(10, derived.get1());
    Assert.assertEquals(10, derived.get2());
    Assert.assertEquals(10, derived.get3());

    base.set1(0);
    base.set2(0);
    base.set3(0);

    derived.set1(10);
    derived.set2(10);
    derived.set3(10);

    Assert.assertEquals(1, base.addAndGet1(1));
    Assert.assertEquals(1, base.addAndGet2(1));
    Assert.assertEquals(1, base.addAndGet3(1));

    Assert.assertEquals(11, derived.addAndGet1(1));
    Assert.assertEquals(11, derived.addAndGet2(1));
    Assert.assertEquals(11, derived.addAndGet3(1));
  }

  private static class BaseClass {
    // STOP: These fields are "shadowed" on purpose -- do not rename to fix eclipse warnings
    protected AtomicInteger sum1 = new AtomicInteger(-1);
    public AtomicInteger    sum2 = new AtomicInteger(-1);
    protected int           sum3 = -1;

    public int addAndGet1(int delta) {
      return sum1.addAndGet(delta);
    }

    public int addAndGet2(int delta) {
      return sum2.addAndGet(delta);
    }

    public int addAndGet3(int delta) {
      sum3 += delta;
      return sum3;
    }

    public int get1() {
      return sum1.get();
    }

    public int get2() {
      return sum2.get();
    }

    public int get3()  {
      return sum3;
    }

    public void set1(int value) {
      sum1.set(value);
    }

    public void set2(int value) {
      sum2.set(value);
    }

    public void set3(int value) {
      sum3 = value;
    }

  }


  private static class DerivedClass extends BaseClass {
    // STOP: These fields are "shadowed" on purpose -- do not rename to fix eclipse warnings
    @SuppressWarnings("hiding")
    public AtomicInteger    sum1 = new AtomicInteger(10);
    @SuppressWarnings("hiding")
    protected AtomicInteger sum2 = new AtomicInteger(10);
    @SuppressWarnings("hiding")
    public int              sum3 = 10;

    public int addAndGet1(int delta) {
      return sum1.addAndGet(delta);
    }

    public int addAndGet2(int delta) {
      return sum2.addAndGet(delta);
    }

    public int addAndGet3(int delta) {
      sum3 += delta;
      return sum3;
    }

    public void set1(int value) {
      sum1.set(value);
    }

    public void set2(int value) {
      sum2.set(value);
    }

    public void set3(int value) {
      sum3 = value;
    }

    public int get1() {
      return sum1.get();
    }

    public int get2() {
      return sum2.get();
    }

    public int get3()  {
      return sum3;
    }
  }

}
