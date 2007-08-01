/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DistributedMethodCallGCTest extends GCTestBase {

  private static List memoryHog = new ArrayList();

  public DistributedMethodCallGCTest() {
    hogSomeHeap();
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    super.doSetUp(t);
    t.getTransparentAppConfig().setAttribute("gc-interval-ms", new Long(getGCIntervalInSeconds() * 1000));
  }

  private void hogSomeHeap() {
    long count = 0;
    long freeTarget = 16L * 1024L * 1024L; // 16MB

    System.gc();
    while (Runtime.getRuntime().freeMemory() > freeTarget) {
      memoryHog.add(new String("+1 awesome points"));
      count++;
    }

    System.err.println("added " + count + " Strings to consume heap");
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private static long DURATION = 3 * 60 * 1000;
    private static long END      = System.currentTimeMillis() + DURATION;

    private final Set   root     = new HashSet();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      DMITarget.setGCTime(((Long)cfg.getAttributeObject("gc-interval-ms")).longValue());

      if (getParticipantCount() < 2) { throw new AssertionError(); }
    }

    protected void runTest() throws Throwable {

      DMITarget.setAppThread(Thread.currentThread());

      while (!shouldEnd()) {
        makeDMICall();
      }
    }

    private void makeDMICall() {
      DMITarget t = new DMITarget();
      synchronized (root) {
        root.add(t);
        root.remove(t);
      }

      t.foo();
      t.foo("asdf", 42);
    }

    private static boolean shouldEnd() {
      return System.currentTimeMillis() > END;
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");

      config.addWriteAutolock("* " + testClassName + ".*(..)");

      spec = config.getOrCreateSpec(DMITarget.class.getName());
      spec.addDistributedMethodCall("foo", "()V", true);
      spec.addDistributedMethodCall("foo", "(Ljava/lang/String;I)V", true);
    }

    private static class DMITarget {

      private static Thread                localAppThread;
      private static final SynchronizedInt count  = new SynchronizedInt(0);
      static volatile long                 gcTime = 0;

      static synchronized void setGCTime(long l) {
        gcTime = l;
      }

      static synchronized void setAppThread(Thread t) {
        if (localAppThread != null) { throw new AssertionError(); }
        localAppThread = t;
      }

      void foo() {
        output();
      }

      void foo(String s, int i) {
        output();
      }

      private void output() {
        if (Thread.currentThread() != localAppThread) {

          int c = count.increment();
          if ((c % 100) == 0) {
            System.err.println("remote DMI executed: " + c);
          }
          if ((c % 1000) == 0) {
            System.gc();
            ThreadUtil.reallySleep(gcTime + 2000);
          }
        }
      }
    }

  }

}
