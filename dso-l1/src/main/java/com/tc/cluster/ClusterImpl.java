/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.cluster;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Util;
import com.tcclient.cluster.ClusterInternal;
import com.tcclient.cluster.ClusterInternalEventsContext;
import com.tcclient.cluster.ClusterNodeStatus;
import com.tcclient.cluster.ClusterNodeStatus.ClusterNodeStateType;
import com.tcclient.cluster.Node;
import com.tcclient.cluster.NodeInternal;
import com.tcclient.cluster.OutOfBandClusterListener;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClusterImpl implements ClusterInternal {

  private static final TCLogger                          LOGGER               = TCLogging
                                                                                  .getLogger(ClusterImpl.class);

  private volatile ClientID                              currentClientID;
  private volatile NodeInternal                          currentNode;

  private final ClusterTopologyImpl                      topology             = new ClusterTopologyImpl();
  private final CopyOnWriteArrayList<ClusterListener>    listeners            = new CopyOnWriteArrayList<ClusterListener>();
  private final Object                                   nodeJoinsClusterSync = new Object();

  private final ReentrantReadWriteLock                   stateLock            = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.ReadLock          stateReadLock        = stateLock.readLock();
  private final ReentrantReadWriteLock.WriteLock         stateWriteLock       = stateLock.writeLock();
  private final ClusterNodeStatus                        nodeStatus           = new ClusterNodeStatus();
  private final FiredEventsStatus                        firedEventsStatus    = new FiredEventsStatus();
  private final OutOfBandNotifier                        outOfBandNotifier    = new OutOfBandNotifier();

  private Sink<ClusterInternalEventsContext> eventsProcessorSink;

  @Override
  public void init(Stage<ClusterInternalEventsContext> clusterEventsStage) {
    this.eventsProcessorSink = clusterEventsStage.getSink();
    outOfBandNotifier.start();
  }

  @Override
  public void shutdown() {
    this.outOfBandNotifier.shutdown();
  }

  @Override
  public void addClusterListener(ClusterListener listener) {

    boolean added = listeners.addIfAbsent(listener);

    if (added) {
      ClusterEvent event = new ClusterEventImpl(currentNode);
      ClusterNodeStateType state = nodeStatus.getState();
      if (state.isNodeLeft()) {
        fireEvent(ClusterEventType.NODE_LEFT, event, listener);
      } else {
        if (state.isNodeJoined()) {
          fireEvent(ClusterEventType.NODE_JOIN, event, listener);
        }
        if (state.areOperationsEnabled()) {
          fireEvent(ClusterEventType.OPERATIONS_ENABLED, event, listener);
        }
      }
    }
  }

  @Override
  public void removeClusterListener(ClusterListener listener) {
    listeners.remove(listener);
  }

  @Override
  public Node getCurrentNode() {
    stateReadLock.lock();
    try {
      return currentNode;
    } finally {
      stateReadLock.unlock();
    }
  }

  @Override
  public ClusterTopology getClusterTopology() {
    return topology;
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
  public Node waitUntilNodeJoinsCluster() {
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
  public void fireThisNodeJoined(ClientID nodeId, ClientID[] clusterMembers) {
    final ClientID newNodeId = nodeId;
    stateWriteLock.lock();
    boolean fireThisNodeJoined = false;
    try {
      if (currentNode == null) {
        // not a reconnect, joining first time
        fireThisNodeJoined = true;
      }
      currentClientID = newNodeId;
      currentNode = topology.registerThisNode(nodeId);
      for (ClientID otherNodeId : clusterMembers) {
        if (!currentClientID.equals(otherNodeId)) {
          topology.registerNode(otherNodeId);
        }
      }
      nodeStatus.operationsEnabled();
      LOGGER.info("NODE_JOINED " + currentClientID);
    } finally {
      stateWriteLock.unlock();

      if (currentNode != null) {
        notifyWaiters();
      }

      final ClusterEventImpl currentThisNodeEvent = new ClusterEventImpl(currentNode);
      // without rejoin, sequence of events:
      // - if node is joining first time, fire node joined of current node
      // - fire ops enabled of current node
      if (fireThisNodeJoined) {
        fireEventToAllListeners(ClusterEventType.NODE_JOIN, currentThisNodeEvent);
      }
      fireEventToAllListeners(ClusterEventType.OPERATIONS_ENABLED, currentThisNodeEvent);
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
  public void fireNodeJoined(ClientID nodeId) {
    if (topology.containsNode(nodeId)) { return; }

    final ClusterEvent event = new ClusterEventImpl(topology.getAndRegisterNode(nodeId));
    fireEventToAllListeners(ClusterEventType.NODE_JOIN, event);
  }

  private void fireEventToAllListeners(ClusterEventType eventType, ClusterEvent event) {
    LOGGER.debug("event fired |" + eventType + "|" + event.getNode());
    for (ClusterListener l : listeners) {
      fireEvent(eventType, event, l);
    }
  }

  @Override
  public void fireNodeLeft(ClientID nodeId) {
    NodeInternal node = topology.getAndRemoveNode(nodeId);
    if (node == null) { return; }
    final ClusterEvent event = new ClusterEventImpl(node);
    fireEventToAllListeners(ClusterEventType.NODE_LEFT, event);
  }

  @Override
  public void fireNodeError() {
    final ClusterEvent event = new ClusterEventImpl(currentNode);
    fireEventToAllListeners(ClusterEventType.NODE_ERROR, event);
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

      final ClusterEvent event = new ClusterEventImpl(currentNode);
      fireEventToAllListeners(ClusterEventType.OPERATIONS_ENABLED, event);
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
    final ClusterEvent event = new ClusterEventImpl(currentNode);
    fireEventToAllListeners(ClusterEventType.OPERATIONS_DISABLED, event);
    firedEventsStatus.operationsDisabledFired();
  }

  private void fireEvent(final ClusterEventType eventType, final ClusterEvent event,
                         final ClusterListener listener) {
    /**
     * use out-of-band notification depending on listener otherwise use the single threaded eventProcessorSink to
     * process the cluster event.
     */
    boolean useOOB = useOOBNotification(eventType, event, listener);
    if (useOOB) {
      outOfBandNotifier.submit(new Runnable() {
        @Override
        public void run() {
          notifyClusterListener(eventType, event, listener);
        }
      });
    } else {
      this.eventsProcessorSink.addSingleThreaded(new ClusterInternalEventsContext(eventType, event, listener));
    }
  }

  private boolean useOOBNotification(ClusterEventType eventType, ClusterEvent event, ClusterListener listener) {
    if (listener instanceof OutOfBandClusterListener) {
      return ((OutOfBandClusterListener) listener).useOutOfBandNotification(eventType, event);
    } else {
      return false;
    }
  }

  @Override
  public void notifyClusterListener(ClusterEventType eventType, ClusterEvent event, ClusterListener listener) {
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
        default: 
          throw new AssertionError("Unhandled event type: " + eventType);            
      }
    } catch (TCNotRunningException tcnre) {
      LOGGER.error("Ignoring TCNotRunningException when notifying " + event + " : " + eventType);
    } catch (Throwable t) {
      LOGGER.error("Problem firing the cluster event : " + eventType + " - " + event, t);
    }

  }

  private static final class FiredEventsStatus {

    private ClusterEventType lastFiredEvent = null;

    public synchronized void operationsDisabledFired() {
      lastFiredEvent = ClusterEventType.OPERATIONS_DISABLED;
      this.notifyAll();
    }

    public synchronized void operationsEnabledFired() {
      lastFiredEvent = ClusterEventType.OPERATIONS_ENABLED;
      this.notifyAll();
    }

    public synchronized void waitUntilOperationsDisabledFired() {
      boolean interrupted = false;
      try {
        while (lastFiredEvent != ClusterEventType.OPERATIONS_DISABLED) {
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

    private void submit(Runnable taskToExecute) {
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
