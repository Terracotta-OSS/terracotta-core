/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.property.RemotePropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat5xRemoteContainer;
import org.codehaus.cargo.container.tomcat.Tomcat5xRemoteDeployer;
import org.codehaus.cargo.container.tomcat.TomcatPropertySet;
import org.codehaus.cargo.container.tomcat.TomcatRuntimeConfiguration;
import org.codehaus.cargo.util.log.SimpleLogger;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.httpinvoker.CommonsHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.remoting.rmi.RmiServiceExporter;
import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.management.JMXConnectorProxy;
import com.tc.test.AppServerInfo;
import com.tc.test.TestConfigObject;
import com.tc.test.server.ServerResult;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerFactory;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.util.AppServerUtil;
import com.tc.text.Banner;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.ThreadDump;
import com.tc.util.runtime.Vm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;

import junit.framework.Assert;

public class GenericServer extends AbstractStoppable implements WebApplicationServer {
  private static final Log                  LOG             = LogFactory.getLog(GenericServer.class);
  private static final String               SERVER          = "server_";
  private static final boolean              GC_LOGGGING     = true;
  private static final boolean              ENABLE_DEBUGGER = false;
  private static final ThreadLocal          dsoEnabled      = new ThreadLocal() {
                                                              protected Object initialValue() {
                                                                return Boolean.TRUE;
                                                              }
                                                            };

  private final int                         jmxRemotePort;
  private final int                         rmiRegistryPort;
  private final AppServerFactory            factory;
  private AppServer                         server;
  private final StandardAppServerParameters parameters;
  private ServerResult                      result;
  private final AppServerInstallation       installation;
  private final Map                         proxyBuilderMap = new HashMap();
  private ProxyBuilder                      proxyBuilder    = null;
  private File                              workingDir;
  private String                            serverInstanceName;
  private final File                        tcConfigFile;
  private final File                        coresidentConfigFile;

  public GenericServer(TestConfigObject config, AppServerFactory factory, AppServerInstallation installation,
                       File tcConfigFile, int serverId, File tempDir) throws Exception {
    this(config, factory, installation, tcConfigFile, null, serverId, tempDir, false, false);
  }

  public GenericServer(TestConfigObject config, AppServerFactory factory, AppServerInstallation installation,
                       File tcConfigFile, File coresidentConfigFile, int serverId, File tempDir, boolean coresident,
                       boolean enableDebug) throws Exception {
    this.factory = factory;
    this.installation = installation;
    this.rmiRegistryPort = AppServerUtil.getPort();
    this.jmxRemotePort = AppServerUtil.getPort();
    this.serverInstanceName = SERVER + serverId;
    this.parameters = (StandardAppServerParameters) factory.createParameters(serverInstanceName);
    this.workingDir = new File(installation.sandboxDirectory(), serverInstanceName);
    this.tcConfigFile = tcConfigFile;
    this.coresidentConfigFile = coresidentConfigFile;

    File bootJarFile = new File(config.normalBootJar());

    if (dsoEnabled()) {
      parameters.appendSysProp("tc.base-dir", System.getProperty(TestConfigObject.TC_BASE_DIR));
      parameters.appendSysProp("com.tc.l1.modules.repositories", System.getProperty("com.tc.l1.modules.repositories"));

      if (coresident) {
        parameters.appendSysProp("tc.config", this.tcConfigFile.getAbsolutePath() + "#"
                                              + this.coresidentConfigFile.getAbsolutePath());
        parameters.appendSysProp("tc.dso.globalmode", false);
      } else {
        parameters.appendSysProp("tc.config", this.tcConfigFile.getAbsolutePath());
      }

      parameters.appendJvmArgs("-Xbootclasspath/p:" + bootJarFile.getAbsolutePath());
      parameters.appendSysProp("tc.classpath", writeTerracottaClassPathFile());
      parameters.appendSysProp("tc.session.classpath", config.sessionClasspath());
    }

    if (!Vm.isIBM() && !(Os.isMac() && Vm.isJDK14())) {
      parameters.appendJvmArgs("-XX:+HeapDumpOnOutOfMemoryError");
    }

    int appId = config.appServerId();
    // glassfish fails with these options on
    if (appId != AppServerInfo.GLASSFISH) {
      parameters.appendSysProp("com.sun.management.jmxremote");
      parameters.appendSysProp("com.sun.management.jmxremote.authenticate", false);
      parameters.appendSysProp("com.sun.management.jmxremote.ssl", false);
      parameters.appendSysProp("com.sun.management.jmxremote.port", this.jmxRemotePort);
    }

    parameters.appendSysProp("rmi.registry.port", this.rmiRegistryPort);

    String[] params = { "tc.classloader.writeToDisk", "tc.objectmanager.dumpHierarchy", "aspectwerkz.deployment.info",
        "aspectwerkz.details", "aspectwerkz.gen.closures", "aspectwerkz.dump.pattern", "aspectwerkz.dump.closures",
        "aspectwerkz.dump.factories", "aspectwerkz.aspectmodules" };
    for (int i = 0; i < params.length; i++) {
      if (Boolean.getBoolean(params[i])) {
        parameters.appendSysProp(params[i], true);
      }
    }

    enableDebug(serverId, enableDebug);

    // app server specific system props
    switch (appId) {
      case AppServerInfo.TOMCAT:
      case AppServerInfo.JBOSS:
        parameters.appendJvmArgs("-Djvmroute=" + serverInstanceName);
        break;
      case AppServerInfo.WEBSPHERE:
        parameters.appendSysProp("javax.management.builder.initial", "");
        break;
      case AppServerInfo.WEBLOGIC:
        // bumped up because ContainerHibernateTest was failing with WL 9
        parameters.appendJvmArgs("-XX:MaxPermSize=128m");
        parameters.appendJvmArgs("-Xms128m -Xmx256m");
        break;
    }

    if (TestConfigObject.getInstance().isSpringTest()) {
      LOG.debug("Creating proxy for Spring test...");
      proxyBuilderMap.put(RmiServiceExporter.class, new RMIProxyBuilder());
      proxyBuilderMap.put(HttpInvokerServiceExporter.class, new HttpInvokerProxyBuilder());
    }
  }

  private static boolean dsoEnabled() {
    return ((Boolean) dsoEnabled.get()).booleanValue();
  }

  public static void setDsoEnabled(boolean b) {
    dsoEnabled.set(Boolean.valueOf(b));
  }

  public StandardAppServerParameters getServerParameters() {
    return parameters;
  }

  public int getPort() {
    if (result == null) { throw new IllegalStateException("Server has not started."); }
    return result.serverPort();
  }

  private void enableDebug(int serverId, final boolean enableDebug) {
    if (GC_LOGGGING && !Vm.isIBM()) {
      parameters.appendJvmArgs("-verbose:gc");
      parameters.appendJvmArgs("-XX:+PrintGCDetails");
      parameters.appendJvmArgs("-XX:+PrintGCTimeStamps");
      parameters.appendJvmArgs("-Xloggc:"
                               + new File(this.installation.sandboxDirectory(), serverInstanceName + "-gc.log")
                                   .getAbsolutePath());
    }

    if (ENABLE_DEBUGGER || enableDebug) {
      int debugPort = 8000 + serverId;
      parameters.appendJvmArgs("-Xdebug");
      parameters.appendJvmArgs("-Xrunjdwp:server=y,transport=dt_socket,address=" + debugPort + ",suspend=y");
      parameters.appendSysProp("aspectwerkz.transform.verbose", true);
      parameters.appendSysProp("aspectwerkz.transform.details", true);
      Banner.warnBanner("Waiting for debugger to connect on port " + debugPort);
    }
  }

  private class RMIProxyBuilder implements ProxyBuilder {
    public Object createProxy(Class serviceType, String url, Map initialContext) throws Exception {
      String rmiURL = "rmi://localhost:" + rmiRegistryPort + "/" + url;
      LOG.debug("Getting proxy for: " + rmiRegistryPort + " on " + result.serverPort());
      Exception e = null;
      for (int i = 5; i > 0; i--) {
        try {
          RmiProxyFactoryBean prfb = new RmiProxyFactoryBean();
          prfb.setServiceUrl(rmiURL);
          prfb.setServiceInterface(serviceType);
          prfb.afterPropertiesSet();
          return prfb.getObject();
        } catch (RemoteLookupFailureException lookupException) {
          e = lookupException;
        }
        Thread.sleep(30 * 1000L);
      }
      throw e;
    }
  }

  public class HttpInvokerProxyBuilder implements ProxyBuilder {
    private HttpClient client;

    public Object createProxy(Class serviceType, String url, Map initialContext) throws Exception {
      String serviceURL = "http://localhost:" + result.serverPort() + "/" + url;
      LOG.debug("Getting proxy for: " + serviceURL);
      HttpInvokerProxyFactoryBean prfb = new HttpInvokerProxyFactoryBean();
      prfb.setServiceUrl(serviceURL);
      prfb.setServiceInterface(serviceType);
      CommonsHttpInvokerRequestExecutor executor;
      if (initialContext != null) {
        client = (HttpClient) initialContext.get(ProxyBuilder.HTTP_CLIENT_KEY);
      }

      if (client == null) {
        executor = new CommonsHttpInvokerRequestExecutor();
        client = executor.getHttpClient();
        if (initialContext != null) {
          initialContext.put(ProxyBuilder.HTTP_CLIENT_KEY, client);
        }
      } else {
        executor = new CommonsHttpInvokerRequestExecutor(client);
      }

      prfb.setHttpInvokerRequestExecutor(executor);
      prfb.afterPropertiesSet();
      return prfb.getObject();
    }

    public HttpClient getClient() {
      return client;
    }

    public void setClient(HttpClient client) {
      this.client = client;
    }
  }

  public Object getProxy(Class serviceType, String url) throws Exception {
    if (this.proxyBuilder != null) { return proxyBuilder.createProxy(serviceType, url, null); }
    Map initCtx = new HashMap();
    initCtx.put(ProxyBuilder.EXPORTER_TYPE_KEY, RmiServiceExporter.class);
    return getProxy(serviceType, url, initCtx);
  }

  public Object getProxy(Class serviceType, String url, Map initialContext) throws Exception {
    Class exporterClass = (Class) initialContext.get(ProxyBuilder.EXPORTER_TYPE_KEY);
    this.proxyBuilder = (ProxyBuilder) proxyBuilderMap.get(exporterClass);
    return this.proxyBuilder.createProxy(serviceType, url, initialContext);
  }

  public MBeanServerConnection getMBeanServerConnection() throws Exception {
    JMXConnectorProxy jmxConnectorProxy = new JMXConnectorProxy("localhost", this.jmxRemotePort);
    return jmxConnectorProxy.getMBeanServerConnection();
  }

  public WebApplicationServer addWarDeployment(Deployment warDeployment, String context) {
    parameters.addWar(context, warDeployment.getFileSystemPath().getFile());
    return this;
  }

  protected void doStart() throws Exception {
    try {
      result = getAppServer().start(parameters);
    } catch (Exception e) {
      dumpThreadsAndRethrow(e);
    }
  }

  private void dumpThreadsAndRethrow(Exception e) throws Exception {
    try {
      if (!Os.isWindows()) {
        ThreadDump.dumpProcessGroup();
      }
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      if (true) throw e; // if (true) used to silence warning
    }
  }

  protected void doStop() throws Exception {
    try {
      server.stop();
    } catch (Exception e) {
      dumpThreadsAndRethrow(e);
    }
  }

  /**
   * url: /<CONTEXT>/<MAPPING>?params=etc
   */
  public WebResponse ping(String url) throws MalformedURLException, IOException, SAXException {
    return ping(url, new WebConversation());
  }

  /**
   * url: /<CONTEXT>/<MAPPING>?params=etc
   */
  public WebResponse ping(String url, WebConversation wc) throws MalformedURLException, IOException, SAXException {
    String fullURL = "http://localhost:" + result.serverPort() + url;
    LOG.debug("Getting page: " + fullURL);

    wc.setExceptionsThrownOnErrorStatus(false);
    WebResponse response = wc.getResponse(fullURL);
    Assert.assertEquals("Server error:\n" + response.getText(), 200, response.getResponseCode());
    LOG.debug("Got page: " + fullURL);
    return response;
  }

  public void redeployWar(Deployment warDeployment, String context) {
    getRemoteDeployer().redeploy(makeWar(context, warDeployment.getFileSystemPath()));
  }

  public void deployWar(Deployment warDeployment, String context) {
    getRemoteDeployer().deploy(makeWar(context, warDeployment.getFileSystemPath()));
  }

  public void undeployWar(Deployment warDeployment, String context) {
    getRemoteDeployer().undeploy(makeWar(context, warDeployment.getFileSystemPath()));
  }

  // TODO - CARGO specific code

  private WAR makeWar(String warContext, FileSystemPath warPath) {
    WAR war = new WAR(warPath.toString());
    war.setContext(warContext);
    war.setLogger(new SimpleLogger());
    return war;
  }

  // TODO - Tomcat specific code

  private Tomcat5xRemoteDeployer getRemoteDeployer() {
    TomcatRuntimeConfiguration runtimeConfiguration = new TomcatRuntimeConfiguration();
    runtimeConfiguration.setProperty(RemotePropertySet.USERNAME, "admin");
    runtimeConfiguration.setProperty(RemotePropertySet.PASSWORD, "");
    runtimeConfiguration.setProperty(TomcatPropertySet.MANAGER_URL, "http://localhost:" + result.serverPort()
                                                                    + "/manager");

    Tomcat5xRemoteContainer remoteContainer = new Tomcat5xRemoteContainer(runtimeConfiguration);
    Tomcat5xRemoteDeployer deployer = new Tomcat5xRemoteDeployer(remoteContainer);
    return deployer;
  }

  // end tomcat specific code

  private String writeTerracottaClassPathFile() {
    FileOutputStream fos = null;

    try {
      File tempFile = new File(installation.sandboxDirectory(), "tc-classpath." + parameters.instanceName());
      fos = new FileOutputStream(tempFile);

      // XXX: total hack to make RequestCountTest pass on 1.4 VMs
      String[] paths = System.getProperty("java.class.path").split(File.pathSeparator);
      StringBuffer cp = new StringBuffer();
      for (int i = 0; i < paths.length; i++) {
        String path = paths[i];
        if (path.endsWith("jboss-jmx-4.0.5.jar")) {
          continue;
        }
        cp.append(path).append(File.pathSeparatorChar);
      }

      fos.write(cp.toString().getBytes());

      return tempFile.toURI().toString();
    } catch (IOException ioe) {
      throw new AssertionError(ioe);
    } finally {
      IOUtils.closeQuietly(fos);
    }

  }

  public Server restart() throws Exception {
    stop();
    start();
    return this;
  }

  public String toString() {
    return "Generic Server" + (result != null ? "; port:" + result.serverPort() : "");
  }

  public File getWorkingDirectory() {
    return workingDir;
  }

  public AppServer getAppServer() {
    if (server == null) {
      server = factory.createAppServer(installation);
    }
    return server;
  }

  public File getTcConfigFile() {
    return tcConfigFile;
  }

  public File getCoresidentConfigFile() {
    return coresidentConfigFile;
  }
}
