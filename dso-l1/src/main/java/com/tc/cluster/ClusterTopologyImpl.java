/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tcclient.cluster.Node;
import com.tcclient.cluster.NodeImpl;
import com.tcclient.cluster.NodeInternal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClusterTopologyImpl implements ClusterTopology {
  private final ConcurrentMap<NodeID, NodeInternal> nodes          = new ConcurrentHashMap<>();

  private final ReentrantReadWriteLock                 nodesLock      = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.ReadLock        nodesReadLock  = nodesLock.readLock();
  private final ReentrantReadWriteLock.WriteLock       nodesWriteLock = nodesLock.writeLock();

  public ClusterTopologyImpl() {
    //
  }

  Collection<NodeInternal> getInternalNodes() {
    nodesReadLock.lock();
    try {
      return Collections.unmodifiableCollection(new ArrayList<>(nodes.values()));
    } finally {
      nodesReadLock.unlock();
    }
  }

  NodeInternal getInternalNode(NodeID nodeId) {
    nodesReadLock.lock();
    try {
      return nodes.get(nodeId);
    } finally {
      nodesReadLock.unlock();
    }
  }

  @Override
  public Collection<Node> getNodes() {
    nodesReadLock.lock();
    try {
      return Collections.unmodifiableCollection(new ArrayList<Node>(nodes.values()));
    } finally {
      nodesReadLock.unlock();
    }
  }

  boolean containsNode(NodeID nodeId) {
    nodesReadLock.lock();
    try {
      return nodes.containsKey(nodeId);
    } finally {
      nodesReadLock.unlock();
    }
  }

  NodeInternal getAndRegisterNode(ClientID nodeId) {
    nodesReadLock.lock();
    try {
      NodeInternal node = nodes.get(nodeId);
      if (node != null) { return node; }
    } finally {
      nodesReadLock.unlock();
    }

    return registerNode(nodeId);
  }

  NodeInternal getAndRemoveNode(NodeID nodeId) {
    nodesWriteLock.lock();
    try {
      NodeInternal node = nodes.remove(nodeId);
      return node;
    } finally {
      nodesWriteLock.unlock();
    }
  }

  NodeInternal registerNode(ClientID nodeId) {
    return registerNodeBase(nodeId, false);
  }

  NodeInternal registerThisNode(ClientID nodeId) {
    return registerNodeBase(nodeId, true);
  }

  NodeInternal updateOnRejoin(ClientID thisNodeId, NodeID[] clusterMembers) {
    nodesWriteLock.lock();
    try {
      for (NodeID otherNode : clusterMembers) {
        if (!thisNodeId.equals(otherNode)) {
          registerNodeBase((ClientID) otherNode, false);
        }
      }
      return registerNodeBase(thisNodeId, true);
    } finally {
      nodesWriteLock.unlock();
    }
  }

  private NodeInternal registerNodeBase(ClientID clientId, boolean isLocalNode) {
    final NodeInternal node = new NodeImpl(clientId.toString(), clientId.toLong());

    nodesWriteLock.lock();
    try {
      NodeInternal old = nodes.putIfAbsent(clientId, node);
      if (old != null) {
        return old;
      } else {
        return node;
      }
    } finally {
      nodesWriteLock.unlock();
    }
  }

  void cleanup() {
    nodesWriteLock.lock();
    try {
      nodes.clear();
    } finally {
      nodesWriteLock.unlock();
    }
  }

}