/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.LinkedNode;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.exception.TCRuntimeException;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.simulator.listener.OutputListener;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ObjectDataTestApp extends AbstractTransparentApp {
  public static final String SYNCHRONOUS_WRITE = "synch-write";

  private final int                threadCount       = 10;
  private final int                workSize          = 1 * 50;
  private final int                testObjectDepth   = 1 * 25;
  // Beware when tuning down the iteration count, it might not run long enough to actually do a useful crash test
  // But, high iteration counts can cause test timeouts in slow boxes : MNK-941
  private final int                iterationCount    = 5;
  private final List               workQueue         = new ArrayList();
  private final Collection         resultSet         = new HashSet();
  private final SynchronizedInt    complete          = new SynchronizedInt(0);
  private final OutputListener     out;
  private final SynchronizedInt    nodes             = new SynchronizedInt(0);

  public ObjectDataTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.out = listenerProvider.getOutputListener();
  }

  public void run() {
    try {
      // set up workers
      WorkerFactory wf = new WorkerFactory(getApplicationId(), workQueue, resultSet, complete);

      for (int i = 0; i < threadCount; i++) {
        Worker worker = wf.newWorker();
        new Thread(worker).start();
      }

      if (nodes.increment() == 1) {
        // if we are the first participant, we control the work queue and do the verifying
        // System.err.println("Populating work queue...");
        populateWorkQueue(workSize, testObjectDepth, workQueue);
        for (int i = 0; i < iterationCount; i++) {
          synchronized (resultSet) {
            while (resultSet.size() < workSize) {
              try {
                resultSet.wait();
              } catch (InterruptedException e) {
                throw new TCRuntimeException(e);
              }
            }
          }
          verify(i + 1, resultSet);
          if (i != (iterationCount - 1)) {
            synchronized (resultSet) {
              for (Iterator iter = resultSet.iterator(); iter.hasNext();) {
                put(workQueue, iter.next());
                iter.remove();
              }
            }
          }
        }
        for (int i = 0; i < wf.getGlobalWorkerCount(); i++) {
          put(workQueue, "STOP");
        }
      }
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  private void populateWorkQueue(final int size, final int depth, final List queue) {
    System.err.println(" Thread - " + Thread.currentThread().getName() + " inside populateWorkQueue !");
    for (int i = 0; i < size; i++) {
      TestObject to = new TestObject("" + i);
      to.populate(depth);
      put(queue, to);
    }
  }

  private void verify(final int expectedValue, final Collection results) {
    synchronized (results) {
      Assert.assertEquals(workSize, results.size());
      int cnt = 0;
      for (Iterator i = results.iterator(); i.hasNext();) {
        TestObject to = (TestObject) i.next();
        if (!to.validate(expectedValue)) { throw new RuntimeException("Failed!"); }
        System.out.println("Verified object " + (cnt++));
      }
    }
  }

  public final void println(final Object o) {
    try {
      out.println(o);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    visitL1DSOConfig(visitor, config, new HashMap());
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config, final Map optionalAttributes) {
    boolean isSynchronousWrite = false;
    if (optionalAttributes.size() > 0) {
      isSynchronousWrite = Boolean.valueOf((String) optionalAttributes.get(ObjectDataTestApp.SYNCHRONOUS_WRITE))
          .booleanValue();
    }

    visitor.visit(config, Barriers.class);

    String testClassName = ObjectDataTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);

    String idProviderClassname = IDProvider.class.getName();
    config.addIncludePattern(idProviderClassname);

    String linkedQueueClassname = LinkedQueue.class.getName();
    config.addIncludePattern(linkedQueueClassname);

    String linkedNodeClassname = LinkedNode.class.getName();
    config.addIncludePattern(linkedNodeClassname);
    //
    // String syncIntClassname = SynchronizedInt.class.getName();
    // config.addIncludeClass(syncIntClassname);
    //
    // String syncVarClassname = SynchronizedVariable.class.getName();
    // config.addIncludeClass(syncVarClassname);

    String testObjectClassname = TestObject.class.getName();
    config.addIncludePattern(testObjectClassname);

    String workerClassname = Worker.class.getName();
    config.addIncludePattern(workerClassname);

    // Create Roots
    spec.addRoot("workQueue", testClassName + ".workQueue");
    spec.addRoot("resultSet", testClassName + ".resultSet");
    spec.addRoot("complete", testClassName + ".complete");
    spec.addRoot("nodes", testClassName + ".nodes");

    String workerFactoryClassname = WorkerFactory.class.getName();
    config.addIncludePattern(workerFactoryClassname);
    TransparencyClassSpec workerFactorySpec = config.getOrCreateSpec(workerFactoryClassname);
    workerFactorySpec.addRoot("globalWorkerCount", workerFactoryClassname + ".globalWorkerCount");

    // Create locks
    String verifyExpression = "* " + testClassName + ".verify(..)";
    addWriteAutolock(config, isSynchronousWrite, verifyExpression);

    String runExpression = "* " + testClassName + ".run(..)";
    addWriteAutolock(config, isSynchronousWrite, runExpression);

    String populateWorkQueueExpression = "* " + testClassName + ".populateWorkQueue(..)";
    addWriteAutolock(config, isSynchronousWrite, populateWorkQueueExpression);

    String putExpression = "* " + testClassName + ".put(..)";
    addWriteAutolock(config, isSynchronousWrite, putExpression);

    String takeExpression = "* " + testClassName + ".take(..)";
    addWriteAutolock(config, isSynchronousWrite, takeExpression);

    // TestObject config
    String incrementExpression = "* " + testObjectClassname + ".increment(..)";
    addWriteAutolock(config, isSynchronousWrite, incrementExpression);

    String populateExpression = "* " + testObjectClassname + ".populate(..)";
    addWriteAutolock(config, isSynchronousWrite, populateExpression);

    String validateExpression = "* " + testObjectClassname + ".validate(..)";
    config.addReadAutolock(validateExpression);

    // Worker factory config
    String workerFactoryExpression = "* " + workerFactoryClassname + ".*(..)";
    addWriteAutolock(config, isSynchronousWrite, workerFactoryExpression);

    // Worker config
    String workerRunExpression = "* " + workerClassname + ".run(..)";
    addWriteAutolock(config, isSynchronousWrite, workerRunExpression);

    new SynchronizedIntSpec().visit(visitor, config);

    // IDProvider config
    String nextIDExpression = "* " + idProviderClassname + ".nextID(..)";
    addWriteAutolock(config, isSynchronousWrite, nextIDExpression);
  }

  private static void addWriteAutolock(final DSOClientConfigHelper config, final boolean isSynchronousWrite, final String methodPattern) {
    if (isSynchronousWrite) {
      config.addSynchronousWriteAutolock(methodPattern);
    } else {
      config.addWriteAutolock(methodPattern);
    }
  }

  public static final class WorkerFactory {
    private int                   localWorkerCount = 0;
    private final SynchronizedInt globalWorkerCount;               // = new SynchronizedInt(0);
    private final List            workQueue;
    private final Collection      results;
    private final Collection      localWorkers     = new HashSet();
    private final SynchronizedInt complete;
    private final String          appId;

    public WorkerFactory(final String appId, final List workQueue, final Collection results, final SynchronizedInt complete) {
      this.appId = appId;
      this.workQueue = workQueue;
      this.results = results;
      this.complete = complete;
      this.globalWorkerCount = new SynchronizedInt(0);
    }

    public Worker newWorker() {
      localWorkerCount++;
      int globalWorkerID = globalWorkerCount.increment();
      // System.err.println("Worker: " + globalWorkerID);
      Worker rv = new Worker("(" + appId + ") : Worker " + globalWorkerID + "," + localWorkerCount, this.workQueue,
                             this.results, this.complete);
      this.localWorkers.add(rv);
      return rv;
    }

    public int getGlobalWorkerCount() {
      return globalWorkerCount.get();
    }
  }

  protected static final class Worker implements Runnable {

    private final String          name;
    private final List            workQueue;
    private final Collection      results;
    private final SynchronizedInt workCompletedCount = new SynchronizedInt(0);
    private final SynchronizedInt objectChangeCount  = new SynchronizedInt(0);

    public Worker(final String name, final List workQueue, final Collection results, final SynchronizedInt complete) {
      this.name = name;
      this.workQueue = workQueue;
      this.results = results;
    }

    public void run() {
      Thread.currentThread().setName(name);
      try {
        while (true) {
          TestObject to;
          Object o = take(workQueue);
          if (o instanceof TestObject) {
            to = (TestObject) o;
            System.err.println(name + " : Got : " + to);
          } else if ("STOP".equals(o)) {
            return;
          } else {
            throw new RuntimeException("Unexpected task: " + o);
          }
          objectChangeCount.add(to.increment());
          synchronized (results) {
            results.add(to);
            results.notifyAll();
          }
          workCompletedCount.increment();
        }
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
    }

    public int getWorkCompletedCount() {
      return this.workCompletedCount.get();
    }

    public int getObjectChangeCount() {
      return this.objectChangeCount.get();
    }
  }

  public static final class Barriers {
    private final Map barriers;
    private final int nodeCount;

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      String classname = Barriers.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(classname);
      spec.addRoot("barriers", classname + ".barriers");
      String barriersExpression = "* " + classname + ".*(..)";
      config.addWriteAutolock(barriersExpression);

      String cyclicBarrierClassname = CyclicBarrier.class.getName();
      config.addIncludePattern(cyclicBarrierClassname);

      // CyclicBarrier config
      String cyclicBarrierExpression = "* " + cyclicBarrierClassname + ".*(..)";
      config.addWriteAutolock(cyclicBarrierExpression);
    }

    public Barriers(final int nodeCount) {
      this.barriers = new HashMap();
      this.nodeCount = nodeCount;
    }

    public int barrier(final int barrierID) {
      try {
        return getOrCreateBarrier(barrierID).barrier();
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

    private CyclicBarrier getOrCreateBarrier(final int barrierID) {
      synchronized (barriers) {
        Integer key = new Integer(barrierID);
        CyclicBarrier rv = (CyclicBarrier) barriers.get(key);
        if (rv == null) {
          rv = new CyclicBarrier(this.nodeCount);
          this.barriers.put(key, rv);
        }
        return rv;
      }
    }

  }

  protected static final class TestObject {
    private TestObject child;
    private int        counter;
    private final List       activity = new ArrayList();
    private final String     id;

    public TestObject(final String id) {
      this.id = id;
    }

    private synchronized void addActivity(final Object msg) {
      activity.add(msg + "\n");
    }

    public void populate(final int count) {
      TestObject to = this;
      for (int i = 0; i < count; i++) {
        synchronized (to) {
          addActivity(this + ": Populated : (i,count) = (" + i + "," + count + ") @ " + new Date() + " by thread "
                      + Thread.currentThread().getName());
          to.child = new TestObject(id + "," + i);
        }
        to = to.child;
      }
    }

    public int increment() {
      TestObject to = this;
      int currentValue = Integer.MIN_VALUE;
      int changeCounter = 0;
      do {
        synchronized (to) {
          // XXX: This synchronization is here to provide transaction boundaries, not because other threads will be
          // fussing with this object.
          if (currentValue == Integer.MIN_VALUE) {
            currentValue = to.counter;
          }
          if (currentValue != to.counter) { throw new RuntimeException("Expected current value=" + currentValue
                                                                       + ", actual current value=" + to.counter); }
          to.addActivity(this + ": increment <inside loop> : old value=" + to.counter + ", thread="
                         + Thread.currentThread().getName() + " - " + to.counter + " @ " + new Date());
          to.counter++;
          changeCounter++;
        }
      } while ((to = to.getChild()) != null);
      return changeCounter;
    }

    public boolean validate(final int expectedValue) {
      TestObject to = this;
      do {
        // XXX: This synchronization is here to provide transaction boundaries, not because other threads will be
        // fussing with this object.
        synchronized (to) {
          if (to.counter != expectedValue) {
            System.err.println("Expected " + expectedValue + " but found: " + to.counter + " on Test Object : " + to);
            System.err.println(" To Activities = " + to.activity);
            System.err.println(" This Activities = " + activity);
            return false;
          }
        }
      } while ((to = to.getChild()) != null);
      return true;
    }

    private synchronized TestObject getChild() {
      return child;
    }

    @Override
    public String toString() {
      return "TestObject@" + System.identityHashCode(this) + "(" + id + ")={ counter = " + counter + " }";
    }
  }

  protected static final class IDProvider {
    private int current;

    public synchronized Integer nextID() {
      int rv = current++;
      // System.err.println("Issuing new id: " + rv);
      return new Integer(rv);
    }

    public synchronized Integer getCurrentID() {
      return new Integer(current);
    }
  }

  private static Object take(final List workQueue2) {
    synchronized (workQueue2) {
      while (workQueue2.size() == 0) {
        try {
          System.err.println(Thread.currentThread().getName() + " : Going to sleep : Size = " + workQueue2.size());
          workQueue2.wait();
          System.err.println(Thread.currentThread().getName() + " : Waking from sleep : Size = " + workQueue2.size());
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      return workQueue2.remove(0);
    }
  }

  private static void put(final List workQueue2, final Object o) {
    synchronized (workQueue2) {
      workQueue2.add(o);
      workQueue2.notify();
    }
  }
}
