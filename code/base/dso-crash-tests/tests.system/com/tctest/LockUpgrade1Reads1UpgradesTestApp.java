/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Map;

public class LockUpgrade1Reads1UpgradesTestApp extends AbstractTransparentApp {

  private Map             root = new HashMap();
  private SynchronizedInt id   = new SynchronizedInt(0);

  public LockUpgrade1Reads1UpgradesTestApp(String globalId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(globalId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = LockUpgrade1Reads1UpgradesTestApp.class.getName();

    config.addRoot(testClassName, "root", "root", true);
    config.addRoot(testClassName, "id", "id", true);

    String methodExpression = "* " + testClassName + ".write(..)";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClassName + ".read(..)";
    config.addReadAutolock(methodExpression);

    methodExpression = "* " + testClassName + ".readAndThenWrite(..)";
    config.addReadAutolock(methodExpression);

    new SynchronizedIntSpec().visit(visitor, config);
  }

  public void run() {
    int myid = id.increment();
    println("My id is : " + myid);
    for (int i = 0; i < 1000; i++) {
      if (myid % 2 == 1) {
        // upgrader
        readAndThenWrite(i);
      } else {
        // reader
        read(i);
      }
    }
  }

  private void read(int i) {
    String key = "What-" + i;
    Object value;
    synchronized (root) {
      value = root.get(key);
    }
    println("Reader : " + key + " -> " + value);
  }

  private void println(String message) {
    System.err.println(Thread.currentThread().getName() + " : " + message);
  }

  private void readAndThenWrite(int i) {
    synchronized (root) {
      root.get("What-" + i);
      write(i);
    }

  }

  private void write(int i) {
    String key = "What-" + i;
    Object old;
    synchronized (root) {
      old = root.put(key, "Nothing-" + i);
    }
    println("Writer : " + key + " -> " + old + " (old)");
  }
}
