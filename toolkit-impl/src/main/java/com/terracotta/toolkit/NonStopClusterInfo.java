/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;

import com.tc.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NonStopClusterInfo implements ClusterInfo {
  private final AsyncToolkitInitializer asyncToolkitInitializer;
  private volatile ClusterInfo          delegate;
  private final Thread                  initializer;
  private final List<ClusterListener>   listeners = new ArrayList<ClusterListener>();

  public NonStopClusterInfo(AsyncToolkitInitializer asyncToolkitInitializer) {
    this.asyncToolkitInitializer = asyncToolkitInitializer;
    initializer = createThreadToInitDelegate();
    initializer.setDaemon(true);
    initializer.start();
  }

  private Thread createThreadToInitDelegate() {
    return new Thread("Non Stop Cluster Info register") {
      @Override
      public void run() {
        ClusterInfo localClusterInfo = getToolkit().getClusterInfo();
        synchronized (NonStopClusterInfo.this) {
          delegate = localClusterInfo;
          for (ClusterListener clusterListener : listeners) {
            delegate.addClusterListener(clusterListener);
          }
          listeners.clear();
        }
      }

      private Toolkit getToolkit() {
        return asyncToolkitInitializer.getToolkit();
      }
    };
  }

  @Override
  public void addClusterListener(ClusterListener listener) {
    if (delegate != null) {
      delegate.addClusterListener(listener);
      return;
    }

    synchronized (NonStopClusterInfo.this) {
      if (delegate != null) {
        delegate.addClusterListener(listener);
        return;
      }

      listeners.add(listener);
    }
  }

  @Override
  public void removeClusterListener(ClusterListener listener) {
    if (delegate != null) {
      delegate.removeClusterListener(listener);
      return;
    }

    synchronized (NonStopClusterInfo.this) {
      if (delegate != null) {
        delegate.removeClusterListener(listener);
        return;
      }

      listeners.remove(listener);
    }
  }

  private void waitForClusterInfoToGetInitialized() {
    boolean interrupted = false;
    while (delegate != null) {
      try {
        initializer.join();
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }

    Util.selfInterruptIfNeeded(interrupted);
  }

  @Override
  public ClusterNode getCurrentNode() {
    if (delegate != null) { return delegate.getCurrentNode(); }

    waitForClusterInfoToGetInitialized();
    return delegate.getCurrentNode();
  }

  @Override
  public boolean areOperationsEnabled() {
    if (delegate == null) { return false; }
    return delegate.areOperationsEnabled();
  }

  @Override
  public Set<ClusterNode> getNodes() {
    if (delegate != null) { return delegate.getNodes(); }

    waitForClusterInfoToGetInitialized();
    return delegate.getNodes();
  }

}
