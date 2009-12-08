/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.util.TcConfigBuilder;

import junit.framework.Test;
import junit.framework.TestSuite;

public abstract class AbstractStandaloneOneServerDeploymentTest extends AbstractDeploymentTest {
  public WebApplicationServer server0;

  public AbstractStandaloneOneServerDeploymentTest() {
    if (commitTimeoutTaskAdded(false, true)) {
      scheduleTimeoutTask();
    }
  }

  public void setServer0(WebApplicationServer server0) {
    this.server0 = server0;
  }

  @Override
  protected boolean shouldKillAppServersEachRun() {
    return false;
  }

  public static abstract class StandaloneOneServerTestSetup extends ServerTestSetup {
    private final Log              logger = LogFactory.getLog(getClass());

    private final Class            testClass;
    private final String           context;
    private final TcConfigBuilder  tcConfigBuilder;

    private boolean                start  = true;

    protected WebApplicationServer server0;

    protected StandaloneOneServerTestSetup(Class testClass, String context) {
      this(testClass, new TcConfigBuilder(), context);
    }

    protected StandaloneOneServerTestSetup(Class testClass, String tcConfigFile, String context) {
      this(testClass, new TcConfigBuilder(tcConfigFile), context);
    }

    protected StandaloneOneServerTestSetup(Class testClass, TcConfigBuilder configBuilder, String context) {
      super(testClass);
      this.testClass = testClass;
      this.context = context;
      this.tcConfigBuilder = configBuilder;
    }

    protected void setStart(boolean start) {
      this.start = start;
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      // override this method to modify tc-config.xml
    }

    @Override
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
        server0 = createServer(deployment);

        TestSuite suite = (TestSuite) getTest();
        for (int i = 0; i < suite.testCount(); i++) {
          Test t = suite.testAt(i);
          if (t instanceof AbstractStandaloneOneServerDeploymentTest) {
            AbstractStandaloneOneServerDeploymentTest test = (AbstractStandaloneOneServerDeploymentTest) t;
            test.setServer0(server0);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        ServerManager sm = getServerManager();
        if (sm != null) {
          sm.stop();
        }
        throw e;
      }
    }

    private WebApplicationServer createServer(Deployment deployment) throws Exception {
      WebApplicationServer server = getServerManager().makeWebApplicationServerNoDso();
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

    protected void configureServerParamers(StandardAppServerParameters params) {
      // override this method to modify jvm args for app server
    }
  }

}
