/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

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
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.text.Banner;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.builtin.ArrayList;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class NewObjectMemoryManagerRaceTest extends ServerCrashingTestBase {
  private static final long TEST_DURATION = 2 * 60 * 1000;
  private static final long END           = System.currentTimeMillis() + TEST_DURATION;

  public NewObjectMemoryManagerRaceTest() {
    super(1); // only need 1 node
    timebombTestForRewrite();
  }

  @Override
  protected Class getApplicationClass() {
    return NewObjectMemoryManagerTestApp.class;
  }

  public static boolean shouldEnd() {
    return Shared.isEnd() || System.currentTimeMillis() > END;
  }

  @Override
  protected void createConfig(TerracottaConfigBuilder cb) {
    String sharedClassName = Shared.class.getName();

    LockConfigBuilder lock = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock.setMethodExpression("* " + sharedClassName + ".put()");
    lock.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock2.setMethodExpression("* " + sharedClassName + ".size()");
    lock2.setLockLevel(LockConfigBuilder.LEVEL_READ);

    cb.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock, lock2 });

    RootConfigBuilder root = new RootConfigBuilderImpl();
    root.setFieldName(sharedClassName + ".queue");
    root.setRootName("queue");

    RootConfigBuilder root2 = new RootConfigBuilderImpl();
    root2.setFieldName(sharedClassName + ".end");
    root2.setRootName("end");

    cb.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root, root2 });

    InstrumentedClassConfigBuilder instrumented = new InstrumentedClassConfigBuilderImpl();
    instrumented.setClassExpression(Foo.class.getName());

    cb.getApplication().getDSO().setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumented });
  }

  private static class Shared {
    // roots
    private static final List queue    = new ArrayList();
    private static boolean    end      = false;

    private static final int  BATCH    = 3000;
    private static int        putCount = 0;

    static Collection take() {
      Collection rv = new java.util.ArrayList();
      synchronized (queue) {
        while (!queue.isEmpty()) {
          rv.add(queue.remove(0));
        }
      }
      return rv;
    }

    static int size() {
      synchronized (queue) {
        return queue.size();
      }
    }

    static void put() {
      while (size() > 0) {
        ThreadUtil.reallySleep(250);
      }

      synchronized (queue) {
        for (int i = 0; i < BATCH; i++) {
          queue.add(new Foo());
          putCount++;
        }
      }

      System.err.println("put " + putCount);
    }

    static boolean isEnd() {
      return end;
    }

    static void end() {
      end = true;
    }

  }

  /**
   * The bug we are reproducing is a data race where the fields in a newly shared object (which are not null) are being
   * nulled by the client side memory manager before the object is dehydrated, causing the object to have null fields
   * when it is shared. This causes other clients to see the object as having null fields instead of the actual data.
   * This is due to a data race in TCChangeBufferImpl.writeTo() where an object is marked as non-NEW too early (before
   * it's been shared).
   */
  public static class NewObjectMemoryManagerTestApp extends ServerCrashingAppBase {

    public NewObjectMemoryManagerTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(Foo.class.getName());
      config.addIncludePattern(Foo.class.getName());

      TransparencyClassSpec spec = config.getOrCreateSpec(Shared.class.getName());
      spec.addRoot("queue", "queue");
      spec.addRoot("end", "end");
      config.addWriteAutolock("* " + Shared.class.getName() + ".take()");
    }

    @Override
    public void runTest() throws Throwable {
      Thread t = new Thread() {
        @Override
        public void run() {
          createNewObjects();
        }
      };
      t.start();

      observeNewObjects();

      t.join(45000);

      if (t.isAlive()) {
        Banner.warnBanner("Spawned client still alive");
      }
    }

    /**
     * One client will create lots of objects that have non-null fields and add them to a shared queue.
     * 
     * @throws InterruptedException
     */
    void createNewObjects() {
      try {
        File workingDir = new File(getConfigFileDirectoryPath(), "external-client");
        FileUtils.forceMkdir(workingDir);

        // spawn the new node with very aggressive L1 cache settings
        List jvmArgs = new java.util.ArrayList();
        jvmArgs.add("-Dcom.tc.l1.cachemanager.logging.enabled=true");
        jvmArgs.add("-Dcom.tc.l1.cachemanager.leastCount=1");
        jvmArgs.add("-Dcom.tc.l1.cachemanager.percentageToEvict=99");
        jvmArgs.add("-Dcom.tc.l1.cachemanager.sleepInterval=1");
        jvmArgs.add("-Dcom.tc.l1.cachemanager.criticalThreshold=2");
        jvmArgs.add("-Dcom.tc.l1.cachemanager.threshold=1");
        jvmArgs.add("-Xms512m");
        jvmArgs.add("-Xmx512m");

        ExtraL1ProcessControl client = new ExtraL1ProcessControl(getHostName(), getPort(), External.class,
                                                                 getConfigFilePath(), Collections.EMPTY_LIST,
                                                                 workingDir, jvmArgs);
        client.setJavaHome(new File(System.getProperty("java.home")));
        client.start();
        int exitCode = client.waitFor();
        if (exitCode != 0) {
          String errorMsg = "Client existed Abnormally. Exit code = " + exitCode;
          System.err.println(errorMsg);
          throw new AssertionError(errorMsg);
        }
      } catch (Throwable t) {
        notifyError(t);
      }
    }

    /**
     * Other clients will look at those shared objects in the queue and check whether the fields are null or not. If
     * things are working as designed, the fields should never be null as they are never null when they are added to the
     * shared queue.
     */
    private void observeNewObjects() throws Throwable {
      int count = 0;

      while (true) {
        Collection taken = Shared.take();
        if (taken.isEmpty()) {
          if (Shared.isEnd()) {
            return;
          } else {
            ThreadUtil.reallySleep(250);
            continue;
          }
        }

        for (Iterator i = taken.iterator(); i.hasNext();) {
          Foo foo = (Foo) i.next();

          if ((++count % 500) == 0) {
            System.err.println("eval'd " + count);
          }

          try {
            foo.validate();
          } catch (Throwable t) {
            Shared.end();
            throw t;
          }
        }
      }
    }
  }

  private static class Foo {
    private final Object f1  = this;
    private final Object f2  = this;
    private final Object f3  = this;
    private final Object f4  = this;
    private final Object f5  = this;
    private final Object f6  = this;
    private final Object f7  = this;
    private final Object f8  = this;
    private final Object f9  = this;
    private final Object f10 = this;
    private final Object f11 = this;
    private final Object f12 = this;
    private final Object f13 = this;
    private final Object f14 = this;
    private final Object f15 = this;
    private final Object f16 = this;
    private final Object f17 = this;
    private final Object f18 = this;
    private final Object f19 = this;
    private final Object f20 = this;
    private final Object f21 = this;
    private final Object f22 = this;
    private final Object f23 = this;
    private final Object f24 = this;
    private final Object f25 = this;
    private final Object f26 = this;
    private final Object f27 = this;
    private final Object f28 = this;
    private final Object f29 = this;
    private final Object f30 = this;

    void validate() {
      Assert.assertNotNull(f1);
      Assert.assertNotNull(f2);
      Assert.assertNotNull(f3);
      Assert.assertNotNull(f4);
      Assert.assertNotNull(f5);
      Assert.assertNotNull(f6);
      Assert.assertNotNull(f7);
      Assert.assertNotNull(f8);
      Assert.assertNotNull(f9);
      Assert.assertNotNull(f10);
      Assert.assertNotNull(f11);
      Assert.assertNotNull(f12);
      Assert.assertNotNull(f13);
      Assert.assertNotNull(f14);
      Assert.assertNotNull(f15);
      Assert.assertNotNull(f16);
      Assert.assertNotNull(f17);
      Assert.assertNotNull(f18);
      Assert.assertNotNull(f19);
      Assert.assertNotNull(f20);
      Assert.assertNotNull(f21);
      Assert.assertNotNull(f22);
      Assert.assertNotNull(f23);
      Assert.assertNotNull(f24);
      Assert.assertNotNull(f25);
      Assert.assertNotNull(f26);
      Assert.assertNotNull(f27);
      Assert.assertNotNull(f28);
      Assert.assertNotNull(f29);
      Assert.assertNotNull(f30);
    }
  }

  public static class External implements Runnable {
    public static void main(String[] args) throws Exception {
      try {
        Loader loader = Loader.create();
        Thread.currentThread().setContextClassLoader(loader);
        Runnable r = (Runnable) loader.loadClass(External.class.getName()).newInstance();
        r.run();
      } finally {
        Shared.end();
      }
    }

    public void run() {
      while (!NewObjectMemoryManagerRaceTest.shouldEnd()) {
        Shared.put();
      }
    }

    static class Loader extends URLClassLoader {

      public Loader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
      }

      static Loader create() {
        URL[] urls = ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs();
        return new Loader(urls, null);
      }
    }

  }

}
