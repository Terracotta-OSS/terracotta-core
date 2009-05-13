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
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CreateExceptionsTest extends TransparentTestBase {

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    public static final AtomicInteger postCreateCalls = new AtomicInteger();

    private final Map                 root            = new ConcurrentHashMap();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);

    }

    @Override
    protected void runTest() throws Throwable {
      assertEquals(0, postCreateCalls.get());
      root.put("foo", new PostCreate());
      assertEquals(1, postCreateCalls.get());

      RuntimeException re = new RuntimeException();
      try {
        root.put("foo", new PostCreate(re));
        fail("should have thrown exception");
      } catch (Throwable t) {
        assertEquals(re, t);
      }
      assertEquals(2, postCreateCalls.get());


      RuntimeException re2 = new RuntimeException();
      try {
        root.put("foo", new Object[] { new PostCreate(re2), new PostCreate(re) });
        fail("should have thrown exception");
      } catch (Throwable t) {
        // can't say for sure which one will be thrown w/o assuming how the traverser visits the graph
        assertTrue(t == re ^ t == re2);
      }
      assertEquals(4, postCreateCalls.get());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");
      config.addWriteAutolock("* " + testClassName + ".*(..)");

      spec = config.getOrCreateSpec(PostCreate.class.getName());
      spec.setPostCreateMethod("postCreate");
    }
  }

  private static class PostCreate {
    private final RuntimeException exception;

    public PostCreate() {
      this(null);
    }

    public PostCreate(RuntimeException re) {
      this.exception = re;
    }

    void postCreate() {
      App.postCreateCalls.incrementAndGet();
      if (exception != null) throw exception;
    }
  }





}
