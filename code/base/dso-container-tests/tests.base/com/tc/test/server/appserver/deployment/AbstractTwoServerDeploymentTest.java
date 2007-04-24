/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    private String tcConfigFile;
    private String context;
    private final Class testClass;

    private boolean start = true;

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
      
      long l1 = System.currentTimeMillis();
      Deployment deployment = makeWAR();
      long l2 = System.currentTimeMillis();
      logger.info("### WAR build "+ (l2-l1)/1000f + " at " + deployment.getFileSystemPath());

      WebApplicationServer server1 = createServer(deployment);
      WebApplicationServer server2 = createServer(deployment);

      TestSuite suite = (TestSuite) getTest();
      for (int i = 0; i < suite.testCount(); i++) {
        AbstractTwoServerDeploymentTest test = (AbstractTwoServerDeploymentTest) suite.testAt(i);
        test.setServer1(server1);
        test.setServer2(server2);
      }
    }

    private WebApplicationServer createServer(Deployment deployment) throws Exception {
      WebApplicationServer server = sm.makeWebApplicationServer(tcConfigFile);
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
    
    protected boolean shouldDisable() {
      boolean rtv = false;
      if (this.fTest instanceof TestSuite) {
        for (Enumeration e=((TestSuite)fTest).tests(); e.hasMoreElements();) {
          Object o = e.nextElement();
          if (o instanceof AbstractTwoServerDeploymentTest) {
            AbstractTwoServerDeploymentTest t = (AbstractTwoServerDeploymentTest)o;
            if (shouldDisableForJavaVersion(t) || shouldDisableForVariants(t)) {
              rtv = true;
              t.disableAllTests();
            }
          }
        }
      }
      return rtv;
    }

    boolean shouldDisableForVariants(AbstractTwoServerDeploymentTest t) {
      for (Iterator iter=t.disabledVariants.entrySet().iterator(); iter.hasNext();) {
        Map.Entry entry = (Map.Entry)iter.next();
        String variantName = (String)entry.getKey();
        List variants = (List)entry.getValue();
        String selected = this.sm.getTestConfig().selectedVariantFor(variantName);
        if (variants.contains(selected)) {
          logger.warn("Test " + t.getName() + " is disabled for " + variantName + " = " + selected );
          return true;
        }
      }
      return false;
    }

    private boolean shouldDisableForJavaVersion(AbstractTwoServerDeploymentTest t) {
      for (Iterator iter=t.disabledJavaVersion.iterator(); iter.hasNext();) {
        String version = (String)iter.next();
        if (version.equals(System.getProperties().getProperty("java.version"))) {
          logger.warn("Test " + t.getName() + " is disabled for " + version );
          return true;
        }
      }
      return false;
    }

  }

}
