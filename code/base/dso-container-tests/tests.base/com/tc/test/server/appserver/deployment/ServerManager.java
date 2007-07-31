/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.util.AppServerUtil;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.PortChooser;
import com.tc.util.runtime.Vm;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

public class ServerManager {

  protected static TCLogger      logger         = TCLogging.getLogger(ServerManager.class);
  private static int             appServerIndex = 0;

  private List                   serversToStop  = new ArrayList();
  private DSOServer              dsoServer;

  private final TestConfigObject config;
  private AppServerFactory       factory;
  private AppServerInstallation  installation;
  private File                   sandbox;
  private File                   tempDir;
  private File                   installDir;
  private File                   warDir;
  private TcConfigBuilder        serverTcConfig = new TcConfigBuilder();

  public ServerManager(final Class testClass) throws Exception {
    PropertiesHackForRunningInEclipse.initializePropertiesWhenRunningInEclipse();
    config = TestConfigObject.getInstance();
    factory = AppServerFactory.createFactoryFromProperties(config);
    installDir = config.appserverServerInstallDir();
    tempDir = TempDirectoryUtil.getTempDirectory(testClass);
    sandbox = AppServerUtil.createSandbox(tempDir);
    warDir = new File(sandbox, "war");
    installation = AppServerUtil.createAppServerInstallation(factory, installDir, sandbox);
    PortChooser pc = new PortChooser();
    serverTcConfig.setDsoPort(pc.chooseRandomPort());
    serverTcConfig.setJmxPort(pc.chooseRandomPort());
  }

  public void addServerToStop(Stoppable stoppable) {
    getServersToStop().add(0, stoppable);
  }

  void stop() {
    logger.info("Stopping all servers");
    for (Iterator it = getServersToStop().iterator(); it.hasNext();) {
      Stoppable stoppable = (Stoppable) it.next();
      try {
        if (!stoppable.isStopped()) {
          logger.debug("About to stop server: " + stoppable.toString());
          stoppable.stop();
        }
      } catch (Exception e) {
        logger.error(stoppable, e);
      }
    }

    AppServerUtil.shutdownAndArchive(sandbox, new File(tempDir, "sandbox"));
  }
  
  void timeout() {
    logger.info("Test has timed out. Force shutdown and archive...");
    AppServerUtil.forceShutdownAndArchive(sandbox, new File(tempDir, "sandbox"));
  }

  protected boolean cleanTempDir() {
    return false;
  }

  void start(boolean withPersistentStore) throws Exception {
    startDSO(withPersistentStore);
  }

  private void startDSO(boolean withPersistentStore) throws Exception {
    dsoServer = new DSOServer(withPersistentStore, tempDir, serverTcConfig);
    if (!Vm.isIBM()) {
      dsoServer.getJvmArgs().add("-XX:+HeapDumpOnOutOfMemoryError");
    }
    dsoServer.getJvmArgs().add("-Xmx128m");
    logger.debug("Starting DSO server with sandbox: " + sandbox.getAbsolutePath());
    dsoServer.start();
    addServerToStop(dsoServer);
  }

  public void restartDSO(boolean withPersistentStore) throws Exception {
    logger.debug("Restarting DSO server : " + dsoServer);
    dsoServer.stop();
    startDSO(withPersistentStore);
  }

  /**
   * tcConfigPath: resource path
   */
  public WebApplicationServer makeWebApplicationServer(String tcConfigPath) throws Exception {
    return makeWebApplicationServer(new TcConfigBuilder(tcConfigPath));
  }

  public WebApplicationServer makeWebApplicationServer(TcConfigBuilder tcConfigBuilder) throws Exception {
    int i = ServerManager.appServerIndex++;

    WebApplicationServer appServer = new GenericServer(config, factory, installation,
                                                       prepareClientTcConfig(tcConfigBuilder), i, tempDir);
    addServerToStop(appServer);
    return appServer;
  }

  public FileSystemPath getTcConfigFile(String tcConfigPath) {
    URL url = getClass().getResource(tcConfigPath);
    Assert.assertNotNull("could not find: " + tcConfigPath, url);
    Assert.assertTrue("should be file:" + url.toString(), url.toString().startsWith("file:"));
    FileSystemPath pathToTcConfigFile = FileSystemPath.makeExistingFile(url.toString().substring("file:".length()));
    return pathToTcConfigFile;
  }

  private TcConfigBuilder prepareClientTcConfig(TcConfigBuilder clientConfig) {
    TcConfigBuilder aCopy = clientConfig.copy();
    aCopy.setDsoPort(getServerTcConfig().getDsoPort());
    aCopy.setJmxPort(getServerTcConfig().getJmxPort());

    int appId = AppServerFactory.getCurrentAppServerId();
    switch (appId) {
      case AppServerFactory.JETTY:
        aCopy.addModule("clustered-jetty-6.1", "1.0.0");
        break;
      case AppServerFactory.WEBSPHERE:
        aCopy.addModule("clustered-websphere-6.1.0.7", "1.0.0");
        break;
      default:
        // nothing for now
    }

    return aCopy;
  }

  void setServersToStop(List serversToStop) {
    this.serversToStop = serversToStop;
  }

  List getServersToStop() {
    return serversToStop;
  }

  public DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return new WARBuilder(warFileName, warDir, config);
  }

  public DeploymentBuilder makeDeploymentBuilder() throws IOException {
    return new WARBuilder(warDir, config);
  }

  public void stopAllWebServers() {
    for (Iterator it = getServersToStop().iterator(); it.hasNext();) {
      Stoppable stoppable = (Stoppable) it.next();
      try {
        if (!(stoppable instanceof DSOServer || stoppable.isStopped())) stoppable.stop();
      } catch (Exception e) {
        logger.error("Unable to stop server: " + stoppable, e);
      }
    }
  }

  public TestConfigObject getTestConfig() {
    return this.config;
  }

  public File getSandbox() {
    return sandbox;
  }

  public File getTempDir() {
    return tempDir;
  }

  public TcConfigBuilder getServerTcConfig() {
    return serverTcConfig;
  }
}
