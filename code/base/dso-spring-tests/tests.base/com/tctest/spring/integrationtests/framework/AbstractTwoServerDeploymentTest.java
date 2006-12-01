/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestSuite;


public abstract class AbstractTwoServerDeploymentTest extends AbstractDeploymentTest {

  public static WebApplicationServer server1;
  public static WebApplicationServer server2;
  
  
  protected static abstract class TwoSvrSetup extends ServerTestSetup {

    Log logger = LogFactory.getLog(getClass());
    
    protected boolean shouldContinue = true;
    
    private String tcConfigFile;
    private String context;
    private final Class testClass;

    protected TwoSvrSetup(Class testClass, String tcConfigFile, String context) {
      super(testClass);
      this.testClass = testClass;
      this.tcConfigFile = tcConfigFile;
      this.context = context;
    }
    
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

    protected void setUp() throws Exception {
      super.setUp();
      
      if (shouldDisable()) {
        this.shouldContinue = false;
      }
      
      setUpTwoWebAppServers();
    }

    protected void setUpTwoWebAppServers() throws Exception {
      long l1 = System.currentTimeMillis();
      Deployment warPath = makeWAR();
      long l2 = System.currentTimeMillis();
      logger.info("### WAR build "+ (l2-l1)/1000f + " at " + warPath.getFileSystemPath());

      server1 = sm.makeWebApplicationServer(tcConfigFile);
      server1.addWarDeployment(warPath, context);
      
      server2 = sm.makeWebApplicationServer(tcConfigFile);
      server2.addWarDeployment(warPath, context);

      server1.start();
      server2.start();
    }

    private Deployment makeWAR() throws Exception {
      DeploymentBuilder builder = super.makeDeploymentBuilder(this.context + ".war");
      builder.addDirectoryOrJARContainingClass(testClass);
      builder.addDirectoryContainingResource(tcConfigFile);
      configureWar(builder);
      return builder.makeDeployment();
    }

    protected abstract void configureWar(DeploymentBuilder builder);
  }

}
