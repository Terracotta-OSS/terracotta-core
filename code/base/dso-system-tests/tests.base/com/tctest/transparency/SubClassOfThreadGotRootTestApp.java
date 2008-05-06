/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.transparency;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SubClassOfThreadGotRootTestApp extends AbstractErrorCatchingTransparentApp {

  private static final int WORKER_COUNT    = 5;
  private static final int WORK_COUNT      = 10;

  private static final int CREATED_WORKERS = 0;
  private static final int CREATED_WORK    = 1;
  private static final int WORK_DONE       = 2;
  private static final int VERIFIED        = 2;

  private ArrayList        workers         = new ArrayList();

  public SubClassOfThreadGotRootTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    Assert.assertTrue(getParticipantCount() == 2);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String workerClass = SubClassOfThreadGotRootTestApp.Worker.class.getName();
    String workClass = SubClassOfThreadGotRootTestApp.Work.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(workerClass);
    String workerMethodExpression = "* " + workerClass + "*.*(..)";
    String workMethodExpression = "* " + workClass + "*.*(..)";
    config.addWriteAutolock(workerMethodExpression);
    config.addWriteAutolock(workMethodExpression);
    spec.addRoot("workQueue", "workQueue");

    CyclicBarrierSpec cbspec = new CyclicBarrierSpec();
    cbspec.visit(visitor, config);

    // config.addExcludePattern("*..SubClassA");

    // Include everything to be instrumented.
    config.addIncludePattern("*..*", false);
  }

  protected void runTest() throws Throwable {

    for (int i = 0; i < WORKER_COUNT; i++) {
      Worker worker = new Worker(getApplicationId(), i);
      workers.add(worker);
      worker.start();
    }

    moveToStageAndWait(CREATED_WORKERS);

    Random r = new Random();

    for (int i = 0; i < WORKER_COUNT; i++) {
      Worker w = (Worker) workers.get(i);
      for (int j = 0; j < WORK_COUNT; j++) {
        w.addWork(new Work(getApplicationId(), r.nextInt(Integer.MAX_VALUE), r.nextInt(Integer.MAX_VALUE)));
      }
    }

    moveToStageAndWait(CREATED_WORK);

    for (int i = 0; i < WORKER_COUNT; i++) {
      Worker w = (Worker) workers.get(i);
      w.waitToCompleteAndStop();
    }

    moveToStageAndWait(WORK_DONE);

    for (int i = 0; i < WORKER_COUNT; i++) {
      Worker w = (Worker) workers.get(i);
      List workDone = w.getWorkDone();
      System.err.println(Thread.currentThread().getName() + " verifying " + workDone.size() + " works done by "
                         + w.getName());
      for (Iterator j = workDone.iterator(); j.hasNext();) {
        Work work = (Work) j.next();
        work.verify();
      }
    }
    moveToStage(VERIFIED);

  }

  // This class has the root
  public class Worker extends Thread {

    List                 workQueue     = new ArrayList();
    List                 workDone      = new ArrayList();

    boolean              stopRequested = false;
    private final String appId;

    public Worker(String appId, int i) {
      super(appId + "," + i);
      this.appId = appId;
    }

    public void run() {
      Random r = new Random();
      while (!stopRequested) {
        Work work = getWork();
        if (work != null) {
          work.run();
          workDone.add(work);
        }
        else if (!stopRequested) {
          ThreadUtil.reallySleep(r.nextInt(2000) + 1);
        }
      }
    }

    private int getSize() {
      synchronized (workQueue) {
        return workQueue.size();
      }
    }

    private Work getWork() {
      synchronized (workQueue) {
        while (workQueue.size() == 0 && !stopRequested) {
          try {
            workQueue.wait();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
        if (stopRequested) return null;
        workQueue.notifyAll();
        for (Iterator iter = workQueue.iterator(); iter.hasNext();) {
          Work work = (Work) iter.next();
          if (!work.getAppId().equals(appId)) {
            iter.remove();
            return work;
          }
        }
        System.err.println("I, " + getName() + " couldn't get any foreign work. Trying again -  Size = " + getSize());
        return null;
      }
    }

    public void addWork(Work work) {
      synchronized (workQueue) {
        workQueue.add(work);
        workQueue.notify();
      }
    }

    public void waitToCompleteAndStop() {
      synchronized (workQueue) {
        while (workQueue.size() != 0) {
          try {
            workQueue.wait();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
        stopRequested = true;
        workQueue.notifyAll();
      }
    }

    public List getWorkDone() {
      return workDone;
    }
  }

  // This is the Portable Object
  public static class Work implements Runnable {

    int                  n1, n2;
    long                 add, sub, multi;
    double               div;

    String               workerName;
    private final String appId;

    public Work(String appId, int n1, int n2) {
      this.appId = appId;
      this.n1 = n1;
      this.n2 = n2;
    }

    public String getAppId() {
      return this.appId;
    }

    public synchronized void run() {
      workerName = Thread.currentThread().getName();
      System.err.println("Worker " + workerName + " working on " + this);
      add = add();
      sub = subtract();
      multi = multiply();
      div = divide();
    }

    private double divide() {
      if (n2 != 0) {
        return n1 / n2;
      } else {
        return Double.NaN;
      }
    }

    private long multiply() {
      return n1 * n2;
    }

    private long subtract() {
      return n1 - n2;
    }

    private long add() {
      return n1 + n2;
    }

    public synchronized void verify() {
      Assert.assertEquals(add, add());
      Assert.assertEquals(sub, subtract());
      Assert.assertEquals(multi, multiply());
      if (Double.isNaN(div)) {
        Assert.assertTrue(Double.isNaN(divide()));
      } else {
        Assert.assertEquals(div, divide());
      }
    }

    public String toString() {
      return "Work(" + appId + "):{" + n1 + "," + n2 + "}";
    }

  }

}
