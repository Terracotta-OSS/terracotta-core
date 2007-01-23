/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.cluster.ClusterEventListener;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;

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
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = ClusterMembershipEventTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("trueCluster", "trueCluster");
    spec.addRoot("start", "start");
    spec.addRoot("newClientUp", "newClientUp");
    spec.addRoot("newClientDown", "newClientDown");

  }

  private final Hashtable     trueCluster      = new Hashtable();
  private final int           initialNodeCount = getParticipantCount();
  private final CyclicBarrier start            = new CyclicBarrier(initialNodeCount);
  private final CyclicBarrier newClientUp      = new CyclicBarrier(initialNodeCount);

  // not shared..
  private final Hashtable     myCluster        = new Hashtable();
  private final CyclicBarrier nodeDisBarrier   = new CyclicBarrier(2);
  private final CyclicBarrier nodeConnBarrier  = new CyclicBarrier(2);
  private String              thisNode;

  public void run() {
    try {
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void runTest() throws Throwable {
    ManagerUtil.addClusterEventListener(this);
    final int nodeToSpawn = start.barrier();
    System.err.println("\n### passed start barrier.  thisNode=" + thisNode);
    assertCluster();
    if (nodeToSpawn == 0) {
      spawnNewClient();
    }
    newClientUp.barrier();
    System.err.println("\n### passed newClientUp barrier.  thisNode=" + thisNode);
    nodeConnBarrier.barrier();
    System.err.println("\n### passed passedNodeConn barrier.  thisNode=" + thisNode);
    nodeDisBarrier.barrier();
    System.err.println("\n### passed nodeDisBarrier barrier.  thisNode=" + thisNode);
    assertCluster();
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
    System.err.println("\n### Started New Client");
    return client;
  }

  public void nodeConnected(String nodeId) {
    System.err.println("\n### nodeConnected: thisNode=" + thisNode + ", nodeId=" + nodeId);
    myCluster.put(nodeId, nodeId);
    try {
      nodeConnBarrier.barrier();
    } catch (Exception e) {
      //
    }
  }

  public void nodeDisconnected(String nodeId) {
    System.err.println("\n### nodeDisconnected: thisNode=" + thisNode + ", nodeId=" + nodeId);
    myCluster.remove(nodeId);
    try {
      nodeDisBarrier.barrier();
    } catch (Exception e) {
      //
    }
  }

  public synchronized void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
    System.err.println("\n### thisNodeConnected->thisNodeId=" + thisNodeId);

    final String prevId;
    synchronized (trueCluster) {
      prevId = (String) trueCluster.put(thisNodeId, thisNodeId);
      thisNode = thisNodeId;
    }
    if (prevId != null) { throw new AssertionError("Error"); }
    for (int i = 0; i < nodesCurrentlyInCluster.length; i++) {
      myCluster.put(nodesCurrentlyInCluster[i], nodesCurrentlyInCluster[i]);
    }
  }

  public void thisNodeDisconnected(String thisNodeId) {
    //
  }

  private void assertCluster() {
    synchronized (trueCluster) {
      if (trueCluster.size() != myCluster.size()) { throw new AssertionError("Error: size mismatch: trueCluster="
                                                                             + trueCluster.size() + ", myCluster="
                                                                             + myCluster.size()); }
      for (Iterator i = myCluster.keySet().iterator(); i.hasNext();) {
        if (trueCluster.get(i.next()) == null) { throw new AssertionError(
                                                                          "Error: cluster membership mismatch: trueCluster: "
                                                                              + trueCluster.keySet() + ", myCluster="
                                                                              + myCluster.keySet()); }
      }
    }
  }

  public static class L1Client {
    public static void main(String args[])  {
      // nothing to do
    }
  }
}
