/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tc.cluster.exceptions.ClusteredListenerException;
import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.ClusterMetaDataManager;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TCMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import junit.framework.Assert;

public class DsoClusterImpl implements DsoClusterInternal {

  private volatile DsoNode               currentNode;

  private final Map<String, DsoNode>     nodes                = new ConcurrentHashMap<String, DsoNode>();
  private final List<DsoClusterListener> listeners            = new CopyOnWriteArrayList<DsoClusterListener>();

  private ClusterMetaDataManager         clusterMetaDataManager;

  private boolean                        isNodeJoined         = false;
  private boolean                        areOperationsEnabled = false;

  public void init(final ClusterMetaDataManager manager) {
    this.clusterMetaDataManager = manager;
  }

  public void addClusterListener(final DsoClusterListener listener) throws ClusteredListenerException {
    final boolean fireThisNodeJoined;
    synchronized (this) {
      fireThisNodeJoined = (currentNode != null);
      if (null == listeners || listeners.contains(listener)) { return; }

      listeners.add(listener);
    }

    if (fireThisNodeJoined) {
      System.out.println(">>>>>> " + currentNode + " - DsoClusterImpl.addClusterListener : fireThisNodeJoined = " + fireThisNodeJoined);
      fireNodeJoined(currentNode.getId());
      fireOperationsEnabled();
    }
  }

  public synchronized void removeClusterListener(final DsoClusterListener listener) {
    listeners.remove(listener);
  }

  public synchronized DsoNode getCurrentNode() {
    return currentNode;
  }

  public DsoClusterTopology getClusterTopology() {
    throw new ImplementMe();
  }

  public Set<DsoNode> getNodesWithObject(final Object object) throws UnclusteredObjectException {
    Assert.assertNotNull(clusterMetaDataManager);

    if (null == object) {
      return Collections.emptySet();
    }

    // we might have to use ManagerUtil.lookupExistingOrNull(object) here if we need to support literals
    if (object instanceof Manageable) {
      Manageable manageable = (Manageable)object;
      if (manageable.__tc_isManaged()) {
        Set<NodeID> response = clusterMetaDataManager.getNodesWithObject(manageable.__tc_managed().getObjectID());
        if (response.isEmpty()) {
          return Collections.emptySet();
        }

        final Set<DsoNode> result = new HashSet<DsoNode>();
        for (NodeID nodeID : response) {
          result.add(getDsoNode(nodeID.toString()));
        }

        return result;
      }
    }

    throw new UnclusteredObjectException(object);
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(final Object... objects) throws UnclusteredObjectException {
    Assert.assertNotNull(clusterMetaDataManager);

    if (null == objects || 0 == objects.length) {
      return Collections.emptyMap();
    }

    return getNodesWithObjects(Arrays.asList(objects));
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(final Collection<?> objects) throws UnclusteredObjectException {
    Assert.assertNotNull(clusterMetaDataManager);

    if (null == objects || 0 == objects.size()) {
      return Collections.emptyMap();
    }

    // ensure that all objects in the collection are managed and collect their object IDs
    // we might have to use ManagerUtil.lookupExistingOrNull(object) here if we need to support literals
    final HashMap<ObjectID, Object> objectIDMapping = new HashMap<ObjectID, Object>();
    for (Object object : objects) {
      if (object != null) {
        if (!(object instanceof Manageable)) {
          throw new UnclusteredObjectException(object);
        } else {
          Manageable manageable = (Manageable)object;
          if (!manageable.__tc_isManaged()) {
            throw new UnclusteredObjectException(object);
          } else {
            objectIDMapping.put(manageable.__tc_managed().getObjectID(), object);
          }
        }
      }
    }

    if (0 == objectIDMapping.size()) {
      return Collections.emptyMap();
    }

    // retrieve the object locality information from the L2
    final Map<ObjectID, Set<NodeID>> response = clusterMetaDataManager.getNodesWithObjects(objectIDMapping.keySet());
    if (response.isEmpty()) {
      return Collections.emptyMap();
    }

    // transform object IDs and node IDs in actual local instances
    Map<Object, Set<DsoNode>> result = new HashMap<Object, Set<DsoNode>>();
    for (Map.Entry<ObjectID, Set<NodeID>> entry : response.entrySet()) {
      final Object object = objectIDMapping.get(entry.getKey());
      Assert.assertNotNull(object);

      final Set<DsoNode> dsoNodes = new HashSet<DsoNode>();
      for (NodeID nodeID : entry.getValue()) {
        dsoNodes.add(getDsoNode(nodeID.toString()));
      }
      result.put(object, dsoNodes);
    }

    return result;
  }

  public <K> Set<K> getKeysForOrphanedValues(final Map<K, ?> map) throws UnclusteredObjectException {
    Assert.assertNotNull(clusterMetaDataManager);

    if (null == map) {
      return Collections.emptySet();
    }

    if (map instanceof Manageable) {
      Manageable manageable = (Manageable)map;
      if (manageable.__tc_isManaged()) {
        if (manageable instanceof TCMap) {
          clusterMetaDataManager.getKeysForOrphanedValues(manageable.__tc_managed().getObjectID());
          return Collections.emptySet();
        } else {
          return Collections.emptySet();
        }
      }
    }

    throw new UnclusteredObjectException(map);
  }

  public <K> Set<K> getKeysForLocalValues(final Map<K, ?> map) throws UnclusteredObjectException {
    if (null == map) {
      return Collections.emptySet();
    }

    if (map instanceof Manageable) {
      Manageable manageable = (Manageable)map;
      if (manageable.__tc_isManaged()) {
        if (manageable instanceof TCMap) {
          final Collection<Map.Entry> localEntries = ((TCMap)manageable).__tc_getAllLocalEntriesSnapshot();
          if (0 == localEntries.size()) {
            return Collections.emptySet();
          }

          final Set<K> result = new HashSet<K>();
          for (Map.Entry entry : localEntries) {
            result.add((K)entry.getKey());
          }

          return result;
        } else {
          return Collections.emptySet();
        }
      }
    }

    throw new UnclusteredObjectException(map);
  }

  public synchronized boolean isNodeJoined() {
    return isNodeJoined;
  }

  public synchronized boolean areOperationsEnabled() {
    return areOperationsEnabled;
  }

  private DsoClusterEvent createEvent(final String nodeId) {
    DsoNode node = getDsoNode(nodeId);
    return new DsoClusterEventImpl(node);
  }

  private DsoNode getDsoNode(final String nodeId) {
    synchronized (this) {
      DsoNode node = nodes.get(nodeId);
      if (null == node) {
        node = new DsoNodeImpl(nodeId);
        nodes.put(nodeId, node);
      }
      return node;
    }
  }

  public void fireThisNodeJoined(final String nodeId) {
    System.out.println(">>>>>> " + currentNode + " - DsoClusterImpl.fireThisNodeJoined(" + nodeId + ")");
    synchronized (this) {
      // we might get multiple calls in a row, ignore all but the first in a row.
      if (currentNode != null) return;

      currentNode = new DsoNodeImpl(nodeId);
      isNodeJoined = true;
      nodes.put(nodeId, currentNode);
    }

    fireNodeJoined(nodeId);
  }

  public void fireThisNodeLeft() {
    System.out.println(">>>>>> " + currentNode + " - DsoClusterImpl.fireThisNodeLeft(" + currentNode.getId() + ")");
    synchronized (this) {
      if (null == currentNode) {
        // client channels closed before we knew the currentNode. Skip the disconnect event in this case
        return;
      }
      isNodeJoined = false;
    }

    fireNodeLeft(currentNode.getId());
  }

  public void fireNodeJoined(final String nodeId) {
    System.out.println(">>>>>> " + currentNode + " - DsoClusterImpl.fireNodeJoined(" + nodeId + ")");
    final DsoClusterEvent event = createEvent(nodeId);
    for (DsoClusterListener listener : listeners) {
      listener.nodeJoined(event);
    }
  }

  public void fireNodeLeft(final String nodeId) {
    System.out.println(">>>>>> " + currentNode + " - DsoClusterImpl.fireNodeLeft(" + nodeId + ")");
    final DsoClusterEvent event = createEvent(nodeId);
    for (DsoClusterListener listener : listeners) {
      listener.nodeLeft(event);
    }
  }

  public void fireOperationsEnabled() {
    // only fire this event when the node has already confirmed its connection through a handshake
    if (currentNode != null) {
      System.out.println(">>>>>> " + currentNode + " - DsoClusterImpl.fireOperationsEnabled(" + currentNode + ")");

      synchronized (this) {
        areOperationsEnabled = true;
      }

      final DsoClusterEvent event = createEvent(currentNode.getId());
      for (DsoClusterListener listener : listeners) {
        listener.operationsEnabled(event);
      }
    }
  }

  public void fireOperationsDisabled() {
    System.out.println(">>>>>> " + currentNode + " - DsoClusterImpl.fireOperationsDisabled(" + currentNode + ")");
    synchronized (this) {
      areOperationsEnabled = false;
    }

    final DsoClusterEvent event = createEvent(currentNode.getId());
    for (DsoClusterListener listener : listeners) {
      listener.operationsDisabled(event);
    }
  }
}