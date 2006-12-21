/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.runner;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.server.NullTCServerInfo;
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
import com.tcsimulator.ControlImpl;
import com.tcsimulator.container.ContainerStateFactoryObject;
import com.tcsimulator.listener.QueuePrinter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Takes an application configuration and some parameters and runs a single-vm, multi-node (multi-classloader) test.
 */
public class DistributedTestRunner implements ResultsListener {

  private final boolean                             startServer;

  private final Class                               applicationClass;
  private final ApplicationConfig                   applicationConfig;
  private final ConfigVisitor                       configVisitor;
  private final ContainerConfig                     containerConfig;
  private final Control                             control;
  private final ResultsListener[]                   resultsListeners;
  private final DSOClientConfigHelper               configHelper;
  private final ApplicationBuilder[]                applicationBuilders;
  private final Container[]                         containers;
  private final ContainerStateFactory               containerStateFactory;
  private final TestGlobalIdGenerator               globalIdGenerator;
  private final DistributedObjectServer             server;
  private final List                                errors  = new ArrayList();
  private final List                                results = new ArrayList();
  private final DistributedTestRunnerConfig         config;
  private final TVSConfigurationSetupManagerFactory configFactory;
  private boolean                                   startTimedOut;
  private boolean                                   executionTimedOut;
  private final int                                 clientCount;
  private final int                                 applicationInstanceCount;
  private final LinkedQueue                         statsOutputQueue;
  private final QueuePrinter                        statsOutputPrinter;

  /**
   * @param applicationClass Class of the application to be executed. It should implement the static method required by
   *        ClassLoaderConfigVisitor.
   * @param applicationConfig Configuration object for the test application.
   * @param nodeCount Number of classloaders to create.
   * @param applicationsPerNode Number of application instances per classloader to execute. This counts as number of
   *        threads per classloader.
   */
  public DistributedTestRunner(DistributedTestRunnerConfig config, TVSConfigurationSetupManagerFactory configFactory,
                               DSOClientConfigHelper configHelper, Class applicationClass,
                               ApplicationConfig applicationConfig, int clientCount, int applicationInstanceCount,
                               boolean startServer) throws Exception {
    this.clientCount = clientCount;
    this.applicationInstanceCount = applicationInstanceCount;
    this.startServer = startServer;
    this.config = config;
    this.configFactory = configFactory;
    this.configHelper = configHelper;
    this.globalIdGenerator = new TestGlobalIdGenerator();
    this.applicationClass = applicationClass;
    this.applicationConfig = applicationConfig;
    this.configVisitor = new ConfigVisitor();
    this.containerConfig = newContainerConfig();
    this.statsOutputQueue = new LinkedQueue();
    this.statsOutputPrinter = new QueuePrinter(this.statsOutputQueue, System.out);
    this.containerStateFactory = new ContainerStateFactoryObject(statsOutputQueue);
    this.control = newContainerControl();
    this.resultsListeners = newResultsListeners(clientCount);
    this.applicationBuilders = newApplicationBuilders(clientCount);
    this.containers = new Container[clientCount];
    for (int i = 0; i < containers.length; i++) {
      containers[i] = new Container(this.containerConfig, this.containerStateFactory, this.globalIdGenerator,
                                    this.control, this.resultsListeners[i], this.applicationBuilders[i]);
    }
    L2TVSConfigurationSetupManager manager = configFactory.createL2TVSConfigurationSetupManager(null);

    this.server = new DistributedObjectServer(manager, new TCThreadGroup(new ThrowableHandler(TCLogging
        .getLogger(DistributedObjectServer.class))), new ConnectionPolicyImpl(-1), new NullTCServerInfo());
  }

  public void run() {
    try {
      Thread statsOutputPrinterThread = new Thread(this.statsOutputPrinter);
      statsOutputPrinterThread.setDaemon(true);
      statsOutputPrinterThread.start();

      visitApplicationClassLoaderConfig();
      if (this.startServer) {
        this.server.start();

        if (this.configFactory instanceof TestTVSConfigurationSetupManagerFactory) {
          TestTVSConfigurationSetupManagerFactory testFactory = (TestTVSConfigurationSetupManagerFactory) this.configFactory;
          ((SettableConfigItem) testFactory.l2DSOConfig().listenPort()).setValue(this.server.getListenPort());
          testFactory.activateConfigurationChange();
        }
      }

      for (int i = 0; i < containers.length; i++) {
        new Thread(containers[i]).start();
      }
      final boolean complete = this.control.waitForAllComplete(this.config.executionTimeout());

      if (!complete) {
        notifyExecutionTimeout();
      }
    } catch (Throwable t) {
      notifyError(new ErrorContext(t));
    } finally {
      if (false && this.startServer) this.server.stop();
    }
  }

  public int getServerPort() {
    return this.server.getListenPort();
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
    } else if (results.size() != containers.length) {
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
    this.configVisitor.visit(this.configHelper, this.applicationClass);
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
    return new ControlImpl(this.clientCount);
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
      L1TVSConfigurationSetupManager l1ConfigManager = this.configFactory.createL1TVSConfigurationSetupManager();
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
    this.server.dump();
  }
}
