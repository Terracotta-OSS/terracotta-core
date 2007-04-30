/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestSuite;


public abstract class AbstractTwoServerDeploymentTest extends AbstractDeploymentTest {
  public WebApplicationServer server1;
  public WebApplicationServer server2;

  public void setServer1(WebApplicationServer server1) {
    this.server1 = server1;
  }

  public void setServer2(WebApplicationServer server2) {
    this.server2 = server2;
  }  
  
  
  public static abstract class TwoServerTestSetup extends ServerTestSetup {
    private Log logger = LogFactory.getLog(getClass());

    private final Class testClass;
    private final String tcConfigFile;
    private final String context;

    private boolean start = true;

    protected WebApplicationServer server1;
    protected WebApplicationServer server2;

    protected TwoServerTestSetup(Class testClass, String tcConfigFile, String context) {
      super(testClass);
      this.testClass = testClass;
      this.tcConfigFile = tcConfigFile;
      this.context = context;
    }
    
    protected void setStart(boolean start) {
      this.start = start;
    }

    protected void setUp() throws Exception {
      super.setUp();
      
      if(shouldDisable()) return;
      
      try {
        long l1 = System.currentTimeMillis();
        Deployment deployment = makeWAR();
        long l2 = System.currentTimeMillis();
        logger.info("### WAR build "+ (l2-l1)/1000f + " at " + deployment.getFileSystemPath());
  
        server1 = createServer(deployment);
        server2 = createServer(deployment);
        
        TestSuite suite = (TestSuite) getTest();
        for (int i = 0; i < suite.testCount(); i++) {
          AbstractTwoServerDeploymentTest test = (AbstractTwoServerDeploymentTest) suite.testAt(i);
          test.setServer1(server1);
          test.setServer2(server2);
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
    }

    private WebApplicationServer createServer(Deployment deployment) throws Exception {
      WebApplicationServer server = getServerManager().makeWebApplicationServer(tcConfigFile);
      server.addWarDeployment(deployment, context);
      if(start) {
        server.start();
      }
      return server;
    }
    
    private Deployment makeWAR() throws Exception {
      DeploymentBuilder builder = makeDeploymentBuilder(this.context + ".war");
      builder.addDirectoryOrJARContainingClass(testClass);
      builder.addDirectoryContainingResource(tcConfigFile);
      configureWar(builder);
      return builder.makeDeployment();
    }
    
    protected abstract void configureWar(DeploymentBuilder builder);
  
  }
  
}
