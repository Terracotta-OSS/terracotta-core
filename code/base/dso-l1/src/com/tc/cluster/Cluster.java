/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public class Cluster {

  private static final boolean  debug     = false;

  private final Map             nodes     = new HashMap();        // <String, Node>
  private final IdentityHashMap listeners = new IdentityHashMap(); // <ClusterEventListener>

  private Node                  thisNode;

  public Node getThisNode() {
    return thisNode;
  }

  public Map getNodes() {
    return nodes;
  }

  public synchronized void thisNodeConnected(final String thisNodeId, String[] nodesCurrentlyInCluster) {
    thisNode = new Node(thisNodeId);
    nodes.put(thisNode.getNodeId(), thisNode);
    for (int i = 0; i < nodesCurrentlyInCluster.length; i++) {
      Node n = new Node(nodesCurrentlyInCluster[i]);
      nodes.put(n.getNodeId(), n);
    }
    log("### Cluster: thisNodeConnected -> " + this);
    fireThisNodeConnectedEvent();
  }

  public synchronized void thisNodeDisconnected() {
    nodes.clear();
    log("### Cluster: thisNodeDisconnected -> " + this);
    if (thisNode != null) fireThisNodeDisconnectedEvent();
  }

  public synchronized void nodeConnected(String nodeId) {
    Node n = new Node(nodeId);
    nodes.put(n.getNodeId(), n);
    log("### Cluster: nodeConnected -> " + this);
    if (thisNode != null) {
      fireNodeConnectedEvent(nodeId);
    }
  }

  public synchronized void nodeDisconnected(String nodeId) {
    nodes.remove(nodeId);
    log("### Cluster: nodeDisconnected -> " + this);
    if (thisNode != null) fireNodeDisconnectedEvent(nodeId);
  }

  public synchronized String toString() {
    return "Cluster{ thisNode=" + thisNode + ", nodesInCluster=" + nodes.keySet() + "}";
  }

  public synchronized void addClusterEventListener(ClusterEventListener cel) {
    Object oldCel = listeners.put(cel, cel);
    if (oldCel == null && thisNode != null) {
      fireThisNodeConnectedEvent();
    }
  }

  private synchronized void fireThisNodeConnectedEvent() {
    assertPre(thisNode != null);
    final String ids[] = getCurrentNodeIds();
    for (Iterator i = listeners.keySet().iterator(); i.hasNext();) {
      ClusterEventListener l = (ClusterEventListener) i.next();
      try {
        l.thisNodeConnected(thisNode.getNodeId(), ids);
      } catch (Throwable e) {
        log(e, thisNode.getNodeId());
      }
    }
  }

  private synchronized void fireThisNodeDisconnectedEvent() {
    assertPre(thisNode != null);
    for (Iterator i = listeners.keySet().iterator(); i.hasNext();) {
      ClusterEventListener l = (ClusterEventListener) i.next();
      try {
        l.thisNodeDisconnected(thisNode.getNodeId());
      } catch (Throwable e) {
        log(e, thisNode.getNodeId());
      }
    }
  }

  private synchronized void fireNodeConnectedEvent(String newNodeId) {
    assertPre(thisNode != null);
    for (Iterator i = listeners.keySet().iterator(); i.hasNext();) {
      ClusterEventListener l = (ClusterEventListener) i.next();
      try {
        l.nodeConnected(newNodeId);
      } catch (Throwable e) {
        log(e, thisNode.getNodeId());
      }
    }
  }

  private synchronized void fireNodeDisconnectedEvent(String nodeId) {
    assertPre(thisNode != null);
    for (Iterator i = listeners.keySet().iterator(); i.hasNext();) {
      ClusterEventListener l = (ClusterEventListener) i.next();
      try {
        l.nodeDisconnected(nodeId);
      } catch (Throwable e) {
        log(e, thisNode.getNodeId());
      }
    }
  }

  private static void assertPre(boolean b) {
    if (!b) throw new AssertionError("Pre-condition failed!");
  }

  private synchronized String[] getCurrentNodeIds() {
    final String[] rv = new String[nodes.size()];
    nodes.keySet().toArray(rv);
    Arrays.sort(rv);
    return rv;
  }

  private static void log(String string) {
    if (debug) System.err.println(string);
  }

  private void log(Throwable e, String nodeId) {
    // FIXME: switch to a real logger
    CharArrayWriter caw = new CharArrayWriter();
    PrintWriter pw = new PrintWriter(caw);
    e.printStackTrace(pw);
    pw.flush();
    final String stack = new String(caw.toString());
    System.err.println("\n\n###################################\n" + "Got Exception -> nodeId = " + nodeId
                       + " cluster -> " + this + "\n" + "Exception = " + stack + "\n"
                       + "###################################\n\n");

  }
}
