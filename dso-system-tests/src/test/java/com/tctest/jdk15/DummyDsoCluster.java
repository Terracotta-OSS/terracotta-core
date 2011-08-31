/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterListener;
import com.tc.cluster.DsoClusterTopology;
import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tc.exception.ImplementMe;
import com.tcclient.cluster.DsoNode;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class DummyDsoCluster implements DsoCluster {

  public void addClusterListener(DsoClusterListener listener) {
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

  public <K> Set<K> getKeysForLocalValues(Map<K, ?> map) throws UnclusteredObjectException {
    throw new ImplementMe();
  }

  public <K> Set<K> getKeysForOrphanedValues(Map<K, ?> map) throws UnclusteredObjectException {
    throw new ImplementMe();
  }

  public Set<DsoNode> getNodesWithObject(Object object) throws UnclusteredObjectException {
    throw new ImplementMe();
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(Object... objects) throws UnclusteredObjectException {
    throw new ImplementMe();
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(Collection<?> objects) throws UnclusteredObjectException {
    throw new ImplementMe();
  }

  public boolean isNodeJoined() {
    throw new ImplementMe();
  }

  public void removeClusterListener(DsoClusterListener listener) {
    throw new ImplementMe();

  }
  
  public DsoNode waitUntilNodeJoinsCluster() {
    throw new ImplementMe();
  }

}
