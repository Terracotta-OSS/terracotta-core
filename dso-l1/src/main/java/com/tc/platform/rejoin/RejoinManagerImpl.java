/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ReconnectionRejectedException;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RejoinManagerImpl implements RejoinManagerInternal {

  private static final TCLogger               logger    = TCLogging.getLogger(RejoinManagerImpl.class);
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

  private void notifyRejoinStart() {
    assertRejoinEnabled();
    for (RejoinLifecycleListener listener : listeners) {
      listener.onRejoinStart();
    }
  }

  private void notifyRejoinComplete() {
    assertRejoinEnabled();
    for (RejoinLifecycleListener listener : listeners) {
      listener.onRejoinComplete();
    }
  }

  @Override
  public void doRejoin(MessageChannel channel) {
    logger.info("Going to initiate rejoin for channel: " + channel);
    notifyRejoinStart();
    try {
      while (true) {
        try {
          reestablishComms(channel);
          break;
        } catch (Throwable t) {
          logger.error("Got error while reestablishing comms, going to retry...", t);
        }
      }
    } finally {
      notifyRejoinComplete();
    }

  }

  private void reestablishComms(MessageChannel channel) {
    //
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
