/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.NotClearable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NotClearableTest extends TransparentTestBase {

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {
    private final ConcurrentHashMap root = new ConcurrentHashMap();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void runTest() throws Throwable {
      ReentrantLock rl = new ReentrantLock();
      ReentrantReadWriteLock rrwl = new ReentrantReadWriteLock();
      root.put("rl", rl);
      root.put("rrwl", rrwl);

      verifyCHMSegments(root);
      verify(rl);
      verify(rrwl);
      verify(rrwl.readLock());
      verify(rrwl.writeLock());
    }

    private static void verifyCHMSegments(ConcurrentHashMap chm) throws Exception {
      Field segmentsField = chm.getClass().getDeclaredField("segments");
      segmentsField.setAccessible(true);
      Object[] o = (Object[]) segmentsField.get(chm);
      for (Object segment : o) {
        verify(segment);
      }
    }

    private static void verify(Object o) {
      if (!(o instanceof NotClearable)) { throw new AssertionError(o.getClass() + " does not implement NotClearable"); }
      TCObject tco = ((Manageable) o).__tc_managed();
      if (tco.canEvict()) { throw new AssertionError(o.getClass() + " can be evicted"); }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");
      String methodExpression = "* " + testClassName + "*.*(..)";
      config.addWriteAutolock(methodExpression);
    }

  }

}
