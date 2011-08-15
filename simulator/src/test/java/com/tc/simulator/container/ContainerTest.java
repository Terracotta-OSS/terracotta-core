/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.simulator.app.Application;
import com.tc.simulator.app.ApplicationBuilder;
import com.tc.simulator.app.ApplicationInstantiationException;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.app.MockApplication;
import com.tc.simulator.control.MockControl;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.simulator.listener.MockResultsListener;
import com.tcsimulator.container.ContainerStateFactoryObject;
import com.tctest.runner.TestGlobalIdGenerator;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class ContainerTest extends TestCase {
  private Container              container;
  private MockContainerConfig    config;
  private ContainerStateFactory  containerStateFactory;
  private TestGlobalIdGenerator  globalIdGenerator;
  private MockControl            control;
  private MockApplicationBuilder applicationBuilder;
  private MockResultsListener    resultsListener;

  protected void setUp() throws Exception {
    super.setUp();

    this.config = new MockContainerConfig();
    this.config.containerStartTimeout = 10 * 1000;
    this.config.applicationStartTimeout = 10 * 1000;
    this.config.applicationExecutionTimeout = 10 * 1000;
    this.config.instanceCount = 10;

    this.containerStateFactory = new ContainerStateFactoryObject(new LinkedQueue());
    this.globalIdGenerator = new TestGlobalIdGenerator();
    this.control = new MockControl();
    this.resultsListener = new MockResultsListener();
    this.applicationBuilder = new MockApplicationBuilder();
    this.applicationBuilder.result = true;
    this.container = new Container(this.config, this.containerStateFactory, this.globalIdGenerator, this.control,
                                   this.resultsListener, this.applicationBuilder);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    assertNotifyCompleteCalled();
  }

  public void testInvalidContainerStartTimeout() throws Exception {
    this.config.containerStartTimeout = 0;
    this.container.run();
    assertResult(false);
    assertContainerConfigException(ContainerConfigException.INVALID_CONTAINER_START_TIMEOUT);

  }

  public void testInvalidApplicationStartTimeout() throws Exception {
    this.config.applicationStartTimeout = 0;
    this.container.run();
    assertResult(false);
    assertContainerConfigException(ContainerConfigException.INVALID_APPLICATION_START_TIMEOUT);
  }

  public void testInvalidApplicationExecutionTimeout() throws Exception {
    this.config.applicationExecutionTimeout = 0;
    this.container.run();
    assertResult(false);
    assertContainerConfigException(ContainerConfigException.INVALID_APPLICATION_EXECUTION_TIMEOUT);
  }

  public void testApplicationInvalidInstanceCount() throws Exception {
    this.config.instanceCount = 0;
    this.container.run();
    assertResult(false);
    assertContainerConfigException(ContainerConfigException.INVALID_APPLICATION_INSTANCE_COUNT);
  }

  public void testExecutionTimeout() throws Exception {
    this.config.applicationExecutionTimeout = 500;
    this.applicationBuilder.waitInterval = 2000;

    this.container.run();

    assertTrue(resultsListener.notifyExecutionTimeoutCalled);
    assertFalse(resultsListener.notifyStartTimeoutCalled);
    assertTrue(control.notifyCompleteCalled);
    assertResult(false);
  }

  public void testAppBuilderThrowsException() throws Exception {
    this.applicationBuilder.throwException = true;
    this.applicationBuilder.exception = new ApplicationInstantiationException(new RuntimeException());

    this.container.run();

    assertResult(false);
    assertSame(this.applicationBuilder.exception, extractError());
  }

  public void testNormalRun() throws Exception {
    this.config.dumpErrors = true;
    this.config.dumpOutput = true;
    this.container.run();
    assertResult(true);
    ContainerResult result = extractResult();
    assertEquals(config.getApplicationInstanceCount(), result.getApplicationInstanceCount());
  }

  private void assertContainerConfigException(ContainerConfigException.Reason reason) {
    assertEquals(1, this.resultsListener.errors.size());
    ErrorContext err = (ErrorContext) this.resultsListener.errors.get(0);
    assertTrue(err.getThrowable() instanceof ContainerConfigException);
    ContainerConfigException e = (ContainerConfigException) err.getThrowable();
    assertSame(reason, e.getReason());
  }

  private void assertNotifyCompleteCalled() throws Exception {
    assertTrue(this.control.notifyCompleteCalled);
  }

  private void assertResult(boolean b) throws Exception {
    ContainerResult result = extractResult();
    assertNotNull(result);
    assertEquals(b, result.success());
  }

  private ContainerResult extractResult() throws Exception {
    return (ContainerResult) this.resultsListener.result;
  }

  private Throwable extractError() throws Exception {
    assertEquals(1, this.resultsListener.errors.size());
    ErrorContext err = (ErrorContext) this.resultsListener.errors.get(0);
    return err.getThrowable();
  }

  static class MockApplicationBuilder implements ApplicationBuilder {
    public List                              applications = new ArrayList();
    public boolean                           throwException;
    public ApplicationInstantiationException exception;
    public long                              waitInterval;
    public boolean                           result;

    public synchronized Application newApplication(String appId, ListenerProvider listenerProvider)
        throws ApplicationInstantiationException {

      if (this.throwException) throw exception;

      MockApplication rv = new MockApplication();
      rv.waitInterval = this.waitInterval;
      rv.result = result;
      applications.add(rv);
      return rv;
    }

    public ClassLoader getContextClassLoader() {
      return getClass().getClassLoader();
    }

    public void setAppConfigAttribute(String key, String value) {
      throw new AssertionError("This method needs to be implemented");
    }

  }

  public class MockContainerConfig implements ContainerConfig {
    public int     instanceCount;
    public long    containerStartTimeout;
    public long    applicationStartTimeout;
    public long    applicationExecutionTimeout;
    public boolean dumpErrors;
    public boolean dumpOutput;
    public boolean aggregate;
    public boolean stream;
    public boolean isMaster;

    public int getApplicationInstanceCount() {
      return this.instanceCount;
    }

    public long getContainerStartTimeout() {
      return this.containerStartTimeout;
    }

    public long getApplicationStartTimeout() {
      return this.applicationStartTimeout;
    }

    public long getApplicationExecutionTimeout() {
      return this.applicationExecutionTimeout;
    }

    public boolean dumpErrors() {
      return this.dumpErrors;
    }

    public boolean dumpOutput() {
      return this.dumpOutput;
    }

    public boolean aggregate() {
      return this.aggregate;
    }

    public boolean stream() {
      return this.stream;
    }

    public boolean isMaster() {
      return this.isMaster;
    }

  }

}