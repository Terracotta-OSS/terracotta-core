/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.atomic.AtomicReference;

public class ClusterEventsBadClientsTestApp extends AbstractTransparentApp {

  private static final String           TRUE  = "true".intern();
  private static final String           FALSE = "false".intern();

  private final AtomicReference<String> root  = new AtomicReference<String>();
  private final CyclicBarrier           barrier;

  @InjectedDsoInstance
  private DsoCluster                    dsoCluster;

  public ClusterEventsBadClientsTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    dsoCluster.addClusterListener(new MyBadClusterListener(appId));
  }

  public void run() {
    try {
      AtomicReference ab = new AtomicReference();
      Assert.assertTrue(ab instanceof Manageable);
      atomicReferenceTesting();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void atomicReferenceTesting() throws Exception {
    int i = 5;
    while (i-- > 0) {
      basicSetTesting();
      compareAndSetTesting();
      weakCompareAndSetTesting();
      getAndSetTesting();
      System.out.println("XXX Iteration " + i + " done. sleeping for 30s");
      ThreadUtil.reallySleep(45 * 1000);
    }
  }

  private void initialize() throws Exception {
    int index = barrier.barrier();

    if (index == 0) {
      root.set(TRUE);
    }

    barrier.barrier();
  }

  private void basicSetTesting() throws Exception {
    initialize();

    Assert.assertEquals(TRUE, root.get());

    barrier.barrier();
  }

  private void compareAndSetTesting() throws Exception {
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      boolean set = root.compareAndSet(TRUE, FALSE);
      if (!set) { throw new AssertionError("not set"); }
    }

    barrier.barrier();

    Assert.assertEquals(FALSE, root.get());

    barrier.barrier();
  }

  private void weakCompareAndSetTesting() throws Exception {
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      // per the javadoc this can fail spuriously. The implementations I've seen have no difference in compareAndSet()
      // and weakCompareAndSet(), but if this starts failing it is worth taking a second look
      boolean set = root.weakCompareAndSet(TRUE, FALSE);
      if (!set) { throw new AssertionError("not set"); }
    }

    barrier.barrier();

    Assert.assertEquals(FALSE, root.get());

    barrier.barrier();
  }

  private void getAndSetTesting() throws Exception {
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      Object val = root.getAndSet(FALSE);
      Assert.assertEquals(TRUE, val);
    }

    barrier.barrier();

    Assert.assertEquals(FALSE, root.get());

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = ClusterEventsBadClientsTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");
  }

  class MyBadClusterListener implements DsoClusterListener {

    private final String name;
    private int          i = 0;

    public MyBadClusterListener(String appId) {
      this.name = "Client[" + appId + "] : ";
    }

    public void nodeJoined(DsoClusterEvent dsoclusterevent) {
      System.out.println(name + "XXX NodeJoined: " + dsoclusterevent.getNode());
    }

    public void nodeLeft(DsoClusterEvent dsoclusterevent) {
      System.out.println(name + "XXX NodeLeft: " + dsoclusterevent.getNode());
    }

    public void operationsEnabled(DsoClusterEvent dsoclusterevent) {
      System.out.println(name + "XXX opsEnabled: " + dsoclusterevent.getNode());
    }

    public void operationsDisabled(DsoClusterEvent dsoclusterevent) {
      i++;
      if (i <= 1) {
        System.out.println(name + "XXX opsDisabled: " + dsoclusterevent.getNode() + "; checking exception handling");
        throw new RuntimeException("XXX I am a BAD man. I don't like ops disabled event.");
      } else {
        System.out.println(name + "XXX opsDisabled: " + dsoclusterevent.getNode()
                           + "; BLOCKING indefinite; checking L1 thread stuck behaviour");
        ThreadUtil.reallySleep(Integer.MAX_VALUE);
        // though the client cluster event handler stage is stuck, L1 still continues to rum and switch over to other
        // server and operate normally
      }
    }
  }

}
