/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.faulting;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.performance.generate.load.Metronome;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class SingleQueueFaultTestApp extends AbstractSingleQueueFaultTestApp {

  private final BlockingQueue rootQueue;

  public SingleQueueFaultTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    rootQueue = new LinkedBlockingQueue(100);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = visitConfig(visitor, config);
    spec = config.getOrCreateSpec(Metronome.class.getName());
    String className = SingleQueueFaultTestApp.class.getName();
    spec = config.getOrCreateSpec(className);
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("rootQueue", "rootQueue");
  }
  
  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    visitConfig(visitor, config);
    config.addIncludePattern(Metronome.class.getName());
    String className = SingleQueueFaultTestApp.class.getName();
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
        ((Metronome) data).object = null; // reference no longer needed
      }
      results().add(data);
    }
  }

  protected String title() {
    return "Single Shared Queue Faulting Throughput";
  }
}
