/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.NullServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.objectserver.control.ExtraProcessServerControl.DebugParams;
import com.tc.simulator.distrunner.ArgException;
import com.tc.simulator.distrunner.ArgParser;
import com.tc.simulator.distrunner.SpecFactoryImpl;
import com.tcsimulator.distrunner.ServerSpec;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Setup {

  private final ServerSpec    testServerSpec;
  private final String        testAppClassName;
  private final ServerControl server;
  private final String        localHostName;
  private final boolean       isServerMachine;
  private final ConfigWriter  configWriter;
  private Sandbox             sandbox;
  private final boolean       debugTestSetup   = false;
  private final boolean       debugTestStarter = false;
  private LinkedJavaProcess   testStarterProcess;
  private LinkedJavaProcess   testSetupProcess;

  private TestEnvironmentView testEnvironmentView;
  private LinkedQueue         eventQueue;
  private LinkedQueue         responseQueue;
  private final boolean       startedWithDSO;

  public Setup(String[] args, ServerSpec serverSpec, String testAppClassName, Collection clientSpecs, int intensity,
               boolean startedWithDSO) throws ClassNotFoundException {

    this.testServerSpec = serverSpec;
    this.testAppClassName = testAppClassName;
    this.startedWithDSO = startedWithDSO;

    InetAddress host;
    try {
      host = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Could not resolve local host name." + e);
    }
    localHostName = host.getHostName();

    this.isServerMachine = testServerSpec.getHostName().equals(localHostName);

    sandbox = new Sandbox(new File(testServerSpec.getTestHome()), Sandbox.TEST_SERVER);

    if (this.isServerMachine) {
      this.server = newServerControl();

      Collection classesToVisit = new ArrayList();
      classesToVisit.add(TestSetup.class);
      classesToVisit.add(TestSpec.class);
      classesToVisit.add(TestStarter.class);
      classesToVisit.add(ContainerBuilder.class);
      classesToVisit.add(GlobalVmNameGenerator.class);
      classesToVisit.add(Class.forName(this.testAppClassName));
      configWriter = new ConfigWriter(testServerSpec, classesToVisit, sandbox);
    } else {
      this.server = null;
      this.configWriter = null;
    }
    ProcessFactory procFactory = new ProcessFactory(sandbox, testServerSpec);

    this.testSetupProcess = procFactory.newDSOJavaProcessInstance(TestSetup.class.getName(), Arrays.asList(args), debugTestSetup);
    this.testStarterProcess = procFactory.newDSOJavaProcessInstance(TestStarter.class.getName(), Collections.EMPTY_LIST,
                                                                    debugTestStarter);
    if (this.isServerMachine) {
      List clientSpecsCopy = new ArrayList();
      for (Iterator i = clientSpecs.iterator(); i.hasNext();) {
        ClientSpec cSpec = (ClientSpec) i.next();
        ClientSpec cCopy = cSpec.copy();
        clientSpecsCopy.add(cCopy);
      }

      this.testEnvironmentView = new TestEnvironmentViewImpl(serverSpec.copy(), clientSpecsCopy, intensity);
    }

    if (this.startedWithDSO) {
      this.eventQueue = new LinkedQueue();
      this.responseQueue = new LinkedQueue();
    }
  }

  public void execute() throws IOException, InterruptedException {
    if (isServerMachine) {
      configWriter.writeConfigFile();
      startServer();

      ((TestEnvironmentViewImpl) this.testEnvironmentView).setServerRunning(ServerView.RUNNING);

      startTestSetup();

      if (this.startedWithDSO) {
        startEventQueueHandler();
      }
    }
    startTestStarter();
  }

  private void startEventQueueHandler() {
    System.out.println("Starting EventQueueHandler...");
    EventQueueHandler eqhandler = new EventQueueHandler(this.eventQueue, this);
    Thread t = new Thread(eqhandler);
    t.setDaemon(true);
    t.start();
  }

  public void crashServer() throws Exception {
    synchronized (this.server) {
      // if (!this.server.isRunning()) { throw new RuntimeException("Server is not running... server crash failed!"); }
      this.server.crash();
      if (this.server.isRunning()) { throw new RuntimeException("srever is still running... server crash failed!"); }

      ((TestEnvironmentViewImpl) this.testEnvironmentView).setServerRunning(ServerView.NOT_RUNNING);

      this.responseQueue.put("Crash successful!");
    }
  }

  public void restartServer() throws Exception {
    synchronized (this.server) {
      if (this.server.isRunning()) { throw new RuntimeException("Server is already running... server restart failed!"); }
      this.server.start();
      if (!this.server.isRunning()) { throw new RuntimeException("Server is not running... server restart failed!"); }

      ((TestEnvironmentViewImpl) this.testEnvironmentView).setServerRunning(ServerView.RUNNING);

      this.responseQueue.put("Restart successful!");
    }
  }

  private ServerControl newServerControl() {
    ServerControl rv;
    boolean mergeOutput = true;
    if (!isServerMachine) {
      rv = new NullServerControl();
    } else {
      rv = new ExtraProcessServerControl(new DebugParams(), this.testServerSpec.getHostName(), this.testServerSpec
          .getDsoPort(), this.testServerSpec.getJmxPort(), this.sandbox.getConfigFile().getAbsolutePath(), this.sandbox
          .getServerHome(), mergeOutput, this.testServerSpec.getJvmOpts(), ArgParser.getUndefinedString());
    }
    return rv;
  }

  private void startServer() {
    try {
      server.crash();
      server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (!server.isRunning()) { throw new RuntimeException("Server still isn't running!"); }
  }

  private void startTestStarter() throws IOException, InterruptedException {
    System.out.println("Starting TestStarter...");
    testStarterProcess.start();
    testStarterProcess.mergeSTDERR();
    testStarterProcess.mergeSTDOUT();
    testStarterProcess.waitFor();
  }

  private void startTestSetup() throws IOException, InterruptedException {
    System.out.println("Starting TestSetup...");
    testSetupProcess.start();
    testSetupProcess.mergeSTDERR();
    testSetupProcess.mergeSTDOUT();
    testSetupProcess.waitFor();
  }

  public static void main(String[] args) throws Throwable {

    Setup setup = null;
    ArgParser argParser = null;
    boolean testServerRequired = true;
    boolean controlServerRequired = false;

    try {
      argParser = new ArgParser(args, new SpecFactoryImpl(), testServerRequired, controlServerRequired);
    } catch (ArgException e) {
      System.err.println("Error parsing arguments: " + e.getMessage());
      System.err.println("\n\n" + ArgParser.usage());
      System.exit(1);
    }

    // TODO: this needs to be fixed if multiple test servers are involved. for now assume there's only one test server.
    setup = new Setup(args, argParser.getServerSpec(), argParser.getTestClassname(), argParser.getClientSpecs(),
                      argParser.getIntensity(), argParser.getTestServerStartMode());
    setup.execute();

  }

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig cfg) {
    cfg.addRoot("testEnvironmentView", Setup.class.getName() + ".testEnvironmentView");
    cfg.addRoot("eventQueue", Setup.class.getName() + ".eventQueue");
    cfg.addRoot("responseQueue", Setup.class.getName() + ".responseQueue");
    cfg.addRoot("testEnvironmentView", "com.tcsimulator.webui.Manager.testEnvironmentView");
    cfg.addRoot("eventQueue", "com.tcsimulator.webui.Manager.eventQueue");
    cfg.addRoot("responseQueue", "com.tcsimulator.webui.Manager.responseQueue");
  }

}
