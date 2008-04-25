/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import com.tc.bundles.BundleSpec;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.test.AppServerInfo;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.util.AppServerUtil;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.PortChooser;
import com.tc.util.TIMUtil;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

public class ServerManager {

  protected static TCLogger           logger         = TCLogging.getLogger(ServerManager.class);
  private static int                  appServerIndex = 0;
  private final boolean               DEBUG_MODE     = false;

  private List                        serversToStop  = new ArrayList();
  private DSOServer                   dsoServer;

  private final TestConfigObject      config;
  private final AppServerFactory      factory;
  private final AppServerInstallation installation;
  private final File                  sandbox;
  private final File                  tempDir;
  private final File                  installDir;
  private final File                  warDir;
  private final File                  tcConfigFile;
  private final TcConfigBuilder       serverTcConfig = new TcConfigBuilder();
  private final Collection            jvmArgs;
  private static int                  serverCounter = 0;

  public ServerManager(final Class testClass, Collection extraJvmArgs) throws Exception {
    PropertiesHackForRunningInEclipse.initializePropertiesWhenRunningInEclipse();
    config = TestConfigObject.getInstance();
    factory = AppServerFactory.createFactoryFromProperties();
    installDir = config.appserverServerInstallDir();
    tempDir = TempDirectoryUtil.getTempDirectory(testClass);
    tcConfigFile = new File(tempDir, "tc-config.xml");
    serverCounter ++;
    sandbox = AppServerUtil.createSandbox(tempDir);
    warDir = new File(sandbox, "war");
    jvmArgs = extraJvmArgs;
    installation = AppServerUtil.createAppServerInstallation(factory, installDir, sandbox);

    if (DEBUG_MODE) {
      serverTcConfig.setDsoPort(9510);
      serverTcConfig.setJmxPort(9520);
    } else {
      PortChooser pc = new PortChooser();
      serverTcConfig.setDsoPort(pc.chooseRandomPort());
      serverTcConfig.setJmxPort(pc.chooseRandomPort());
    }
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
    System.err.println("Test has timed out. Force shutdown and archive...");
    AppServerUtil.forceShutdownAndArchive(sandbox, new File(tempDir, "sandbox"));
  }

  protected boolean cleanTempDir() {
    return false;
  }

  void start(boolean withPersistentStore) throws Exception {
    startDSO(withPersistentStore);
  }

  private void startDSO(boolean withPersistentStore) throws Exception {
    File workDir = new File(tempDir, "dso-server-" + serverCounter);
    workDir.mkdirs();
    dsoServer = new DSOServer(withPersistentStore, workDir, serverTcConfig);
    if (!Vm.isIBM() && !(Os.isMac() && Vm.isJDK14())) {
      dsoServer.getJvmArgs().add("-XX:+HeapDumpOnOutOfMemoryError");
    }
    dsoServer.getJvmArgs().add("-Xmx128m");

    for (Iterator iterator = jvmArgs.iterator(); iterator.hasNext();) {
      dsoServer.getJvmArgs().add(iterator.next());
    }

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
   * tcConfigResourcePath: resource path
   */
  public WebApplicationServer makeWebApplicationServer(String tcConfigResourcePath) throws Exception {
    return makeWebApplicationServer(new TcConfigBuilder(tcConfigResourcePath));
  }

  public WebApplicationServer makeWebApplicationServer(TcConfigBuilder tcConfigBuilder) throws Exception {
    int i = ServerManager.appServerIndex++;

    WebApplicationServer appServer = new GenericServer(config, factory, installation,
                                                       prepareClientTcConfig(tcConfigBuilder).getTcConfigFile(), i,
                                                       tempDir);
    addServerToStop(appServer);
    return appServer;
  }

  public WebApplicationServer makeCoresidentWebApplicationServer(TcConfigBuilder config0, TcConfigBuilder config1, final boolean enableDebug) throws Exception {
    int i = ServerManager.appServerIndex++;
    WebApplicationServer appServer = new GenericServer(config, factory, installation,
                                                       config0.getTcConfigFile(), config1.getTcConfigFile(), i,
                                                       tempDir, true, enableDebug);
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

  private TcConfigBuilder prepareClientTcConfig(TcConfigBuilder clientConfig) throws IOException {
    TcConfigBuilder aCopy = clientConfig.copy();
    aCopy.setTcConfigFile(tcConfigFile);
    aCopy.setDsoPort(getServerTcConfig().getDsoPort());
    aCopy.setJmxPort(getServerTcConfig().getJmxPort());

    int appId = config.appServerId();
    switch (appId) {
      case AppServerInfo.JETTY:
        prepareClientTcConfigForJetty(aCopy);
        break;
      case AppServerInfo.WEBSPHERE:
        aCopy.addModule(TIMUtil.WEBSPHERE_6_1_0_7, TIMUtil.getVersion(TIMUtil.WEBSPHERE_6_1_0_7));
        break;
      default:
        // nothing for now
    }
    aCopy.saveToFile();
    return aCopy;
  }

  private void prepareClientTcConfigForJetty(TcConfigBuilder clientConfig) {
    // assume tim-jetty-6.1.4 locates under $HOME/.m2/repository
    File m2File = new File(System.getProperty("user.home") + File.separatorChar + ".m2" + File.separatorChar
                           + "repository");
    BundleSpec spec = TIMUtil.getBundleSpec(TIMUtil.JETTY_6_1);
    clientConfig.addRepository(m2File.toURI().toString());
    clientConfig.addModule(spec.getName(), spec.getGroupId(), spec.getVersion());
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

  public File getTcConfigFile() {
    return tcConfigFile;
  }


  public String toString() {
    return "ServerManager{" +
           "dsoServer=" + dsoServer.toString() +
           ", sandbox=" + sandbox.getAbsolutePath() +
           ", warDir=" + warDir.getAbsolutePath() +
           ", jvmArgs=" + jvmArgs +
           '}';
  }
}
