/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterListener;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.util.concurrent.atomic.AtomicBoolean;

public class NonStopClusterListener implements ClusterListener {
  // Initially Operations are Disabled.
  private final AtomicBoolean             operationsEnabled = new AtomicBoolean(false);
  private final AbortableOperationManager abortableOperationManager;
  private volatile String                 nodeErrorMessage  = null;

  public NonStopClusterListener(AbortableOperationManager abortableOperationManager) {
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public void onClusterEvent(ClusterEvent event) {
    switch (event.getType()) {
      case OPERATIONS_DISABLED:
        operationsEnabled.set(false);
        break;
      case OPERATIONS_ENABLED:
        synchronized (this) {
          operationsEnabled.set(true);
          this.notifyAll();
          break;
        }
      case NODE_ERROR:
        nodeErrorMessage = event.getDetailedMessage();
        operationsEnabled.set(false);
        break;
      default:
        // no op
        break;
    }
  }

  /**
   * returns true If cluster Operations are enabled.
   * 
   */
  public boolean areOperationsEnabled() {
    return operationsEnabled.get();
  }

  /**
   * waits until cluster Operations are enabled.
   * 
   */
  public void waitUntilOperationsEnabled() {
    if (nodeErrorMessage != null) { throw new ToolkitAbortableOperationException(); }
    
    if (!operationsEnabled.get()) {
      synchronized (this) {
        boolean interrupted = false;
        try {
          while (!operationsEnabled.get()) {
            if (nodeErrorMessage != null) { throw new ToolkitAbortableOperationException(); }
            try {
              this.wait();
            } catch (InterruptedException e) {
              if (abortableOperationManager.isAborted()) { throw new ToolkitAbortableOperationException(); }
              interrupted = true;
            }
          }
        } finally {
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
  }

  /**
   * @return the error message incase the node is in unrecoverable error state or null if there is no node error.
   */
  public String getNodeErrorMessage() {
    return nodeErrorMessage;
  }

  /**
   * return true of the current node is in unrecoverable error state.
   */
  public boolean isNodeError() {
    return (nodeErrorMessage != null) ? true : false;
  }
}
