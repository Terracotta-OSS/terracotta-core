/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster.mock;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.cluster.DsoClusterTopology;
import com.tc.cluster.DsoNode;
import com.tc.cluster.exceptions.ClusteredListenerException;
import com.tc.cluster.exceptions.UnclusteredObjectException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements the {@code DsoCluster} interface by simulating this node being part of a cluster where all data is local
 * on this node.
 * <p>
 * This class is particularly useful to use with fields that will be injected with a real DsoCluster instance once the
 * application is actually running with Terracotta DSO enabled. Having an instance of {@code MockDsoCluster} assigned to
 * a field, allows code to be written without checks that would have to account for running code without DSO. Using
 * {@code MockDsoCluster} in code makes writing unit tests a lot easier.
 */
public class MockDsoCluster implements DsoCluster {

  private final DsoNode            mockNode;
  private final DsoClusterTopology mockTopology;
  private final Set<DsoNode>       mockNodeSet;

  public MockDsoCluster() {
    mockNode = new MockDsoNode();
    mockTopology = new MockDsoClusterTopology(mockNode);

    final Set<DsoNode> nodeSet = new HashSet<DsoNode>();
    nodeSet.add(mockNode);
    mockNodeSet = Collections.unmodifiableSet(nodeSet);
  }

  public void addClusterListener(final DsoClusterListener listener) throws ClusteredListenerException {
    if (listener != null) {
      final DsoClusterEvent event = new MockDsoClusterEvent(mockNode);
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
    return mockTopology;
  }

  public DsoNode getCurrentNode() {
    return mockNode;
  }

  public Set<DsoNode> getNodesWithObject(final Object object) throws UnclusteredObjectException {
    if (null == object) { return Collections.emptySet(); }

    return mockNodeSet;
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(final Object... objects) throws UnclusteredObjectException {
    if (null == objects || 0 == objects.length) { return Collections.emptyMap(); }

    return getNodesWithObjects(Arrays.asList(objects));
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(final Collection<?> objects) throws UnclusteredObjectException {
    if (null == objects || 0 == objects.size()) { return Collections.emptyMap(); }

    final Map<Object, Set<DsoNode>> result = new HashMap<Object, Set<DsoNode>>();
    for (Object object : objects) {
      result.put(object, mockNodeSet);
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
