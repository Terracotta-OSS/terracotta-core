/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public class Cluster {

  private static final TCLogger logger    = TCLogging.getLogger(Cluster.class);
  private static final boolean  debug     = false;

  private final Map             nodes     = new HashMap();                     // <String, Node>
  private final IdentityHashMap listeners = new IdentityHashMap();             // <ClusterEventListener>

  private Node                  thisNode;

  public synchronized Node getThisNode() {
    return thisNode;
  }

  // for tests
  Map getNodes() {
    return nodes;
  }

  public synchronized void thisNodeConnected(final String thisNodeId, String[] nodesCurrentlyInCluster) {
    nodes.clear();

    thisNode = new Node(thisNodeId);
    nodes.put(thisNode.getNodeId(), thisNode);

    for (int i = 0; i < nodesCurrentlyInCluster.length; i++) {
      Node n = new Node(nodesCurrentlyInCluster[i]);
      nodes.put(n.getNodeId(), n);
    }

    debug("### Cluster: thisNodeConnected -> " + this);
    fireThisNodeConnectedEvent();
  }

  public synchronized void thisNodeDisconnected() {
    debug("### Cluster: thisNodeDisconnected -> " + this);
    fireThisNodeDisconnectedEvent();
    thisNode = null;
  }

  public synchronized void nodeConnected(String nodeId) {
    Node n = new Node(nodeId);

    // the server should not be sending this event to us
    if (n.equals(thisNode)) { throw new AssertionError("received message for self"); }

    nodes.put(n.getNodeId(), n);
    debug("### Cluster: nodeConnected -> " + this);
    fireNodeConnectedEvent(nodeId);
  }

  public synchronized void nodeDisconnected(String nodeId) {
    nodes.remove(nodeId);
    debug("### Cluster: nodeDisconnected -> " + this);
    fireNodeDisconnectedEvent(nodeId);
  }

  public synchronized String toString() {
    // NOTE: this method is used in the error logging
    return "Cluster{ thisNode=" + thisNode + ", nodesInCluster=" + nodes.keySet() + "}";
  }

  public synchronized void addClusterEventListener(ClusterEventListener cel) {
    // If this assertion is going off, you're (probably) trying to add your listener too early
    assertPre(thisNode != null);

    Object oldCel = listeners.put(cel, cel);
    if (oldCel == null) {
      fireThisNodeConnectedEvent(cel, getCurrentNodeIds());
    }
  }

  private void fireThisNodeConnectedEvent(ClusterEventListener cel, String[] ids) {
    try {
      cel.thisNodeConnected(thisNode.getNodeId(), ids);
    } catch (Throwable t) {
      log(t);
    }
  }

  private void fireThisNodeConnectedEvent() {
    assertPre(thisNode != null);
    final String ids[] = getCurrentNodeIds();
    for (Iterator i = listeners.keySet().iterator(); i.hasNext();) {
      ClusterEventListener l = (ClusterEventListener) i.next();
      fireThisNodeConnectedEvent(l, ids);
    }
  }

  private void fireThisNodeDisconnectedEvent() {
    if (thisNode == null) {
      // client channels be closed before we know thisNodeID. Skip the disconnect event in this case
      return;
    }
    for (Iterator i = listeners.keySet().iterator(); i.hasNext();) {
      ClusterEventListener l = (ClusterEventListener) i.next();
      try {
        l.thisNodeDisconnected(thisNode.getNodeId());
      } catch (Throwable t) {
        log(t);
      }
    }
  }

  private void fireNodeConnectedEvent(String newNodeId) {
    if (thisNode == null) { return; }
    for (Iterator i = listeners.keySet().iterator(); i.hasNext();) {
      ClusterEventListener l = (ClusterEventListener) i.next();
      try {
        l.nodeConnected(newNodeId);
      } catch (Throwable t) {
        log(t);
      }
    }
  }

  private void fireNodeDisconnectedEvent(String nodeId) {
    if (thisNode == null) { return; }
    for (Iterator i = listeners.keySet().iterator(); i.hasNext();) {
      ClusterEventListener l = (ClusterEventListener) i.next();
      try {
        l.nodeDisconnected(nodeId);
      } catch (Throwable t) {
        log(t);
      }
    }
  }

  private static void assertPre(boolean b) {
    if (!b) throw new AssertionError("Pre-condition failed!");
  }

  private String[] getCurrentNodeIds() {
    final String[] rv = new String[nodes.size()];
    nodes.keySet().toArray(rv);
    Arrays.sort(rv);
    return rv;
  }

  private static void debug(String string) {
    if (debug) System.err.println(string);
  }

  private void log(Throwable t) {
    logger.error("Unhandled exception in event callback " + this, t);
  }
}
