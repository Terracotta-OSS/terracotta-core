/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.exposed;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.cluster.ClusterEventListener;
import com.tc.management.AbstractTerracottaMBean;

import java.util.ArrayList;
import java.util.List;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

public class TerracottaCluster extends AbstractTerracottaMBean implements TerracottaClusterMBean, ClusterEventListener {

  public static final String     THIS_NODE_CONNECTED    = "com.tc.cluster.event.thisNodeConnected";
  public static final String     THIS_NODE_DISCONNECTED = "com.tc.cluster.event.thisNodeDisconnected";
  public static final String     NODE_CONNECTED         = "com.tc.cluster.event.nodeConnected";
  public static final String     NODE_DISCONNECTED      = "com.tc.cluster.event.nodeDisconnected";
  public static final String[]   ALL_EVENTS             = new String[] { THIS_NODE_CONNECTED, THIS_NODE_DISCONNECTED,
      NODE_CONNECTED, NODE_DISCONNECTED                };
  public static final String     DESCRIPTION            = "Terracotta Cluster Event Notification";

  private final List             nodes;

  private String                 thisNodeId;
  private boolean                isThisNodeConnected;

  private final SynchronizedLong notificationSequence   = new SynchronizedLong(0L);

  public TerracottaCluster()
      throws NotCompliantMBeanException {
    super(TerracottaClusterMBean.class, true);
    nodes = new ArrayList();
  }

  public void reset() {
    // nothing to do
  }

  public String getNodeId() {
    return thisNodeId;
  }

  public String[] getNodesInCluster() {
    synchronized (nodes) {
      String[] rv = new String[nodes.size()];
      nodes.toArray(rv);
      return rv;
    }
  }

  public boolean isConnected() {
    return isThisNodeConnected;
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    return new MBeanNotificationInfo[] { new MBeanNotificationInfo(ALL_EVENTS, AttributeChangeNotification.class
        .getName(), DESCRIPTION) };
  }

  /**
   * ClusterEventListener callback method...
   */
  public void nodeConnected(String nodeId) {
    synchronized (nodes) {
      nodes.add(nodeId);
    }
    makeNotification(NODE_CONNECTED, nodeId);
  }

  /**
   * ClusterEventListener callback method...
   */
  public void nodeDisconnected(String nodeId) {
    synchronized (nodes) {
      nodes.remove(nodeId);
    }
    makeNotification(NODE_DISCONNECTED, nodeId);
}

  /**
   * ClusterEventListener callback method...
   */
  public void thisNodeConnected(String thisNode, String[] nodesCurrentlyInCluster) {
    synchronized (nodes) {
      thisNodeId = thisNode;
      isThisNodeConnected = true;
      for (int i = 0; i < nodesCurrentlyInCluster.length; i++) {
        nodes.add(nodesCurrentlyInCluster[i]);
      }
    }
    makeNotification(THIS_NODE_CONNECTED, thisNodeId);
  }

  /**
   * ClusterEventListener callback method...
   */
  public void thisNodeDisconnected(String thisNode) {
    synchronized (nodes) {
      isThisNodeConnected = false;
      nodes.clear();
      nodes.add(thisNodeId);
    }
    makeNotification(THIS_NODE_DISCONNECTED, thisNodeId);
  }

  private void makeNotification(String event, String msg) {
    sendNotification(new Notification(event, this, notificationSequence.increment(), msg));
  }
}
