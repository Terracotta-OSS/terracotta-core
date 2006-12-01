/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tctest.spring.bean.IMasterBean;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.Deployment;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;
import com.tctest.spring.integrationtests.framework.ServerTestSetup;

import junit.framework.Test;

/**
 * Test application context loaded from multiple bean configs. 
 * 
 * @author Eugene Kuleshov
 */
public class MultifileContextTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME = "MasterService";
  
  
  public void testBeanFromMultipleContexts() throws Exception {
    server1.start();
    server2.start();

    try {
      
    IMasterBean masterBean1 = (IMasterBean) server1.getProxy(IMasterBean.class, REMOTE_SERVICE_NAME);
    IMasterBean masterBean2 = (IMasterBean) server2.getProxy(IMasterBean.class, REMOTE_SERVICE_NAME);

    ISingleton singleton1 = masterBean1.getSingleton();
    ISingleton singleton2 = masterBean2.getSingleton();
    
    assertNotNull(singleton1);
    assertNotNull(singleton2);
    
    } finally {
      server1.stop();
      server2.stop();
    }
  }


  private static class MultipleBeanDefsTestSetup extends ServerTestSetup {

    private MultipleBeanDefsTestSetup() {
      super(MultifileContextTest.class);
    }

    protected void setUp() throws Exception {
      super.setUp();
      
      String tcConfigFile = "/tc-config-files/multifile-context-tc-config.xml";
      String context = "test-multifile-context";

      DeploymentBuilder builder = super.makeDeploymentBuilder(context+".war");
      
      builder.addDirectoryOrJARContainingClass(MultifileContextTest.class);
      builder.addDirectoryContainingResource(tcConfigFile);
      
      builder.addBeanDefinitionFile("/WEB-INF/multifile-context-factory*.xml");
      
      builder.addResource("/com/tctest/spring", "multifile-context-factory1.xml", "/WEB-INF/");
      builder.addResource("/com/tctest/spring", "multifile-context-factory2.xml", "/WEB-INF/");
      
      builder.addRemoteService(REMOTE_SERVICE_NAME, "distributedMasterBean", IMasterBean.class);
      
      Deployment warPath = builder.makeDeployment();

      server1 = sm.makeWebApplicationServer(tcConfigFile);
      server1.addWarDeployment(warPath, context);
      
      server2 = sm.makeWebApplicationServer(tcConfigFile);
      server2.addWarDeployment(warPath, context);
    }

  }

  
  public static Test suite() {
    return new MultipleBeanDefsTestSetup();
  }

}
