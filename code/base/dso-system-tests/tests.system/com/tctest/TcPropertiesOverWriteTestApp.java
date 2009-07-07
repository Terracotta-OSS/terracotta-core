/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TcPropertiesOverWriteTestApp extends AbstractTransparentApp {

  public static final String      CONFIG_FILE = "config-file";
  public static final String      PORT_NUMBER = "port-number";
  public static final String      HOST_NAME   = "host-name";
  public static final String      JMX_PORT    = "jmx-port";

  private final ApplicationConfig appConfig;
  private ExtraL1ProcessControl   client;
  private static Object           obj         = new Object();

  public TcPropertiesOverWriteTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    appConfig = cfg;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = TcPropertiesOverWriteTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);
    config.addWriteAutolock("* " + testClass + "*.*(..)");

    new SynchronizedIntSpec().visit(visitor, config);
    spec.addRoot("obj", "obj");
  }

  public void run() {
    startNewL1Process();
  }

  private void startNewL1Process() {
    try {
      final String hostName = appConfig.getAttribute(HOST_NAME);
      final int portNumber = Integer.parseInt(appConfig.getAttribute(PORT_NUMBER));
      final File configFile = new File(appConfig.getAttribute(CONFIG_FILE));
      File workingDir = new File(configFile.getParentFile(), "l1client");
      List jvmArgs = new ArrayList();
      jvmArgs.add("-Dtc.node-name=node123");
      jvmArgs.add("-Dtc.config=" + configFile.getAbsolutePath());
      setJvmArgsTcProperties(jvmArgs);
      FileUtils.forceMkdir(workingDir);
      String tcPropertiesFile = workingDir + "/tc.properties";
      createTCPropertiesFile(tcPropertiesFile);
      // set the local tc.properties file location
      jvmArgs.add("-Dcom.tc.properties=" + tcPropertiesFile);

      client = new ExtraL1ProcessControl(hostName, portNumber, L1Client.class, configFile.getAbsolutePath(), null,
                                         workingDir, jvmArgs);
      client.start();
      synchronized (obj) {
        obj.wait();
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void setJvmArgsTcProperties(List jvmArgs) {
    // these properties will not get overwritten by the config
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES + "="
                + TcPropertiesOverWriteTest.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES_VALUE);
  }

  private void createTCPropertiesFile(String file) {
    // properties written in this config file would get overridden by the one in tc-config
    try {
      FileWriter outFile = new FileWriter(file);
      PrintWriter out = new PrintWriter(outFile);
      out.println(TCPropertiesConsts.L1_CACHEMANAGER_LEASTCOUNT + "="
                  + TcPropertiesOverWriteTest.L1_CACHEMANAGER_LEASTCOUNT_VALUE);
      out.close();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

  }

  public static class L1Client {

    public static void main(String args[]) {
      L1Client client = new L1Client();
      client.execute();
      synchronized (obj) {
        obj.notify();
      }
    }

    private void execute() {
      TCProperties tcProps = ManagerUtil.getTCProperties();
      Assert.eval(tcProps.getProperty(TCPropertiesConsts.L1_CACHEMANAGER_ENABLED.toUpperCase())
          .equals(TcPropertiesOverWriteTest.L1_CACHEMANAGER_ENABLED_VALUE));
      Assert.eval(tcProps.getProperty(TCPropertiesConsts.LOGGING_MAX_LOGFILE_SIZE)
          .equals(TcPropertiesOverWriteTest.L1_LOGGING_MAX_LOGFILE_SIZE_VALUE));
      Assert.eval(tcProps.getProperty(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES.replace("e", "E"))
          .equals(TcPropertiesOverWriteTest.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES_VALUE));
      Assert.eval(tcProps.getProperty(TCPropertiesConsts.L1_CACHEMANAGER_LEASTCOUNT.toLowerCase())
          .equals(TcPropertiesOverWriteTest.L1_CACHEMANAGER_LEASTCOUNT_VALUE));
    }
  }
}
