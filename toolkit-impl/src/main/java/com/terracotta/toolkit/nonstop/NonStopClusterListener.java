/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.internal.ToolkitInternal;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class NonStopClusterListener implements ClusterListener {
  private final AtomicBoolean operationsEnabled = new AtomicBoolean(true);

  public NonStopClusterListener(FutureTask<ToolkitInternal> toolkitDelegateFutureTask) {
    registerToToolkit(toolkitDelegateFutureTask);
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
        operationsEnabled.set(true);
        break;
      default:
        // no op
        break;
    }
  }

  public boolean areOperationsEnabled() {
    return operationsEnabled.get();
  }
}
