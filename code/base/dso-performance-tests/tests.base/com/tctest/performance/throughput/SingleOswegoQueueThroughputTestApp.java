/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.throughput;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.LinkedQueueSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.performance.generate.load.Metronome;

public final class SingleOswegoQueueThroughputTestApp extends AbstractSingleQueueThroughputTestApp {

  private final LinkedQueue rootQueue;

  public SingleOswegoQueueThroughputTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    rootQueue = new LinkedQueue();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(Metronome.class.getName());
    String className = SingleOswegoQueueThroughputTestApp.class.getName();
    spec = config.getOrCreateSpec(className);
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("rootQueue", "rootQueue");
    new LinkedQueueSpec().visit(visitor, config);
  }
  
  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    config.addIncludePattern(Metronome.class.getName());
    String className = SingleOswegoQueueThroughputTestApp.class.getName();
    config.addIncludePattern(className);
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addRoot("rootQueue", className + ".rootQueue");
    new LinkedQueueSpec().visit(visitor, config);
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
    return "Single Shared Oswego Queue Throughput";
  }
}
