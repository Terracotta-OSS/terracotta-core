/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.LockDefinitionImpl;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

public class InstrumentedConstructorTestApp extends AbstractTransparentApp {

  private final CyclicBarrier barrier;
  private final DataRoot      dataRoot = new DataRoot();

  public InstrumentedConstructorTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();

      TestConstructorClass c = new TestConstructorClass();
      testShared(index, c);

      c = new TestConstructorClass(10L);
      testShared(index, c);

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testShared(int index, TestConstructorClass c) throws Exception {
    if (index == 0) {
      synchronized (dataRoot) {
        dataRoot.setC1(c);
      }
    }

    barrier.barrier();

    Assert.assertNotNull(dataRoot.getC1());

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = InstrumentedConstructorTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "$TestConstructorClass.*(..)";
    LockDefinition definition = new LockDefinitionImpl("nameLock", ConfigLockLevel.WRITE);
    definition.commit();
    config.addLock(methodExpression, definition);

    methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("dataRoot", "dataRoot");
  }

  private static class DataRoot {
    private TestConstructorClass c1;

    public DataRoot() {
      super();
    }

    public void setC1(TestConstructorClass c1) {
      this.c1 = c1;
    }

    public TestConstructorClass getC1() {
      return c1;
    }
  }

  private static class TestConstructorSuperClass {
    @SuppressWarnings("unused")
    private String s;

    public TestConstructorSuperClass() {
      //
    }

    public TestConstructorSuperClass(String s) {
      this.s = s;
    }

  }

  private static class TestConstructorClass extends TestConstructorSuperClass {
    public TestConstructorClass() {
      super(new StringBuffer("testString").toString());
    }

    public TestConstructorClass(TestConstructorClass c) {
      //
    }

    public TestConstructorClass(long k) {
      this(new TestConstructorClass());
    }
  }

}
