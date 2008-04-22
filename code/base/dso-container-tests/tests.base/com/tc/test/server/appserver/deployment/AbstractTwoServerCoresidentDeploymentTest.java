/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.util.TcConfigBuilder;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

public abstract class AbstractTwoServerCoresidentDeploymentTest extends AbstractDeploymentTest {
  public WebApplicationServer server0;
  public WebApplicationServer server1;

  public void setServer0(WebApplicationServer server0) {
    this.server0 = server0;
  }

  public void setServer1(WebApplicationServer server1) {
    this.server1 = server1;
  }

  protected boolean shouldKillAppServersEachRun() {
    return false;
  }

  public static abstract class TwoServerCoresidentTestSetup extends CoresidentServerTestSetup {
    private Log logger = LogFactory.getLog(getClass());

    private final Class testClass;
    private final String context;
    private final TcConfigBuilder tcConfigBuilder;

    private boolean start = true;

    protected WebApplicationServer server0;
    protected WebApplicationServer server1;

    protected TwoServerCoresidentTestSetup(Class testClass, String context) {
      this(testClass, new TcConfigBuilder(), context);
    }

    protected TwoServerCoresidentTestSetup(Class testClass, String tcConfigFile, String context) {
      this(testClass, new TcConfigBuilder(tcConfigFile), context);
    }

    protected TwoServerCoresidentTestSetup(Class testClass, TcConfigBuilder configBuilder, String context) {
      super(testClass);
      this.testClass = testClass;
      this.context = context;
      this.tcConfigBuilder = configBuilder;
    }

    protected void setStart(boolean start) {
      this.start = start;
    }

    protected void setUp() throws Exception {
      if (shouldDisable()) return;
      super.setUp();
      try {
        getServerManagers();

        Deployment deployment = null;
        for (int i = 0; i < getServerManagers().length; i++) {
          ServerManager serverManager = getServerManagers()[i];
          long l1 = System.currentTimeMillis();
          deployment = makeWAR(serverManager);
          long l2 = System.currentTimeMillis();
          logger.info("### WAR build " + (l2 - l1) / 1000f + " at " + deployment.getFileSystemPath() + " for " + serverManager);
        }

        configureTcConfig(tcConfigBuilder);
        server0 = createServer(deployment, enableContainerDebug(0));
        server1 = createServer(deployment, enableContainerDebug(1));

        TestSuite suite = (TestSuite)getTest();
        for (int i = 0; i < suite.testCount(); i++) {
          Test t = suite.testAt(i);
          if (t instanceof AbstractTwoServerCoresidentDeploymentTest) {
            AbstractTwoServerCoresidentDeploymentTest test = (AbstractTwoServerCoresidentDeploymentTest)t;
            test.setServer0(server0);
            test.setServer1(server1);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
    }

    protected boolean enableContainerDebug(final int serverId) {
      return false;
    }

    private WebApplicationServer createServer(Deployment deployment, boolean enableDebug) throws Exception {
      TcConfigBuilder config0 = tcConfigBuilder.copy();
      prepareClientConfig(config0, getServerManagers()[0].getServerTcConfig());
      TcConfigBuilder config1 = tcConfigBuilder.copy();
      prepareClientConfig(config1, getServerManagers()[1].getServerTcConfig());
      //use server_0's sandbox
      WebApplicationServer server = getServerManagers()[0].makeCoresidentWebApplicationServer(config0, config1, enableDebug);
      configureServerParamers(server.getServerParameters());
      server.addWarDeployment(deployment, context);
      if (start) {
        server.start();
      }
      return server;
    }

    private void prepareClientConfig(final TcConfigBuilder clientConfig, final TcConfigBuilder serverTcConfig) throws IOException {
      clientConfig.setDsoPort(serverTcConfig.getDsoPort());
      clientConfig.setJmxPort(serverTcConfig.getJmxPort());
      clientConfig.setTcConfigFile(new File(serverTcConfig.getTcConfigFile().getParentFile(), "tc-client-config.xml"));
      clientConfig.saveToFile();
    }

    private Deployment makeWAR(ServerManager serverManager) throws Exception {
      DeploymentBuilder builder = makeDeploymentBuilder(serverManager, this.context + ".war");
      builder.addDirectoryOrJARContainingClass(testClass);
      configureWar(builder);
      return builder.makeDeployment();
    }

    protected abstract void configureWar(DeploymentBuilder builder);

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      // override this method to modify tc-config.xml
    }

    protected void configureServerParamers(StandardAppServerParameters params) {
      // override this method to modify jvm args for app server
    }
  }

}