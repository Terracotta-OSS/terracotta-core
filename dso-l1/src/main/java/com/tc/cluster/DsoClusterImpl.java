/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientObjectManager;
import com.tc.object.ClusterMetaDataManager;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TCMap;
import com.tc.object.bytecode.TCServerMap;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Util;
import com.tcclient.cluster.ClusterInternalEventsContext;
import com.tcclient.cluster.ClusterNodeStatus;
import com.tcclient.cluster.DsoClusterInternal;
import com.tcclient.cluster.DsoClusterInternalEventsGun;
import com.tcclient.cluster.DsoNode;
import com.tcclient.cluster.DsoNodeInternal;
import com.tcclient.cluster.DsoNodeMetaData;
import com.tcclient.cluster.OutOfBandDsoClusterListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DsoClusterImpl implements DsoClusterInternal, DsoClusterInternalEventsGun {

  private static final TCLogger                  LOGGER               = TCLogging.getLogger(DsoClusterImpl.class);

  private volatile ClientID                      currentClientID;
  private volatile DsoNodeInternal               currentNode;

  private final DsoClusterTopologyImpl           topology             = new DsoClusterTopologyImpl();
  private final List<DsoClusterListener>         listeners            = new CopyOnWriteArrayList<DsoClusterListener>();
  private final Object                           nodeJoinsClusterSync = new Object();

  private final ReentrantReadWriteLock           stateLock            = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.ReadLock  stateReadLock        = stateLock.readLock();
  private final ReentrantReadWriteLock.WriteLock stateWriteLock       = stateLock.writeLock();
  private final ClusterNodeStatus                nodeStatus           = new ClusterNodeStatus();
  private final FiredEventsStatus                firedEventsStatus    = new FiredEventsStatus();
  private final OutOfBandNotifier                outOfBandNotifier    = new OutOfBandNotifier();

  private ClusterMetaDataManager                 clusterMetaDataManager;
  private ClientObjectManager                    clientObjectManager;

  private Sink                                   eventsProcessorSink;

  public void init(final ClusterMetaDataManager metaDataManager, final ClientObjectManager objectManager,
                   final Stage dsoClusterEventsStage) {
    this.clusterMetaDataManager = metaDataManager;
    this.clientObjectManager = objectManager;
    this.eventsProcessorSink = dsoClusterEventsStage.getSink();

    for (DsoNodeInternal node : topology.getInternalNodes()) {
      retrieveMetaDataForDsoNode(node);
    }
    outOfBandNotifier.start();
  }

  public void shutdown() {
    this.outOfBandNotifier.shutdown();
  }

  public void addClusterListener(final DsoClusterListener listener) {

    stateWriteLock.lock();
    try {
      if (listeners.contains(listener)) { return; }
      listeners.add(listener);
    } finally {
      stateWriteLock.unlock();
    }

    if (nodeStatus.getState().isNodeLeft()) {
      fireEvent(DsoClusterEventType.NODE_LEFT, new DsoClusterEventImpl(currentNode), listener);
    } else {
      if (nodeStatus.getState().isNodeJoined()) {
        fireNodeJoinedInternal(currentNode, new DsoClusterEventImpl(currentNode), listener);
      }
      if (nodeStatus.getState().areOperationsEnabled()) {
        fireEvent(DsoClusterEventType.OPERATIONS_ENABLED, new DsoClusterEventImpl(currentNode), listener);
      }
    }
  }

  public void removeClusterListener(final DsoClusterListener listener) {
    stateWriteLock.lock();
    try {
      listeners.remove(listener);
    } finally {
      stateWriteLock.unlock();
    }
  }

  public DsoNode getCurrentNode() {
    stateReadLock.lock();
    try {
      return currentNode;
    } finally {
      stateReadLock.unlock();
    }
  }

  public DsoClusterTopology getClusterTopology() {
    return topology;
  }

  public Set<DsoNode> getNodesWithObject(final Object object) throws UnclusteredObjectException {
    Assert.assertNotNull(clusterMetaDataManager);

    if (null == object) { return Collections.emptySet(); }

    // we might have to use ManagerUtil.lookupExistingOrNull(object) here if we need to support literals
    if (object instanceof Manageable) {
      Manageable manageable = (Manageable) object;
      if (manageable.__tc_isManaged()) {
        ObjectID objectId = manageable.__tc_managed().getObjectID();
        Set<NodeID> response = mergeLocalInformation(objectId, clusterMetaDataManager.getNodesWithObject(objectId));
        if (response.isEmpty()) { return Collections.emptySet(); }

        final Set<DsoNode> result = new HashSet<DsoNode>();
        for (NodeID nodeID : response) {
          result.add(topology.getAndRegisterDsoNode(nodeID));
        }

        return result;
      }
    }

    throw new UnclusteredObjectException(object);
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(final Object... objects) throws UnclusteredObjectException {
    Assert.assertNotNull(clusterMetaDataManager);

    if (null == objects || 0 == objects.length) { return Collections.emptyMap(); }

    return getNodesWithObjects(Arrays.asList(objects));
  }

  public <K> Map<K, Set<DsoNode>> getNodesWithKeys(final Map<K, ?> map, final Collection<? extends K> keys)
      throws UnclusteredObjectException {
    Assert.assertNotNull(clusterMetaDataManager);

    if (null == keys || 0 == keys.size() || null == map) { return Collections.emptyMap(); }

    Map<K, Set<DsoNode>> result = new HashMap<K, Set<DsoNode>>();

    if (map instanceof Manageable) {
      Manageable manageable = (Manageable) map;
      if (manageable.__tc_isManaged()) {
        Map<K, Set<NodeID>> rawResult = null;
        if (manageable instanceof TCMap) {
          rawResult = clusterMetaDataManager.getNodesWithKeys((TCMap) map, keys);
        } else if (manageable instanceof TCServerMap) {
          rawResult = clusterMetaDataManager.getNodesWithKeys((TCServerMap) map, keys);
        }

        if (rawResult != null) {
          for (Map.Entry<K, Set<NodeID>> entry : rawResult.entrySet()) {
            Set<DsoNode> dsoNodes = new HashSet<DsoNode>(rawResult.entrySet().size(), 1.0f);
            for (NodeID nodeID : entry.getValue()) {
              DsoNodeInternal dsoNode = topology.getAndRegisterDsoNode(nodeID);
              dsoNodes.add(dsoNode);
            }
            result.put(entry.getKey(), dsoNodes);
          }
        }
      }
    }
    return result;
  }

  public Map<?, Set<DsoNode>> getNodesWithObjects(final Collection<?> objects) throws UnclusteredObjectException {
    Assert.assertNotNull(clusterMetaDataManager);

    if (null == objects || 0 == objects.size()) { return Collections.emptyMap(); }

    // ensure that all objects in the collection are managed and collect their object IDs
    // we might have to use ManagerUtil.lookupExistingOrNull(object) here if we need to support literals
    final HashMap<ObjectID, Object> objectIDMapping = new HashMap<ObjectID, Object>();
    for (Object object : objects) {
      if (object != null) {
        if (!(object instanceof Manageable)) {
          throw new UnclusteredObjectException(object);
        } else {
          Manageable manageable = (Manageable) object;
          if (!manageable.__tc_isManaged()) {
            throw new UnclusteredObjectException(object);
          } else {
            objectIDMapping.put(manageable.__tc_managed().getObjectID(), object);
          }
        }
      }
    }

    if (0 == objectIDMapping.size()) { return Collections.emptyMap(); }

    // retrieve the object locality information from the L2
    final Map<ObjectID, Set<NodeID>> response = mergeLocalInformation(clusterMetaDataManager
        .getNodesWithObjects(objectIDMapping.keySet()));
    if (response.isEmpty()) { return Collections.emptyMap(); }

    // transform object IDs and node IDs in actual local instances
    Map<Object, Set<DsoNode>> result = new IdentityHashMap<Object, Set<DsoNode>>();
    for (Map.Entry<ObjectID, Set<NodeID>> entry : response.entrySet()) {
      final Object object = objectIDMapping.get(entry.getKey());
      Assert.assertNotNull(object);

      final Set<DsoNode> dsoNodes = new HashSet<DsoNode>();
      for (NodeID nodeID : entry.getValue()) {
        dsoNodes.add(topology.getAndRegisterDsoNode(nodeID));
      }
      result.put(object, dsoNodes);
    }

    return result;
  }

  public <K> Set<K> getKeysForOrphanedValues(final Map<K, ?> map) throws UnclusteredObjectException {
    Assert.assertNotNull(clusterMetaDataManager);

    if (null == map) { return Collections.emptySet(); }

    if (map instanceof Manageable) {
      Manageable manageable = (Manageable) map;
      if (manageable.__tc_isManaged() && manageable instanceof TCMap) {
        final Set<K> result = new HashSet<K>();
        final Set keys = clusterMetaDataManager.getKeysForOrphanedValues((TCMap) map);
        for (Object key : keys) {
          if (key instanceof ObjectID) {
            try {
              result.add((K) clientObjectManager.lookupObject((ObjectID) key));
            } catch (ClassNotFoundException e) {
              Assert.fail("Unexpected ClassNotFoundException for key '" + key + "' : " + e.getMessage());
            }
          } else {
            result.add((K) key);
          }
        }
        return result;
      }
    }

    // if either the map isn't clustered, or it doesn't implement partial map capabilities, then no key are orphaned
    return Collections.emptySet();
  }

  public <K> Set<K> getKeysForLocalValues(final Map<K, ?> map) throws UnclusteredObjectException {
    if (null == map) { return Collections.emptySet(); }

    if (map instanceof Manageable) {
      Manageable manageable = (Manageable) map;
      if (manageable.__tc_isManaged() && manageable instanceof TCMap) {
        final Collection<Map.Entry> localEntries = ((TCMap) manageable).__tc_getAllEntriesSnapshot();
        if (0 == localEntries.size()) { return Collections.emptySet(); }

        final Set<K> result = new HashSet<K>();
        for (Map.Entry entry : localEntries) {
          if (!(entry.getValue() instanceof ObjectID) || clientObjectManager.isLocal((ObjectID) entry.getValue())) {
            result.add((K) entry.getKey());
          }
        }

        return result;
      }
    }

    // if either the map isn't clustered, or it doesn't implement partial map capabilities, then all the keys are local
    return map.keySet();
  }

  public DsoNodeMetaData retrieveMetaDataForDsoNode(final DsoNodeInternal node) {
    Assert.assertNotNull(clusterMetaDataManager);
    return clusterMetaDataManager.retrieveMetaDataForDsoNode(node);
  }

  public boolean isNodeJoined() {
    stateReadLock.lock();
    try {
      return nodeStatus.getState().isNodeJoined();
    } finally {
      stateReadLock.unlock();
    }
  }

  public boolean areOperationsEnabled() {
    stateReadLock.lock();
    try {
      return nodeStatus.getState().areOperationsEnabled();
    } finally {
      stateReadLock.unlock();
    }
  }

  public DsoNode waitUntilNodeJoinsCluster() {
    /*
     * It might be nice to throw InterruptedException here, but since the method is defined inside tim-api, we can't so
     * re-interrupting once the node is identified is the best option we have available
     */
    boolean interrupted = false;
    try {
      synchronized (nodeJoinsClusterSync) {
        while (currentNode == null) {
          try {
            nodeJoinsClusterSync.wait();
          } catch (InterruptedException e) {
            interrupted = true;
          }
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    return currentNode;
  }

  private void notifyWaiters() {
    synchronized (nodeJoinsClusterSync) {
      nodeJoinsClusterSync.notifyAll();
    }
  }

  public void fireThisNodeJoined(final NodeID nodeId, final NodeID[] clusterMembers) {
    stateWriteLock.lock();
    try {

      // we might get multiple calls in a row, ignore all but the first one
      if (currentNode != null) { return; }

      currentClientID = (ClientID) nodeId;
      currentNode = topology.registerThisDsoNode(nodeId);
      nodeStatus.nodeJoined();
      nodeStatus.operationsEnabled();

      for (NodeID otherNodeId : clusterMembers) {
        if (!currentClientID.equals(otherNodeId)) {
          topology.registerDsoNode(otherNodeId);
        }
      }
    } finally {
      stateWriteLock.unlock();

      if (currentNode != null) {
        notifyWaiters();
      } else {
        fireNodeJoined(nodeId);
      }

      fireOperationsEnabled();
    }

  }

  public void fireThisNodeLeft() {
    boolean fireOperationsDisabled = false;
    stateWriteLock.lock();
    try {
      // We may get a node left event without ever seeing a node joined event, just ignore
      // the node left event in that case
      if (!nodeStatus.getState().isNodeJoined()) { return; }
      if (nodeStatus.getState().areOperationsEnabled()) {
        fireOperationsDisabled = true;
      }
      nodeStatus.nodeLeft();
    } finally {
      stateWriteLock.unlock();
    }

    if (fireOperationsDisabled) {
      fireOperationsDisabledNoCheck();
    } else {
      // don't fire node left right away, but wait until operations disabled is fired first
      firedEventsStatus.waitUntilOperationsDisabledFired();
    }
    fireNodeLeft(new ClientID(currentNode.getChannelId()));
  }

  public void fireNodeJoined(final NodeID nodeId) {
    if (topology.containsDsoNode(nodeId)) { return; }

    final DsoClusterEvent event = new DsoClusterEventImpl(topology.getAndRegisterDsoNode(nodeId));
    for (DsoClusterListener listener : listeners) {
      fireNodeJoinedInternal(topology.getInternalNode(nodeId), event, listener);
    }
  }

  private void fireNodeJoinedInternal(final DsoNodeInternal node, final DsoClusterEvent event,
                                      final DsoClusterListener listener) {
    if (node != null) {
      retrieveMetaDataForDsoNode(node);
    }
    fireEvent(DsoClusterEventType.NODE_JOIN, event, listener);
  }

  public void fireNodeLeft(final NodeID nodeId) {
    if (!topology.containsDsoNode(nodeId)) { return; }

    final DsoClusterEvent event = new DsoClusterEventImpl(topology.getAndRemoveDsoNode(nodeId));
    for (DsoClusterListener listener : listeners) {
      fireEvent(DsoClusterEventType.NODE_LEFT, event, listener);
    }
  }

  public void fireOperationsEnabled() {
    if (currentNode != null) {
      stateWriteLock.lock();
      try {
        if (nodeStatus.getState().areOperationsEnabled()) { return; }
        nodeStatus.operationsEnabled();
      } finally {
        stateWriteLock.unlock();
      }

      final DsoClusterEvent event = new DsoClusterEventImpl(currentNode);
      for (DsoClusterListener listener : listeners) {
        fireEvent(DsoClusterEventType.OPERATIONS_ENABLED, event, listener);
      }
      firedEventsStatus.operationsEnabledFired();
    }
  }

  public void fireOperationsDisabled() {
    stateWriteLock.lock();
    try {
      if (!nodeStatus.getState().areOperationsEnabled()) { return; }
      nodeStatus.operationsDisabled();
    } finally {
      stateWriteLock.unlock();
    }

    fireOperationsDisabledNoCheck();
  }

  private void fireOperationsDisabledNoCheck() {
    final DsoClusterEvent event = new DsoClusterEventImpl(currentNode);
    for (DsoClusterListener listener : listeners) {
      fireEvent(DsoClusterEventType.OPERATIONS_DISABLED, event, listener);
    }
    firedEventsStatus.operationsDisabledFired();
  }

  private void fireEvent(final DsoClusterEventType eventType, final DsoClusterEvent event,
                         final DsoClusterListener listener) {
    /**
     * use out-of-band notification depending on listener otherwise use the single threaded eventProcessorSink to
     * process the cluster event.
     */
    if (useOOBNotification(eventType, event, listener)) {
      outOfBandNotifier.submit(new Runnable() {
        public void run() {
          notifyDsoClusterListener(eventType, event, listener);
        }
      });
    } else {
      this.eventsProcessorSink.add(new ClusterInternalEventsContext(eventType, event, listener));
    }
  }

  private boolean useOOBNotification(DsoClusterEventType eventType, DsoClusterEvent event, DsoClusterListener listener) {
    if (listener instanceof OutOfBandDsoClusterListener) {
      return ((OutOfBandDsoClusterListener) listener).useOutOfBandNotification(eventType, event);
    } else {
      return false;
    }
  }

  public void notifyDsoClusterListener(DsoClusterEventType eventType, DsoClusterEvent event, DsoClusterListener listener) {
    try {
      switch (eventType) {
        case NODE_JOIN:
          listener.nodeJoined(event);
          break;
        case NODE_LEFT:
          listener.nodeLeft(event);
          break;
        case OPERATIONS_ENABLED:
          listener.operationsEnabled(event);
          break;
        case OPERATIONS_DISABLED:
          listener.operationsDisabled(event);
          break;
        default:
          throw new AssertionError("Unknown type of cluster event - " + eventType);
      }
    } catch (Throwable e) {
      log(event, e);
    }
  }

  private void log(final DsoClusterEvent event, final Throwable t) {
    LOGGER.error("Problem firing the cluster event : " + event, t);
  }

  private Map<ObjectID, Set<NodeID>> mergeLocalInformation(Map<ObjectID, Set<NodeID>> serverResult) {
    if (currentClientID != null) {
      for (Map.Entry<ObjectID, Set<NodeID>> e : serverResult.entrySet()) {
        Set<NodeID> filtered = mergeLocalInformation(e.getKey(), e.getValue());
        if (filtered != e.getValue()) {
          e.setValue(filtered);
        }
      }
    }
    return serverResult;
  }

  private Set<NodeID> mergeLocalInformation(ObjectID objectId, Set<NodeID> serverResult) {
    if (clientObjectManager.isLocal(objectId)) {
      serverResult.add(currentClientID);
    }
    return serverResult;
  }

  private static final class FiredEventsStatus {

    private DsoClusterEventType lastFiredEvent = null;

    public synchronized void operationsDisabledFired() {
      lastFiredEvent = DsoClusterEventType.OPERATIONS_DISABLED;
      this.notifyAll();
    }

    public synchronized void operationsEnabledFired() {
      lastFiredEvent = DsoClusterEventType.OPERATIONS_ENABLED;
      this.notifyAll();
    }

    public synchronized void waitUntilOperationsDisabledFired() {
      boolean interrupted = false;
      try {
        while (lastFiredEvent != DsoClusterEventType.OPERATIONS_DISABLED) {
          try {
            this.wait();
          } catch (InterruptedException e) {
            interrupted = true;
          }
        }
      } finally {
        Util.selfInterruptIfNeeded(interrupted);
      }
    }
  }

  private static class OutOfBandNotifier {
    private static final String                 TASK_THREAD_PREFIX   = "Out of band notifier";
    private static final long                   TASK_RUN_TIME_MILLIS = TCPropertiesImpl
                                                                         .getProperties()
                                                                         .getLong(TCPropertiesConsts.L1_CLUSTEREVENTS_OOB_JOINTIME_MILLIS,
                                                                                  100);
    private final LinkedBlockingQueue<Runnable> taskQueue            = new LinkedBlockingQueue<Runnable>();
    private volatile long                       count                = 0;
    private volatile boolean                    shutdown;

    private void submit(final Runnable taskToExecute) {
      taskQueue.add(taskToExecute);
    }

    private void start() {
      Thread outOfBandNotifierThread = new Thread(new Runnable() {

        public void run() {

          Runnable taskToExecute;
          while (true) {

            if (shutdown) { return; }

            try {
              taskToExecute = taskQueue.take();
            } catch (InterruptedException e) {
              continue;
            }

            Thread oobTask = new Thread(taskToExecute, TASK_THREAD_PREFIX + " - " + count++);
            oobTask.setDaemon(true);
            oobTask.start();
            try {
              oobTask.join(TASK_RUN_TIME_MILLIS);
            } catch (InterruptedException e) {
              continue;
            }
          }

        }
      }, TASK_THREAD_PREFIX + " - Main");

      outOfBandNotifierThread.setDaemon(true);
      outOfBandNotifierThread.start();
    }

    public void shutdown() {
      this.shutdown = true;
      this.taskQueue.add(new Runnable() {
        public void run() {
          // dummy task to notify other thread
        }
      });
    }
  }

}