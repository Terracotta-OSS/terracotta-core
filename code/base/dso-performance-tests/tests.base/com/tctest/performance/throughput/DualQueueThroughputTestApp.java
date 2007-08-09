/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.throughput;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ITransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.performance.generate.load.Metronome;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class DualQueueThroughputTestApp extends AbstractDualQueueThroughputTestApp {

  private final BlockingQueue rootQueue1;
  private final BlockingQueue rootQueue2;

  public DualQueueThroughputTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    rootQueue1 = new LinkedBlockingQueue();
    rootQueue2 = new LinkedBlockingQueue();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    ITransparencyClassSpec spec = config.getOrCreateSpec(Metronome.class.getName());
    String className = DualQueueThroughputTestApp.class.getName();
    spec = config.getOrCreateSpec(className);
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("rootQueue1", "rootQueue1");
    spec.addRoot("rootQueue2", "rootQueue2");
  }
  
  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    config.addIncludePattern(Metronome.class.getName());
    String className = DualQueueThroughputTestApp.class.getName();
    config.addIncludePattern(className);
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addRoot("rootQueue1", className + ".rootQueue1");
    config.addRoot("rootQueue2", className + ".rootQueue2");
  }

  // Abstraction is not worth the effort
  protected void populate1(Object data) throws InterruptedException {
    if (data instanceof Metronome) {
      synchronized (data) {
        ((Metronome) data).starttime = System.nanoTime();
      }
    }
    rootQueue1.put(data);
  }

  protected void populate2(Object data) throws InterruptedException {
    if (data instanceof Metronome) {
      synchronized (data) {
        ((Metronome) data).starttime = System.nanoTime();
      }
    }
    rootQueue2.put(data);
  }

  protected void retrieve1() throws InterruptedException {
    Object data;
    data = rootQueue1.take();
    if (data instanceof Metronome) {
      synchronized (data) {
        ((Metronome) data).endtime = System.nanoTime();
      }
      results1().add(data);
    }
  }

  protected void retrieve2() throws InterruptedException {
    Object data;
    data = rootQueue2.take();
    if (data instanceof Metronome) {
      synchronized (data) {
        ((Metronome) data).endtime = System.nanoTime();
      }
      results2().add(data);
    }
  }

  protected String title() {
    return "Dual Shared Queue Throughput";
  }
}
