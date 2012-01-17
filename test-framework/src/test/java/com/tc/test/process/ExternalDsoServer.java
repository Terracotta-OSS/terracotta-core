/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;

import com.tc.config.Loader;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.util.PortChooser;
import com.terracottatech.config.Server;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

/**
 * This class will start a DSO server in Enterprise mode, out of process
 * 
 * @author hhuynh
 */
public class ExternalDsoServer {
  private static final String       SERVER_CONFIG_FILENAME = "server-config.xml";

  private ExtraProcessServerControl serverProc;
  private final File                serverLog;
  private final File                configFile;
  private boolean                   persistentMode;
  private int                       dsoPort;
  private int                       jmxPort;
  private final List                jvmArgs                = new ArrayList();
  private final File                workingDir;
  private String                    serverName;
  private boolean                   inited                 = false;

  private FileOutputStream          logOutputStream;

  public ExternalDsoServer(File workingDir, InputStream configInput) throws IOException {
    this(workingDir, configInput, null);
  }
  
  public ExternalDsoServer(File workingDir, InputStream configInput, String serverName) throws IOException {
    this(workingDir, saveToFile(configInput, workingDir), serverName);
  }

  public ExternalDsoServer(File workingDir, File configInput, String serverName) {
    try {
      this.workingDir = workingDir;
      this.configFile = configInput;
      this.serverLog = new File(workingDir, "dso-server.log");
      this.serverName = serverName;

      TcConfigDocument tcConfigDocument = new Loader().parse(new FileInputStream(configFile));
      TcConfig tcConfig = tcConfigDocument.getTcConfig();
      Server[] servers = tcConfig.getServers().getServerArray();

      if (serverName != null) {
        boolean foundServer = false;
        for (Server server : servers) {
          if (server.getName().equals(serverName)) {
            foundServer = true;
            jmxPort = server.getJmxPort().getIntValue();
            dsoPort = server.getDsoPort().getIntValue();
            break;
          }
        }
        if (!foundServer) { throw new RuntimeException("Can't find " + serverName + " from config input"); }
      } else {
        jmxPort = servers[0].getJmxPort().getIntValue();
        dsoPort = servers[0].getDsoPort().getIntValue();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public File getWorkingDir() {
    return this.workingDir;
  }
  
  private static File saveToFile(InputStream configInput, File workingDir) throws IOException  {
    File config = new File(workingDir, SERVER_CONFIG_FILENAME);
    FileOutputStream out = new FileOutputStream(config);
    IOUtils.copy(configInput, out);
    out.close();
    return config;
  }

  public ExternalDsoServer(File workingDir) {
    PortChooser portChooser = new PortChooser();
    this.workingDir = workingDir;
    this.dsoPort = portChooser.chooseRandomPort();
    this.jmxPort = portChooser.chooseRandomPort();
    this.serverLog = new File(workingDir, "dso-server.log");
    try {
      this.configFile = writeConfig();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void start() throws Exception {
    initStart();
    serverProc.start();
  }

  public void startWithoutWait() throws Exception {
    initStart();
    serverProc.startWithoutWait();
  }

  public void startAndWait(long seconds) throws Exception {
    initStart();
    serverProc.startAndWait(seconds);
  }

  private void initStart() throws FileNotFoundException {
    logOutputStream = new FileOutputStream(serverLog);
    serverProc = new ExtraProcessServerControl("localhost", dsoPort, jmxPort, configFile.getAbsolutePath(), false);
    serverProc.setServerName(serverName);
    serverProc.writeOutputTo(logOutputStream);
    serverProc.getJvmArgs().addAll(jvmArgs);
    inited = true;
  }

  public void stop() throws Exception {
    Assert.assertNotNull(serverProc);
    Assert.assertNotNull(logOutputStream);
    serverProc.shutdown();
    inited = false;
    IOUtils.closeQuietly(logOutputStream);
  }

  public boolean isRunning() {
    Assert.assertNotNull(serverProc);
    return serverProc.isRunning();
  }

  public File getServerLog() {
    return serverLog;
  }

  public void setPersistentMode(boolean persistentMode) {
    this.persistentMode = persistentMode;
  }

  public File getConfigFile() {
    return configFile;
  }

  private File writeConfig() throws IOException {
    File theConfigFile = new File(workingDir, SERVER_CONFIG_FILENAME);

    TerracottaConfigBuilder builder = TerracottaConfigBuilder.newMinimalInstance();
    builder.getSystem().setConfigurationModel("development");

    L2ConfigBuilder l2 = builder.getServers().getL2s()[0];
    l2.setDSOPort(dsoPort);
    l2.setJMXPort(jmxPort);
    l2.setData(workingDir + File.separator + "data");
    l2.setLogs(workingDir + File.separator + "logs");
    l2.setStatistics(workingDir + File.separator + "stats");
    if (persistentMode) {
      l2.setPersistenceMode("permanent-store");
    }

    String configAsString = builder.toString();

    FileOutputStream fileOutputStream = new FileOutputStream(theConfigFile);
    PrintWriter out = new PrintWriter(fileOutputStream, true);
    out.println(configAsString);
    out.close();
    return theConfigFile;
  }

  @Override
  public String toString() {
    return "DSO server; serverport:" + dsoPort + "; adminPort:" + jmxPort;
  }

  public int getServerPort() {
    return dsoPort;
  }

  public int getAdminPort() {
    return jmxPort;
  }

  public List getJvmArgs() {
    return jvmArgs;
  }

  public void addJvmArg(String jvmarg) {
    jvmArgs.add(jvmarg);
  }

  public void dumpServerControl() throws Exception {
    this.serverProc.dumpServerControl();
  }

  public boolean isInitialized() {
    return this.inited;
  }
}
