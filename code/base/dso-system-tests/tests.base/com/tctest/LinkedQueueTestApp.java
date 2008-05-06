/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.LinkedQueueSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Random;

public class LinkedQueueTestApp extends AbstractTransparentApp {

  public static int       COUNT       = 500;
  public static int       DEBUG_COUNT = 100;

  private LinkedQueue     queue   = new LinkedQueue();
  //private List queue = new ArrayList();
  private SynchronizedInt in          = new SynchronizedInt(0);
  private SynchronizedInt out         = new SynchronizedInt(0);

  public LinkedQueueTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    Random random = new Random();
    while (out.get() < COUNT) {
      if ((random.nextInt(2) == 0) || (in.get() >= COUNT)) {
          get();
      } else {
          put();
      }
    }
  }

  private void get() {
    synchronized (out) {
      try {
      if (!queue.isEmpty()) {
        Integer i = (Integer) queue.take();
        //Integer i = (Integer) take(queue);
        if (i.intValue() != out.increment()) {
          throw new AssertionError(" Got = " + i.intValue() + " and Expected = " + out.get());
        }
        //if ((i.intValue() % DEBUG_COUNT) == 0) {
        println("                 Got : " + i);
        //}
      }
      } catch (InterruptedException e) {
      throw new AssertionError(e);
      }
    }

  }

  private void put() {
    synchronized (in) {
      try {
      Integer i = new Integer(in.increment());
      queue.put(i);
      //put(queue, i);
      println("Put : " + i);
      } catch (InterruptedException e) {
      throw new AssertionError(e);
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    String testClassName = LinkedQueueTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);

    // Create Roots
    spec.addRoot("queue", testClassName + ".queue");
    spec.addRoot("in", testClassName + ".in");
    spec.addRoot("out", testClassName + ".out");

    // Create locks
    String runExpression = "* " + testClassName + ".*(..)";
    System.err.println("Adding write autolock for: " + runExpression);
    config.addWriteAutolock(runExpression);

    new SynchronizedIntSpec().visit(visitor, config);

    new LinkedQueueSpec().visit(visitor, config);
  }

//  private static Object take(List workQueue2) {
//    synchronized (workQueue2) {
//      while (workQueue2.size() == 0) {
//        try {
//          workQueue2.wait();
//        } catch (InterruptedException e) {
//          throw new RuntimeException(e);
//        }
//      }
//      return workQueue2.remove(0);
//    }
//  }
//
//  private static void put(List workQueue2, Object o) {
//    synchronized (workQueue2) {
//      workQueue2.add(o);
//      workQueue2.notify();
//    }
//  }

  private static void println(Object o) {
    System.err.println(Thread.currentThread().getName() + " : " + String.valueOf(o));
  }

}
