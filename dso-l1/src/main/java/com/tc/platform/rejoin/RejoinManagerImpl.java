/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

import com.tc.net.ReconnectionRejectedException;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.platform.rejoin.RejoinLifecycleListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RejoinManagerImpl implements RejoinManager {

  private final List<RejoinLifecycleListener> listeners = new CopyOnWriteArrayList<RejoinLifecycleListener>();
  private final boolean                       rejoinEnabled;
  private final ReconnectionRejectedHandler   reconnectionRejectedHandler;

  public RejoinManagerImpl(boolean isRejoinEnabled) {
    this.rejoinEnabled = isRejoinEnabled;
    this.reconnectionRejectedHandler = new ReconnectionRejectedHandlerImpl(rejoinEnabled);
  }

  @Override
  public boolean isRejoinEnabled() {
    return rejoinEnabled;
  }

  @Override
  public void addListener(RejoinLifecycleListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(RejoinLifecycleListener listener) {
    listeners.remove(listener);
  }

  private void assertRejoinEnabled() {
    if (!rejoinEnabled) { throw new AssertionError("Trying to do rejoin when its disabled"); }
  }

  public void notifyRejoinStart() {
    assertRejoinEnabled();
    for (RejoinLifecycleListener listener : listeners) {
      listener.onRejoinStart();
    }
  }

  public void notifyRejoinComplete() {
    assertRejoinEnabled();
    for (RejoinLifecycleListener listener : listeners) {
      listener.onRejoinComplete();
    }
  }

  @Override
  public ReconnectionRejectedHandler getReconnectionRejectedHandler() {
    return reconnectionRejectedHandler;
  }

  private static class ReconnectionRejectedHandlerImpl implements ReconnectionRejectedHandler {
    private final boolean rejoin;

    public ReconnectionRejectedHandlerImpl(boolean rejoin) {
      this.rejoin = rejoin;
    }

    @Override
    public void reconnectionRejected(ReconnectionRejectedCleanupAction cleanup) throws ReconnectionRejectedException {
      if (rejoin) {
        cleanup.reconnectionRejectedCleanupAction();
        throw new ReconnectionRejectedException("Reconnection rejected due to stack not found. Rejoin Behaviour.");
      } else {
        throw new ReconnectionRejectedException("Reconnection rejected due to stack not found. Default Behaviour.");
      }
    }
  }
}
