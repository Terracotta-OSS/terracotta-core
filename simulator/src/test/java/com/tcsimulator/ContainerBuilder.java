/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.TCRuntimeException;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.simulator.app.ApplicationBuilder;
import com.tc.simulator.app.DSOApplicationBuilder;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.app.GlobalIdGenerator;
import com.tc.simulator.container.Container;
import com.tc.simulator.container.ContainerConfig;
import com.tc.simulator.container.ContainerStateFactory;
import com.tc.simulator.control.Control;
import com.tc.simulator.crasher.ControlProvider;
import com.tc.simulator.listener.ResultsListener;
import com.tcsimulator.container.ContainerStateFactoryObject;

public class ContainerBuilder implements Runnable, ResultsListener {

  private static final LinkedQueue statsOutputQueue = new LinkedQueue();

  private final TestSpec           testSpec;
  private final String             vmName;

  public ContainerBuilder(String vmName) {
    this.vmName = vmName;
    testSpec = new TestSpec();
  }

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig cfg) {
    cfg.addRoot("testSpec", ContainerBuilder.class.getName() + ".testSpec");
    cfg.addIncludePattern(ContainerBuilder.class.getName());
    cfg.addWriteAutolock("* " + ContainerBuilder.class.getName() + ".*(..)");
    visitor.visitDSOApplicationConfig(cfg, ControlProviderImpl.class);
    visitor.visitDSOApplicationConfig(cfg, ControlImpl.class);
    visitor.visitDSOApplicationConfig(cfg, DistributedGlobalIdGenerator.class);
    visitor.visitDSOApplicationConfig(cfg, ContainerStateFactoryObject.class);
  }

  private void println(Object msg) {
    System.out.println("ContainerBuilder: " + msg);
  }

  public void run() {
    ContainerStateFactory containerStateFactory;
    Control control;
    GlobalIdGenerator globalIdGenerator;
    ApplicationBuilder appBuilder;

    try {
      containerStateFactory = new ContainerStateFactoryObject(statsOutputQueue);
      globalIdGenerator = new DistributedGlobalIdGenerator();
      ControlProvider provider = new ControlProviderImpl();
      control = provider.getOrCreateControlByName(getClass().getName(), this.testSpec.getGlobalVmCount());
      appBuilder = new DSOApplicationBuilder(testSpec.getTestConfig(), Thread.currentThread().getContextClassLoader());
    } catch (Throwable t) {
      throw new TCRuntimeException(t);
    }
    Container container = new Container(new ContainerConfigObject(), containerStateFactory, globalIdGenerator, control,
                                        this, appBuilder);
    container.run();
  }

  public static void main(String[] args) throws Exception {
    String vmName = args[0];
    new ContainerBuilder(vmName).run();
  }

  private final class ContainerConfigObject implements ContainerConfig {

    public int getApplicationInstanceCount() {
      synchronized (testSpec) {
        return testSpec.getContainerSpecFor(vmName).getExecutionCount();
      }
    }

    public long getContainerStartTimeout() {
      return 5 * 60 * 1000;
    }

    public long getApplicationStartTimeout() {
      return 5 * 60 * 1000;
    }

    public long getApplicationExecutionTimeout() {
      synchronized (testSpec) {
        return testSpec.getAppExecutionTimeout();
      }
    }

    public boolean isMaster() {
      return false;
    }

  }

  /*********************************************************************************************************************
   * ResultsListener interface
   */

  public void setGlobalId(long globalId) {
    return;
  }

  public void notifyStartTimeout() {
    println("Start timeout!");
    Thread.dumpStack();
  }

  public void notifyExecutionTimeout() {
    println("Execution timeout!");
    Thread.dumpStack();
  }

  public void notifyError(ErrorContext ectxt) {
    ectxt.dump(System.err);
  }

  public void notifyResult(Object result) {
    println("Result: " + result);
  }
}
