/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.spring.bean.IMasterBean;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.SpringTwoServerTestSetup;

import junit.framework.Test;

/**
 * Test application context loaded from multiple bean configs.
 * 
 * @author Eugene Kuleshov
 */
public class MultifileContextTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME = "MasterService";

  public void testBeanFromMultipleContexts() throws Exception {
    IMasterBean masterBean1 = (IMasterBean) server0.getProxy(IMasterBean.class, REMOTE_SERVICE_NAME);
    IMasterBean masterBean2 = (IMasterBean) server1.getProxy(IMasterBean.class, REMOTE_SERVICE_NAME);

    ISingleton singleton1 = masterBean1.getSingleton();
    ISingleton singleton2 = masterBean2.getSingleton();

    assertNotNull(singleton1);
    assertNotNull(singleton2);
  }

  private static class MultipleBeanDefsTestSetup extends SpringTwoServerTestSetup {

    private MultipleBeanDefsTestSetup() {
      super(MultifileContextTest.class, "/tc-config-files/multifile-context-tc-config.xml", "test-multifile-context");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile("/WEB-INF/multifile-context-factory*.xml");

      builder.addResource("/com/tctest/spring", "multifile-context-factory1.xml", "/WEB-INF/");
      builder.addResource("/com/tctest/spring", "multifile-context-factory2.xml", "/WEB-INF/");

      builder.addRemoteService(REMOTE_SERVICE_NAME, "distributedMasterBean", IMasterBean.class);
    }

  }

  public static Test suite() {
    return new MultipleBeanDefsTestSetup();
  }

}
