/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.app.MockApplication;
import com.tc.simulator.control.MockControl;
import com.tc.simulator.listener.MockResultsListener;
import com.tc.simulator.listener.MockStatsListener;
import com.tc.simulator.listener.StatsListener;
import com.tc.simulator.listener.StatsListenerFactory;

import java.util.Properties;

import junit.framework.TestCase;

public class ExecutionInstanceTest extends TestCase {

  ApplicationRunner                   executionInstance;
  private MockExecutionInstanceConfig config;
  private MockControl                 control;
  private MockResultsListener         resultsListener;
  private MockApplication             application;

  public void setUp() throws Exception {
    this.config = new MockExecutionInstanceConfig();
    this.config.startTimeout = 1;
    this.control = new MockControl();
    this.resultsListener = new MockResultsListener();
    this.application = new MockApplication();
    this.executionInstance = new ApplicationRunner(config, control, this.resultsListener, application,
                                                   new TestStatsListenerFactory());
  }

  public void testStartTimeoutLessThanOne() throws Exception {
    this.config.startTimeout = 0;
    this.executionInstance.run();
    assertEquals(Boolean.FALSE, this.resultsListener.result);
    assertEquals(1, resultsListener.errors.size());
    ErrorContext err = (ErrorContext) resultsListener.errors.get(0);
    assertTrue(err.getThrowable() instanceof ApplicationRunnerConfigException);
    assertTrue(control.notifyCompleteCalled);
  }

  public void testBasicRun() throws Exception {
    this.executionInstance.run();
    assertTrue(this.control.waitForStartCalled);
    assertTrue(this.control.notifyCompleteCalled);

    assertEquals(0, this.resultsListener.errors.size());
    assertEquals(Boolean.TRUE, this.resultsListener.result);
    assertTrue(control.notifyCompleteCalled);
  }

  public void testExceptionInApplication() throws Exception {
    this.application.throwException = true;
    this.application.exception = new RuntimeException("This is a test exception.  Don't worry about me.");
    this.executionInstance.run();
    assertEquals(1, this.resultsListener.errors.size());
    ErrorContext err = (ErrorContext) this.resultsListener.errors.get(0);
    assertSame(application.exception, err.getThrowable());
    assertEquals(Boolean.FALSE, this.resultsListener.result);
    assertTrue(control.notifyCompleteCalled);
  }

  private static class MockExecutionInstanceConfig implements ApplicationRunnerConfig {

    public long startTimeout;

    public long getStartTimeout() {
      return this.startTimeout;
    }

  }

  private static final class TestStatsListenerFactory implements StatsListenerFactory {

    public StatsListener newStatsListener(Properties properties) {
      return new MockStatsListener();
    }
    
  }
  
}