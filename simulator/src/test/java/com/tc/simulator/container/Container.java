/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
import com.tc.simulator.listener.MutationCompletionListener;
import com.tc.simulator.listener.OutputListener;
import com.tc.simulator.listener.ResultsListener;
import com.tc.simulator.listener.StatsListenerFactory;
import com.tc.util.Assert;
import com.tcsimulator.ControlImpl;
import com.tcsimulator.listener.ApplicationListenerProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Container implements Runnable {
  private static final boolean             DEBUG = false;

  private final ContainerConfig            config;
  private final ContainerState             containerState;
  private final GlobalIdGenerator          idGenerator;
  private final Control                    control;
  private final ResultsListener            resultsListener;
  private final ApplicationBuilder         applicationBuilder;
  private final String                     globalId;
  private final boolean                    isMutateValidateTest;
  private final boolean                    isMutator;
  private final MutationCompletionListener mutationCompletionListener;
  private final ControlImpl                applicationControl;

  public Container(ContainerConfig config, ContainerStateFactory containerStateFactory, GlobalIdGenerator idGenerator,
                   Control control, ResultsListener resultsListener, ApplicationBuilder applicationBuilder) {
    this(config, containerStateFactory, idGenerator, control, resultsListener, applicationBuilder, false, true);
  }

  public Container(ContainerConfig config, ContainerStateFactory containerStateFactory, GlobalIdGenerator idGenerator,
                   Control control, ResultsListener resultsListener, ApplicationBuilder applicationBuilder,
                   boolean isMutateValidateTest, boolean isMutator) {
    this.config = config;
    this.idGenerator = idGenerator;
    this.control = control;
    this.resultsListener = resultsListener;
    this.applicationBuilder = applicationBuilder;
    this.isMutateValidateTest = isMutateValidateTest;
    this.isMutator = isMutator;
    this.globalId = this.idGenerator.nextId() + "";

    applicationControl = new ControlImpl(this.config.getApplicationInstanceCount(), this.control);
    applicationControl.setExecutionTimeout(this.config.getApplicationExecutionTimeout());

    mutationCompletionListener = applicationControl;

    this.containerState = containerStateFactory.newContainerState(this.globalId);
    Assert.assertNoNullElements(new Object[] { this.config, this.idGenerator, this.control, this.resultsListener,
        this.applicationBuilder });
  }

  /**
   * Make applications go.
   */
  public synchronized void run() {
    debugPrintln("isMutateValidateTest=[" + isMutateValidateTest + "]");

    SynchronizedBoolean isRunning = new SynchronizedBoolean(true);
    try {
      if (!validateConfig()) return;
      debugPrintln("***** config is alright");

      if (isMutator) {
        debugPrintln("******* isMutator=[" + isMutator + "]");
        if (!waitForStart()) return;
      } else {
        if (!waitForMutationCompleteTestWide()) return;
      }

      ContainerExecutionInstance containerExecution = new ContainerExecutionInstance();

      startInstances(containerExecution);

      if (isMutateValidateTest) {
        if (isMutator) {
          // wait for all app instances to complete mutation
          if (!waitForMutationComplete()) return;
          control.notifyMutationComplete();
        }
        if (!waitForValidationStart()) return;
        control.notifyValidationStart();
      }

      if (!waitForAllComplete()) return;

      notifyResult(containerExecution);

    } catch (Throwable t) {
      notifyError("Unexpected error executing application.", t);
    } finally {
      isRunning.set(false);
      control.notifyComplete();
      try {
        this.control.waitForAllComplete(this.config.getApplicationExecutionTimeout());
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
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
    println("Waiting for all containers to start...");
    this.control.waitForStart();
    rv = true;
    println("Done waiting for all containers to start.");
    return rv;
  }

  private boolean waitForMutationCompleteTestWide() throws InterruptedException {
    boolean rv = false;
    println("Waiting for all containers to finish mutation...");
    this.control.waitForMutationComplete(config.getApplicationExecutionTimeout());
    rv = true;
    println("Done waiting for all containers to finish mutation.");
    return rv;
  }

  /*********************************************************************************************************************
   * Private stuff
   */

  private ApplicationExecutionInstance newExecutionInstance(ContainerExecutionInstance containerExecution)
      throws ApplicationInstantiationException {
    String appId = this.idGenerator.nextId() + "";
    System.err.println("Creating new execution instance: " + appId);
    OutputListener outputListener = this.containerState.newOutputListener();
    ApplicationExecutionInstance executionInstance = new ApplicationExecutionInstance(containerExecution,
                                                                                      applicationControl,
                                                                                      this.containerState);

    ApplicationListenerProvider appListeners = new ApplicationListenerProvider(outputListener, executionInstance,
                                                                               this.mutationCompletionListener,
                                                                               this.containerState);

    if (isMutateValidateTest) {
      debugPrintln("****** Container:  appId=[" + appId + "] isMutator=[" + isMutator + "] isMutateValidateTest=["
                   + isMutateValidateTest + "]");
      applicationBuilder.setAppConfigAttribute(appId, "" + isMutator);
    }

    Application application = applicationBuilder.newApplication(appId, appListeners);
    executionInstance.setApplication(application);

    return executionInstance;
  }

  private void println(String msg) {
    System.out.println("Container: " + msg);
  }

  private void startInstances(ContainerExecutionInstance containerExecution) throws ApplicationInstantiationException {
    println("Starting application execution...");
    for (int i = 0; i < config.getApplicationInstanceCount(); i++) {
      println("execution " + (i + 1) + " of " + config.getApplicationInstanceCount());
      ApplicationExecutionInstance executionInstance = newExecutionInstance(containerExecution);
      containerExecution.addExecution(executionInstance);
      executionInstance.start();
    }
    println("All application executions are started.");
  }

  private boolean waitForAllComplete() throws InterruptedException {
    println("Waiting for all applications to complete.  Timeout: " + config.getApplicationExecutionTimeout());
    if (!applicationControl.waitForAllComplete(config.getApplicationExecutionTimeout())) {
      resultsListener.notifyExecutionTimeout();
      notifyFailure();
      println("Application execution timed out.");
      return false;
    }
    println("Application execution completed.");
    return true;
  }

  private boolean waitForMutationComplete() throws InterruptedException {
    println("Waiting for all applications to finish mutation.  Timeout: " + config.getApplicationExecutionTimeout());
    if (!applicationControl.waitForMutationComplete(config.getApplicationExecutionTimeout())) {
      resultsListener.notifyExecutionTimeout();
      notifyFailure();
      println("Application execution timed out.");
      return false;
    }
    println("Application mutation completed.");
    return true;
  }

  private boolean waitForValidationStart() throws InterruptedException {
    println("Waiting for all applications/participants to be ready to begin validation.  Timeout: "
            + config.getApplicationExecutionTimeout());
    if (!applicationControl.waitForValidationStart(config.getApplicationExecutionTimeout())) {
      resultsListener.notifyExecutionTimeout();
      notifyFailure();
      println("Application execution timed out.");
      return false;
    }
    println("Application ready to start validation.");
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

  private ApplicationRunner newApplicationRunner(Control appControl, ResultsListener appRunnerResultsListener,
                                                 Application application, StatsListenerFactory statsListenerFactory) {

    ApplicationRunnerConfig exeConfig = new ApplicationRunnerConfig() {
      public long getStartTimeout() {
        return config.getApplicationStartTimeout();
      }
    };

    ApplicationRunner runner = new ApplicationRunner(exeConfig, appControl, appRunnerResultsListener, application,
                                                     statsListenerFactory);

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
    private final Control                    appControl;
    private Application                      application;
    private Object                           result;
    private final StatsListenerFactory       statsListenerFactory;

    ApplicationExecutionInstance(ContainerExecutionInstance containerExecution, Control control,
                                 StatsListenerFactory statsListenerFactory) {
      this.containerExecution = containerExecution;
      this.appControl = control;
      this.statsListenerFactory = statsListenerFactory;
    }

    void setApplication(Application app) {
      this.application = app;
    }

    void start() {
      ApplicationRunner runner = newApplicationRunner(appControl, this, this.application, this.statsListenerFactory);

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