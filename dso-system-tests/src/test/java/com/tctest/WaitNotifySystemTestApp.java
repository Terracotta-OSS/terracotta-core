/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WaitNotifySystemTestApp extends AbstractTransparentApp {
  private static final TCLogger logger     = TCLogging.getTestingLogger(WaitNotifySystemTestApp.class);

  // roots
  private final List            queue      = new LinkedList();
  private final Set             takers     = new HashSet();
  private final Set             putters    = new HashSet();
  private final Set             workers    = new HashSet();
  private final List            takeCounts = new LinkedList();
  private final Flag            first      = new Flag();

  private static final int      PUTS       = 50;
  private static final boolean  debug      = true;
  private Random                random;

  public WaitNotifySystemTestApp(String globalId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(globalId, cfg, listenerProvider);

    if (getParticipantCount() < 3) { throw new RuntimeException("Must have at least 3 participants to run this test"); }

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = WaitNotifySystemTestApp.class.getName();

    List roots = Arrays.asList(new Object[] { "queue", "takers", "putters", "workers", "takeCounts", "first" });
    for (Iterator iter = roots.iterator(); iter.hasNext();) {
      String root = (String) iter.next();
      config.addRoot(new Root(testClassName, root, root + "Lock"), true);
      System.err.println("Adding root for " + testClassName + "." + root);
    }

    String methodExpression = "* " + testClassName + "*.*(..)";
    System.err.println("Adding autolock for: " + methodExpression);
    config.addWriteAutolock(methodExpression);

    config.addIncludePattern(Flag.class.getName());
    config.addIncludePattern(WorkItem.class.getName());
  }

  public void run() {
    random = new Random(new Random(System.currentTimeMillis() + getApplicationId().hashCode()).nextLong());

    try {
      run0();
      notifyResult(Boolean.TRUE);
    } catch (Throwable t) {
      notifyError(t);
      notifyResult(Boolean.FALSE);
    }
  }

  public void run0() throws Throwable {
    final long id = Long.valueOf(getApplicationId()).longValue();

    if (first.attemptSet()) {
      // I am the master, bow before me
      runMaster(getParticipantCount() - 1);
    } else {
      if (random.nextBoolean()) {
        runPutter(id);
      } else {
        runTaker(id);
      }
    }
  }

  private void runTaker(long id) throws InterruptedException {
    Thread.currentThread().setName("TAKER-" + id);

    Long myID = Long.valueOf(id);
    synchronized (takers) {
      takers.add(myID);
    }

    synchronized (workers) {
      workers.add(myID);
      workers.notify();
    }

    log("STARTED");

    long count = 0;
    try {
      synchronized (queue) {
        while (true) {
          if (queue.size() > 0) {
            WorkItem wi = (WorkItem) queue.remove(0);
            // log("TOOK " + wi);
            if (wi.isStop()) {
              log("took a stop item");
              return;
            }
            count++;
          } else {
            if (random.nextBoolean()) {
              long millis = random.nextInt(10000);
              if (random.nextBoolean()) {
                int nanos = random.nextInt(10000);
                // log("wait(" + millis + ", " + nanos + ")");
                queue.wait(millis, nanos);
              } else {
                // log("wait(" + millis + ")");
                queue.wait(millis);
              }
            } else {
              // log("wait()");
              queue.wait();
            }
          }
        }
      }

    } finally {
      log("adding to takeCount");
      synchronized (takeCounts) {
        takeCounts.add(Long.valueOf(count));
      }

      log("removing self from takers set");
      synchronized (takers) {
        takers.remove(myID);
        takers.notify();
      }

      log("ENDED");
    }
  }

  private void runPutter(long id) {
    Thread.currentThread().setName("PUTTER-" + id);

    Long myID = Long.valueOf(id);
    synchronized (putters) {
      putters.add(myID);
    }

    synchronized (workers) {
      workers.add(myID);
      workers.notify();
    }

    log("STARTED");

    try {
      for (int i = 0; i < PUTS; i++) {
        synchronized (queue) {
          WorkItem newWork = new WorkItem(myID.toString() + "-" + i);

          // log("PUTTING new work: " + newWork);

          queue.add(newWork);

          if (random.nextBoolean()) {
            // log("notify all");
            queue.notifyAll();
          } else {
            // log("notify");
            queue.notify();
          }
        }
      }
    } finally {
      log("removing self from putters set");
      synchronized (putters) {
        putters.remove(myID);
        putters.notify();
      }

      log("ENDED");
    }
  }

  private void runMaster(int workerCount) throws InterruptedException {
    Thread.currentThread().setName("MASTER");

    waitForAllWorkers(workerCount);

    log("All worker nodes started");

    final Long workerIDs[];
    synchronized (workers) {
      workerIDs = (Long[]) workers.toArray(new Long[] {});
    }

    final long extraTakerID = getUniqueId(workerIDs);
    final List next = new ArrayList(workers);
    next.add(Long.valueOf(extraTakerID));
    final long extraPutterID = getUniqueId((Long[]) next.toArray(new Long[] {}));

    // start up another taker and putter locally. Do this for two reasons:
    // 1) Taker/Putter choice is made randomly, thus it is possible that there are only putters or takers out there
    // 2) To force some wait/notify in the intra-VM context
    Thread extraTaker = new Thread(new Runnable() {
      public void run() {
        try {
          runTaker(extraTakerID);
        } catch (Throwable t) {
          WaitNotifySystemTestApp.this.notifyError(t);
        }
      }
    });
    extraTaker.start();

    Thread extraPutter = new Thread(new Runnable() {
      public void run() {
        try {
          runPutter(extraPutterID);
        } catch (Throwable t) {
          WaitNotifySystemTestApp.this.notifyError(t);
        }
      }
    });
    extraPutter.start();

    workerCount += 2;
    waitForAllWorkers(workerCount);

    log("Extra workers started");

    final int numTakers;
    synchronized (takers) {
      numTakers = takers.size();
    }
    log("takers count = " + numTakers);

    final int numPutters = workerCount - numTakers;
    log("putters count = " + numPutters);

    // wait for all putters to finish
    synchronized (putters) {
      while (putters.size() > 0) {
        log("waiting for putters: " + putters.size());
        putters.wait();
      }
    }

    log("All putters done");

    // tell the takers to stop
    synchronized (queue) {
      for (int i = 0; i < numTakers; i++) {
        queue.add(WorkItem.STOP);
      }
      queue.notifyAll();
    }

    log("Takers told to stop");

    // wait for all the takers to finish
    synchronized (takers) {
      while (takers.size() > 0) {
        log("waiting for takers: " + takers.size());
        takers.wait();
      }
    }

    log("Takers all done");

    // total up the work items each taker saw
    long total = 0;
    synchronized (takeCounts) {
      log("Collecting take counts");

      if (takeCounts.size() != numTakers) {
        // shouldn't happen, but if something is wrong, it might be useful to know how many take counts were there
        throw new RuntimeException("Wrong number of take counts: " + takeCounts.size() + " != " + numTakers);
      }

      for (Iterator iter = takeCounts.iterator(); iter.hasNext();) {
        Long count = (Long) iter.next();
        total += count.longValue();
      }
    }

    // verify the results
    final int expectedTotal = numPutters * PUTS;
    if (total != expectedTotal) { throw new RuntimeException("Expected " + expectedTotal + ", but we got " + total); }
  }

  private void waitForAllWorkers(int workerCount) throws InterruptedException {
    // wait for everyone to start
    synchronized (workers) {
      while (workers.size() < workerCount) {
        final int lastCount = workers.size();
        log("waiting for workers " + workers.size());
        workers.wait();
        if (lastCount == workers.size()) {
          log("Size=" + lastCount + " didn't change!!!");
        }
      }
    }
  }

  private long getUniqueId(Long[] workerIDs) {
    while (true) {
      final long candidate = random.nextInt(Integer.MAX_VALUE);
      boolean okay = true;
      for (Long workerID : workerIDs) {
        if (workerID.longValue() == candidate) {
          okay = false;
          break;
        }

        if (okay) { return candidate; }
      }
    }
  }

  private static void log(String msg) {
    if (debug) logger.info(msg);
  }

  private static class Flag {
    private boolean set = false;

    synchronized boolean attemptSet() {
      if (!set) {
        set = true;
        return true;
      }
      return false;
    }
  }

  private static class WorkItem {
    static final WorkItem STOP = new WorkItem("STOP");

    private final String  name;

    WorkItem(String name) {
      this.name = name;
    }

    boolean isStop() {
      return STOP.name.equals(name);
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

}