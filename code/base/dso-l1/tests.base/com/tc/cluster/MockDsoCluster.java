/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster;

import com.tc.cluster.exceptions.ClusteredListenerException;
import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tc.exception.ImplementMe;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MockDsoCluster implements DsoCluster {

  public void addClusterListener(final DsoClusterListener listener) throws ClusteredListenerException {
    throw new ImplementMe();
  }

  public boolean areOperationsEnabled() {
    throw new ImplementMe();
  }

  public DsoClusterTopology getClusterTopology() {
    throw new ImplementMe();
  }

  public DsoNode getCurrentNode() {
    throw new ImplementMe();
  }

  public <K> Set<K> getKeysForLocalValues(final Map<K, ?> map) throws UnclusteredObjectException {
    throw new ImplementMe();
  }

  public <K> Set<K> getKeysForOrphanedValues(final Map<K, ?> map) throws UnclusteredObjectException {
    throw new ImplementMe();
  }

  public Set<DsoNode> getNodesWithObject(final Object object) throws UnclusteredObjectException {
    throw new ImplementMe();
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(final Object... objects) throws UnclusteredObjectException {
    throw new ImplementMe();
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(final Collection<?> objects) throws UnclusteredObjectException {
    throw new ImplementMe();
  }

  public boolean isNodeJoined() {
    throw new ImplementMe();
  }

  public void removeClusterListener(final DsoClusterListener listener) {
    throw new ImplementMe();
  }

}
