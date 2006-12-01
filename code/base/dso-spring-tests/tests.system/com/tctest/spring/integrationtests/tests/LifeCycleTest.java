/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tctest.spring.bean.ILifeCycle;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;

import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * Test the clustered bean behavior with those life cycle methods 1. setBeanName 2. setApplicationContext 3.
 * afterPropertiesSet 4. destroy
 * 
 * @author Liyu Yi
 */
public class LifeCycleTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "LifeCycle";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/lifecycle-tc-config.xml";

  private static ILifeCycle   mLifeCycleBean1;
  private static ILifeCycle   mLifeCycleBean2;

  public void test() throws Exception {

    logger.debug("testing bean life cycle");

    long systemId1 = mLifeCycleBean1.getSystemId();
    long systemId2 = mLifeCycleBean2.getSystemId();

    assertTrue("Transient properties also share the same value.", systemId1 != systemId2);

    List prop1 = mLifeCycleBean1.getProp();
    List prop2 = mLifeCycleBean2.getProp();

    // property might be referenced by other beans
    assertEquals(prop1, prop2);
    assertTrue(prop1.contains("" + systemId1));
    assertTrue(prop1.contains("" + systemId2));

    List records1 = mLifeCycleBean1.getInvocationRecords();
    List records2 = mLifeCycleBean2.getInvocationRecords();

    // check regular contract
    assertTrue("Bean1 afterPropertiesSet not invoked: " + records1, records1
        .contains("afterPropertiesSet-" + systemId1));
    assertTrue("Bean2 afterPropertiesSet not invoked: " + records2, records2
        .contains("afterPropertiesSet-" + systemId2));

    assertTrue("Bean1 setBeanName not invoked: " + records1, records1.contains("setBeanName-" + systemId1));
    assertTrue("Bean2 setBeanName not invoked: " + records2, records2.contains("setBeanName-" + systemId2));

    assertTrue("Bean1 setApplicationContext not invoked: " + records1, records1.contains("setApplicationContext-"
        + systemId1));
    assertTrue("Bean2 setApplicationContext not invoked: " + records2, records2.contains("setApplicationContext-"
        + systemId2));

    // check distributed behavior
    assertTrue("Replication failure: " + records1, records1.contains("afterPropertiesSet-" + systemId2));
    assertTrue("Replication failure: " + records2, records2.contains("afterPropertiesSet-" + systemId1));

    assertTrue("Replication failure: " + records1, records1.contains("setBeanName-" + systemId2));
    assertTrue("Replication failure: " + records2, records2.contains("setBeanName-" + systemId1));

    assertTrue("Replication failure: " + records1, records1.contains("setApplicationContext-" + systemId2));
    assertTrue("Replication failure: " + records2, records2.contains("setApplicationContext-" + systemId1));

    mLifeCycleBean1.closeAppCtx();
    mLifeCycleBean2.closeAppCtx();

    records1 = mLifeCycleBean1.getInvocationRecords();
    records2 = mLifeCycleBean2.getInvocationRecords();

    assertTrue("Bean1 destroy not invoked: " + records1, records1.contains("destroy-" + systemId1));
    assertTrue("Bean2 destroy not invoked: " + records2, records2.contains("destroy-" + systemId2));

    assertTrue("Replication failure: " + records1, records1.contains("destroy-" + systemId2));
    assertTrue("Replication failure: " + records2, records2.contains("destroy-" + systemId1));

    logger.debug("!!!! Asserts passed !!!");
  }

  private static class LifeCycleTestSetup extends TwoSvrSetup {
    private LifeCycleTestSetup() {
      super(LifeCycleTest.class, CONFIG_FILE_FOR_TEST, "test-lifecycle");
    }

    protected void setUp() throws Exception {
      super.setUp();

      mLifeCycleBean1 = (ILifeCycle) server1.getProxy(ILifeCycle.class, REMOTE_SERVICE_NAME);
      mLifeCycleBean2 = (ILifeCycle) server2.getProxy(ILifeCycle.class, REMOTE_SERVICE_NAME);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);

      builder.addRemoteService(RmiServiceExporter.class, "LifeCycle", "lifeCycleBean", ILifeCycle.class);

      builder.addRemoteServiceBlock("<bean id=\"lifeCycleBean\" class=\"com.tctest.spring.bean.LifeCycleBean\">" + "\n"
          + "<property name=\"prop\" ref=\"recorder\"/>" + "\n" + "</bean>");

    }
  }

  private static class RmiServiceExporter extends org.springframework.remoting.rmi.RmiServiceExporter {
    public void destroy() {
      logger.info("destroy method override in RmiServiceExporter.");
    }
  }

  /**
   * JUnit test loader entry point
   */
  public static Test suite() {
    TestSetup setup = new LifeCycleTestSetup();
    return setup;
  }

}
