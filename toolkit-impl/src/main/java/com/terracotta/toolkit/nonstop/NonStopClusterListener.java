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
  // Initally Operations are Disabled.
  private final AtomicBoolean             operationsEnabled = new AtomicBoolean(false);
  private final AbortableOperationManager abortableOperationManager;

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
      default:
        // no op
        break;
    }
  }

  public boolean areOperationsEnabled() {
    return operationsEnabled.get();
  }

  public void waitUntilOperationsEnabled() {
    if (!operationsEnabled.get()) {
      synchronized (this) {
        boolean interrupted = false;
        while (!operationsEnabled.get()) {
          try {
            this.wait();
          } catch (InterruptedException e) {
            if (abortableOperationManager.isAborted()) { throw new ToolkitAbortableOperationException(); }
            interrupted = true;
          }
        }

        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
