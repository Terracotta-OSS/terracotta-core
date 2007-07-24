/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.util.TcConfigBuilder;

import junit.framework.Test;
import junit.framework.TestSuite;

public abstract class AbstractOneServerDeploymentTest extends AbstractDeploymentTest {
  public WebApplicationServer server1;

  public void setServer1(WebApplicationServer server1) {
    this.server1 = server1;
  }

  protected boolean shouldKillAppServersEachRun() {
    return false;
  }

  public static abstract class OneServerTestSetup extends ServerTestSetup {
    private Log                    logger = LogFactory.getLog(getClass());

    private final Class            testClass;
    private final String           context;
    private final TcConfigBuilder  tcConfigBuilder;

    private boolean                start  = true;

    protected WebApplicationServer server1;

    protected OneServerTestSetup(Class testClass, String context) {
      this(testClass, new TcConfigBuilder(), context);
    }
    
    protected OneServerTestSetup(Class testClass, String tcConfigFile, String context) {
      this(testClass, new TcConfigBuilder(tcConfigFile), context);
    }

    protected OneServerTestSetup(Class testClass, TcConfigBuilder configBuilder, String context) {
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
        getServerManager();

        long l1 = System.currentTimeMillis();
        Deployment deployment = makeWAR();
        long l2 = System.currentTimeMillis();
        logger.info("### WAR build " + (l2 - l1) / 1000f + " at " + deployment.getFileSystemPath());

        configureTcConfig(tcConfigBuilder);
        server1 = createServer(deployment);

        TestSuite suite = (TestSuite) getTest();
        for (int i = 0; i < suite.testCount(); i++) {
          Test t = suite.testAt(i);
          if (t instanceof AbstractOneServerDeploymentTest) {
            AbstractOneServerDeploymentTest test = (AbstractOneServerDeploymentTest) t;
            test.setServer1(server1);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
    }

    private WebApplicationServer createServer(Deployment deployment) throws Exception {
      WebApplicationServer server = getServerManager().makeWebApplicationServer(tcConfigBuilder);
      server.addWarDeployment(deployment, context);
      configureServerParamers(server.getServerParameters());
      if (start) {
        server.start();
      }
      return server;
    }

    private Deployment makeWAR() throws Exception {
      DeploymentBuilder builder = makeDeploymentBuilder(this.context + ".war");
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
