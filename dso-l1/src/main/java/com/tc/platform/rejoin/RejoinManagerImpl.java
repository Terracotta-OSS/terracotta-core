/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ClientMessageChannel;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RejoinManagerImpl implements RejoinManagerInternal {

  private static final TCLogger                logger           = TCLogging.getLogger(RejoinManagerImpl.class);
  private final List<RejoinLifecycleListener>  listeners        = new CopyOnWriteArrayList<RejoinLifecycleListener>();
  private final boolean                        rejoinEnabled;
  private final RejoinWorker                   rejoinWorker;
  private final AtomicBoolean                  rejoinInProgress = new AtomicBoolean(false);

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

  private void notifyRejoinStart() {
    assertRejoinEnabled();
    logger.info("Notifying rejoin start...");
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
  public void initiateRejoin(ClientMessageChannel channel) {
    assertRejoinEnabled();
    if (!rejoinInProgress.get()) {
      rejoinWorker.requestRejoin(channel);
    } else {
      logger.info("Ignoring rejoin request as already rejoinInProgress for channel: " + channel);
    }
  }

  @Override
  public boolean thisNodeJoined(ClientID newNodeId) {
    logger.info("This node joined the cluster - rejoinEnabled: " + rejoinEnabled + ", rejoin in progress:"
                + rejoinInProgress.get() + ", newNodeId: " + newNodeId);
    if (rejoinEnabled) {
      // called when all channels have connected and handshake is complete
      if (rejoinInProgress.compareAndSet(true, false)) {
        // take care of any cleanup/re-initialization
        notifyRejoinComplete();
        return true;
      }
    }
    return false;
  }

  // only called by rejoin worker
  private void doRejoin(ClientMessageChannel channel) {
    logger.info("Doing rejoin for channel: " + channel);
    if (rejoinInProgress.compareAndSet(false, true)) {
      try {
        notifyRejoinStart();
        while (true) {
          try {
            channel.reopen();
            break;
          } catch (Throwable t) {
            logger.warn("Error during channel open : " + channel + " ", t);
            try {
              TimeUnit.SECONDS.sleep(1L);
            } catch (InterruptedException e) {
              logger.warn("got inturrupted while sleeping before reopen of channel " + channel);
            }
          }
        }
      } catch (Throwable th) {
        // RejoinWorker thread should not die and should be able to accept more rejoin requests if one rejoin request
        // fails for some reason
        rejoinInProgress.set(false);
        logger.warn("Error during rejoin : " + channel + " " + th);
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
    private final Queue<ClientMessageChannel> rejoinRequestedChannels = new LinkedList<ClientMessageChannel>();

    public RejoinWorker(RejoinManagerImpl rejoinManager) {
      this.manager = rejoinManager;
    }

    private void requestRejoin(ClientMessageChannel channel) {
      synchronized (monitor) {
        if (shutdown) {
          logger.info("Ignoring rejoin request as already shutdown - channel: " + channel);
          return;
        }
        rejoinRequestedChannels.add(channel);
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

    private ClientMessageChannel waitUntilRejoinRequestedOrShutdown() {
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
