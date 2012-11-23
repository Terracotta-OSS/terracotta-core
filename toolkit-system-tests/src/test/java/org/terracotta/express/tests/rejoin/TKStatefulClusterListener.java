/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;

import java.util.concurrent.TimeUnit;

public class TKStatefulClusterListener {

  private static final int  MAX_WAIT_SECONDS = 180;

  private final ClusterInfo clusterInfo;
  private final Status      status;

  public TKStatefulClusterListener(ToolkitInternal toolkit) {
    ToolkitLogger logger = toolkit.getLogger("com.tc.TKStatefulClusterListener");
    status = new Status(logger);
    this.clusterInfo = toolkit.getClusterInfo();
    clusterInfo.addClusterListener(new ClusterListenerImpl(status, logger));
  }

  public void waitUntilRejoin() {
    status.waitUntil(State.REJOINED);
  }

  public void waitUntilOffline() {
    status.waitUntil(State.OFFLINE);
  }

  public void waitUntilOnline() {
    status.waitUntil(State.ONLINE);
  }

  private static class ClusterListenerImpl implements ClusterListener {

    private final Status        status;

    private final ToolkitLogger logger;

    public ClusterListenerImpl(Status status, ToolkitLogger logger) {
      this.logger = logger;
      this.status = status;
    }

    @Override
    public void onClusterEvent(ClusterEvent event) {
      logger.info("[C L U S T E R   E V E N T] " + event);
      switch (event.getType()) {
        case NODE_JOINED:
          break;
        case NODE_LEFT:
          break;
        case NODE_REJOINED:
          status.rejoined(event);
          break;
        case OPERATIONS_ENABLED:
          status.online(event);
          break;
        case OPERATIONS_DISABLED:
          status.offline(event);
          break;
      }
    }
  }

  private static class Status {

    private State               state = State.INIT;
    private ClusterEvent        lastReceivedEvent;
    private final ToolkitLogger logger;

    public Status(ToolkitLogger logger) {
      this.logger = logger;
    }

    public void offline(ClusterEvent event) {
      update(State.OFFLINE, event);
    }

    public void online(ClusterEvent event) {
      update(State.ONLINE, event);
    }

    public void rejoined(ClusterEvent event) {
      update(State.REJOINED, event);
    }

    private synchronized void update(State newState, ClusterEvent event) {
      this.state = newState;
      this.lastReceivedEvent = event;
      notifyAll();
    }

    public synchronized void waitUntil(State expected) {
      final long start = System.nanoTime();
      while (true) {
        logger.info("[W A I T E R] Waiting until " + expected + ", current: " + state + ", lastReceivedEvent: "
                    + lastReceivedEvent);
        if (state == expected) {
          break;
        }
        final long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
        if (elapsedSeconds > MAX_WAIT_SECONDS) { throw new AssertionError("Maximum wait time over, waiting for "
                                                                          + expected + ", elapsed: " + elapsedSeconds
                                                                          + " seconds"); }
        try {
          this.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException();
        }
      }
    }

  }

  private static enum State {
    INIT, ONLINE, OFFLINE, REJOINED;
  }

}
