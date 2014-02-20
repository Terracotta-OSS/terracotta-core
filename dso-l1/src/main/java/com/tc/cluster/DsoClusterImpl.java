/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClientObjectManager;
import com.tc.object.ClusterMetaDataManager;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TCMap;
import com.tc.object.bytecode.TCServerMap;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Util;
import com.tcclient.cluster.ClusterInternalEventsContext;
import com.tcclient.cluster.ClusterNodeStatus;
import com.tcclient.cluster.ClusterNodeStatus.ClusterNodeStateType;
import com.tcclient.cluster.DsoClusterInternal;
import com.tcclient.cluster.DsoClusterInternalEventsGun;
import com.tcclient.cluster.DsoNode;
import com.tcclient.cluster.DsoNodeInternal;
import com.tcclient.cluster.DsoNodeMetaData;
import com.tcclient.cluster.OutOfBandDsoClusterListener;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DsoClusterImpl implements DsoClusterInternal, DsoClusterInternalEventsGun {

  private static final TCLogger                          LOGGER               = TCLogging
                                                                                  .getLogger(DsoClusterImpl.class);

  private volatile ClientID                              currentClientID;
  private volatile DsoNodeInternal                       currentNode;

  private final DsoClusterTopologyImpl                   topology             = new DsoClusterTopologyImpl();
  private final CopyOnWriteArrayList<DsoClusterListener> listeners            = new CopyOnWriteArrayList<DsoClusterListener>();
  private final Object                                   nodeJoinsClusterSync = new Object();

  private final ReentrantReadWriteLock                   stateLock            = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.ReadLock          stateReadLock        = stateLock.readLock();
  private final ReentrantReadWriteLock.WriteLock         stateWriteLock       = stateLock.writeLock();
  private final ClusterNodeStatus                        nodeStatus           = new ClusterNodeStatus();
  private final FiredEventsStatus                        firedEventsStatus    = new FiredEventsStatus();
  private final OutOfBandNotifier                        outOfBandNotifier    = new OutOfBandNotifier();

  private ClusterMetaDataManager                         clusterMetaDataManager;
  private Sink                                           eventsProcessorSink;

  private final RejoinManagerInternal                    rejoinManager;

  public DsoClusterImpl(RejoinManagerInternal rejoinManager) {
    this.rejoinManager = rejoinManager;
  }

  @Override
  public void init(final ClusterMetaDataManager metaDataManager, final ClientObjectManager objectManager,
                   final Stage dsoClusterEventsStage) {
    this.clusterMetaDataManager = metaDataManager;
    this.eventsProcessorSink = dsoClusterEventsStage.getSink();

    for (DsoNodeInternal node : topology.getInternalNodes()) {
      retrieveMetaDataForDsoNode(node);
    }
    outOfBandNotifier.start();
  }

  @Override
  public void shutdown() {
    this.outOfBandNotifier.shutdown();
  }

  @Override
  public void addClusterListener(final DsoClusterListener listener) {

    boolean added = listeners.addIfAbsent(listener);

    if (added) {
      DsoClusterEvent event = new DsoClusterEventImpl(currentNode);
      ClusterNodeStateType state = nodeStatus.getState();
      if (state.isNodeLeft()) {
        fireEvent(DsoClusterEventType.NODE_LEFT, event, listener);
      } else {
        if (state.isNodeJoined()) {
          fireEvent(DsoClusterEventType.NODE_JOIN, event, listener);
        }
        if (state.areOperationsEnabled()) {
          fireEvent(DsoClusterEventType.OPERATIONS_ENABLED, event, listener);
        }
      }
    }
  }

  @Override
  public void removeClusterListener(final DsoClusterListener listener) {
    listeners.remove(listener);
  }

  @Override
  public DsoNode getCurrentNode() {
    stateReadLock.lock();
    try {
      return currentNode;
    } finally {
      stateReadLock.unlock();
    }
  }

  @Override
  public DsoClusterTopology getClusterTopology() {
    return topology;
  }

  @Override
  public <K> Map<K, Set<DsoNode>> getNodesWithKeys(final Map<K, ?> map, final Collection<? extends K> keys) {
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
              DsoNodeInternal dsoNode = topology.getAndRegisterDsoNode((ClientID) nodeID);
              dsoNodes.add(dsoNode);
            }
            result.put(entry.getKey(), dsoNodes);
          }
        }
      }
    }
    return result;
  }

  @Override
  public DsoNodeMetaData retrieveMetaDataForDsoNode(final DsoNodeInternal node) {
    Assert.assertNotNull(clusterMetaDataManager);
    return clusterMetaDataManager.retrieveMetaDataForDsoNode(node);
  }

  @Override
  public boolean isNodeJoined() {
    return nodeStatus.getState().isNodeJoined();
  }

  @Override
  public boolean areOperationsEnabled() {
    return nodeStatus.getState().areOperationsEnabled();
  }

  @Override
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

  @Override
  public void fireThisNodeJoined(final ClientID nodeId, final ClientID[] clusterMembers) {
    final ClientID newNodeId = nodeId;
    final boolean rejoinHappened = rejoinManager.thisNodeJoined(newNodeId);
    stateWriteLock.lock();
    boolean fireThisNodeJoined = false;
    DsoNodeInternal oldNode = currentNode;
    try {
      if (rejoinHappened) {
        // node rejoined, update current node
        currentClientID = newNodeId;
        currentNode = topology.updateOnRejoin(currentClientID, clusterMembers);
      } else {
        if (currentNode == null) {
          // not a reconnect, joining first time
          fireThisNodeJoined = true;
        }
        currentClientID = newNodeId;
        currentNode = topology.registerThisDsoNode(nodeId);
        for (ClientID otherNodeId : clusterMembers) {
          if (!currentClientID.equals(otherNodeId)) {
            topology.registerDsoNode(otherNodeId);
          }
        }
      }
      nodeStatus.operationsEnabled();
      LOGGER.info("NODE_JOINED " + currentClientID + " rejoinHappened " + rejoinHappened);
    } finally {
      stateWriteLock.unlock();

      if (currentNode != null) {
        notifyWaiters();
      }

      final DsoClusterEventImpl currentThisNodeEvent = new DsoClusterEventImpl(currentNode);
      if (rejoinHappened) {
        // with rejoin, sequence of events:
        // - old node leaves
        // - new node joins
        // - new node ops enabled
        // - new node rejoin event
        DsoClusterEvent thisOldNodeLeftEvent = new DsoClusterEventImpl(oldNode);
        fireEventToAllListeners(DsoClusterEventType.NODE_LEFT, thisOldNodeLeftEvent);
        fireEventToAllListeners(DsoClusterEventType.NODE_JOIN, currentThisNodeEvent);
        fireEventToAllListeners(DsoClusterEventType.OPERATIONS_ENABLED, currentThisNodeEvent);
        fireEventToAllListeners(DsoClusterEventType.NODE_REJOINED, currentThisNodeEvent);
      } else {
        // without rejoin, sequence of events:
        // - if node is joining first time, fire node joined of current node
        // - fire ops enabled of current node
        if (fireThisNodeJoined) {
          fireEventToAllListeners(DsoClusterEventType.NODE_JOIN, currentThisNodeEvent);
        }
        fireEventToAllListeners(DsoClusterEventType.OPERATIONS_ENABLED, currentThisNodeEvent);
      }
    }
  }

  @Override
  public void cleanup() {
    // cleanup on rejoin start
    // remove all cluster members
    topology.cleanup();
  }

  @Override
  public void fireThisNodeLeft() {
    boolean fireOperationsDisabled = false;
    stateWriteLock.lock();
    try {
      // We may get a node left event without ever seeing a node joined event, just ignore
      // the node left event in that case
      if (!nodeStatus.getState().isNodeJoined()) {
        LOGGER.info("ignoring NODE_LEFT " + currentClientID + " because nodeStatus " + nodeStatus.getState());
        return;
      }
      if (nodeStatus.getState().areOperationsEnabled()) {
        fireOperationsDisabled = true;
      }
      nodeStatus.nodeLeft();
      LOGGER.info("NODE_LEFT " + currentClientID + " nodeStatus " + nodeStatus);
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

  @Override
  public void fireNodeJoined(final ClientID nodeId) {
    if (topology.containsDsoNode(nodeId)) { return; }

    final DsoClusterEvent event = new DsoClusterEventImpl(topology.getAndRegisterDsoNode(nodeId));
    DsoNodeInternal node = topology.getInternalNode(nodeId);
    if (node != null && clusterMetaDataManager != null) {
      retrieveMetaDataForDsoNode(node);
    }
    fireEventToAllListeners(DsoClusterEventType.NODE_JOIN, event);
  }

  private void fireEventToAllListeners(final DsoClusterEventType eventType, final DsoClusterEvent event) {
    LOGGER.debug("event fired |" + eventType + "|" + event.getNode());
    for (DsoClusterListener l : listeners) {
      fireEvent(eventType, event, l);
    }
  }

  @Override
  public void fireNodeLeft(final ClientID nodeId) {
    DsoNodeInternal node = topology.getAndRemoveDsoNode(nodeId);
    if (node == null) { return; }
    final DsoClusterEvent event = new DsoClusterEventImpl(node);
    fireEventToAllListeners(DsoClusterEventType.NODE_LEFT, event);
  }

  @Override
  public void fireNodeError() {
    final DsoClusterEvent event = new DsoClusterEventImpl(currentNode);
    fireEventToAllListeners(DsoClusterEventType.NODE_ERROR, event);
  }

  @Override
  public void fireOperationsEnabled() {
    if (currentNode != null) {
      stateWriteLock.lock();
      try {
        if (nodeStatus.getState().areOperationsEnabled()) {
          LOGGER
              .info("ignoring OPERATIONS_ENABLED " + currentClientID + " because nodeStatus " + nodeStatus.getState());
          return;
        }
        nodeStatus.operationsEnabled();
      } finally {
        stateWriteLock.unlock();
      }

      final DsoClusterEvent event = new DsoClusterEventImpl(currentNode);
      fireEventToAllListeners(DsoClusterEventType.OPERATIONS_ENABLED, event);
      firedEventsStatus.operationsEnabledFired();
    }
  }

  @Override
  public void fireOperationsDisabled() {
    stateWriteLock.lock();
    try {
      if (!nodeStatus.getState().areOperationsEnabled()) {
        LOGGER.info("ignoring OPERATIONS_DISABLED " + currentClientID + " because nodeStatus " + nodeStatus.getState());
        return;
      }
      nodeStatus.operationsDisabled();
    } finally {
      stateWriteLock.unlock();
    }

    fireOperationsDisabledNoCheck();
  }

  private void fireOperationsDisabledNoCheck() {
    final DsoClusterEvent event = new DsoClusterEventImpl(currentNode);
    fireEventToAllListeners(DsoClusterEventType.OPERATIONS_DISABLED, event);
    firedEventsStatus.operationsDisabledFired();
  }

  private void fireEvent(final DsoClusterEventType eventType, final DsoClusterEvent event,
                         final DsoClusterListener listener) {
    /**
     * use out-of-band notification depending on listener otherwise use the single threaded eventProcessorSink to
     * process the cluster event.
     */
    boolean useOOB = useOOBNotification(eventType, event, listener);
    if (useOOB) {
      outOfBandNotifier.submit(new Runnable() {
        @Override
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

  @Override
  public void notifyDsoClusterListener(DsoClusterEventType eventType, DsoClusterEvent event, DsoClusterListener listener) {
    try {
      switch (eventType) {
        case NODE_JOIN:
          listener.nodeJoined(event);
          return;
        case NODE_LEFT:
          listener.nodeLeft(event);
          return;
        case OPERATIONS_ENABLED:
          listener.operationsEnabled(event);
          return;
        case OPERATIONS_DISABLED:
          listener.operationsDisabled(event);
          return;
        case NODE_REJOINED:
          listener.nodeRejoined(event);
          return;
        case NODE_ERROR:
          listener.nodeError(event);
          return;
      }
      throw new AssertionError("Unhandled event type: " + eventType);
    } catch (TCNotRunningException tcnre) {
      LOGGER.error("Ignoring TCNotRunningException when notifying " + event + " : " + eventType);
    } catch (Throwable t) {
      LOGGER.error("Problem firing the cluster event : " + eventType + " - " + event, t);
    }

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

        @Override
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
        @Override
        public void run() {
          // dummy task to notify other thread
        }
      });
    }
  }

}
