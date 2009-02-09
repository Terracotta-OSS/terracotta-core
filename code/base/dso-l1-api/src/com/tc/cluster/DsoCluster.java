/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tc.cluster.exceptions.ClusteredListenerException;
import com.tc.cluster.exceptions.UnclusteredObjectException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface DsoCluster {
  public void addClusterListener(DsoClusterListener listener) throws ClusteredListenerException;

  public void removeClusterListener(DsoClusterListener listener);

  public DsoClusterTopology getClusterTopology();

  public DsoNode getCurrentNode();

  public boolean isNodeJoined();

  public boolean areOperationsEnabled();

  public Set<DsoNode> getNodesWithObject(Object object) throws UnclusteredObjectException;

  public Map<?, Set<DsoNode>> getNodesWithObjects(Collection<?> objects) throws UnclusteredObjectException;

  public <K> Set<K> getKeysForOrphanedValues(Map<K, ?> map) throws UnclusteredObjectException;

  public <K> Set<K> getKeysForLocalValues(Map<K, ?> map) throws UnclusteredObjectException;
}