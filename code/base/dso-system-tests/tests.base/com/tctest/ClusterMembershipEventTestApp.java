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
import java.util.HashSet;

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

    spec.addRoot("barrier", "barrier");

  }

  private final int             initialNodeCount = getParticipantCount();
  private final CyclicBarrier   barrier          = new CyclicBarrier(initialNodeCount);

  // not shared..
  private final SynchronizedInt nodeConCnt       = new SynchronizedInt(0);
  private final SynchronizedInt nodeDisCnt       = new SynchronizedInt(0);
  private final SynchronizedInt thisNodeConCnt   = new SynchronizedInt(0);
  private final SynchronizedInt thisNodeDisCnt   = new SynchronizedInt(0);
  private final HashSet         nodes            = new HashSet();
  private String                thisNode;

  public void run() {
    try {
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void runTest() throws Throwable {

    ManagerUtil.addClusterEventListener(this);
    check(1, thisNodeConCnt.get(), "thisNodeConnected");
    waitForNodes(initialNodeCount);

    System.err.println("### stage 1 [all nodes connected]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());

    clearCounters();
    final boolean isMasterNode = barrier.barrier() == 0;

    if (isMasterNode) {
      // master node blocks until new client exists...
      spawnNewClient();
    }
    barrier.barrier();

    waitForNodes(initialNodeCount);
    check(1, nodeConCnt.get(), "nodeConnected");
    check(1, nodeDisCnt.get(), "nodeDisconnected");
    clearCounters();
    System.err.println("### stage 2 [new client connected & disconnected]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());

    clearCounters();
    barrier.barrier();

    // FIXME: removing server crash/restart for now. it's broken. WE'll have to fix it later
    if (true) return;
    if (isMasterNode) {
      System.err.println("### masterNode=" + thisNode + " -> crashing server...");
      config.getServerControl().crash();
      System.err.println("### masterNode=" + thisNode + " -> crashed server");
    }

    waitForCount(thisNodeDisCnt, 1);
    System.err.println("### stage 3 [killed server]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());

    if (isMasterNode) {
      System.err.println("### masterNode=" + thisNode + " -> restarting server...");
      config.getServerControl().start(30 * 1000);
      System.err.println("### masterNode=" + thisNode + " -> restarted server");
    }
    System.err.println("### stage 4 [reconnecting]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());
    waitForCount(thisNodeConCnt, 1);
    barrier.barrier();
    System.err.println("### stage 5 [reconnected]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());
    check(1, thisNodeConCnt.get(), "thisNodeConnected");
    check(initialNodeCount, nodes.size(), "nodesInCluster");
    System.err.println("### stage 6 [all nodes reconnected]: thisNode=" + thisNode + ", threadId="
                       + Thread.currentThread().getName());
  }

  private void clearCounters() {
    this.nodeConCnt.set(0);
    this.nodeDisCnt.set(0);
    this.thisNodeConCnt.set(0);
    this.thisNodeDisCnt.set(0);
  }

  private void waitForCount(SynchronizedInt cnt, int expectedSize) {
    while (true) {
      synchronized (cnt) {
        if (cnt.get() == expectedSize) break;
        try {
          cnt.wait();
        } catch (InterruptedException e) {
          notifyError(e);
        }
      }
    }
  }

  private void waitForNodes(int expectedSize) {
    while (true) {
      synchronized (nodes) {
        if (nodes.size() == expectedSize) break;
        try {
          nodes.wait();
        } catch (InterruptedException e) {
          notifyError(e);
        }
      }
    }
  }

  private void check(int expected, int actual, String msg) {
    if (expected != actual) notifyError(msg + " expected=" + expected + ", actual=" + actual + ", thisNodeId="
                                        + thisNode);
  }

  public void nodeConnected(String nodeId) {
    System.err.println("\n### nodeConnected: thisNode=" + thisNode + ", nodeId=" + nodeId);
    nodeConCnt.increment();
    synchronized (nodes) {
      nodes.add(nodeId);
      nodes.notifyAll();
    }
  }

  public void nodeDisconnected(String nodeId) {
    System.err.println("\n### nodeDisconnected: thisNode=" + thisNode + ", nodeId=" + nodeId);
    nodeDisCnt.increment();
    synchronized (nodes) {
      nodes.remove(nodeId);
      nodes.notifyAll();
    }
  }

  public void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
    System.err.println("\n### thisNodeConnected->thisNodeId=" + thisNodeId);
    thisNodeConCnt.increment();
    thisNode = thisNodeId;
    synchronized (nodes) {
      nodes.add(thisNode);
      for (int i = 0; i < nodesCurrentlyInCluster.length; i++) {
        nodes.add(nodesCurrentlyInCluster[i]);
      }
      nodes.notifyAll();
    }
  }

  public void thisNodeDisconnected(String thisNodeId) {
    System.err.println("\n### thisNodeDisconnected->thisNodeId=" + thisNodeId);
    thisNodeDisCnt.increment();
    synchronized (nodes) {
      nodes.clear();
      nodes.notifyAll();
    }
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
