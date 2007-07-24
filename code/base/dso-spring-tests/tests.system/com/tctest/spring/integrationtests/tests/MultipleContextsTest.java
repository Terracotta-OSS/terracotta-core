/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.spring.bean.ISimpleBean;
import com.tctest.spring.integrationtests.SpringTwoServerTestSetup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import junit.framework.Test;

/**
 * Test behavior of distributed and non-distributed application contexts in one web application
 * 
 * @author Liyu Yi
 */
public class MultipleContextsTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME  = "SimpleBean";
  private static final String CONFIG_FILE_FOR_TEST = "/tc-config-files/multicontext-tc-config.xml";

  private ISimpleBean  bean11;                                                              // shared
  private ISimpleBean  bean12;                                                              // shared
  private ISimpleBean  bean13;                                                              // NOT shared

  private ISimpleBean  bean21;                                                              // shared
  private ISimpleBean  bean22;                                                              // shared
  private ISimpleBean  bean23;                                                              // NOT shared

  protected void setUp() throws Exception {
    super.setUp();
    bean11 = (ISimpleBean) server0.getProxy(ISimpleBean.class, REMOTE_SERVICE_NAME + "1");
    bean12 = (ISimpleBean) server0.getProxy(ISimpleBean.class, REMOTE_SERVICE_NAME + "2");
    bean13 = (ISimpleBean) server0.getProxy(ISimpleBean.class, REMOTE_SERVICE_NAME + "3");
    
    bean21 = (ISimpleBean) server1.getProxy(ISimpleBean.class, REMOTE_SERVICE_NAME + "1");
    bean22 = (ISimpleBean) server1.getProxy(ISimpleBean.class, REMOTE_SERVICE_NAME + "2");
    bean23 = (ISimpleBean) server1.getProxy(ISimpleBean.class, REMOTE_SERVICE_NAME + "3");
  }
  
  public void testBeansFrom2ClusteredContexts() throws Exception {
    // all of them have unique id
    Set s = new HashSet();
    s.add(new Long(bean11.getId()));
    s.add(new Long(bean12.getId()));
    s.add(new Long(bean21.getId()));
    s.add(new Long(bean22.getId()));

    assertEquals("Pre-condition checking failed" + " - " + bean11.getId() + " - " + bean12.getId() + " - " + bean21.getId() + " - " + bean22.getId(), 4, s.size());

    // beans are clustered
    assertEquals("Replication test faled", bean11.getSharedId(), bean21.getSharedId());
    assertEquals("Replication test faled", bean12.getSharedId(), bean22.getSharedId());

    // but NOT across contexts
    assertTrue("Replication test faled", bean11.getSharedId() != bean12.getSharedId());
  }

  public void testBeansFrom1Clustered1LocalContexts() throws Exception {
    // all of them have unique id
    Set s = new HashSet();
    s.add(new Long(bean11.getId()));
    s.add(new Long(bean13.getId()));
    s.add(new Long(bean21.getId()));
    s.add(new Long(bean23.getId()));

    assertEquals("Pre-condition checking failed" + " - " + bean11.getId() + " - " + bean13.getId() + " - " + bean21.getId() + " - " + bean23.getId(),  
                 4, s.size());

    // only the clustered have the same value
    assertEquals("Replication test faled", bean11.getSharedId(), bean21.getSharedId());
    assertTrue("Replication test faled", bean13.getSharedId() != bean23.getSharedId());

    // and NOT across contexts
    assertTrue("Replication test faled", bean11.getSharedId() != bean13.getSharedId());
    assertTrue("Replication test faled", bean21.getSharedId() != bean23.getSharedId());
  }

  private static class MultipleContextsTestSetup extends SpringTwoServerTestSetup {

    private MultipleContextsTestSetup() {
      super(MultipleContextsTest.class, CONFIG_FILE_FOR_TEST, "test-multicontext");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addListener(MultiContextLoaderListener.class);
    }
    
  }

  
  public static class MultiContextLoaderListener implements ServletContextListener {
    private List contexts = new ArrayList();

    public void contextInitialized(ServletContextEvent event) {
      try {
        contexts.add(new ClassPathXmlApplicationContext(
            new String[] { "classpath:/com/tctest/spring/multictx-beanfactory1.xml" }));
        contexts.add(new ClassPathXmlApplicationContext(
            new String[] { "classpath:/com/tctest/spring/multictx-beanfactory2.xml" }));
        contexts.add(new ClassPathXmlApplicationContext(
            new String[] { "classpath:/com/tctest/spring/multictx-beanfactory3.xml" }));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    public void contextDestroyed(ServletContextEvent event) {
      for (Iterator iter = contexts.iterator(); iter.hasNext();) {
        ((ClassPathXmlApplicationContext) iter.next()).close();
      }
    }
  }

  public static Test suite() {
    return new MultipleContextsTestSetup();
  }
}
