/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

final class HttpLoadClient {

  private final LinkedBlockingQueue workQ;
  private final PooledExecutor      requestExecutor;
  private final RequestCounter      counter;
  private final StatsCollector      collector;

  protected HttpLoadClient(LinkedBlockingQueue workQ, PooledExecutor requestExecutor, String[] hosts, int sessionCount,
                           int stickyRatio, RequestCounter counter) {

    this.counter = counter;
    this.workQ = workQ;
    this.requestExecutor = requestExecutor;
    this.collector = new StatsCollector();
  }

  public void execute() throws InterruptedException {
    while (true) {
      WorkItem workItem = (WorkItem) workQ.take();
      if (workItem.expired(System.currentTimeMillis())) continue;
      if (workItem.stop()) break;
      requestExecutor.execute(new Requestor(workItem));
    }
    requestExecutor.shutdownAfterProcessingCurrentlyQueuedTasks();
    requestExecutor.awaitTerminationAfterShutdown();
  }

  public StatsCollector getCollector() {
    return collector;
  }

  private void err(String str) {
    System.err.println(str);
  }

  private class Requestor implements Runnable {
    private WorkItem workItem;

    private Requestor(WorkItem workItem) {
      this.workItem = workItem;
    }

    public void run() {
      try {
        workItem.execute(collector);
        counter.increment();
      } catch (IOException e) {
        err("--Host Response Dropped");
      } catch (NullPointerException e) {
        err("--Client Connection Stale");
      } catch (IllegalStateException e) {
        err("--Client Connection is not Open");
      } finally {
        workItem.done();
      }
    }
  }

}
