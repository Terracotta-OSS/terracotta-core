/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.terracotta.modules.tool.config.Config;
import org.terracotta.tools.cli.TIMGetTool;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.test.AppServerInfo;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.util.AppServerUtil;
import com.tc.text.Banner;
import com.tc.util.PortChooser;
import com.tc.util.ProductInfo;
import com.tc.util.TIMUtil;
import com.tc.util.TcConfigBuilder;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

public class ServerManager {

  private static final String         SESSION_TIM_TESTS_PROPERTIES = "/com/tctest/session-tim/tests.properties";

  // The internal repository is listed first since it is the preferred repo
  private static final String[]       TIM_GET_URLS                 = {
      "http://forge-dev.terracotta.lan/api/index.xml.gz", "http://forge.terracotta.org/api/index.xml.gz" };

  protected final static TCLogger     logger                       = TCLogging.getLogger(ServerManager.class);
  private static int                  appServerIndex               = 0;
  private final boolean               DEBUG_MODE                   = false;

  private List                        serversToStop                = new ArrayList();
  private DSOServer                   dsoServer;

  private final TestConfigObject      config;
  private final AppServerFactory      factory;
  private final AppServerInstallation installation;
  private final File                  sandbox;
  private final File                  tempDir;
  private final File                  installDir;
  private final File                  warDir;
  private final File                  tcConfigFile;
  private final TcConfigBuilder       serverTcConfig               = new TcConfigBuilder();
  private final Collection            jvmArgs;
  private final boolean               useTimGet;
  private final Map<String, String>   resolved                     = Collections
                                                                       .synchronizedMap(new HashMap<String, String>());

  private static int                  serverCounter                = 0;

  public ServerManager(final Class testClass, Collection extraJvmArgs) throws Exception {
    config = TestConfigObject.getInstance();
    factory = AppServerFactory.createFactoryFromProperties();
    installDir = config.appserverServerInstallDir();
    tempDir = TempDirectoryUtil.getTempDirectory(testClass);
    tcConfigFile = new File(tempDir, "tc-config.xml");
    sandbox = AppServerUtil.createSandbox(tempDir);

    warDir = new File(sandbox, "war");
    jvmArgs = extraJvmArgs;
    installation = AppServerUtil.createAppServerInstallation(factory, installDir, sandbox);

    useTimGet = determineSessionMethod();

    if (DEBUG_MODE) {
      serverTcConfig.setDsoPort(9510);
      serverTcConfig.setJmxPort(9520);
    } else {
      PortChooser pc = new PortChooser();
      serverTcConfig.setDsoPort(pc.chooseRandomPort());
      serverTcConfig.setJmxPort(pc.chooseRandomPort());
    }
  }

  private boolean determineSessionMethod() {
    InputStream in = TIMUtil.class.getResourceAsStream(SESSION_TIM_TESTS_PROPERTIES);
    if (in == null) {
      // no properties supplied, use tim-get to find the session TIM(
      Banner
          .infoBanner(SESSION_TIM_TESTS_PROPERTIES + " not found -- tim.get will be used to resolve container TIM(s)");
      return true;
    }
    return false;
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
    File workDir = new File(tempDir, "dso-server-" + serverCounter++);
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

  public WebApplicationServer makeCoresidentWebApplicationServer(TcConfigBuilder config0, TcConfigBuilder config1,
                                                                 final boolean enableDebug) throws Exception {
    int i = ServerManager.appServerIndex++;
    WebApplicationServer appServer = new GenericServer(config, factory, installation, config0.getTcConfigFile(),
                                                       config1.getTcConfigFile(), i, tempDir, true, enableDebug);
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

    if (useTimGet) {
      aCopy.addRepository(getTimGetModulesDir());
    }

    int appId = config.appServerId();
    switch (appId) {
      case AppServerInfo.GLASSFISH: {
        AppServerInfo info = config.appServerInfo();
        String major = info.getMajor();
        if (major.equals("v1")) {
          aCopy.addModule(TIMUtil.GLASSFISH_V1, resolveContainerTIM(TIMUtil.GLASSFISH_V1));
        } else if (major.equals("v2")) {
          aCopy.addModule(TIMUtil.GLASSFISH_V2, resolveContainerTIM(TIMUtil.GLASSFISH_V2));
        } else {
          throw new RuntimeException("unexpected version: " + info);
        }
        break;
      }
      case AppServerInfo.JETTY: {
        AppServerInfo info = config.appServerInfo();
        String major = info.getMajor();
        String minor = info.getMinor();
        if (major.equals("6") || minor.startsWith("1.")) {
          aCopy.addModule(TIMUtil.JETTY_6_1, resolveContainerTIM(TIMUtil.JETTY_6_1));
        } else {
          throw new RuntimeException("unexpected version: " + info);
        }
        break;
      }
      case AppServerInfo.WASCE: {
        AppServerInfo info = config.appServerInfo();
        String major = info.getMajor();
        String minor = info.getMinor();
        if (major.equals("1") && minor.startsWith("0.")) {
          aCopy.addModule(TIMUtil.WASCE_1_0, resolveContainerTIM(TIMUtil.WASCE_1_0));
        } else {
          throw new RuntimeException("unexpected version: " + info);
        }
        break;
      }
      case AppServerInfo.WEBLOGIC: {
        AppServerInfo info = config.appServerInfo();
        String major = info.getMajor();
        if (major.equals("9")) {
          aCopy.addModule(TIMUtil.WEBLOGIC_9, resolveContainerTIM(TIMUtil.WEBLOGIC_9));
        } else if (major.equals("10")) {
          aCopy.addModule(TIMUtil.WEBLOGIC_10, resolveContainerTIM(TIMUtil.WEBLOGIC_10));
        } else {
          throw new RuntimeException("unexpected major version: " + info);
        }
        break;
      }
      case AppServerInfo.JBOSS: {
        AppServerInfo info = config.appServerInfo();
        String major = info.getMajor();
        String minor = info.getMinor();
        if (major.equals("4")) {
          if (minor.startsWith("0.")) {
            aCopy.addModule(TIMUtil.JBOSS_4_0, resolveContainerTIM(TIMUtil.JBOSS_4_0));
          } else if (minor.startsWith("2.")) {
            aCopy.addModule(TIMUtil.JBOSS_4_2, resolveContainerTIM(TIMUtil.JBOSS_4_2));
          } else {
            throw new RuntimeException("unexpected version: " + info);
          }
        } else if (major.equals("3")) {
          if (minor.startsWith("2.")) {
            aCopy.addModule(TIMUtil.JBOSS_3_2, resolveContainerTIM(TIMUtil.JBOSS_3_2));
          } else {
            throw new RuntimeException("unexpected version: " + info);
          }
        } else {
          throw new RuntimeException("unexpected major version: " + info);
        }
        break;
      }
      case AppServerInfo.TOMCAT: {
        AppServerInfo info = config.appServerInfo();
        String major = info.getMajor();
        String minor = info.getMinor();
        if (major.equals("5")) {
          if (minor.startsWith("0.")) {
            aCopy.addModule(TIMUtil.TOMCAT_5_0, resolveContainerTIM(TIMUtil.TOMCAT_5_0));
          } else if (minor.startsWith("5.")) {
            aCopy.addModule(TIMUtil.TOMCAT_5_5, resolveContainerTIM(TIMUtil.TOMCAT_5_5));
          } else {
            throw new RuntimeException("unexpected 5.x version: " + info);
          }
        } else if (major.equals("6")) {
          if (minor.startsWith("0.")) {
            aCopy.addModule(TIMUtil.TOMCAT_6_0, resolveContainerTIM(TIMUtil.TOMCAT_6_0));
          } else {
            throw new RuntimeException("unexpected 6.x version: " + info);
          }
        } else {
          throw new RuntimeException("unexpected major version: " + info);
        }
        break;
      }
      default:
        // nothing for now
    }
    aCopy.saveToFile();
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

  public File getTcConfigFile() {
    return tcConfigFile;
  }

  @Override
  public String toString() {
    return "ServerManager{" + "dsoServer=" + dsoServer.toString() + ", sandbox=" + sandbox.getAbsolutePath()
           + ", warDir=" + warDir.getAbsolutePath() + ", jvmArgs=" + jvmArgs + '}';
  }

  private String resolveContainerTIM(String name) {
    String ver = resolved.get(name);
    if (ver != null) { return ver; }

    ver = internalResolve(name);
    resolved.put(name, ver);
    return ver;
  }

  private String internalResolve(String name) {
    if (useTimGet) {
      try {
        return runTimGet(name);
      } catch (Exception e) {
        if (e instanceof RuntimeException) throw (RuntimeException) e;
        throw new RuntimeException(e);
      }
    }

    Properties props = new Properties();
    try {
      props.load(TIMUtil.class.getResourceAsStream(SESSION_TIM_TESTS_PROPERTIES));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return props.getProperty("version");
  }

  private String getTimGetModulesDir() {
    File sandoxModules = new File(sandbox, "modules");
    if (!sandoxModules.exists()) {
      if (!sandoxModules.mkdirs()) { throw new RuntimeException("cannot create " + sandoxModules); }
    }
    return sandoxModules.getAbsolutePath();
  }

  private String runTimGet(String name) throws Exception {
    for (String url : TIM_GET_URLS) {
      try {
        System.setProperty(Config.KEYSPACE + Config.TC_VERSION, ProductInfo.getInstance().mavenArtifactsVersion());
        System.setProperty(Config.KEYSPACE + Config.INCLUDE_SNAPSHOTS, "true");
        System.setProperty(Config.KEYSPACE + Config.MODULES_DIR, getTimGetModulesDir());
        System.setProperty(Config.KEYSPACE + Config.DATA_FILE, new File(this.sandbox, "tim-get.index")
            .getAbsolutePath());
        System.setProperty(Config.KEYSPACE + Config.DATA_FILE_URL, url);

        TIMGetTool.mainWithExceptions(new String[] { "install", name });

        // This is a bit of hack, but without some mods to tim-get I'm not sure how to determine the version
        File src = new File(getTimGetModulesDir() + "/org/terracotta/modules/" + name);
        if (!src.isDirectory()) { throw new RuntimeException(src + " is not a directory"); }

        String[] entries = src.list();
        if (entries.length != 1) { throw new RuntimeException("unexpected directory contents ["
                                                              + Arrays.asList(entries) + "] in " + src); }
        return entries[0];
      } catch (Exception e) {
        Banner.errorBanner("Error using url [" + url + "] for tim-get, moving on to the next one");
        e.printStackTrace();
      }
    }

    throw new RuntimeException("Unable to resolve TIM with name " + name + " from any repository");
  }
}
