/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.httpclient.HttpClient;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.property.RemotePropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat5xRemoteContainer;
import org.codehaus.cargo.container.tomcat.Tomcat5xRemoteDeployer;
import org.codehaus.cargo.container.tomcat.TomcatPropertySet;
import org.codehaus.cargo.container.tomcat.TomcatRuntimeConfiguration;
import org.codehaus.cargo.util.log.SimpleLogger;
import org.springframework.jmx.support.MBeanServerConnectionFactoryBean;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.httpinvoker.CommonsHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.remoting.rmi.RmiServiceExporter;
import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.TestConfigObject;
import com.tc.test.server.ServerResult;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;
import com.tc.test.server.tcconfig.TerracottaServerConfigGenerator;
import com.tc.test.server.util.AppServerUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;

import junit.framework.Assert;

public class GenericServer extends AbstractStoppable implements WebApplicationServer {

  private int                         jmxRemotePort;

  private int                         rmiRegistryPort;

  int                                 contextId       = 1;

  private NewAppServerFactory         factory;

  private AppServer                   server;

  private StandardAppServerParameters parameters;

  private ServerResult                result;

  private final AppServerInstallation installation;

  private final Map                   proxyBuilderMap = new HashMap();

  static final boolean                MONKEY_MODE     = true;

  private ProxyBuilder                proxyBuilder    = null;

  public GenericServer(TestConfigObject config, NewAppServerFactory factory, AppServerInstallation installation,
                       FileSystemPath tcConfigPath, int serverId, File tempDir) throws Exception {
    this(config, factory, installation, new SpringTerracottaAppServerConfig(tcConfigPath.getFile()), serverId, tempDir);
  }

  public GenericServer(TestConfigObject config, NewAppServerFactory factory, AppServerInstallation installation,
                       StandardTerracottaAppServerConfig terracottaConfig, int serverId, File tempDir) throws Exception {
    this.factory = factory;
    this.installation = installation;
    this.rmiRegistryPort = AppServerUtil.getPort();
    this.jmxRemotePort = AppServerUtil.getPort();

    parameters = (StandardAppServerParameters) factory.createParameters("server_" + serverId);

    TerracottaServerConfigGenerator configGenerator = new TerracottaServerConfigGenerator(tempDir, terracottaConfig);
    File bootJarFile = new File(config.normalBootJar());

    /*
     * String[] commandLine = new String[] { "-f", configGenerator.configPath()};
     * StandardTVSConfigurationSetupManagerFactory configManagerFactory = // new
     * StandardTVSConfigurationSetupManagerFactory(commandLine, false, new FatalIllegalConfigurationChangeHandler());
     * 
     * boolean quiet = false; TCLogger tclogger = quiet ? new NullTCLogger() : CustomerLogging.getConsoleLogger();
     * L1TVSConfigurationSetupManager configManager =
     * configManagerFactory.createL1TVSConfigurationSetupManager(tclogger);
     * 
     * ClassLoader systemLoader = ClassLoader.getSystemClassLoader(); StandardDSOClientConfigHelper configHelper = new
     * StandardDSOClientConfigHelper(configManager, false);
     * 
     * 
     * new BootJarTool(configHelper, bootJarFile, systemLoader, quiet).generateJar();
     */

    parameters.enableDSO(configGenerator, bootJarFile);
    parameters.appendSysProp("com.sun.management.jmxremote");
    parameters.appendSysProp("com.sun.management.jmxremote.authenticate", false);
    parameters.appendSysProp("com.sun.management.jmxremote.ssl", false);

    // needed for websphere jmx bug
    if (NewAppServerFactory.WEBSPHERE.equals(config.appserverFactoryName())) {
      parameters.appendSysProp("javax.management.builder.initial", "");
    }

    parameters.appendSysProp("com.sun.management.jmxremote.port", this.jmxRemotePort);
    parameters.appendSysProp("rmi.registry.port", this.rmiRegistryPort);

    String[] params = { "tc.classloader.writeToDisk", "tc.objectmanager.dumpHierarchy", "aspectwerkz.deployment.info",
        "aspectwerkz.details", "aspectwerkz.gen.closures", "aspectwerkz.dump.pattern", "aspectwerkz.dump.closures",
        "aspectwerkz.dump.factories", "aspectwerkz.aspectmodules" };
    for (int i = 0; i < params.length; i++) {
      if (Boolean.getBoolean(params[i])) {
        parameters.appendSysProp(params[i], true);
      }
    }

    if (!MONKEY_MODE) {
      int debugPort = AppServerUtil.getPort();
      logger.info("Debug port=" + debugPort);
      parameters.appendJvmArgs(" -Xdebug -Xrunjdwp:transport=dt_socket,address=" + debugPort + ",server=y,suspend=n ");
      // -Daspectwerkz.transform.verbose=true -Daspectwerkz.transform.details=true
      parameters.appendSysProp("aspectwerkz.transform.verbose", true);
      parameters.appendSysProp("aspectwerkz.transform.details", true);
    }

    parameters.appendSysProp("tc.tests.configuration.modules.url", System
        .getProperty("tc.tests.configuration.modules.url"));

    proxyBuilderMap.put(RmiServiceExporter.class, new RMIProxyBuilder());
    proxyBuilderMap.put(HttpInvokerServiceExporter.class, new HttpInvokerProxyBuilder());
  }

  public StandardAppServerParameters getServerParameters() {
    return parameters;
  }

  private class RMIProxyBuilder implements ProxyBuilder {
    public Object createProxy(Class serviceType, String url, Map initialContext) throws Exception {
      String rmiURL = "rmi://localhost:" + rmiRegistryPort + "/" + url;
      logger.debug("Getting proxy for: " + rmiRegistryPort + " on " + result.serverPort());
      Exception e = null;
      for (int i = 5; i >= 0; i++) {
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
      logger.debug("Getting proxy for: " + serviceURL);
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
    MBeanServerConnectionFactoryBean factoryBean = new MBeanServerConnectionFactoryBean();
    factoryBean.setServiceUrl("service:jmx:rmi:///jndi/rmi://localhost:" + this.jmxRemotePort + "/jmxrmi");
    factoryBean.afterPropertiesSet();
    return (MBeanServerConnection) factoryBean.getObject();
  }

  public WebApplicationServer addWarDeployment(Deployment warDeployment, String context) {
    parameters.addWar(context, warDeployment.getFileSystemPath().getFile());
    return this;
  }

  protected void doStart() throws Exception {
    server = factory.createAppServer(installation);
    result = server.start(parameters);
  }

  protected void doStop() throws Exception {
    server.stop();
  }

  public WebResponse ping(String url) throws MalformedURLException, IOException, SAXException {
    return ping(url, new WebConversation());
  }

  public WebResponse ping(String url, WebConversation wc) throws MalformedURLException, IOException, SAXException {
    String fullURL = "http://localhost:" + result.serverPort() + url;
    logger.debug("Getting page: " + fullURL);

    wc.setExceptionsThrownOnErrorStatus(false);
    WebResponse response = wc.getResponse(fullURL);
    Assert.assertEquals("Server error:\n" + response.getText(), 200, response.getResponseCode());
    logger.debug("Got page: " + fullURL);
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

  public Server restart() throws Exception {
    stop();
    start();
    return this;
  }

  public String toString() {
    return "Generic Server" + (result != null ? "; port:" + result.serverPort() : "");
  }
}
