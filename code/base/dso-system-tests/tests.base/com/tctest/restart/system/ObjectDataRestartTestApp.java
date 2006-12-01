/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

public class ObjectDataRestartTestApp extends AbstractTransparentApp {

  private int             threadCount     = 10;
  private int             workSize        = 1 * 100;
  private int             testObjectDepth = 1 * 50;
  // I had to dial this down considerably because this takes a long time to run.
  private int             iterationCount  = 1 * 2;
  private List            workQueue       = new ArrayList();
  private Collection      resultSet       = new HashSet();
  private SynchronizedInt complete        = new SynchronizedInt(0);
  private OutputListener  out;
  private SynchronizedInt nodes           = new SynchronizedInt(0);

  public ObjectDataRestartTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
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

  private void populateWorkQueue(int size, int depth, List queue) {
    System.err.println(" Thread - " + Thread.currentThread().getName() + " inside populateWorkQueue !");
    for (int i = 0; i < size; i++) {
      TestObject to = new TestObject("" + i);
      to.populate(depth);
      put(queue, to);
    }
  }

  private void verify(int expectedValue, Collection results) {
    synchronized (results) {
      Assert.assertEquals(workSize, results.size());
      for (Iterator i = results.iterator(); i.hasNext();) {
        TestObject to = (TestObject) i.next();
        if (!to.validate(expectedValue)) { throw new RuntimeException("Failed!"); }
      }
    }
  }

  public final void println(Object o) {
    try {
      out.println(o);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    visitor.visit(config, Barriers.class);

    String testClassName = ObjectDataRestartTestApp.class.getName();
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
    config.addWriteAutolock(verifyExpression);

    String runExpression = "* " + testClassName + ".run(..)";
    config.addWriteAutolock(runExpression);

    String populateWorkQueueExpression = "* " + testClassName + ".populateWorkQueue(..)";
    config.addWriteAutolock(populateWorkQueueExpression);

    String putExpression = "* " + testClassName + ".put(..)";
    config.addWriteAutolock(putExpression);

    String takeExpression = "* " + testClassName + ".take(..)";
    config.addWriteAutolock(takeExpression);

    // TestObject config
    String incrementExpression = "* " + testObjectClassname + ".increment(..)";
    config.addWriteAutolock(incrementExpression);

    String populateExpression = "* " + testObjectClassname + ".populate(..)";
    config.addWriteAutolock(populateExpression);

    String validateExpression = "* " + testObjectClassname + ".validate(..)";
    config.addReadAutolock(validateExpression);

    // Worker factory config
    String workerFactoryExpression = "* " + workerFactoryClassname + ".*(..)";
    config.addWriteAutolock(workerFactoryExpression);

    // Worker config
    String workerRunExpression = "* " + workerClassname + ".run(..)";
    config.addWriteAutolock(workerRunExpression);

    new SynchronizedIntSpec().visit(visitor, config);

    // IDProvider config
    String nextIDExpression = "* " + idProviderClassname + ".nextID(..)";
    config.addWriteAutolock(nextIDExpression);
  }

  public static final class WorkerFactory {
    private int                   localWorkerCount = 0;
    private final SynchronizedInt globalWorkerCount;               // = new SynchronizedInt(0);
    private final List            workQueue;
    private final Collection      results;
    private final Collection      localWorkers     = new HashSet();
    private final SynchronizedInt complete;
    private final String          appId;

    public WorkerFactory(String appId, List workQueue, Collection results, SynchronizedInt complete) {
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

  private static final class Worker implements Runnable {

    private final String          name;
    private final List            workQueue;
    private final Collection      results;
    private final SynchronizedInt workCompletedCount = new SynchronizedInt(0);
    private final SynchronizedInt objectChangeCount  = new SynchronizedInt(0);

    public Worker(String name, List workQueue, Collection results, SynchronizedInt complete) {
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

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
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

    public Barriers(int nodeCount) {
      this.barriers = new HashMap();
      this.nodeCount = nodeCount;
    }

    public int barrier(int barrierID) {
      try {
        return getOrCreateBarrier(barrierID).barrier();
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

    private CyclicBarrier getOrCreateBarrier(int barrierID) {
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

  private static final class TestObject {
    private TestObject child;
    private int        counter;
    private List       activity = new ArrayList();
    private String     id;

    public TestObject(String id) {
      this.id = id;
    }

    private synchronized void addActivity(Object msg) {
      activity.add(msg + "\n");
    }

    public void populate(int count) {
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
          if(currentValue == Integer.MIN_VALUE) {
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

    public boolean validate(int expectedValue) {
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

    public String toString() {
      return "TestObject@" + System.identityHashCode(this) + "(" + id + ")={ counter = " + counter +" }";
    }
  }

  private static final class IDProvider {
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

  private static Object take(List workQueue2) {
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

  private static void put(List workQueue2, Object o) {
    synchronized (workQueue2) {
      workQueue2.add(o);
      workQueue2.notify();
      System.err.println(Thread.currentThread().getName() + " : notifying All : Size = " + workQueue2.size());
    }
  }
}
