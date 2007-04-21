/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests.framework;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.test.TestConfigObject;
import com.tc.test.server.AbstractDBServer;
import com.tc.test.server.HSqlDBServer;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

public class ServerManager {
  private static int             appServerIndex = 0;

  private List                   serversToStop  = new ArrayList();
  protected Log                  logger         = LogFactory.getLog(getClass());
  private DSOServer              dsoServer;

  private final TestConfigObject config;
  private NewAppServerFactory    factory;

  static final boolean           MONKEY_MODE    = true;

  public ServerManager(final Class testClass) throws Exception {
    PropertiesHackForRunningInEclipse.initializePropertiesWhenRunningInEclipse();

    config = TestConfigObject.getInstance();
    factory = NewAppServerFactory.createFactoryFromProperties(config);

    tempDir = TempDirectoryUtil.getTempDirectory(testClass);

    workingDir = workingDir();

    String appserverURLBase = config.appserverURLBase();
    String appserverHome = config.appserverHome();

    if (appserverHome != null && !appserverHome.trim().equals("")) {
      File home = new File(appserverHome);
      installation = factory.createInstallation(home, workingDir);

    } else if (appserverURLBase != null && !appserverURLBase.trim().equals("")) {
      URL host = new URL(appserverURLBase);
      installation = factory.createInstallation(host, serverInstallDir(), workingDir);

    } else {
      throw new AssertionError(
                               "No container installation available. You must define one of the following config properties:\n"
                                   + TestConfigObject.APP_SERVER_HOME + "\nor\n"
                                   + TestConfigObject.APP_SERVER_REPOSITORY_URL_BASE);
    }
  }

  private AppServerInstallation installation;
  private File                  workingDir;

  private File                  tempDir;

  private synchronized File serverInstallDir() {
    return makeDir(config.appserverServerInstallDir());
  }

  private synchronized File workingDir() throws IOException {
    String osName = config.osName();
    File dir = null;

    if (osName != null && osName.startsWith("Windows")) {
      // MNK-89: Lousy Windows file system!
      dir = makeDir(config.appserverWorkingDir() + File.separator);
      File guaranteedUniqueFile = File.createTempFile("servermanager-unique-", "-marker", dir);
      dir = makeDir(guaranteedUniqueFile.getAbsolutePath() + "-dir" + File.separator);
    } else {
      dir = makeDir(tempDir.getAbsolutePath());
    }
    FileUtils.cleanDirectory(dir);
    return dir;
  }

  private File makeDir(String dirPath) {
    File dir = new File(dirPath);
    if (dir.exists()) return dir;
    dir.mkdirs();
    return dir;
  }

  void addServerToStop(Stoppable stoppable) {
    getServersToStop().add(0, stoppable);
  }

  void stop() {
    for (Iterator it = getServersToStop().iterator(); it.hasNext();) {
      Stoppable stoppable = (Stoppable) it.next();
      try {
        if (!stoppable.isStopped()) stoppable.stop();
      } catch (Exception e) {
        logger.error(stoppable, e);
      }
    }
    File toDir = new File(tempDir, "working");

    if (MONKEY_MODE) {
      logger.warn("working dir under short-name: " + workingDir);
      logger.warn("working dir under temp: " + toDir);

      if (!workingDir.getPath().equals(toDir.getPath())) {
        logger.warn("Moving " + workingDir + "->" + toDir);

        if (!workingDir.renameTo(toDir)) {
          logger.warn("could not rename: " + workingDir + "->" + toDir);
        } else {
          logger.warn("Moved " + workingDir + "->" + toDir);
        }
      }
    }
  }

  protected boolean cleanTempDir() {
    return true;
  }

  void start(boolean withPersistentStore) throws Exception {
    startDSO(withPersistentStore);
  }

  private void startDSO(boolean withPersistentStore) throws Exception {
    dsoServer = new DSOServer(withPersistentStore, workingDir);
    dsoServer.start();
    addServerToStop(dsoServer);
  }

  public void restartDSO(boolean withPersistentStore) throws Exception {
    dsoServer.stop();
    startDSO(withPersistentStore);
  }

  public WebApplicationServer makeWebApplicationServer(String tcConfigPath) throws Exception {
    return makeWebApplicationServer(getTcConfigFile(tcConfigPath));
  }

  public AbstractDBServer makeDBServer(String dbType, String dbName, int serverPort) {
    // XXX this should use server factory
    AbstractDBServer svr = new HSqlDBServer(dbName, serverPort);
    this.addServerToStop(svr);
    return svr;
  }

  public FileSystemPath getTcConfigFile(String tcConfigPath) {
    URL url = getClass().getResource(tcConfigPath);
    Assert.assertNotNull("could not find: " + tcConfigPath, url);
    Assert.assertTrue("should be file:" + url.toString(), url.toString().startsWith("file:"));
    FileSystemPath pathToTcConfigFile = FileSystemPath.makeExistingFile(url.toString().substring("file:".length()));
    return pathToTcConfigFile;
  }

  public WebApplicationServer makeWebApplicationServer(FileSystemPath tcConfigPath) throws Exception {
    int i = ServerManager.appServerIndex++;

    WebApplicationServer tomcatServer = new GenericServer(config, factory, installation, tcConfigPath, i, tempDir);
    addServerToStop(tomcatServer);
    return tomcatServer;
  }

  public WebApplicationServer makeWebApplicationServer(StandardTerracottaAppServerConfig tcConfig) throws Exception {
    int i = ServerManager.appServerIndex++;

    WebApplicationServer tomcatServer = new GenericServer(config, factory, installation, tcConfig, i, tempDir);
    addServerToStop(tomcatServer);
    return tomcatServer;
  }

  void setServersToStop(List serversToStop) {
    this.serversToStop = serversToStop;
  }

  List getServersToStop() {
    return serversToStop;
  }

  public DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return new WARBuilder(warFileName, tempDir, config);
  }

  public DeploymentBuilder makeDeploymentBuilder() throws IOException {
    return new WARBuilder(tempDir, config);
  }

  public StandardTerracottaAppServerConfig getConfig() {
    return factory.createTcConfig(installation.dataDirectory());
  }

  public void stopAllWebServers() {
    for (Iterator it = getServersToStop().iterator(); it.hasNext();) {
      Stoppable stoppable = (Stoppable) it.next();
      try {
        if (!(stoppable instanceof DSOServer || stoppable.isStopped())) stoppable.stop();
      } catch (Exception e) {
        logger.error(stoppable, e);
      }
    }
  }

  public TestConfigObject getTestConfig() {
    return this.config;
  }
}
