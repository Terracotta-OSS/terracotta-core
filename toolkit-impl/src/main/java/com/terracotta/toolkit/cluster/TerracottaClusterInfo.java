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

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterTopology;
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
    dsoCluster.addClusterListener(new ClusterListenerWrapper(listener));
  }

  @Override
  public void removeClusterListener(ClusterListener listener) {
    dsoCluster.removeClusterListener(new ClusterListenerWrapper(listener));
  }

  @Override
  public Set<ClusterNode> getNodes() {
    return getNodes(dsoCluster.getClusterTopology());
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
    return new TerracottaNode(dsoCluster.getCurrentNode());
  }

  @Override
  public boolean areOperationsEnabled() {
    return dsoCluster.areOperationsEnabled();
  }

  public <K> Map<K, Set<ClusterNode>> getNodesWithKeys(final Map<K, ?> map, final Collection<? extends K> keys) {
    Map<K, Set<ClusterNode>> translation = new HashMap<K, Set<ClusterNode>>();
    Map<K, Set<DsoNode>> result = ((DsoClusterInternal) dsoCluster).getNodesWithKeys(map, keys);
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

  private ClusterEvent translateEvent(final DsoClusterEvent event, final Type type) {
    return new ClusterEvent() {
      @Override
      public ClusterNode getNode() {
        return new TerracottaNode(event.getNode());
      }

      @Override
      public Type getType() {
        return type;
      }
    };
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
        default:
          throw new AssertionError("Unknown type: " + type);
      }
    }

    @Override
    public int hashCode() {
      return listener.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof ClusterListenerWrapper) {
        return listener == ((ClusterListenerWrapper) o).listener;
      } else {
        return false;
      }
    }
  }

}
