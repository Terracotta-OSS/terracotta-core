/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.runner;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.server.TCServerImpl;
import com.tc.simulator.app.ApplicationBuilder;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.DSOApplicationBuilder;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.container.Container;
import com.tc.simulator.container.ContainerConfig;
import com.tc.simulator.container.ContainerResult;
import com.tc.simulator.container.ContainerStateFactory;
import com.tc.simulator.control.Control;
import com.tc.simulator.listener.ResultsListener;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.util.concurrent.ThreadUtil;
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
  private static final boolean                          DEBUG   = false;

  private final boolean                                 startServer;

  private final Class                                   applicationClass;
  private final ApplicationConfig                       applicationConfig;
  private final ConfigVisitor                           configVisitor;
  private final ContainerConfig                         containerConfig;
  private final Control                                 control;
  private final ResultsListener[]                       resultsListeners;
  private final DSOClientConfigHelper                   configHelper;
  private final ContainerStateFactory                   containerStateFactory;
  private final TestGlobalIdGenerator                   globalIdGenerator;
  private final TCServerImpl                            server;
  private final List                                    errors  = new ArrayList();
  private final List                                    results = new ArrayList();
  private final DistributedTestRunnerConfig             config;
  private final TestTVSConfigurationSetupManagerFactory configFactory;
  private boolean                                       startTimedOut;
  private boolean                                       executionTimedOut;
  private final int                                     clientCount;
  private final int                                     applicationInstanceCount;
  private final LinkedQueue                             statsOutputQueue;
  private final QueuePrinter                            statsOutputPrinter;

  private final Map                                     optionalAttributes;

  private final boolean                                 isMutatorValidatorTest;
  private final int                                     validatorCount;
  private final boolean                                 isActivePassiveTest;
  private final ActivePassiveServerManager              serverManager;

  private ApplicationBuilder[]                          applicationBuilders;
  private Container[]                                   containers;
  private Container[]                                   validatorContainers;

  /**
   * @param applicationClass Class of the application to be executed. It should implement the static method required by
   *        ClassLoaderConfigVisitor.
   * @param applicationConfig Configuration object for the test application.
   * @param nodeCount Number of classloaders to create.
   * @param applicationsPerNode Number of application instances per classloader to execute. This counts as number of
   *        threads per classloader.
   */
  public DistributedTestRunner(DistributedTestRunnerConfig config,
                               TestTVSConfigurationSetupManagerFactory configFactory,
                               DSOClientConfigHelper configHelper, Class applicationClass, Map optionalAttributes,
                               ApplicationConfig applicationConfig, int clientCount, int applicationInstanceCount,
                               boolean startServer, boolean isMutatorValidatorTest, int validatorCount,
                               boolean isActivePassiveTest, ActivePassiveServerManager serverManager) throws Exception {
    this.optionalAttributes = optionalAttributes;
    this.clientCount = clientCount;
    this.applicationInstanceCount = applicationInstanceCount;
    this.startServer = startServer;
    this.config = config;
    this.configFactory = configFactory;
    this.configHelper = configHelper;
    this.isMutatorValidatorTest = isMutatorValidatorTest;
    this.validatorCount = validatorCount;
    this.isActivePassiveTest = isActivePassiveTest;
    this.serverManager = serverManager;
    this.globalIdGenerator = new TestGlobalIdGenerator();
    this.applicationClass = applicationClass;
    this.applicationConfig = applicationConfig;
    this.configVisitor = new ConfigVisitor();
    this.containerConfig = newContainerConfig();
    this.statsOutputQueue = new LinkedQueue();
    this.statsOutputPrinter = new QueuePrinter(this.statsOutputQueue, System.out);
    this.containerStateFactory = new ContainerStateFactoryObject(statsOutputQueue);
    this.control = newContainerControl();

    this.resultsListeners = newResultsListeners(this.clientCount + this.validatorCount);

    initializedClients();

    L2TVSConfigurationSetupManager manager;
    if (DEBUG) {
      this.configFactory.addServerToL2Config("testing", 8510, 8520);
      this.configFactory.activateConfigurationChange();
      manager = this.configFactory.createL2TVSConfigurationSetupManager("testing");
    } else {
      manager = this.configFactory.createL2TVSConfigurationSetupManager(null);
    }

    if (this.startServer) {
      server = new TCServerImpl(manager, new TCThreadGroup(new ThrowableHandler(TCLogging
          .getLogger(DistributedObjectServer.class))));
    } else {
      server = null;
    }
  }

  private void initializedClients() throws Exception {
    applicationBuilders = newApplicationBuilders(this.clientCount + this.validatorCount);

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

  public void run() {
    try {

      debugPrintln("***** control=[" + control.toString() + "]");

      Thread statsOutputPrinterThread = new Thread(this.statsOutputPrinter);
      statsOutputPrinterThread.setDaemon(true);
      statsOutputPrinterThread.start();

      visitApplicationClassLoaderConfig();
      if (this.startServer) {
        this.server.start();

        // ((SettableConfigItem) this.configFactory.l2DSOConfig().listenPort()).setValue(getServerPort());
        this.configFactory.addServerToL1Config(null, getActiveServerPort(), -1);
        this.configFactory.activateConfigurationChange();
      }

      for (int i = 0; i < containers.length; i++) {
        new Thread(containers[i]).start();
      }

      // wait for all mutators to finish before starting the validators
      if (isMutatorValidatorTest) {
        final boolean mutationComplete = this.control.waitForMutationComplete(config.executionTimeout());

        if (!mutationComplete) {
          notifyExecutionTimeout();
        }

        if (isActivePassiveTest) {
          checkForErrors();
        }

        for (int i = 0; i < validatorContainers.length; i++) {
          new Thread(validatorContainers[i]).start();
        }

        if (isActivePassiveTest && serverManager.crashActiveServerAfterMutate()) {
          Thread.sleep(5000);
          debugPrintln("***** DTR: Crashing active server");
          serverManager.crashActive();
          debugPrintln("***** DTR: Notifying the test-wide control");
          control.notifyValidationStart();
        }
      }

      if (isActivePassiveTest) {
        checkForErrors();
      }

      final boolean complete = this.control.waitForAllComplete(this.config.executionTimeout());

      if (!complete) {
        notifyExecutionTimeout();
      }

      if (isActivePassiveTest) {
        checkForErrors();
      }
    } catch (Throwable t) {
      notifyError(new ErrorContext(t));
    } finally {
      if (false && this.startServer) this.server.stop();
    }
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

  private int getActiveServerPort() {
    while (!server.isActive()) {
      System.err.println("Waiting for Server to become Active ...");
      ThreadUtil.reallySleep(500);
    }
    return this.server.getDSOListenPort();
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

  private void visitApplicationClassLoaderConfig() {
    if (optionalAttributes.size() > 0) {
      this.configVisitor.visit(this.configHelper, this.applicationClass, this.optionalAttributes);
    } else {
      this.configVisitor.visit(this.configHelper, this.applicationClass);
    }
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
    boolean crashActiveServerAfterMutate;
    if (isActivePassiveTest) {
      crashActiveServerAfterMutate = serverManager.crashActiveServerAfterMutate();
    } else {
      crashActiveServerAfterMutate = false;
    }
    return new ControlImpl(this.clientCount, validatorCount, crashActiveServerAfterMutate);
  }

  private ResultsListener[] newResultsListeners(int count) {
    ResultsListener[] rv = new ResultsListener[count];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = this;
    }
    return rv;
  }

  private ApplicationBuilder[] newApplicationBuilders(int count) throws Exception {
    ApplicationBuilder[] rv = new ApplicationBuilder[count];

    for (int i = 0; i < rv.length; i++) {
      L1TVSConfigurationSetupManager l1ConfigManager;
      l1ConfigManager = this.configFactory.createL1TVSConfigurationSetupManager();
      l1ConfigManager.setupLogging();
      PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(l1ConfigManager);
      rv[i] = new DSOApplicationBuilder(this.configHelper, this.applicationConfig, components);
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
