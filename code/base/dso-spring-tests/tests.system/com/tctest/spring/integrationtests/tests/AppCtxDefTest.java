/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tctest.spring.integrationtests.tests;

import com.tctest.spring.bean.AppCtxDefBean;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.Deployment;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;
import com.tctest.spring.integrationtests.framework.ServerTestSetup;
import com.tctest.spring.integrationtests.framework.WebApplicationServer;

import junit.framework.Test;


/**
 * Test ApplicationContext definition
 * 
 * LKC-1762: Test: ApplicationContext/Bean definition test
 * https://jira.terracotta.lan/jira/browse/LKC-1762
 * 
 * Single definition file
 * Multiple bean definition files
 * Include test for circular dependencies between files
 * Tests verify that AppCtx/BeanFactory ends up being distributed/non-distributed
 */
public class AppCtxDefTest extends AbstractTwoServerDeploymentTest {

  public static Test suite() {
    return new AppContextDefinitionTestSetup();
  }

  
  public void testAppCtxDef1local() throws Exception {
    verifyClustered("appCtxDef1local_beanA",  22);
  }
  
  public void testAppCtxDef1shared() throws Exception {
    verifyClustered("appCtxDef1shared_beanA", 23);
  }
  
  public void testAppCtxDef2local() throws Exception {
    verifyClustered("appCtxDef2local_beanB",  32);
    verifyClustered("appCtxDef2local_beanC",  33);
  }

  public void testAppCtxDef2shared() throws Exception {
    verifyClustered("appCtxDef2shared_beanB", 35);
    verifyClustered("appCtxDef2shared_beanC", 36);
  }
  
  
  private void verifyClustered(String service, int n) throws Exception {
    boolean isLocal = service.indexOf("local")>-1;
    
    AppCtxDefBean bean1 = (AppCtxDefBean) server1.getProxy(AppCtxDefBean.class, service);
    AppCtxDefBean bean2 = (AppCtxDefBean) server2.getProxy(AppCtxDefBean.class, service);
    
    bean1.setValue(n);
    assertTrue("Expected "+(isLocal ? "local " : "shared ")+service, n==bean2.getValue() ? !isLocal : isLocal);
    
    bean2.setValue(n+11);
    assertTrue("Expected "+(isLocal ? "local " : "shared ")+service, (n+11)==bean1.getValue() ? !isLocal : isLocal);
  }

  
  private static class AppContextDefinitionTestSetup extends ServerTestSetup {

    private AppContextDefinitionTestSetup() {
      super(AppCtxDefTest.class);
    }

    protected void setUp() throws Exception {
      super.setUp();

      server1 = createServer();
      server2 = createServer();
    }

    private WebApplicationServer createServer() throws Exception {
      WebApplicationServer server = sm.makeWebApplicationServer("/tc-config-files/appctxdef-tc-config.xml");
      
      server.addWarDeployment(createAppCtxDef1war("appCtxDef1local"),  "appCtxDef1local");
      server.addWarDeployment(createAppCtxDef1war("appCtxDef1shared"), "appCtxDef1shared");
      server.addWarDeployment(createAppCtxDef2war("appCtxDef2local"),  "appCtxDef2local");
      server.addWarDeployment(createAppCtxDef2war("appCtxDef2shared"), "appCtxDef2shared");
      server.start();
      
      return server;
    }

    private Deployment createAppCtxDef1war(String warName) throws Exception {
      return createWarTemplate(warName)
          .addBeanDefinitionFile("classpath:/com/tctest/spring/appCtxDef1.xml")
          .addRemoteService(warName+"_beanA", "bean1", AppCtxDefBean.class)
          .makeDeployment();
    }

    private Deployment createAppCtxDef2war(String warName) throws Exception {
      return createWarTemplate(warName)
          .addBeanDefinitionFile("classpath:/com/tctest/spring/appCtxDef2a.xml")
          .addBeanDefinitionFile("classpath:/com/tctest/spring/appCtxDef2.xml")
          .addRemoteService(warName+"_beanB", "bean1", AppCtxDefBean.class)
          .addRemoteService(warName+"_beanC", "bean2", AppCtxDefBean.class)
          .makeDeployment();
    }
    
    private DeploymentBuilder createWarTemplate(String warName) {
      return makeDeploymentBuilder(warName+".war")
          .addDirectoryOrJARContainingClass(AppContextDefinitionTestSetup.class)
          .addDirectoryContainingResource("/tc-config-files/appctxdef-tc-config.xml");
    }
    
  }

}

