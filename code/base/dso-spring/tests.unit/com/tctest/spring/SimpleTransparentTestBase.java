/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tcspring.DistributableBeanFactory;
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;

public abstract class SimpleTransparentTestBase extends TransparentTestBase {

  protected abstract int getNodeCount();

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(getNodeCount()).setApplicationInstancePerClientCount(getExecutionCount())
        .setIntensity(getLoopIterations());
    initializeTestRunner();
  }

  protected final int getLoopIterations() {
    return 1;
  }

  protected final int getExecutionCount() {
    return 1;
  }

  public abstract static class AbstractSimpleTransparentApp extends AbstractTransparentApp {
    public AbstractSimpleTransparentApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public final void run() {
      Thread t = Thread.currentThread();
      ClassLoader cl = t.getContextClassLoader();

      try {
        t.setContextClassLoader(getClass().getClassLoader());

        doIt();

      } catch (Throwable e) {
        notifyError(e);

      } finally {
        t.setContextClassLoader(cl);

      }
    }

    protected abstract void doIt() throws Throwable;

  }

  protected static void assertDistributed(ClassPathXmlApplicationContext ctx, Object bean) {
    DistributableBeanFactory distributableBeanFactory = ((DistributableBeanFactory) ctx.getBeanFactory());
    final boolean isDistributed = distributableBeanFactory.isDistributed(bean);
    assertTrue("Expected to be distributed" + bean, isDistributed);
  }

}
