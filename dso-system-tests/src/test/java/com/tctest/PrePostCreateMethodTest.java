/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.builtin.ConcurrentHashMap;
import com.tctest.builtin.HashSet;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PrePostCreateMethodTest extends TransparentTestBase {

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
    public static final AtomicInteger preCreateCalls  = new AtomicInteger();

    private final Map                 root            = new ConcurrentHashMap();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);

    }

    @Override
    protected void runTest() throws Throwable {
      testBasic();
      testExceptions();
      testSubclass();
      testMultiple();
      testGraphs();
    }

    private void testMultiple() {
      reset();
      root.put("post", new SubclassWithPostCreate());
      assertEquals(2, postCreateCalls.get());
      root.put("pre", new SubclassWithPreCreate());
      assertEquals(2, preCreateCalls.get());
    }

    private void reset() {
      preCreateCalls.set(0);
      postCreateCalls.set(0);
    }

    private void testSubclass() {
      reset();
      root.put("post", new PostSubclass());
      assertEquals(1, postCreateCalls.get());
      root.put("pre", new PreSubclass());
      assertEquals(1, postCreateCalls.get());
    }

    private void testBasic() {
      reset();
      root.put("post", new PostCreate());
      root.put("pre", new PreCreate());
      assertEquals(1, postCreateCalls.get());
      assertEquals(1, preCreateCalls.get());
    }

    private void testExceptions() {
      reset();

      RuntimeException re = new RuntimeException();
      try {
        root.put("post", new PostCreate(re));
        fail("should have thrown exception");
      } catch (Throwable t) {
        assertEquals(re, t);
      }
      assertEquals(1, postCreateCalls.get());

      PreCreate pre = new PreCreate(re);
      try {
        root.put("pre", new PreCreate(re));
        fail("should have thrown exception");
      } catch (Throwable t) {
        assertEquals(re, t);
      }
      assertEquals(1, preCreateCalls.get());
      assertFalse(ManagerUtil.isManaged(pre));

      // All of the post create methods should be called even if one (or more) in the traversal throws an exception
      RuntimeException re2 = new RuntimeException();
      try {
        root.put("array", new Object[] { new PostCreate(re2), new PostCreate(re) });
        fail("should have thrown exception");
      } catch (Throwable t) {
        // can't say for sure which one will be thrown w/o assuming how the traverser visits the graph
        assertTrue(t == re ^ t == re2);
      }
      assertEquals(3, postCreateCalls.get());
    }

    private void testGraphs() {
      reset();
      Collection<PostCreate> posts = new HashSet<PostCreate>();
      posts.add(new PostCreate());
      posts.add(new PostCreate());
      Collection<PreCreate> pres = new HashSet<PreCreate>();
      pres.add(new PreCreate());
      pres.add(new PreCreate());

      root.put("posts", posts);
      root.put("pres", pres);
      assertEquals(2, postCreateCalls.get());
      assertEquals(2, preCreateCalls.get());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClassName = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("root", "root");
      config.addWriteAutolock("* " + testClassName + ".*(..)");

      spec = config.getOrCreateSpec(PostCreate.class.getName());
      spec.setPostCreateMethod("postCreate");
      spec.setHonorTransient(true);

      spec = config.getOrCreateSpec(PreCreate.class.getName());
      spec.setPreCreateMethod("preCreate");
      spec.setHonorTransient(true);

      spec = config.getOrCreateSpec(SubclassWithPostCreate.class.getName());
      spec.setPostCreateMethod("subclassPostCreate");

      spec = config.getOrCreateSpec(SubclassWithPreCreate.class.getName());
      spec.setPreCreateMethod("subclassPreCreate");

      config.addIncludePattern(PostSubclass.class.getName());
      config.addIncludePattern(PreSubclass.class.getName());
    }
  }

  private static class PostCreate {
    private final transient RuntimeException exception;

    public PostCreate() {
      this(null);
    }

    public PostCreate(RuntimeException re) {
      this.exception = re;
    }

    @SuppressWarnings("unused")
    void postCreate() {
      App.postCreateCalls.incrementAndGet();
      if (exception != null) throw exception;
    }
  }

  private static class PostSubclass extends PostCreate {
    // a subclass that directly has no preCreate method, but the super class does
  }

  private static class PreSubclass extends PreCreate {
    // a subclass that directly has no preCreate method, but the super class does
  }

  private static class SubclassWithPostCreate extends PostCreate {
    @SuppressWarnings("unused")
    void subclassPostCreate() {
      App.postCreateCalls.incrementAndGet();
    }
  }

  private static class SubclassWithPreCreate extends PreCreate {
    @SuppressWarnings("unused")
    void subclassPreCreate() {
      App.preCreateCalls.incrementAndGet();
    }
  }

  private static class PreCreate {
    private final transient RuntimeException re;

    public PreCreate(RuntimeException re) {
      this.re = re;
    }

    public PreCreate() {
      this(null);
    }

    @SuppressWarnings("unused")
    void preCreate() {
      App.preCreateCalls.incrementAndGet();
      if (re != null) { throw re; }
    }
  }

}
