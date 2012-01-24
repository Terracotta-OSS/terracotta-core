/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.test.schema.InstrumentedClassConfigBuilderImpl;
import com.tc.config.test.schema.RootConfigBuilderImpl;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.ServerCrashingAppBase;
import com.tctest.ServerCrashingTestBase;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.builtin.Lock;
import com.tctest.jdk15.RecalledLockNeverRecalledTest.RecalledLockNeverRecalledTestApp.EmptyClient;

public class RecalledLockNeverRecalledTest extends ServerCrashingTestBase {

  private static final int NODE_COUNT = 2;

  public RecalledLockNeverRecalledTest() {
    super(NODE_COUNT);
    timebombTestForRewrite();
  }

  @Override
  protected Class getApplicationClass() {
    return RecalledLockNeverRecalledTestApp.class;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
    // DO 5 second lock GCs
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L1_LOCKMANAGER_TIMEOUT_INTERVAL, "5000");
  }

  @Override
  protected void createConfig(TerracottaConfigBuilder cb) {
    String testClassName = RecalledLockNeverRecalledTestApp.class.getName();
    String clientClassName = EmptyClient.class.getName();

    RootConfigBuilder root = new RootConfigBuilderImpl();
    root.setFieldName(testClassName + ".lock");
    root.setRootName("lock");
    RootConfigBuilder root2 = new RootConfigBuilderImpl();
    root2.setFieldName(testClassName + ".barrier");
    root2.setRootName("queue");
    cb.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root, root2 });

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(clientClassName + "*");

    cb.getApplication().getDSO()
        .setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumented1, instrumented2 });
  }

  public static class RecalledLockNeverRecalledTestApp extends ServerCrashingAppBase {

    private final Lock          lock = new Lock();
    private final CyclicBarrier barrier;

    public RecalledLockNeverRecalledTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      if (getParticipantCount() != NODE_COUNT) { throw new AssertionError(
                                                                          "Need two and only two node for this test to work"); }
      barrier = new CyclicBarrier(getParticipantCount());
    }

    @Override
    protected void runTest() throws Throwable {
      long gcTime = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.L1_LOCKMANAGER_TIMEOUT_INTERVAL);
      Assert.assertEquals(5000, gcTime);
      int index = barrier.await();
      switch (index) {
        case 0:
          node0();
          break;
        case 1:
          node1();
          break;
        default:
          throw new AssertionError("Node count should be 2 !! : idx  = " + index);
      }
    }

    /**
     * The purpose of this Node is to grab a READ lock greedily and then use it for sometime and then never use it so it
     * gets recalled.
     */
    private void node0() throws Exception {
      String nodeName = "Node 0";
      lock(nodeName); // GET Greedy lock
      unlock(nodeName);

      // Now don't use this lock for 30 seconds so Lock GC kicks in
      log("Node 0 : Sleeping for 30 secs ");
      ThreadUtil.reallySleep(30000);

      do {
        log("Node 0 : Asking for READ lock again");
        lock.readLock(); // Request Lock, this wait in the server first time, then it should be greedy
        log("Node 0 : Got READ lock again");
        ThreadUtil.reallySleep(3000);
        lock.readUnlock();
      } while (barrier.getNumberWaiting() == 0);

      barrier.await();
      log("Node 0: Exiting");
    }

    private void lock(String nodeName) {
      log(nodeName + ": Asking for READ lock ");
      lock.readLock();
    }

    private void unlock(String nodeName) {
      log(nodeName + ": Releasing READ lock ");
      lock.readUnlock();
    }

    /**
     * The purpose of this Node is to grab a READ lock and
     * 
     * @throws Exception
     */
    private void node1() throws Exception {

      String nodeName = "Node 1";
      lock(nodeName); // GET Greedy lock

      // Just sleep holding it
      log("Node 1 : Sleeping for 60 secs ");
      ThreadUtil.reallySleep(60000);

      log("Node 1 : Spawing a new Node");
      spawnAndWaitForNode();
      log("Node 1 : Spawned node exited");

      // Just sleep holding it
      log("Node 1 : Sleeping for 10 secs ");
      ThreadUtil.reallySleep(10000);

      // Now unlock it so that the recallCommit is called.
      unlock(nodeName);

      // Because of a bug in the Server lock manager, the should never be granted.
      lock(nodeName);
      unlock(nodeName);

      barrier.await();
      log("Node 1: Exiting");
    }

    private void spawnAndWaitForNode() throws Exception {
      spawnNewClientAndWaitForCompletion("Node 2", EmptyClient.class);
    }

    /**
     * The sole purpose of this spawned node is to exit at the right time.
     */
    public static final class EmptyClient {
      public static void main(String args[]) {
        System.err.println("Spawned Client exiting ...");
      }
    }

    private void log(String msg) {
      System.err.println(Thread.currentThread() + msg);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass;
      TransparencyClassSpec spec;
      String methodExpression;

      testClass = RecalledLockNeverRecalledTest.class.getName();
      spec = config.getOrCreateSpec(testClass);
      // methodExpression = "* " + testClass + "*.*(..)";
      // config.addWriteAutolock(methodExpression);

      config.addIncludePattern(testClass + "$*");

      testClass = RecalledLockNeverRecalledTestApp.class.getName();
      spec = config.getOrCreateSpec(testClass);

      methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("barrier", "barrier");
      spec.addRoot("lock", "lock");
    }

  }

}
