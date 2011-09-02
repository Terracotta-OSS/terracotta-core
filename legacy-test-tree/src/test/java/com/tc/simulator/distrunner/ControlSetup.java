/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.distrunner;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ExtraProcessServerControl.DebugParams;
import com.tc.objectserver.control.ServerControl;
import com.tcsimulator.ConfigWriter;
import com.tcsimulator.ProcessFactory;
import com.tcsimulator.Sandbox;
import com.tcsimulator.Setup;
import com.tcsimulator.distrunner.ServerSpec;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ControlSetup {

  private final ServerSpec        controlServerSpec;
  private final ConfigWriter      configWriter;
  private final Sandbox           sandbox;
  private final ServerControl     server;
  private final boolean           debugSetup = false;
  private final LinkedJavaProcess setupProcess;
  private final boolean           isServerMachine;
  private final String            localHostName;
  private final boolean           startWithDSO;

  public ControlSetup(String[] args, ServerSpec controlServerSpec, boolean startWithDSO, String javaHome) {

    this.controlServerSpec = controlServerSpec;
    this.startWithDSO = startWithDSO;

    InetAddress host;
    try {
      host = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Could not resolve local host name." + e);
    }
    localHostName = host.getHostName();

    if (this.localHostName.equals(this.controlServerSpec.getHostName())) {
      this.isServerMachine = true;
    } else {
      this.isServerMachine = false;
    }

    this.sandbox = new Sandbox(new File(this.controlServerSpec.getTestHome()), Sandbox.CONTROL_SERVER);

    if (this.isServerMachine) {
      Collection classesToVisit = new ArrayList();
      classesToVisit.add(Setup.class);
      this.configWriter = new ConfigWriter(this.controlServerSpec, classesToVisit, sandbox);
      this.server = newServerControl();
    } else {
      this.configWriter = null;
      this.server = null;
    }

    ProcessFactory procFactory = new ProcessFactory(sandbox, this.controlServerSpec);

    if (this.startWithDSO) {
      this.setupProcess = procFactory.newDSOJavaProcessInstance(Setup.class.getName(), Arrays.asList(args), debugSetup);
    } else {
      this.setupProcess = procFactory.newJavaProcessInstance(Setup.class.getName(), Arrays.asList(args), debugSetup, javaHome);
    }
  }

  private void execute() throws IOException, InterruptedException {
    if (this.isServerMachine && this.startWithDSO) {
      configWriter.writeConfigFile();
      startServer();
    }
    startSetup();
  }

  private void startSetup() throws IOException, InterruptedException {
    System.out.println("Starting Setup...");
    setupProcess.start();
    setupProcess.mergeSTDERR();
    setupProcess.mergeSTDOUT();
    setupProcess.waitFor();
  }

  private ServerControl newServerControl() {
    boolean mergeOutput = true;
    return new ExtraProcessServerControl(new DebugParams(), this.controlServerSpec.getHostName(),
                                         this.controlServerSpec.getDsoPort(), this.controlServerSpec.getJmxPort(),
                                         this.sandbox.getConfigFile().getAbsolutePath(), this.sandbox.getServerHome(),
                                         mergeOutput, this.controlServerSpec.getJvmOpts(), ArgParser
                                             .getUndefinedString());
  }

  private void startServer() {
    try {
      this.server.crash();
      this.server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (!this.server.isRunning()) { throw new RuntimeException("Server still isn't running!"); }
  }

  public static void main(String[] args) throws IOException, InterruptedException {

    boolean controlServerRequired = true;
    boolean testServerRequired = true;
    ArgParser parser = null;

    try {
      parser = new ArgParser(args, new SpecFactoryImpl(), testServerRequired, controlServerRequired);
    } catch (ArgException e) {
      System.err.println("Error parsing arguments: " + e.getMessage());
      System.err.println("\n\n" + ArgParser.usage());
      System.exit(1);
    }

    ControlSetup controlSetup = new ControlSetup(args, parser.getControlServerSpec(), parser.getTestServerStartMode(),
                                                 parser.getJavaHome());
    controlSetup.execute();
  }
}
