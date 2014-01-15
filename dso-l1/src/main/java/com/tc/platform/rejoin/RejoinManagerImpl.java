/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RejoinManagerImpl implements RejoinManagerInternal {
  private static final TCLogger               logger              = TCLogging.getLogger(RejoinManagerImpl.class);
  private static final long                   REJOIN_SLEEP_MILLIS = TCPropertiesImpl
                                                                      .getProperties()
                                                                      .getLong(TCPropertiesConsts.L2_L1REJOIN_SLEEP_MILLIS,
                                                                               100);
  private final List<RejoinLifecycleListener> listeners           = new CopyOnWriteArrayList<RejoinLifecycleListener>();
  private final boolean                       rejoinEnabled;
  private final RejoinWorker                  rejoinWorker;
  private final AtomicBoolean                 rejoinInProgress    = new AtomicBoolean(false);
  private final AtomicBoolean                 reopenInProgress    = new AtomicBoolean(false);
  private volatile int                        rejoinCount         = 0;

  public RejoinManagerImpl(boolean isRejoinEnabled) {
    this.rejoinEnabled = isRejoinEnabled;
    this.rejoinWorker = new RejoinWorker(this);
  }

  @Override
  public void start() {
    Thread th = new Thread(rejoinWorker, "Rejoin worker thread");
    th.setDaemon(true);
    th.start();
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

  private void notifyRejoinStart(ClientMessageChannel channel) {
    assertRejoinEnabled();
    rejoinInProgress.set(true);
    logger.info("Notifying rejoin start... " + channel + " rejoin count " + rejoinCount);
    rejoinCount++;
    // this calls cleanup for all ClearableCallbacks
    for (RejoinLifecycleListener listener : listeners) {
      listener.onRejoinStart();
    }
    logger.info("Notified rejoin start...");
  }

  private void notifyRejoinComplete() {
    assertRejoinEnabled();
    logger.info("Notifying rejoin complete...");
    for (RejoinLifecycleListener listener : listeners) {
      listener.onRejoinComplete();
    }
    logger.info("Notified rejoin complete...");
  }

  @Override
  public void requestRejoin(ClientMessageChannel channel) {
    assertRejoinEnabled();
    rejoinWorker.requestRejoin(channel);
  }

  @Override
  public boolean thisNodeJoined(ClientID newNodeId) {
    logger.info("This node joined the cluster - rejoinEnabled: " + rejoinEnabled + " newNodeId: " + newNodeId);
    if (rejoinEnabled) {
      // called when all channels have connected and handshake is complete
      synchronized (rejoinInProgress) {
        if (rejoinInProgress.get()) {
          // take care of any cleanup/re-initialization
          notifyRejoinComplete();
          rejoinInProgress.set(false);
          return true;
        }
      }
    }
    return false;
  }

  void setReopenInProgress(boolean value) {
    reopenInProgress.set(value);
  }

  boolean isReopenInProgress() {
    return reopenInProgress.get();
  }

  @Override
  public boolean isRejoinInProgress() {
    return rejoinInProgress.get();
  }

  // only called by rejoin worker
  void doRejoin(ClientMessageChannel channel) {
    notifyRejoinStart(channel);
    doReopen(channel);
  }

  void doReopen(ClientMessageChannel channel) {
    while (!rejoinWorker.shutdown) {
      try {
        logger.info("rejoin request for channel: " + channel);
        channel.reopen();
        reopenInProgress.set(false);
        break;
      } catch (Throwable t) {
        logger.warn("Error during channel open " + t);
        try {
          TimeUnit.MILLISECONDS.sleep(REJOIN_SLEEP_MILLIS);
        } catch (InterruptedException e) {
          logger.warn("got inturrupted while sleeping before reopen of channel " + channel);
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  @Override
  public void shutdown() {
    rejoinWorker.shutdown();
  }

  static class RejoinWorker implements Runnable {

    private final Object                monitor                 = new Object();
    private volatile RejoinManagerImpl  manager;
    private volatile boolean            shutdown                = false;
    private Queue<ClientMessageChannel> rejoinRequestedChannels = new LinkedList<ClientMessageChannel>();

    public RejoinWorker(RejoinManagerImpl rejoinManager) {
      this.manager = rejoinManager;
    }

    void requestRejoin(ClientMessageChannel channel) {
      synchronized (monitor) {
        if (shutdown) {
          logger.info("Ignoring rejoin request for channel " + channel + " as shutdown already");
          return;
        }
        if (manager.isReopenInProgress()) {
          logger.info("Ignoring rejoin request for channel " + channel + " as reopenInProgress already");
          return;
        }
        rejoinRequestedChannels.add(channel);
        logger.info("added rejoin request for channel " + channel + " total " + rejoinRequestedChannels.size());
        monitor.notifyAll();
      }
    }

    @Override
    public void run() {
      while (true) {
        ClientMessageChannel channel = waitUntilRejoinRequestedOrShutdown();
        if (shutdown) return;
        manager.doRejoin(channel);
      }
    }

    ClientMessageChannel waitUntilRejoinRequestedOrShutdown() {
      synchronized (monitor) {
        while (rejoinRequestedChannels.isEmpty()) {
          if (shutdown) { return null; }
          try {
            monitor.wait();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
        manager.setReopenInProgress(true);
        ClientMessageChannel channel = rejoinRequestedChannels.remove();
        rejoinRequestedChannels.clear();
        return channel;
      }
    }

    private void shutdown() {
      synchronized (monitor) {
        this.shutdown = true;
        monitor.notifyAll();
      }
    }

    public void setRejoinRequestedChannelListForTesting(Queue rejoinRequestsQueue) {
      this.rejoinRequestedChannels = rejoinRequestsQueue;
    }
  }

  @Override
  public int getRejoinCount() {
    return rejoinCount;
  }

  RejoinWorker getRejoinWorkerThread() {
    return rejoinWorker;
  }
}
