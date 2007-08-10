/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.throughput;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.performance.generate.load.Metronome;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class SingleQueueThroughputTestApp extends AbstractSingleQueueThroughputTestApp {

  private final BlockingQueue rootQueue;

  public SingleQueueThroughputTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    rootQueue = new LinkedBlockingQueue();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(Metronome.class.getName());
    String className = SingleQueueThroughputTestApp.class.getName();
    spec = config.getOrCreateSpec(className);
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("rootQueue", "rootQueue");
  }
  
  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    config.addIncludePattern(Metronome.class.getName());
    String className = SingleQueueThroughputTestApp.class.getName();
    config.addIncludePattern(className);
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addRoot("rootQueue", className + ".rootQueue");
  }

  protected void populate(Object data) throws InterruptedException {
    if (data instanceof Metronome) {
      synchronized (data) {
        ((Metronome) data).starttime = System.nanoTime();
      }
    }
    rootQueue.put(data);
  }

  protected void retrieve() throws InterruptedException {
    Object data;
    data = rootQueue.take();
    if (data instanceof Metronome) {
      synchronized (data) {
        ((Metronome) data).endtime = System.nanoTime();
      }
      results().add(data);
    }
  }

  protected String title() {
    return "Single Shared Queue Throughput";
  }
}
