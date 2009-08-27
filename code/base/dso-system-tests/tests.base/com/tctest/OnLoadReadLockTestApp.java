/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.logging.Log4JAppenderToTCAppender;
import com.tc.logging.LogLevel;
import com.tc.logging.TCAppender;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.Manager;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class OnLoadReadLockTestApp extends AbstractErrorCatchingTransparentApp {
  private final CyclicBarrier barrier;

  private MyObject            root;

  private final LogAppender   appender;

  public OnLoadReadLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.barrier = new CyclicBarrier(getParticipantCount());

    appender = new LogAppender();
  }

  @Override
  protected void runTest() throws Throwable {
    if (0 == barrier.await()) {
      root = new MyObject();
      root.setObject(new MyObject1());
    }

    barrier.await();

    Log4JAppenderToTCAppender wrappedAppender = TCLogging.addAppender(Manager.class.getName(), appender);
    root.getObject();
    TCLogging.removeAppender(Manager.class.getName(), wrappedAppender);

    barrier.await();

    Assert.assertEquals(0, appender.takeLoggedMessages().length());
  }

  private static class LogAppender implements TCAppender {

    private final StringBuilder log = new StringBuilder();

    public void append(LogLevel level, Object message, Throwable throwable) {
      log.append(message).append("\n");
    }

    String takeLoggedMessages() {
      return log.toString().trim();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    new CyclicBarrierSpec().visit(visitor, config);

    TransparencyClassSpec spec = config.getOrCreateSpec(MyObject.class.getName());
    config.addWriteAutolock("* " + MyObject.class.getName() + "*.*(..)");

    spec = config.getOrCreateSpec(MyObject1.class.getName());
    spec.setCallMethodOnLoad("initialize");
    config.addReadAutolock("* " + MyObject1.class.getName() + "*.*(..)");

    spec = config.getOrCreateSpec(OnLoadReadLockTestApp.class.getName());
    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");
  }

  private static class MyObject {
    private MyObject1 object;

    public synchronized MyObject1 getObject() {
      return object;
    }

    public synchronized void setObject(MyObject1 object) {
      this.object = object;
    }
  }

  private static class MyObject1 {
    @SuppressWarnings("unused")
    private List list;

    public MyObject1() {
      initialize();
    }

    public void initialize() {
      synchronized (this) {
        list = new ArrayList();
      }
    }

  }
}
