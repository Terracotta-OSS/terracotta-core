/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.util.TcConfigBuilder;

import junit.framework.Test;
import junit.framework.TestSuite;

public abstract class AbstractTwoServerDeploymentTest extends AbstractDeploymentTest {
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
  
  private static abstract class TwoServerTestSetupBase extends ServerTestSetup {
    private final Log              logger = LogFactory.getLog(getClass());

    private final Class            testClass;
    private final String           context0;
    private final String           context1;
    private final TcConfigBuilder  tcConfigBuilder;

    private boolean                start  = true;

    protected WebApplicationServer server0;
    protected WebApplicationServer server1;

    protected TwoServerTestSetupBase(Class testClass, TcConfigBuilder configBuilder, String context0, String context1) {
      super(testClass);
      this.testClass = testClass;
      this.context0 = context0;
      this.context1 = context1;
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
        Deployment deployment0 = makeWAR(0);
        long l2 = System.currentTimeMillis();
        logger.info("### WAR build 0 " + (l2 - l1) / 1000f + " at " + deployment0.getFileSystemPath());
        Deployment deployment1 = null;
        if (null != context1) {
          deployment1 = makeWAR(1);
          long l3 = System.currentTimeMillis();
          logger.info("### WAR build 1 " + (l3 - l2) / 1000f + " at " + deployment1.getFileSystemPath());
        }

        configureTcConfig(tcConfigBuilder);
        server0 = createServer(deployment0, context0);
        if (null != deployment1) {
          server1 = createServer(deployment1, context1);
        } else {
          server1 = createServer(deployment0, context0);
        }

        TestSuite suite = (TestSuite) getTest();
        for (int i = 0; i < suite.testCount(); i++) {
          Test t = suite.testAt(i);
          if (t instanceof AbstractTwoServerDeploymentTest) {
            AbstractTwoServerDeploymentTest test = (AbstractTwoServerDeploymentTest) t;
            test.setServer0(server0);
            test.setServer1(server1);
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

    private WebApplicationServer createServer(Deployment deployment, String context) throws Exception {
      WebApplicationServer server = getServerManager().makeWebApplicationServer(tcConfigBuilder);
      configureServerParamers(server.getServerParameters());
      server.addWarDeployment(deployment, context);
      if (start) {
        server.start();
      }
      return server;
    }

    private Deployment makeWAR(int server) throws Exception {
      String context = (server == 0) ? context0 : context1;
      DeploymentBuilder builder = makeDeploymentBuilder(
          context + ".war");
      builder.addDirectoryOrJARContainingClass(testClass);
      configureWar(server, builder);
      return builder.makeDeployment();
    }

    /**
     * Override this method to add directories or jars to the builder
     */
    protected abstract void configureWar(int server, DeploymentBuilder builder);

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      // override this method to modify tc-config.xml
    }

    protected void configureServerParamers(StandardAppServerParameters params) {
      // override this method to modify jvm args for app server
    }
  }

  /**
   * Use this setup for two servers both running the same app context.
   */
  public static abstract class TwoServerTestSetup extends TwoServerTestSetupBase {
    protected TwoServerTestSetup(Class testClass, String context) {
      this(testClass, new TcConfigBuilder(), context);
    }

    protected TwoServerTestSetup(Class testClass, String tcConfigFile, String context) {
      this(testClass, new TcConfigBuilder(tcConfigFile), context);
    }

    protected TwoServerTestSetup(Class testClass, TcConfigBuilder configBuilder, String context) {
      super(testClass, configBuilder, context, null);
    }
    
    final protected void configureWar(int server, DeploymentBuilder builder) {
      configureWar(builder);
    }
    
    protected abstract void configureWar(DeploymentBuilder builder);
  }
  
  /**
   * Use this setup for two servers, each with its own context.
   * The test class and config will still be shared by both.  To add
   * additional classes to just one of the contexts, override
   * {@link #configureWar(int, DeploymentBuilder)}.
   */
  public static abstract class TwoContextTestSetup extends TwoServerTestSetupBase {
    protected TwoContextTestSetup(Class testClass, String tcConfigFile, String context0, String context1) {
      super(testClass, new TcConfigBuilder(tcConfigFile), context0, context1);
    }

  }
}
