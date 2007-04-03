/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;

public class LockUpgradeAndReadTestApp extends AbstractTransparentApp {

  private Map             root = new HashMap();
  private SynchronizedInt id   = new SynchronizedInt(0);

  public LockUpgradeAndReadTestApp(String globalId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(globalId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = LockUpgradeAndReadTestApp.class.getName();

    config.addRoot(testClassName, "root", "root", true);
    config.addRoot(testClassName, "id", "id", true);

    String methodExpression = "* " + testClassName + ".write(..)";
    config.addWriteAutolock(methodExpression);
    
    methodExpression = "* " + testClassName + ".write2(..)";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClassName + ".read(..)";
    config.addReadAutolock(methodExpression);
    
    methodExpression = "* " + testClassName + ".read2(..)";
    config.addReadAutolock(methodExpression);

    methodExpression = "* " + testClassName + ".readAndThenWrite(..)";
    config.addReadAutolock(methodExpression);
    
    methodExpression = "* " + testClassName + ".readAndThenWrite2(..)";
    config.addReadAutolock(methodExpression);

    new SynchronizedIntSpec().visit(visitor, config);
  }
  
  public void run() {
    try {
      int myid = id.increment();
      
      test(myid);
    } catch (Throwable t) {
      notifyError(t);
    }
  }
  
  private void test(int myid) throws Exception {
    println("My id is : " + myid);
    for (int i = 0; i < 500; i++) {
      if (myid == 1) {
        final int j = i;
        final CyclicBarrier barrier1 = new CyclicBarrier(2);
        final CyclicBarrier barrier2 = new CyclicBarrier(2);
        Thread t1 = new Thread(new Runnable() {
          public void run() {
            try {
              read(j, barrier1, barrier2);
              barrier1.barrier();
            } catch (Throwable t) {
              notifyError(t);
            }
          }
        });
        t1.start();
        readAndThenWrite(i, barrier2, true);
        barrier1.barrier();
        readAndThenWrite(i, null, false);
        barrier1.barrier();
      } 
    }
  }

  private void read(int i, CyclicBarrier barrier1, CyclicBarrier barrier2) throws Exception {
    String key = "What-" + i;
    Object value;
    barrier2.barrier();
    synchronized (root) {
      value = root.get(key);
    }
    println("Reader : " + key + " -> " + value);
    barrier1.barrier();
  }

  private void println(String message) {
    System.err.println(Thread.currentThread().getName() + " : " + message);
  }

  private void readAndThenWrite(int i, CyclicBarrier barrier, boolean useBarrier) throws Exception {
    synchronized (root) {
      root.get("What-" + i);
      write(i, barrier, useBarrier);
    }

  }

  private void write(int i, CyclicBarrier barrier, boolean useBarrier) throws Exception {
    String key = "What-" + i;
    Object old;
    synchronized (root) {
      if (useBarrier) {
        barrier.barrier();
      }
      old = root.put(key, "Nothing-" + i);
    }
    println("Writer : " + key + " -> " + old + " (old)");
  }
}
