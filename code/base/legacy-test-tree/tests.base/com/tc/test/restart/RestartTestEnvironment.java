/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.restart;

import org.apache.commons.io.FileUtils;

import com.tc.config.schema.MockIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.L2SConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.IntraProcessServerControl;
import com.tc.objectserver.control.NullServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.objectserver.control.ExtraProcessServerControl.DebugParams;
import com.tc.test.TestConfigObject;
import com.tc.util.PortChooser;
import com.tctest.restart.TestThreadGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RestartTestEnvironment {

  public static final OperatingMode                     DEV_MODE          = new OperatingMode();
  public static final OperatingMode                     PROD_MODE         = new OperatingMode();

  private static TCLogger                               logger            = TCLogging
                                                                              .getTestingLogger(RestartTestEnvironment.class);
  private final PortChooser                             portChooser;
  private StandardTVSConfigurationSetupManagerFactory   config;
  private File                                          configFile;

  private File                                          dbhome;
  private ServerControl                                 server;
  private final ServerControl                           serverWrapper     = new ServerWrapper();
  private TestThreadGroup                               threadGroup;

  private boolean                                       isPersistent      = true;
  private boolean                                       isParanoid        = true;
  private final File                                    tempDirectory;
  private boolean                                       setUp;
  private boolean                                       mergeServerOutput = true;
  private int                                           serverPort;
  private int                                           adminPort;
  private final OperatingMode                           operatingMode;
  private final TestTVSConfigurationSetupManagerFactory configFactory;

  public RestartTestEnvironment(File tempDirectory, PortChooser portChooser, OperatingMode operatingMode) {
    this(tempDirectory, portChooser, operatingMode, null);
  }

  public RestartTestEnvironment(File tempDirectory, PortChooser portChooser, OperatingMode operatingMode,
                                TestTVSConfigurationSetupManagerFactory configFactory) {
    this.tempDirectory = tempDirectory;
    this.portChooser = portChooser;
    this.operatingMode = operatingMode;
    this.configFactory = configFactory;
    if (!tempDirectory.isDirectory()) {
      //
      throw new AssertionError("Temp directory is not a directory: " + tempDirectory);
    }
    this.configFile = new File(this.tempDirectory, "restart-test-config.xml");
  }

  public void setIsPersistent(boolean b) {
    isPersistent = b;
  }

  public void setIsParanoid(boolean b) {
    this.isParanoid = b;
  }

  public void setUp() throws Exception {
    writeL2Config();
    initConfig();

    dbhome = new File(this.tempDirectory, "l2-data/objectdb");
    System.err.println("DBHome: " + dbhome.getAbsolutePath());
    System.out.println("dbhome: " + dbhome);
    if (dbhome.exists()) FileUtils.cleanDirectory(dbhome);
    if (server != null && server.isRunning()) {
      logger.info("L2 is running... Shutting it down.");
      server.shutdown();
      for (int i = 0; i < 3 && server.isRunning(); i++) {
        Thread.sleep(1000);
      }
    }
    logger.info("Making sure L2 isn't running...");
    if (server != null && server.isRunning()) throw new AssertionError("L2 is currently running, but shouldn't be.");
    threadGroup = new TestThreadGroup(Thread.currentThread().getThreadGroup(), "TEST THREAD GROUP");

    this.server = new NullServerControl();
    this.setUp = true;
  }

  private void initConfig() throws Exception {
    // FIXME 2005-12-01 andrew -- This MockIllegalConfigurationChangeHandler probably isn't right. We should fix it.

    config = new StandardTVSConfigurationSetupManagerFactory(new String[] {
        StandardTVSConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, this.configFile.getAbsolutePath() },
                                                             true, new MockIllegalConfigurationChangeHandler());
  }

  private void writeL2Config() throws Exception {
    assertServerOff();

    TerracottaConfigBuilder builder = TerracottaConfigBuilder.newMinimalInstance();

    String configurationModel;
    if (operatingMode == DEV_MODE) {
      configurationModel = "development";
    } else if (operatingMode == PROD_MODE) {
      configurationModel = "production";
    } else {
      throw new AssertionError("Unknown operating mode.");
    }
    builder.getSystem().setConfigurationModel(configurationModel);

    String persistenceMode = L2ConfigBuilder.PERSISTENCE_MODE_TEMPORARY_SWAP_ONLY;
    if (isPersistent && isParanoid) {
      persistenceMode = L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE;
    } else if (isPersistent) {
      persistenceMode = L2ConfigBuilder.PERSISTENCE_MODE_TEMPORARY_SWAP_ONLY;
    }

    L2ConfigBuilder l2 = new L2ConfigBuilder();
    l2.setDSOPort(serverPort);
    l2.setJMXPort(adminPort);
    l2.setData(tempDirectory.getAbsolutePath());
    l2.setPersistenceMode(persistenceMode);
    if (configFactory != null) {
      l2.setGCEnabled(configFactory.getGCEnabled());
      l2.setGCVerbose(configFactory.getGCVerbose());
      l2.setGCInterval(configFactory.getGCIntervalInSec());
    }
    L2ConfigBuilder[] l2s = new L2ConfigBuilder[] { l2 };
    L2SConfigBuilder servers = new L2SConfigBuilder();
    servers.setL2s(l2s);
    builder.setServers(servers);

    String configAsString = builder.toString();

    System.err.println("Writing config to file:" + configFile.getAbsolutePath() + configAsString);
    FileOutputStream fileOutputStream = new FileOutputStream(configFile);
    PrintWriter out = new PrintWriter((fileOutputStream));
    out.println(configAsString);
    out.flush();
    out.close();

    initConfig();
  }

  public Collection getThreadGroupErrors() {
    return this.threadGroup.getErrors();
  }

  public void startNewClientThread(Runnable runnable) {
    new Thread(this.threadGroup, runnable).start();
  }

  public boolean hasThreadGroupErrors() {
    return threadGroup.getErrors().size() > 0;
  }

  public boolean hasActiveClients() {
    return this.threadGroup.activeCount() > 0;
  }

  public ThreadGroup getThreadGroup() {
    return this.threadGroup;
  }

  public ServerControl getServer() {
    return this.server;
  }

  public void startServer(long timeout) throws Exception {
    assertServerOff();
    assertSetUp();
    server.start(timeout);
  }

  public void shutdownServer() throws Exception {
    assertServerNotNull();
    server.shutdown();
  }

  public ServerControl newExtraProcessServer(List jvmArgs) throws FileNotFoundException {
    assertServerOff();
    File javaHome = null;
    try {
      String javaHomeString = TestConfigObject.getInstance().getL2StartupJavaHome();
      if (javaHomeString != null) {
        javaHome = new File(javaHomeString);
      }
    } catch (Exception e) {
      // ignore, leaving javaHome as null
    }
    this.server = new ExtraProcessServerControl(new DebugParams(), "localhost", serverPort, adminPort, this.configFile
        .getAbsolutePath(), mergeServerOutput, javaHome, jvmArgs);
    return serverWrapper;
  }
  
  public ServerControl newExtraProcessServer() throws FileNotFoundException {
    return (newExtraProcessServer(new ArrayList()));
  }

  public ServerControl newIntraProcessServer() throws ConfigurationSetupException {
    assertServerOff();
    this.server = new IntraProcessServerControl(this.config.createL2TVSConfigurationSetupManager(null), "localhost");
    return serverWrapper;
  }

  private void assertServerNotNull() {
    if (this.server == null) throw new AssertionError("Server is null.");
  }

  private void assertServerOff() {
    if (this.server != null && this.server.isRunning()) {
      //
      throw new AssertionError("Server is not off.");
    }
  }

  private void assertServerOn() {
    assertServerNotNull();
    if (!this.server.isRunning()) { throw new AssertionError("Server is not on."); }
  }

  private void assertSetUp() {
    if (!this.setUp) throw new AssertionError("Not set up.");
  }

  public File getDBHome() {
    return this.dbhome;
  }

  public void setServerPort(int i) {
    this.serverPort = i;
  }

  public void setAdminPort(int i) {
    this.adminPort = i;
  }

  public int chooseServerPort() {
    this.serverPort = portChooser.chooseRandomPort();
    return this.serverPort;
  }

  public int chooseAdminPort() {
    this.adminPort = portChooser.chooseRandomPort();
    return this.adminPort;
  }

  public void choosePorts() {
    chooseServerPort();
    chooseAdminPort();
  }

  public int getServerPort() {
    return this.serverPort;
  }

  public int getAdminPort() {
    return this.adminPort;
  }

  public static final class OperatingMode {
    private OperatingMode() {
      //
    }
  }

  private final class ServerWrapper implements ServerControl {

    public void mergeSTDOUT() {
      assertServerOff();
      server.mergeSTDOUT();
    }

    public void mergeSTDERR() {
      assertServerOff();
      server.mergeSTDERR();
    }

    public void attemptShutdown() throws Exception {
      assertServerOn();
      server.attemptShutdown();
    }

    public void shutdown() throws Exception {
      assertServerOn();
      server.shutdown();
    }

    public void crash() throws Exception {
      assertServerOn();
      server.crash();
    }

    public void start(long timeout) throws Exception {
      assertSetUp();
      assertServerNotNull();
      server.start(timeout);
    }

    public boolean isRunning() {
      assertServerNotNull();
      return server.isRunning();
    }

    public void clean() {
      assertServerNotNull();
      assertServerOff();
      server.clean();
    }

    public void waitUntilShutdown() throws Exception {
      assertServerNotNull();
      server.waitUntilShutdown();
    }

    public int getDsoPort() {
      assertServerNotNull();
      return server.getDsoPort();
    }

    public int getAdminPort() {
      assertServerNotNull();
      return server.getAdminPort();
    }
  }
}
