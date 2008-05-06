/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class SingleVMWaitNotifyTestApp extends AbstractTransparentApp {
  private static Random                 random    = new Random();
  private static int                    TAKERS    = random.nextInt(7) + 5;
  private static int                    PUTTERS   = random.nextInt(7) + 5;

  private static final SynchronizedLong takeCount = new SynchronizedLong(0);

  protected static final int            PUTS      = 200;

  // root
  private final List                    queue     = new LinkedList();

  public SingleVMWaitNotifyTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    if (getParticipantCount() != 1) { throw new RuntimeException("participant count must be 1"); }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = SingleVMWaitNotifyTestApp.class.getName();
    config.addIncludePattern(testClassName);
    String root = "queue";
    config.addRoot(new Root(testClassName, root, root + "Lock"), true);
    System.err.println("Adding root for " + testClassName + "." + root);

    String methodExpression = "* " + testClassName + "*.*(..)";
    System.err.println("Adding autolock for: " + methodExpression);
    config.addWriteAutolock(methodExpression);

    //config.addIncludePattern(Item.class.getName());
    config.addIncludePattern(testClassName + "$*");
  }

  public void run() {
    System.out.println("Number of putters: " + PUTTERS);
    System.out.println("Number of takers: " + TAKERS);

    ArrayList threads = new ArrayList();

    for (int i = 0; i < PUTTERS; i++) {
      threads.add(createPutter(i));
    }

    for (int i = 0; i < TAKERS; i++) {
      threads.add(createTaker(i));
    }

    // randomize the thread positions in the array, such that they start at random times
    Collections.shuffle(threads, random);
    for (Iterator iter = threads.iterator(); iter.hasNext();) {
      Thread t = (Thread) iter.next();
      t.start();
    }

    final Thread me = Thread.currentThread();
    Thread timeout = new Thread(new Runnable() {
      public void run() {
        ThreadUtil.reallySleep(1000 * 60 * 5);
        me.interrupt();
      }
    });
    timeout.setDaemon(true);
    timeout.start();

    waitForThreads(threads, true);

    // queue up the stop messages for the takers
    synchronized (queue) {
      for (int i = 0; i < TAKERS; i++) {
        queue.add(new Item(true));
      }
      queue.notifyAll();
    }

    // wait for everyone to finish (should just be the takers)
    waitForThreads(threads, false);

    final long expected = PUTTERS * PUTS;
    final long actual = takeCount.get();
    if (expected != actual) { throw new RuntimeException(actual + " != " + expected); }

    notifyResult(Boolean.TRUE);
  }

  private void waitForThreads(ArrayList threads, boolean justPutters) {
    for (Iterator iter = threads.iterator(); iter.hasNext();) {
      Thread t = (Thread) iter.next();

      if (!justPutters || t.getName().startsWith("PUTTER")) {
        try {
          t.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      }
    }
  }

  private Thread createTaker(int i) {
    Thread rv = new Thread(new Runnable() {
      public void run() {
        try {
          synchronized (queue) {
            while (true) {
              if (queue.size() > 0) {
                Item item = (Item) queue.remove(0);
                if (item.stop) { return; }
                takeCount.increment();
              } else {
                try {
                  queue.wait();
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
              }
            }
          }
        } catch (Throwable t) {
          notifyError(t);
        }
      }
    }, "TAKER " + i);

    rv.setDaemon(true);
    return rv;
  }

  private Thread createPutter(final int id) {
    Thread rv = new Thread( new Runnable() {
      public void run() {
        try {
          for (int i = 0; i < PUTS; i++) {
            synchronized (queue) {
              queue.add(new Item());
              queue.notifyAll();
            }
          }
        } catch (Throwable t) {
          notifyError(t);
        }
      }
    }, "PUTTER " + id);

    rv.setDaemon(true);
    return rv;
  }

  static class Item {
    private final boolean stop;

    Item() {
      this(false);
    }

    Item(boolean stop) {
      this.stop = stop;
    }

    boolean isStop() {
      return this.stop;
    }

  }

}