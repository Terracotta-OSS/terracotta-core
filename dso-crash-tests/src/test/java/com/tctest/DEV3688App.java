/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.test.schema.InstrumentedClassConfigBuilderImpl;
import com.tc.config.test.schema.LockConfigBuilderImpl;
import com.tc.config.test.schema.RootConfigBuilderImpl;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class DEV3688App extends AbstractTransparentApp {
  private final static int                  BARRIER_COUNT     = 2;
  private final static ArrayList<IntNumber> mySharedArrayList = new ArrayList<IntNumber>();
  private final static CyclicBarrier        barrier           = new CyclicBarrier(BARRIER_COUNT);

  public DEV3688App(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  public void run() {
    // do initialization to the array -- add some 1000 numbers and wait for 2nd client to come up
    synchronized (mySharedArrayList) {
      int length = mySharedArrayList.size();
      for (int i = length; i < 1000 + length; i++) {
        if (i % 100 == 0) {
          System.out.println("Adding " + i);
        }
        mySharedArrayList.add(new IntNumber(i));
      }
    }

    waitOnBarrier();
  }

  private void waitOnBarrier() {
    try {
      barrier.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (BrokenBarrierException e) {
      e.printStackTrace();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(DEV3688App.class.getName());

    config.addIncludePattern(IntNumber.class.getName());
    config.addIncludePattern(DEV3688App.class.getName());
    config.addIncludePattern(DEV3688Worker.class.getName());

    String testClass = DEV3688App.class.getName();

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    String methodExpression2 = "* " + DEV3688Worker.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression2);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("mySharedArrayList", "mySharedArrayList");
  }

  public static class IntNumber {
    private int i;

    public IntNumber(int i) {
      this.i = i;
    }

    public int get() {
      return i;
    }

    public void set(int i) {
      this.i = i;
    }
  }

  public static class DEV3688Worker {
    public void run() {
      synchronized (mySharedArrayList) {
        int length = mySharedArrayList.size();
        for (int i = length; i < 1000 + length; i++) {
          if (i % 100 == 0) {
            System.out.println("Adding " + i);
          }
          mySharedArrayList.add(new IntNumber(i));
        }
      }

      waitOnBarrier();
    }

    private void waitOnBarrier() {
      try {
        barrier.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (BrokenBarrierException e) {
        e.printStackTrace();
      }
    }

    public static void main(String[] args) {
      new DEV3688Worker().run();
    }
  }

  public static TerracottaConfigBuilder getTerracottaConfigBuilder() {
    try {
      TerracottaConfigBuilder cb = new TerracottaConfigBuilder();

      InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
      instrumented1.setClassExpression(DEV3688App.class.getName());

      InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
      instrumented2.setClassExpression(DEV3688App.DEV3688Worker.class.getName());

      InstrumentedClassConfigBuilder instrumented3 = new InstrumentedClassConfigBuilderImpl();
      instrumented3.setClassExpression(DEV3688App.IntNumber.class.getName());

      cb.getApplication().getDSO()
          .setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumented1, instrumented2, instrumented3 });

      LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
      lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
      lock1.setMethodExpression("* " + DEV3688App.class.getName() + "*.*(..)");

      LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
      lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
      lock2.setMethodExpression("* " + DEV3688App.DEV3688Worker.class.getName() + "*.*(..)");

      cb.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2 });

      RootConfigBuilder root1 = new RootConfigBuilderImpl();
      root1.setFieldName(DEV3688App.class.getName() + ".barrier");
      root1.setRootName("barrier");

      RootConfigBuilder root2 = new RootConfigBuilderImpl();
      root2.setFieldName(DEV3688App.class.getName() + ".mySharedArrayList");
      root2.setRootName("mySharedArrayList");

      cb.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root1, root2 });

      return cb;
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

}
