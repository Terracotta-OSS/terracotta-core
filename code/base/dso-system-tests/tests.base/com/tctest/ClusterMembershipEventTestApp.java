/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.cluster.ClusterEventListener;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;

public class ClusterMembershipEventTestApp extends AbstractTransparentApp implements ClusterEventListener {

  public static final String      CONFIG_FILE = "config-file";
  public static final String      PORT_NUMBER = "port-number";
  public static final String      HOST_NAME   = "host-name";

  private final ApplicationConfig config;

  public ClusterMembershipEventTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    new CyclicBarrierSpec().visit(visitor, config);
    new SynchronizedIntSpec().visit(visitor, config);

    String testClass = ClusterMembershipEventTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("stage1", "stage1");
    spec.addRoot("stage2", "stage2");
    spec.addRoot("stage3", "stage3");
    spec.addRoot("stage4", "stage4");
    spec.addRoot("stage5", "stage5");
    spec.addRoot("stage6", "stage6");

  }

  private final int             initialNodeCount          = getParticipantCount();
  private final CyclicBarrier   stage1                    = new CyclicBarrier(initialNodeCount);
  private final CyclicBarrier   stage2                    = new CyclicBarrier(initialNodeCount);
  private final CyclicBarrier   stage3                    = new CyclicBarrier(initialNodeCount);
  private final CyclicBarrier   stage4                    = new CyclicBarrier(initialNodeCount);
  private final CyclicBarrier   stage5                    = new CyclicBarrier(initialNodeCount);
  private final CyclicBarrier   stage6                    = new CyclicBarrier(initialNodeCount);

  // not shared..
  private final SynchronizedInt localNodeCount            = new SynchronizedInt(0);
  private final SynchronizedInt localThisNodeConCallCount = new SynchronizedInt(0);
  private final SynchronizedInt localThisNodeDisconCount  = new SynchronizedInt(0);

  private final CyclicBarrier   callbackBarrier           = new CyclicBarrier(2);
  private CyclicBarrier         localThisNodeConBarrier   = null;
  private CyclicBarrier         localNodeConBarrier       = null;
  private CyclicBarrier         localNodeDisBarrier       = null;

  private String                thisNode;

  public void run() {
    try {
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void runTest() throws Throwable {
    final boolean isMasterNode = stage1.barrier() == 0;
    System.err.println("### thisNode=" + thisNode + " -> stage # 1");
    // stage - all nodes are up
    localThisNodeConBarrier = null;
    ManagerUtil.addClusterEventListener(this);

    // diff nodes will get a diff mix of thisNodeConnected + nodeConnected events.
    // by checking that each node has a consitent view of the cluster we also ensure that
    // all events generated so far have been consumed.
    checkCountTimed(localThisNodeConCallCount, 1, 1, 0, "localThisNodeConCallCount");
    checkCountTimed(localNodeCount, initialNodeCount, 10, 5 * 1000, "localNodeCount");
    stage2.barrier();
    System.err.println("### thisNode=" + thisNode + " -> stage # 2");

    // stage - all nodes got thisNodeConnected/nodeConnected callback, and all nodes have a consistent view of the
    // cluster. Prepare to check nodeConnected
    localThisNodeConBarrier = null;
    localNodeConBarrier = callbackBarrier;

    stage3.barrier();
    System.err.println("### thisNode=" + thisNode + " -> stage # 3");

    if (isMasterNode) {
      spawnNewClient();
    }
    callbackBarrier.barrier();
    stage4.barrier();
    System.err.println("### thisNode=" + thisNode + " -> stage # 4");

    // stage - all nodes got nodeConnected callback. prepare to test nodeDisconnected event
    localNodeConBarrier = null;
    localNodeDisBarrier = callbackBarrier;
    stage5.barrier();
    System.err.println("### thisNode=" + thisNode + " -> stage # 5");

    if (isMasterNode) {
      spawnNewClient();
    }
    callbackBarrier.barrier();
    stage6.barrier();
    System.err.println("### thisNode=" + thisNode + " -> stage # 6");

    // now let's try to kill the server and see if all nodes get thisNodeDisconnected event
    localNodeDisBarrier = null;
    localNodeConBarrier = null;
    localThisNodeConBarrier = null;
    
    if (isMasterNode) {
      config.getServerControl().crash();
      checkCountTimed(localThisNodeDisconCount, 1, 10, 5 * 1000, "localThisNodeDisconnectedCount");
      config.getServerControl().start(30 * 1000);
    }
    stage6.barrier();
    checkCountTimed(localThisNodeDisconCount, 1, 10, 5 * 1000, "localThisNodeDisconnectedCount");
  }

  private void checkCountTimed(SynchronizedInt actualSI, final int expected, final int slices, final long sliceMillis,
                               String msg) throws InterruptedException {
    // wait until all nodes have the right picture of the cluster
    int actual = 0;
    int i;
    for (i = 0; i < slices; i++) {
      actual = actualSI.get();
      if (actual > expected || actual < 0) {
        notifyError("Wrong Count: expected=" + expected + ", actual=" + actual);
      }
      if (actual < expected) {
        Thread.sleep(sliceMillis);
      } else {
        break;
      }
    }
    if (i == slices) {
      notifyError("Wrong Count: expected=" + expected + ", actual=" + actual);
    }
    System.err.println("\n### nodeId = " + thisNode + " -> check '" + msg + "' passed in " + i + " slices");
  }

  public void nodeConnected(String nodeId) {
    System.err.println("\n### nodeConnected: thisNode=" + thisNode + ", nodeId=" + nodeId);
    try {
      localNodeCount.increment();
      hurdleIfNeeded(localNodeConBarrier);
    } catch (Exception e) {
      //
    }
  }

  public void nodeDisconnected(String nodeId) {
    System.err.println("\n### nodeDisconnected: thisNode=" + thisNode + ", nodeId=" + nodeId);
    try {
      hurdleIfNeeded(localNodeDisBarrier);
    } catch (Exception e) {
      //
    }
  }

  public void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
    System.err.println("\n### thisNodeConnected->thisNodeId=" + thisNodeId);
    localThisNodeConCallCount.increment();
    localNodeCount.set(nodesCurrentlyInCluster.length);
    thisNode = thisNodeId;
    hurdleIfNeeded(localThisNodeConBarrier);
  }

  private void hurdleIfNeeded(CyclicBarrier cb) {
    if (cb != null) try {
      cb.barrier();
    } catch (Exception e) {
      notifyError(e);
    }
  }

  public void thisNodeDisconnected(String thisNodeId) {
    System.err.println("\n### thisNodeDisconnected->thisNodeId=" + thisNodeId);
    localThisNodeDisconCount.increment();
  }

  public static class L1Client {
    public static void main(String args[]) {
      // nothing to do
    }
  }

  private ExtraL1ProcessControl spawnNewClient() throws Exception {
    final String hostName = config.getAttribute(HOST_NAME);
    final int port = Integer.parseInt(config.getAttribute(PORT_NUMBER));
    final File configFile = new File(config.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client-0");
    FileUtils.forceMkdir(workingDir);

    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, L1Client.class, configFile
        .getAbsolutePath(), new String[0], workingDir);
    client.start(20000);
    client.mergeSTDERR();
    client.mergeSTDOUT();
    client.waitFor();
    System.err.println("\n### Started New Client");
    return client;
  }

}
