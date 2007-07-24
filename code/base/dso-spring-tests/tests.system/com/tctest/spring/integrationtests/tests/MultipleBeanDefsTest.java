/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.spring.bean.IMasterBean;
import com.tctest.spring.integrationtests.SpringTwoServerTestSetup;

import java.util.List;

import junit.framework.Test;

/**
 * This is really testing a single WebApplicationContext with multiple bean definition files
 * Testing the following features
 * 1. proxy is correctly invoked
 * 2. The shared bean is cached in the mixin cache
 * 
 * @author Liyu Yi
 */
public class MultipleBeanDefsTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "MasterService";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory.xml \n classpath:/com/tctest/spring/beanfactory-master.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/multibeandef-tc-config.xml";

  private IMasterBean masterBean1;
  private IMasterBean masterBean2;

  protected void setUp() throws Exception {
    super.setUp();
    masterBean1 = (IMasterBean) server0.getProxy(IMasterBean.class, REMOTE_SERVICE_NAME);
    masterBean2 = (IMasterBean) server1.getProxy(IMasterBean.class, REMOTE_SERVICE_NAME);
  }
  
  /**
   * Test regular shared bean behavior
   */
  public void testBeanFromMultipleContexts() throws Exception {
    List values1 = masterBean1.getValues();
    List values2 = masterBean2.getValues();
    
    assertEquals("Pre-condition checking failed" + values1, 0, values1.size());
    assertEquals("Pre-condition checking failed" + values2, 0, values2.size());
    
    masterBean1.addValue("masterBean1");
    masterBean2.addValue("masterBean2");

    values1 = masterBean1.getValues();
    values2 = masterBean2.getValues();

    assertEquals("Not shared correctly" + values1, 2, values1.size());
    assertEquals("Not shared correctly" + values2, 2, values2.size());
    
    assertTrue(values1.contains("masterBean1"));
    assertTrue(values1.contains("masterBean2"));
    assertTrue(values2.contains("masterBean1"));
    assertTrue(values2.contains("masterBean2"));
  }

  /**
   * 
   */
  public void testUsingSharedBeanReferenceAcrossCluster() throws Exception {
    assertTrue("After a round trip, failed the check for ==", masterBean1.isTheSameSingletonReferenceUsed());
    assertTrue("After a round trip, failed the check for ==", masterBean2.isTheSameSingletonReferenceUsed());
  }

  private static class MultipleBeanDefsTestSetup extends SpringTwoServerTestSetup {

    private MultipleBeanDefsTestSetup() {
      super(MultipleBeanDefsTest.class, CONFIG_FILE_FOR_TEST, "test-multibeandef");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(REMOTE_SERVICE_NAME, "distributedMasterBean", IMasterBean.class);
    }
  }

  public static Test suite() {
    return new MultipleBeanDefsTestSetup();
  }

}
