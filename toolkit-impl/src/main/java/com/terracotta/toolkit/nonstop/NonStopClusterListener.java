/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class NonStopClusterListener implements ClusterListener {
  private final AtomicBoolean             operationsEnabled = new AtomicBoolean(true);
  private final AbortableOperationManager abortableOperationManager;

  public NonStopClusterListener(FutureTask<ToolkitInternal> toolkitDelegateFutureTask,
                                AbortableOperationManager abortableOperationManager) {
    registerToToolkit(toolkitDelegateFutureTask);
    this.abortableOperationManager = abortableOperationManager;
  }

  private void registerToToolkit(final FutureTask<ToolkitInternal> toolkitDelegateFutureTask) {
    Thread t = new Thread("Non Stop Cluster Listener register") {
      @Override
      public void run() {
        getToolkit(toolkitDelegateFutureTask).getClusterInfo().addClusterListener(NonStopClusterListener.this);
      }

      private Toolkit getToolkit(FutureTask<ToolkitInternal> toolkitDelegate) {
        try {
          return toolkitDelegate.get();
        } catch (Exception e) {
          // TODO
          e.printStackTrace();
        }
        return null;
      }
    };
    t.setDaemon(true);
    t.start();
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
