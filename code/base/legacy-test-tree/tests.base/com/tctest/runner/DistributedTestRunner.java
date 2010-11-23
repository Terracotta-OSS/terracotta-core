/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.runner;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.object.TestClientConfigHelperFactory;
import com.tc.server.AbstractServerFactory;
import com.tc.server.TCServer;
import com.tc.simulator.app.ApplicationBuilder;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.DSOApplicationBuilder;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.container.Container;
import com.tc.simulator.container.ContainerConfig;
import com.tc.simulator.container.ContainerResult;
import com.tc.simulator.container.ContainerStateFactory;
import com.tc.simulator.container.IsolationClassLoaderFactory;
import com.tc.simulator.control.Control;
import com.tc.simulator.listener.ResultsListener;
import com.tc.test.MultipleServerManager;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersTestSetupManager;
import com.tcsimulator.ControlImpl;
import com.tcsimulator.container.ContainerStateFactoryObject;
import com.tcsimulator.listener.QueuePrinter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Takes an application configuration and some parameters and runs a single-vm, multi-node (multi-classloader) test.
 */
public class DistributedTestRunner implements ResultsListener {
  private static final boolean                          DEBUG       = true;

  private final boolean                                 startServer;

  private final Class                                   applicationClass;
  private final ApplicationConfig                       applicationConfig;
  private final ContainerConfig                         containerConfig;
  private final ResultsListener[]                       resultsListeners;
  private final TestClientConfigHelperFactory           configHelperFactory;
  private final ContainerStateFactory                   containerStateFactory;
  private final TestGlobalIdGenerator                   globalIdGenerator;
  private final TCServer                                server;
  private final List                                    errors      = new ArrayList();
  private final List                                    results     = new ArrayList();
  private final DistributedTestRunnerConfig             config;
  private final TestConfigurationSetupManagerFactory configFactory;
  private Control                                       control;
  private boolean                                       startTimedOut;
  private boolean                                       executionTimedOut;
  private final int                                     clientCount;
  private final int                                     applicationInstanceCount;
  private final LinkedQueue                             statsOutputQueue;
  private final QueuePrinter                            statsOutputPrinter;

  private final Map                                     optionalAttributes;

  private final boolean                                 isMutatorValidatorTest;
  private final int                                     validatorCount;
  private final boolean                                 isMultipleServerTest;
  private final MultipleServerManager                   serverManager;

  private final int                                     adaptedMutatorCount;
  private final int                                     adaptedValidatorCount;
  private final Map                                     adapterMap;

  private ApplicationBuilder[]                          applicationBuilders;
  private Container[]                                   containers;
  private Container[]                                   validatorContainers;
  private final List                                    postActions = new ArrayList();

  /**
   * @param applicationClass Class of the application to be executed. It should implement the static method required by
   *        ClassLoaderConfigVisitor.
   * @param applicationConfig Configuration object for the test application.
   * @param nodeCount Number of classloaders to create.
   * @param applicationsPerNode Number of application instances per classloader to execute. This counts as number of
   *        threads per classloader.
   */
  public DistributedTestRunner(DistributedTestRunnerConfig config,
                               TestConfigurationSetupManagerFactory configFactory,
                               TestClientConfigHelperFactory configHelperFactory, Class applicationClass,
                               Map optionalAttributes, ApplicationConfig applicationConfig, boolean startServer,
                               boolean isMutatorValidatorTest, boolean isMultipleServerTest,
                               MultipleServerManager serverManager, TransparentAppConfig transparentAppConfig)
      throws Exception {
    this.optionalAttributes = optionalAttributes;
    this.clientCount = transparentAppConfig.getClientCount();
    this.applicationInstanceCount = transparentAppConfig.getApplicationInstancePerClientCount();
    this.startServer = startServer;
    this.config = config;
    this.configFactory = configFactory;
    this.configHelperFactory = configHelperFactory;
    this.isMutatorValidatorTest = isMutatorValidatorTest;
    this.validatorCount = transparentAppConfig.getValidatorCount();
    this.isMultipleServerTest = isMultipleServerTest;
    this.serverManager = serverManager;
    this.globalIdGenerator = new TestGlobalIdGenerator();
    this.applicationClass = applicationClass;
    this.applicationConfig = applicationConfig;
    this.containerConfig = newContainerConfig();
    this.statsOutputQueue = new LinkedQueue();
    this.statsOutputPrinter = new QueuePrinter(this.statsOutputQueue, System.out);
    this.containerStateFactory = new ContainerStateFactoryObject(statsOutputQueue);

    adaptedMutatorCount = transparentAppConfig.getAdaptedMutatorCount();
    adaptedValidatorCount = transparentAppConfig.getAdaptedValidatorCount();
    adapterMap = (Map) transparentAppConfig.getAttributeObject(TransparentAppConfig.adapterMapKey);
    if (adapterMap == null) {
      debugPrintln("***** adapter map is null!");
    }

    this.resultsListeners = newResultsListeners(this.clientCount + this.validatorCount);

    initializedClients();

    if (this.startServer) {
      server = instantiateTCServer();
    } else {
      server = null;
    }
  }

  public void addPostAction(PostAction action) {
    this.postActions.add(action);
  }

  protected TCServer instantiateTCServer() {
    try {
      AbstractServerFactory serverFactory = AbstractServerFactory.getFactory();
      return serverFactory.createServer(configFactory.getL2TVSConfigurationSetupManager(),
                                        new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(TCServer.class))));

    } catch (Exception e) {
      throw new RuntimeException("Error while instantiating TCServerImpl from DistributedTestRunner", e);
    }
  }

  private void initializedClients() throws Exception {
    applicationBuilders = newApplicationBuilders(this.clientCount + this.validatorCount);
    initializeContainers();
  }

  private void initializeContainers() {
    this.control = newContainerControl();
    createMutators();
    createValidators();
  }

  private void createMutators() {
    containers = new Container[clientCount];
    boolean mutator = true;
    for (int i = 0; i < containers.length; i++) {
      containers[i] = new Container(this.containerConfig, this.containerStateFactory, this.globalIdGenerator,
                                    this.control, this.resultsListeners[i], this.applicationBuilders[i],
                                    this.isMutatorValidatorTest, mutator);
    }
  }

  private void createValidators() {
    boolean isMutator = false;
    validatorContainers = new Container[validatorCount];
    for (int i = 0; i < validatorContainers.length; i++) {
      validatorContainers[i] = new Container(containerConfig, containerStateFactory, globalIdGenerator, control,
                                             resultsListeners[i + clientCount], applicationBuilders[i + clientCount],
                                             isMutatorValidatorTest, isMutator);
    }
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  public void startServer() {
    try {
      startStatsOutputPrinterThread();
      if (this.startServer) {
        this.server.start();
      }
    } catch (Throwable t) {
      notifyError(new ErrorContext(t));
    }
  }

  private void startStatsOutputPrinterThread() {
    Thread statsOutputPrinterThread = new Thread(this.statsOutputPrinter);
    statsOutputPrinterThread.setDaemon(true);
    statsOutputPrinterThread.start();
  }

  public void run() {
    try {
      debugPrintln("***** control=[" + control.toString() + "]");

      for (Container container : containers) {
        new Thread(container).start();
      }

      // wait for all mutators to finish before starting the validators
      if (isMutatorValidatorTest) {
        final boolean mutationComplete = this.control.waitForMutationComplete(config.executionTimeout());

        if (!mutationComplete) {
          notifyExecutionTimeout();
        }

        if (isMultipleServerTest) {
          checkForErrors();
        }

        for (Container validatorContainer : validatorContainers) {
          new Thread(validatorContainer).start();
        }

        if (isMultipleServerTest && shouldCrashActiveServersAfterMutate()) {
          Thread.sleep(5000);
          debugPrintln("***** DTR: Crashing active server");
          serverManager.crashActiveServers();
          debugPrintln("***** DTR: Notifying the test-wide control");
          control.notifyValidationStart();
        }
      }

      if (isMultipleServerTest) {
        checkForErrors();
      }

      final boolean complete = this.control.waitForAllComplete(this.config.executionTimeout());

      if (!complete) {
        notifyExecutionTimeout();
      }

      if (isMultipleServerTest) {
        checkForErrors();
      }
    } catch (Throwable t) {
      notifyError(new ErrorContext(t));
    } finally {

      try {
        if (this.startServer) {
          executePostActions();
        }
      } catch (Exception e) {
        notifyError(new ErrorContext(e));
      }
    }
  }

  // spawn new control to re-run the test
  public void rerun() {
    initializeContainers();

    try {
      debugPrintln("***** control=[" + control.toString() + "]");

      for (Container container : containers) {
        new Thread(container).start();
      }

      // wait for all mutators to finish before starting the validators
      if (isMutatorValidatorTest) {
        final boolean mutationComplete = this.control.waitForMutationComplete(config.executionTimeout());

        if (!mutationComplete) {
          notifyExecutionTimeout();
        }

        if (isMultipleServerTest) {
          checkForErrors();
        }

        for (Container validatorContainer : validatorContainers) {
          new Thread(validatorContainer).start();
        }

        if (isMultipleServerTest && shouldCrashActiveServersAfterMutate()) {
          Thread.sleep(5000);
          debugPrintln("***** DTR: Crashing active server");
          serverManager.crashActiveServers();
          debugPrintln("***** DTR: Notifying the test-wide control");
          control.notifyValidationStart();
        }
      }

      if (isMultipleServerTest) {
        checkForErrors();
      }

      final boolean complete = this.control.waitForAllComplete(this.config.executionTimeout());

      if (!complete) {
        notifyExecutionTimeout();
      }

      if (isMultipleServerTest) {
        checkForErrors();
      }
    } catch (Throwable t) {
      notifyError(new ErrorContext(t));
    } finally {

      try {
        if (this.startServer) {
          executePostActions();
        }
      } catch (Exception e) {
        notifyError(new ErrorContext(e));
      }
    }
  }

  private void executePostActions() throws Exception {
    for (Iterator iter = postActions.iterator(); iter.hasNext();) {
      PostAction postAction = (PostAction) iter.next();
      postAction.execute();
    }
  }

  private boolean shouldCrashActiveServersAfterMutate() {
    MultipleServersTestSetupManager testSetupManager = serverManager.getMultipleServersTestSetupManager();
    if (testSetupManager.getServerCrashMode().equals(MultipleServersCrashMode.CRASH_AFTER_MUTATE)) return true;
    return false;
  }

  private void checkForErrors() throws Exception {
    List l = serverManager.getErrors();
    if (l.size() > 0) {
      for (Iterator iter = l.iterator(); iter.hasNext();) {
        Exception e = (Exception) iter.next();
        e.printStackTrace();
      }
      throw (Exception) l.get(l.size() - 1);
    }
  }

  public boolean success() {
    synchronized (results) {
      for (Iterator i = results.iterator(); i.hasNext();) {
        ContainerResult result = (ContainerResult) i.next();
        if (!result.success()) {
          System.out.print(result);
          return false;
        }
      }
    }

    if (errors.size() > 0) {
      System.err.println(errors.size() + " ERRORS PRESENT");
      return false;
    } else if (startTimedOut) {
      System.err.println("START TIMED OUT; timeout=" + this.config.startTimeout());
      return false;
    } else if (executionTimedOut) {
      System.err.println("EXECUTION TIMED OUT; timeout=" + this.config.executionTimeout());
      return false;
    } else if (results.size() != (containers.length + validatorContainers.length)) {
      System.err.println(results.size() + " results present, EXPECTING " + containers.length);
      return false;
    } else {
      return true;
    }

    // unreachable
  }

  public List getErrors() {
    synchronized (errors) {
      return new ArrayList(errors);
    }
  }

  public boolean startTimedOut() {
    return startTimedOut;
  }

  public boolean executionTimedOut() {
    return executionTimedOut;
  }

  private ContainerConfig newContainerConfig() {
    return new ContainerConfig() {

      public int getApplicationInstanceCount() {
        return applicationInstanceCount;
      }

      public long getContainerStartTimeout() {
        return config.startTimeout();
      }

      public long getApplicationStartTimeout() {
        return config.startTimeout();
      }

      public long getApplicationExecutionTimeout() {
        return config.executionTimeout();
      }

      public boolean isMaster() {
        return false;
      }

    };
  }

  private Control newContainerControl() {
    boolean crashActiveServerAfterMutate = isMultipleServerTest ? shouldCrashActiveServersAfterMutate() : false;
    return new ControlImpl(this.clientCount, validatorCount, crashActiveServerAfterMutate);
  }

  private ResultsListener[] newResultsListeners(int count) {
    ResultsListener[] rv = new ResultsListener[count];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = this;
    }
    return rv;
  }

  private ApplicationBuilder[] newApplicationBuilders(int totalClientCount) throws Exception {
    debugPrintln("***** Creating " + totalClientCount + " DSOApplicationBuilders");
    ApplicationBuilder[] rv = new ApplicationBuilder[totalClientCount];

    for (int i = 0; i < rv.length; i++) {
      L1ConfigurationSetupManager l1ConfigManager;
      l1ConfigManager = this.configFactory.getL1TVSConfigurationSetupManager();
      l1ConfigManager.setupLogging();
      if (adapterMap != null
          && ((i < clientCount && i < adaptedMutatorCount) || (i >= clientCount && i < (adaptedValidatorCount + clientCount)))) {
        IsolationClassLoaderFactory configHelperFactoryData = new IsolationClassLoaderFactory(configHelperFactory,
                                                                                              applicationClass,
                                                                                              optionalAttributes,
                                                                                              l1ConfigManager,
                                                                                              adapterMap);
        rv[i] = new DSOApplicationBuilder(configHelperFactoryData, this.applicationConfig);

        for (Iterator iter = adapterMap.keySet().iterator(); iter.hasNext();) {
          String adapteeName = (String) iter.next();
          Class adapterClass = (Class) adapterMap.get(adapteeName);
          debugPrintln("***** Adding adapter to appBuilder=[" + i + "] adapteeName=[" + adapteeName
                       + "] adapterClass=[" + adapterClass.getName() + "]");
        }
      } else {
        IsolationClassLoaderFactory configHelperFactoryData = new IsolationClassLoaderFactory(configHelperFactory,
                                                                                              applicationClass,
                                                                                              optionalAttributes,
                                                                                              l1ConfigManager, null);
        rv[i] = new DSOApplicationBuilder(configHelperFactoryData, this.applicationConfig);
        debugPrintln("***** Creating normal DSOApplicationBuilder: i=[" + i + "]");
      }
    }
    return rv;
  }

  /*********************************************************************************************************************
   * ResultsListener interface
   */

  public void setGlobalId(long globalId) {
    return;
  }

  public void notifyStartTimeout() {
    this.startTimedOut = true;
  }

  public void notifyExecutionTimeout() {
    this.executionTimedOut = true;
  }

  public void notifyError(ErrorContext ectxt) {
    synchronized (this.errors) {
      ectxt.dump(System.err);
      this.errors.add(ectxt);
    }
  }

  public void notifyResult(Object result) {
    synchronized (this.results) {
      this.results.add(result);
    }
  }

  public void dumpServer() {
    if (server != null && startServer) {
      System.out.println("Dumping intra-process server");
      server.dump();
    }
  }

}
