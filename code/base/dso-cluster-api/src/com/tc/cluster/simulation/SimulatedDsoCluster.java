/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster.simulation;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.cluster.DsoClusterTopology;
import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tcclient.cluster.DsoNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements the {@code DsoCluster} interface by simulating a node that's connected to a cluster with operations
 * enabled and all data being local on this node.
 * <p>
 * This class is particularly useful to use with fields that will be injected with a real DsoCluster instance once the
 * application is actually running with Terracotta DSO enabled. Having an instance of {@code SimulatedDsoCluster}
 * assigned to a field, allows code to be written without checks that would have to account for running code without
 * DSO. Using {@code SimulatedDsoCluster} in code makes writing unit tests a lot easier.
 *
 * @since 3.0.0
 */
public class SimulatedDsoCluster implements DsoCluster {

  private final DsoNode            node;
  private final DsoClusterTopology topology;
  private final Set<DsoNode>       nodeSet;

  /**
   * Creates a new {@code SimulatedDsoCluster} instance.
   */
  public SimulatedDsoCluster() {
    node = new SimulatedDsoNode();
    topology = new SimulatedDsoClusterTopology(node);

    final Set<DsoNode> nodes = new HashSet<DsoNode>();
    nodes.add(node);
    nodeSet = Collections.unmodifiableSet(nodes);
  }

  public void addClusterListener(final DsoClusterListener listener) {
    if (listener != null) {
      final DsoClusterEvent event = new SimulatedDsoClusterEvent(node);
      listener.nodeJoined(event);
      listener.operationsEnabled(event);
    }
  }

  public void removeClusterListener(final DsoClusterListener listener) {
    // no-op
  }

  public boolean isNodeJoined() {
    return true;
  }

  public boolean areOperationsEnabled() {
    return true;
  }

  public DsoClusterTopology getClusterTopology() {
    return topology;
  }

  public DsoNode getCurrentNode() {
    return node;
  }

  public Set<DsoNode> getNodesWithObject(final Object object) throws UnclusteredObjectException {
    if (null == object) { return Collections.emptySet(); }

    return nodeSet;
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(final Object... objects) throws UnclusteredObjectException {
    if (null == objects || 0 == objects.length) { return Collections.emptyMap(); }

    return getNodesWithObjects(Arrays.asList(objects));
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(final Collection<?> objects) throws UnclusteredObjectException {
    if (null == objects || 0 == objects.size()) { return Collections.emptyMap(); }

    final Map<Object, Set<DsoNode>> result = new HashMap<Object, Set<DsoNode>>();
    for (Object object : objects) {
      result.put(object, nodeSet);
    }
    return result;
  }

  public <K> Set<K> getKeysForLocalValues(final Map<K, ?> map) throws UnclusteredObjectException {
    if (null == map) { return Collections.emptySet(); }
    return map.keySet();
  }

  public <K> Set<K> getKeysForOrphanedValues(final Map<K, ?> map) throws UnclusteredObjectException {
    return Collections.emptySet();
  }
}
