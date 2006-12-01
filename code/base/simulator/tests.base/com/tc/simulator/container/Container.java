/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.simulator.app.Application;
import com.tc.simulator.app.ApplicationBuilder;
import com.tc.simulator.app.ApplicationInstantiationException;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.app.GlobalIdGenerator;
import com.tc.simulator.control.Control;
import com.tc.simulator.control.TCBrokenBarrierException;
import com.tc.simulator.listener.OutputListener;
import com.tc.simulator.listener.ResultsListener;
import com.tc.simulator.listener.StatsListenerFactory;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tcsimulator.ControlImpl;
import com.tcsimulator.listener.ApplicationListenerProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Container implements Runnable {

  private final ContainerConfig    config;
  private final ContainerState     containerState;
  private final GlobalIdGenerator  idGenerator;
  private final Control            control;
  private final ResultsListener    resultsListener;
  private final ApplicationBuilder applicationBuilder;
  private final String             globalId;

  public Container(ContainerConfig config, ContainerStateFactory containerStateFactory, GlobalIdGenerator idGenerator,
                   Control control, ResultsListener resultsListener, ApplicationBuilder applicationBuilder) {
    this.config = config;
    this.idGenerator = idGenerator;
    this.control = control;
    this.resultsListener = resultsListener;
    this.applicationBuilder = applicationBuilder;
    this.globalId = this.idGenerator.nextId() + "";

    this.containerState = containerStateFactory.newContainerState(this.globalId);
    Assert.assertNoNullElements(new Object[] { this.config, this.idGenerator, this.control, this.resultsListener,
        this.applicationBuilder });
  }

  /**
   * Make applications go.
   */
  public synchronized void run() {
    Thread.currentThread().setContextClassLoader(applicationBuilder.getContextClassLoader());

    SynchronizedBoolean isRunning = new SynchronizedBoolean(true);
    try {
      if (!validateConfig()) return;
      if (!waitForStart()) return;
      Control applicationControl = new ControlImpl(this.config.getApplicationInstanceCount(), this.config
          .getApplicationInstanceCount());
      ContainerExecutionInstance containerExecution = new ContainerExecutionInstance();

      startInstances(containerExecution, applicationControl);

      if (!waitForAllComplete(applicationControl)) return;
      notifyResult(containerExecution);
    } catch (Throwable t) {
      notifyError("Unexpected error executing application.", t);
    } finally {
      isRunning.set(false);
      this.control.notifyComplete();
      try {
        this.control.waitForAllComplete(this.config.getApplicationExecutionTimeout());
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private boolean validateConfig() {
    if (this.config.getContainerStartTimeout() < 1) {
      notifyError(new ContainerConfigException("Container start timeout must be greater than zero.",
                                               ContainerConfigException.INVALID_CONTAINER_START_TIMEOUT));
      return false;
    }
    if (this.config.getApplicationStartTimeout() < 1) {
      notifyError(new ContainerConfigException("Application start timeout must be greater than zero.",
                                               ContainerConfigException.INVALID_APPLICATION_START_TIMEOUT));
      return false;
    }
    if (this.config.getApplicationExecutionTimeout() == 0) {
      notifyError(new ContainerConfigException("Application execution timeout must be greater than or less than zero.",
                                               ContainerConfigException.INVALID_APPLICATION_EXECUTION_TIMEOUT));
      return false;
    }
    if (this.config.getApplicationInstanceCount() < 1) {
      notifyError(new ContainerConfigException("Application instance count must be greater than zero.",
                                               ContainerConfigException.INVALID_APPLICATION_INSTANCE_COUNT));
      return false;
    }
    return true;
  }

  private boolean waitForStart() throws TCBrokenBarrierException, InterruptedException {
    boolean rv = false;
    try {
      println("Waiting for all containers to start...");
      this.control.waitForStart(this.config.getContainerStartTimeout());
      rv = true;
    } catch (TCTimeoutException e) {
      rv = false;
      this.resultsListener.notifyStartTimeout();
      notifyFailure();
    }
    println("Done waiting for all containers to start.");
    return rv;
  }

  /*********************************************************************************************************************
   * Private stuff
   */

  private ApplicationExecutionInstance newExecutionInstance(ContainerExecutionInstance containerExecution,
                                                            Control applicationControl)
      throws ApplicationInstantiationException {
    String appId = this.idGenerator.nextId() + "";
    System.err.println("Creating new execution instance: " + appId);
    OutputListener outputListener = this.containerState.newOutputListener();
    ApplicationExecutionInstance executionInstance = new ApplicationExecutionInstance(containerExecution,
                                                                                      applicationControl, this.containerState);

    ApplicationListenerProvider appListeners = new ApplicationListenerProvider(outputListener, executionInstance,
                                                                               this.containerState);
    Application application = applicationBuilder.newApplication(appId, appListeners);
    executionInstance.setApplication(application);

    return executionInstance;
  }

  private void println(String msg) {
    System.out.println("Container: " + msg);
  }

  private void startInstances(ContainerExecutionInstance containerExecution, Control applicationControl)
      throws ApplicationInstantiationException {
    println("Starting application execution...");
    for (int i = 0; i < config.getApplicationInstanceCount(); i++) {
      println("exeution " + (i + 1) + " of " + config.getApplicationInstanceCount());
      ApplicationExecutionInstance executionInstance = newExecutionInstance(containerExecution, applicationControl);
      containerExecution.addExecution(executionInstance);
      executionInstance.start();
    }
    println("All application executions are started.");
  }

  private boolean waitForAllComplete(Control applicationControl) throws InterruptedException {
    println("Waiting for all containers to complete.  Timeout: " + config.getApplicationExecutionTimeout());
    if (!applicationControl.waitForAllComplete(config.getApplicationExecutionTimeout())) {
      resultsListener.notifyExecutionTimeout();
      notifyFailure();
      println("Application execution timed out.");
      return false;
    }
    println("Application execution completed.");
    return true;
  }

  private void notifyError(Throwable t) {
    notifyError("", t);
  }

  private void notifyError(String message, Throwable t) {
    notifyError(new ErrorContext(message, t));
  }

  private void notifyError(ErrorContext ctxt) {
    ctxt.dump(System.err);
    this.resultsListener.notifyError(ctxt);
    notifyFailure();
  }

  private void notifyFailure() {
    System.err.println(Thread.currentThread() + ": failure");
    Thread.dumpStack();
    this.resultsListener.notifyResult(new ContainerResult());
  }

  private void notifyResult(ContainerExecutionInstance containerExecution) {
    this.resultsListener.notifyResult(new ContainerResult(containerExecution));
  }

  private ApplicationRunner newApplicationRunner(Control applicationControl, ResultsListener appRunnerResultsListener,
                                                 Application application, StatsListenerFactory statsListenerFactory) {

    ApplicationRunnerConfig exeConfig = new ApplicationRunnerConfig() {
      public long getStartTimeout() {
        return config.getApplicationStartTimeout();
      }
    };

    ApplicationRunner runner = new ApplicationRunner(exeConfig, applicationControl, appRunnerResultsListener,
                                                     application, statsListenerFactory);

    return runner;
  }

  /*********************************************************************************************************************
   * Helper classes.
   */

  static final class ContainerExecutionInstance {
    private final List executions                  = new ArrayList();
    private boolean    applicationStartTimeout     = false;
    private boolean    applicationExecutionTimeout = false;

    synchronized void addExecution(ApplicationExecutionInstance execution) {
      this.executions.add(execution);
    }

    synchronized int getExecutionCount() {
      return this.executions.size();
    }

    synchronized void notifyApplicationStartTimeout() {
      this.applicationStartTimeout = true;
    }

    synchronized void notifyApplicationExecutionTimeout() {
      this.applicationExecutionTimeout = true;
    }

    private synchronized boolean compileResults() {
      boolean result = true;
      for (Iterator i = executions.iterator(); i.hasNext();) {
        ApplicationExecutionInstance execution = (ApplicationExecutionInstance) i.next();
        if (!execution.application.interpretResult(execution.result)) result = false;
      }
      return result;
    }

    synchronized boolean getResult() {
      boolean compiledResults = compileResults();
      System.err.println("start timeout: " + this.applicationStartTimeout);
      System.err.println("execution timeout: " + this.applicationExecutionTimeout);
      System.err.println("compiled results: " + compiledResults);
      return !this.applicationStartTimeout && !this.applicationExecutionTimeout && compiledResults;
    }
  }

  final class ApplicationExecutionInstance implements ResultsListener {
    private final ContainerExecutionInstance containerExecution;
    private final Control                    applicationControl;
    private Application                      application;
    private Object                           result;
    private final StatsListenerFactory              statsListenerFactory;

    ApplicationExecutionInstance(ContainerExecutionInstance containerExecution, Control control,
                                 StatsListenerFactory statsListenerFactory) {
      this.containerExecution = containerExecution;
      this.applicationControl = control;
      this.statsListenerFactory = statsListenerFactory;
    }

    void setApplication(Application app) {
      this.application = app;
    }

    void start() {
      ApplicationRunner runner = newApplicationRunner(applicationControl, this, this.application, this.statsListenerFactory);

      ClassLoader loader = application.getClass().getClassLoader();
      Thread thread = new Thread(runner);
      thread.setContextClassLoader(loader);
      thread.start();
    }

    Application getApplication() {
      return this.application;
    }

    public void setGlobalId(long globalId) {
      return;
    }

    public void notifyStartTimeout() {
      this.containerExecution.notifyApplicationStartTimeout();
    }

    public void notifyExecutionTimeout() {
      this.containerExecution.notifyApplicationExecutionTimeout();
    }

    public void notifyError(ErrorContext ectxt) {
      Container.this.notifyError(ectxt);
    }

    public void notifyResult(Object theResult) {
      this.result = theResult;
    }

    Object getResult() {
      return this.result;
    }

  }

}