/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.ReconnectionRejectedException;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tcclient.cluster.DsoClusterInternalEventsGun;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class RejoinManagerImpl implements RejoinManagerInternal {

  private static final TCLogger                logger           = TCLogging.getLogger(RejoinManagerImpl.class);
  private final List<RejoinLifecycleListener>  listeners        = new CopyOnWriteArrayList<RejoinLifecycleListener>();
  private final boolean                        rejoinEnabled;
  private final ReconnectionRejectedHandler    reconnectionRejectedHandler;
  private volatile DsoClusterInternalEventsGun dsoClusterInternal;
  private final RejoinWorker                   rejoinWorker;
  private final AtomicBoolean                  rejoinInProgress = new AtomicBoolean(false);

  public RejoinManagerImpl(boolean isRejoinEnabled) {
    this.rejoinEnabled = isRejoinEnabled;
    this.reconnectionRejectedHandler = new ReconnectionRejectedHandlerImpl(rejoinEnabled);
    this.rejoinWorker = new RejoinWorker(this);
    Thread th = new Thread(rejoinWorker, "Rejoin worker thread");
    th.setDaemon(true);
    th.start();
  }

  @Override
  public void init(DsoClusterInternalEventsGun dsoClusterEventsGun) {
    this.dsoClusterInternal = dsoClusterEventsGun;
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
    if (!rejoinEnabled) { throw new AssertionError("Rejoin is not enabled"); }
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
  public void initiateRejoin(MessageChannel channel) {
    assertRejoinEnabled();
    if (rejoinInProgress.compareAndSet(false, true)) {
      // rejoin starting for first time, other channels can also initiate rejoin simultaneously
      notifyRejoinStart();
    }
    rejoinWorker.requestRejoin(channel);
  }

  @Override
  public void thisNodeJoinedCallback(ClientID oldNodeId, ClientID newNodeId) {
    logger.info("This node joined the cluster - rejoinEnabled: " + rejoinEnabled + ", rejoin in progress:"
                + rejoinInProgress.get() + ", oldNodeId: " + oldNodeId + ", newNodeId: " + newNodeId);
    if (rejoinEnabled) {
      // called when all channels have connected and handshake is complete
      if (rejoinInProgress.compareAndSet(true, false)) {
        // take care of any cleanup/reinitialization
        notifyRejoinComplete();
        // fire rejoin event
        dsoClusterInternal.fireNodeRejoined(oldNodeId, newNodeId);
      }
    }
  }

  private void reestablishComms(MessageChannel channel) {
    logger.info("Reestablishing comms for channel: " + channel);
    while (true) {
      try {
        channel.reopen();
        break;
      } catch (Throwable t) {
        logger.error("Got error while reestablishing channel, going to retry... channel: " + channel, t);
      }
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

  @Override
  public void shutdown() {
    rejoinWorker.shutdown();
  }

  private static class RejoinWorker implements Runnable {

    private final Object                monitor                 = new Object();
    private volatile RejoinManagerImpl  manager;
    private volatile boolean            shutdown                = false;
    private final Queue<MessageChannel> rejoinRequestedChannels = new LinkedList<MessageChannel>();

    public RejoinWorker(RejoinManagerImpl rejoinManager) {
      this.manager = rejoinManager;
    }

    private void requestRejoin(MessageChannel channel) {
      synchronized (monitor) {
        rejoinRequestedChannels.add(channel);
        monitor.notifyAll();
      }
    }

    @Override
    public void run() {
      while (true) {
        MessageChannel channel = waitUntilRejoinRequestedOrShutdown();
        if (shutdown) return;
        manager.reestablishComms(channel);
      }
    }

    private MessageChannel waitUntilRejoinRequestedOrShutdown() {
      synchronized (monitor) {
        while (rejoinRequestedChannels.isEmpty()) {
          if (shutdown) { return null; }
          try {
            monitor.wait();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
        return rejoinRequestedChannels.remove();
      }
    }

    private void shutdown() {
      synchronized (monitor) {
        this.shutdown = true;
        monitor.notifyAll();
      }
    }
  }
}
