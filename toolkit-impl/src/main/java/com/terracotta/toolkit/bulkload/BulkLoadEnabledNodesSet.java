/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.bulkload;

import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.platform.PlatformService;
import com.tcclient.cluster.DsoClusterInternal.DsoClusterEventType;
import com.tcclient.cluster.DsoNode;
import com.tcclient.cluster.OutOfBandDsoClusterListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BulkLoadEnabledNodesSet {

  private static final String             BULK_LOAD_NODES_SET_PREFIX = "__tc_bulk-load-nodes-set_for_cache_";

  private static final TCLogger           LOGGER                     = TCLogging
                                                                         .getLogger(BulkLoadEnabledNodesSet.class);

  private final DsoCluster                dsoCluster;
  private final ToolkitSet<String>        bulkLoadEnabledNodesSet;
  private final ToolkitLock               clusteredLock;
  private final String                    name;
  private final CleanupOnRejoinListener cleanupOnNodeLeftListener;

  private volatile boolean                currentNodeBulkLoadEnabled = false;
  private final boolean                   loggingEnabled;

  protected BulkLoadEnabledNodesSet(PlatformService platformService, String name, ToolkitInternal toolkit,
                                    BulkLoadConstants bulkLoadConstants) {
    this.name = name;
    this.dsoCluster = platformService.getDsoCluster();
    bulkLoadEnabledNodesSet = toolkit.getSet(BULK_LOAD_NODES_SET_PREFIX + name, String.class);
    this.loggingEnabled = bulkLoadConstants.isLoggingEnabled();
    clusteredLock = bulkLoadEnabledNodesSet.getReadWriteLock().writeLock();
    cleanupOnNodeLeftListener = new CleanupOnRejoinListener(this);
    dsoCluster.addClusterListener(cleanupOnNodeLeftListener);
    cleanupOfflineNodes();
  }

  public boolean isBulkLoadEnabledInNode() {
    return currentNodeBulkLoadEnabled;
  }

  private void debug(String msg) {
    List<String> nodes = new ArrayList(bulkLoadEnabledNodesSet);
    LOGGER.debug("['" + name + "'] " + msg + " [bulk-load enabled nodes: " + nodes + "]");
  }

  /**
   * Remove offline nodes from the nodes set
   */
  private void cleanupOfflineNodes() {
    clusteredLock.lock();
    try {

      Collection<DsoNode> liveNodes = dsoCluster.getClusterTopology().getNodes();
      ArrayList<String> defunctNodes = new ArrayList<String>(bulkLoadEnabledNodesSet);

      if (loggingEnabled) {
        debug("Cleaning up offline nodes. Live nodes: " + liveNodes);
      }
      // remove live nodes
      for (DsoNode node : liveNodes) {
        defunctNodes.remove(node.getId());
      }

      // clean up defunct nodes
      for (String nodeId : defunctNodes) {
        bulkLoadEnabledNodesSet.remove(nodeId);
      }
      if (defunctNodes.size() > 0) {
        clusteredLock.getCondition().signalAll();
      }

      if (loggingEnabled) {
        debug("Offline nodes cleanup complete");
      }
    } finally {
      clusteredLock.unlock();
    }
  }

  /**
   * Add the current node in the bulk-load enabled nodes set
   */
  public void addCurrentNode() {
    if (!currentNodeBulkLoadEnabled) {
      clusteredLock.lock();
      try {
        if (!currentNodeBulkLoadEnabled) {
          addCurrentNodeToBulkLoadSet();
          currentNodeBulkLoadEnabled = true;
        }
      } finally {
        clusteredLock.unlock();
      }
    }
  }

  public void addCurrentNodeInternal() {
    if (currentNodeBulkLoadEnabled) {
      clusteredLock.lock();
      try {
        if (currentNodeBulkLoadEnabled) {
          addCurrentNodeToBulkLoadSet();
        }
      } finally {
        clusteredLock.unlock();
      }
    }
  }

  private void addCurrentNodeToBulkLoadSet() {
    String currentNodeId = dsoCluster.getCurrentNode().getId();
    bulkLoadEnabledNodesSet.add(currentNodeId);
    if (loggingEnabled) {
      debug("Added current node ('" + currentNodeId + "')");
    }
  }

  /**
   * Remove the current node from the bulk-load enabled nodes set
   */
  public void removeCurrentNode() {
    if (currentNodeBulkLoadEnabled) {
      clusteredLock.lock();
      try {
        if (currentNodeBulkLoadEnabled) {
          removeNodeIdAndNotifyAll(dsoCluster.getCurrentNode().getId());
          currentNodeBulkLoadEnabled = false;
        }
      } finally {
        clusteredLock.unlock();
      }
    }
  }

  /**
   * This method should be called under write lock
   */
  private void removeNodeIdAndNotifyAll(String nodeId) {
    try {
      bulkLoadEnabledNodesSet.remove(nodeId);
      if (loggingEnabled) {
        debug("Removed node ('" + nodeId + "'), going to signal all.");
      }
    } finally {
      // notify all waiters
      clusteredLock.getCondition().signalAll();
    }
  }

  /**
   * Wait until the bulk-load enabled nodes set is empty
   */
  public void waitUntilSetEmpty() throws InterruptedException {
    clusteredLock.lock();
    try {
      while (!bulkLoadEnabledNodesSet.isEmpty()) {

        if (loggingEnabled) {
          debug("Waiting until bulk-load enabled nodes list is empty" + bulkLoadEnabledNodesSet.size());
        }
        // wait until somebody removes from the nodes set
        clusteredLock.getCondition().await(10, TimeUnit.SECONDS);
        if (bulkLoadEnabledNodesSet.size() > 0) {
          cleanupOfflineNodes();
        }
      }
    } finally {
      clusteredLock.unlock();
    }
  }

  public boolean isBulkLoadEnabledInCluster() {
    clusteredLock.lock();
    try {
      cleanupOfflineNodes();
      boolean rv = (bulkLoadEnabledNodesSet.size() != 0);
      return rv;
    } finally {
      clusteredLock.unlock();
    }
  }

  private static class CleanupOnRejoinListener implements OutOfBandDsoClusterListener {

    private final BulkLoadEnabledNodesSet nodesSet;

    public CleanupOnRejoinListener(BulkLoadEnabledNodesSet nodesSet) {
      this.nodesSet = nodesSet;
    }

    private void handleNodeRejoined(DsoClusterEvent event) {
      nodesSet.addCurrentNodeInternal();
      nodesSet.cleanupOfflineNodes();
    }

    @Override
    public void nodeJoined(DsoClusterEvent event) {
      // no operation

    }

    @Override
    public void nodeLeft(final DsoClusterEvent event) {
      // no operation
    }

    @Override
    public void operationsEnabled(DsoClusterEvent event) {
      // no operation

    }

    @Override
    public void operationsDisabled(DsoClusterEvent event) {
      // no operation

    }

    @Override
    public void nodeRejoined(DsoClusterEvent event) {
      try {
        handleNodeRejoined(event);
      } catch (RejoinException e) {
        LOGGER.warn("error during handleNodeRejoined " + e);
      } catch (TCNotRunningException e) {
        LOGGER.info("Ignoring TCNotRunningException in handleNodeRejoined " + e);
      }

    }

    @Override
    public boolean useOutOfBandNotification(DsoClusterEventType type, DsoClusterEvent event) {
      // no operation
      return false;
    }

    @Override
    public void nodeError(DsoClusterEvent event) {
      //
    }
  }

  public void disposeLocally() {
    LOGGER.info("Cleaning up BulkLoadEnabledNodesSet");
    dsoCluster.removeClusterListener(cleanupOnNodeLeftListener);
  }

}
