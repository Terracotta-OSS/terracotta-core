/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.cluster;

import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterEvent.Type;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.internal.cluster.OutOfBandClusterListener;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterTopology;
import com.tc.exception.PlatformRejoinException;
import com.tc.platform.PlatformService;
import com.tcclient.cluster.DsoClusterInternal;
import com.tcclient.cluster.DsoClusterInternal.DsoClusterEventType;
import com.tcclient.cluster.DsoNode;
import com.tcclient.cluster.OutOfBandDsoClusterListener;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TerracottaClusterInfo implements ClusterInfo {

  private final DsoCluster dsoCluster;

  public TerracottaClusterInfo(PlatformService platformService) {
    this.dsoCluster = platformService.getDsoCluster();
  }

  @Override
  public void addClusterListener(ClusterListener listener) {
    try {
      dsoCluster.addClusterListener(new ClusterListenerWrapper(listener));
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public void removeClusterListener(ClusterListener listener) {
    try {
      dsoCluster.removeClusterListener(new ClusterListenerWrapper(listener));
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }

  }

  @Override
  public Set<ClusterNode> getNodes() {
    try {
      return getNodes(dsoCluster.getClusterTopology());
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }

  }

  public Set<ClusterNode> getNodes(DsoClusterTopology dsoClusterTopology) {
    Collection<DsoNode> dsoNodes = dsoClusterTopology.getNodes();
    Set<ClusterNode> nodes = new HashSet<ClusterNode>(dsoNodes.size());

    for (DsoNode dsoNode : dsoNodes) {
      nodes.add(new TerracottaNode(dsoNode));
    }

    return Collections.unmodifiableSet(nodes);
  }

  @Override
  public ClusterNode getCurrentNode() {
    try {
      return new TerracottaNode(dsoCluster.getCurrentNode());
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
  }

  @Override
  public boolean areOperationsEnabled() {
    try {
      return dsoCluster.areOperationsEnabled();
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }

  }

  public <K> Map<K, Set<ClusterNode>> getNodesWithKeys(final Map<K, ?> map, final Collection<? extends K> keys) {
    Map<K, Set<ClusterNode>> translation = new HashMap<K, Set<ClusterNode>>();
    Map<K, Set<DsoNode>> result;
    try {
      result = ((DsoClusterInternal) dsoCluster).getNodesWithKeys(map, keys);
    } catch (PlatformRejoinException e) {
      throw new RejoinException(e);
    }
    Map<DsoNode, ClusterNode> nodes = new HashMap<DsoNode, ClusterNode>();
    for (Entry<K, Set<DsoNode>> entry : result.entrySet()) {
      Set<ClusterNode> clusterNodes = new HashSet<ClusterNode>();
      for (DsoNode dsoNode : entry.getValue()) {
        ClusterNode clusterNode = nodes.get(dsoNode);
        if (clusterNode == null) {
          clusterNode = new TerracottaNode(dsoNode);
          nodes.put(dsoNode, clusterNode);
        }
        clusterNodes.add(clusterNode);
      }
      translation.put(entry.getKey(), Collections.unmodifiableSet(clusterNodes));
    }
    return Collections.unmodifiableMap(translation);
  }

  private static ClusterEvent translateEvent(final DsoClusterEvent event, final Type type) {
    return new ClusterEventImpl(event.getNode(), type);
  }

  private static ClusterEvent translateEvent(final DsoClusterEvent event, final Type type, String msg) {
    return new ClusterEventImpl(event.getNode(), type, msg);
  }

  private class ClusterListenerWrapper implements OutOfBandDsoClusterListener {

    private final ClusterListener listener;

    private ClusterListenerWrapper(ClusterListener listener) {
      this.listener = listener;
    }

    @Override
    public void operationsEnabled(final DsoClusterEvent event) {
      listener.onClusterEvent(translateEvent(event, Type.OPERATIONS_ENABLED));
    }

    @Override
    public void operationsDisabled(DsoClusterEvent event) {
      listener.onClusterEvent(translateEvent(event, Type.OPERATIONS_DISABLED));
    }

    @Override
    public void nodeLeft(DsoClusterEvent event) {
      listener.onClusterEvent(translateEvent(event, Type.NODE_LEFT));
    }

    @Override
    public void nodeJoined(DsoClusterEvent event) {
      listener.onClusterEvent(translateEvent(event, Type.NODE_JOINED));
    }

    @Override
    public void nodeRejoined(DsoClusterEvent event) {
      listener.onClusterEvent(translateEvent(event, Type.NODE_REJOINED));
    }

    @Override
    public void nodeError(DsoClusterEvent event) {
      listener
          .onClusterEvent(translateEvent(event, Type.NODE_ERROR,
                                         "NODE_ERROR: Rejoin is not possible: Either Rejoin rejected or Rejoin not enabled"));
    }

    @Override
    public boolean useOutOfBandNotification(DsoClusterEventType type, DsoClusterEvent event) {
      if (listener instanceof OutOfBandClusterListener) {
        return ((OutOfBandClusterListener) listener)
            .useOutOfBandNotification(translateEvent(event, translateType(type)));
      } else {
        return false;
      }
    }

    private Type translateType(DsoClusterEventType type) {
      switch (type) {
        case NODE_JOIN:
          return Type.NODE_JOINED;
        case NODE_LEFT:
          return Type.NODE_LEFT;
        case OPERATIONS_ENABLED:
          return Type.OPERATIONS_ENABLED;
        case OPERATIONS_DISABLED:
          return Type.OPERATIONS_DISABLED;
        case NODE_REJOINED:
          return Type.NODE_REJOINED;
        case NODE_ERROR:
          return Type.NODE_ERROR;
      }
      throw new AssertionError("Unhandled event type: " + type);
    }

    @Override
    public int hashCode() {
      return listener.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof ClusterListenerWrapper) {
        return listener.equals(((ClusterListenerWrapper) o).listener);
      } else {
        return false;
      }
    }

  }

  private static class ClusterEventImpl implements ClusterEvent {

    private final TerracottaNode clusterNode;
    private final Type           type;
    private final String         msg;

    public ClusterEventImpl(DsoNode node, Type type) {
      this(node, type, null);
    }

    public ClusterEventImpl(DsoNode node, Type type, String msg) {
      clusterNode = new TerracottaNode(node);
      this.type = type;
      this.msg = "Node: " + node.toString() + ", Type: " + type + (msg == null ? "" : " - " + msg);
    }

    @Override
    public ClusterNode getNode() {
      return clusterNode;
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public String toString() {
      return "ClusterEvent [type=" + type + ", clusterNode=" + clusterNode + ", msg=" + msg + "]";
    }

    @Override
    public String getDetailedMessage() {
      return msg;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clusterNode == null) ? 0 : clusterNode.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ClusterEventImpl other = (ClusterEventImpl) obj;
      if (clusterNode == null) {
        if (other.clusterNode != null) return false;
      } else if (!clusterNode.equals(other.clusterNode)) return false;
      if (type != other.type) return false;
      return true;
    }

  }

}
