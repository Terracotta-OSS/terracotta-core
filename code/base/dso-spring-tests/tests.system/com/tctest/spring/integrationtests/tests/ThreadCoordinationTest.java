/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.TestCallback;
import com.tctest.spring.bean.IActiveBean;

import junit.framework.Test;

/**
 * LKC-1760: Test: thread coordination for spring beans
 * https://jira.terracotta.lan/jira//browse/LKC-1760
 * 
 * Test that thread coordination is working across two nodes
 * 
 * Auto locks 
 * Named locks
 * 
 * Locking on bean 
 * Locking on reference in bean (?????) 
 * wait/notify etc. 
 */
public class ThreadCoordinationTest extends AbstractTwoServerDeploymentTest {

  private static final int MAX_NOTIFY_DELAY = 1;
  private static final int MAX_REPLICATION_DELAY = 10;

  public void testAutolock() throws Throwable {
    logger.info("Start testAutolock()");
    checkThreadCoordination("beanWithAutolock");
    logger.info("End testAutolock()");
  }

  public void testNamedLock() throws Throwable {
    logger.info("Start testNamedLock()");
    checkThreadCoordination("beanWithNamedLock");
    logger.info("End testNamedLock()");
  }
  
  public void testWaitNotify() throws Throwable {
    logger.info("Start testWaitNotify()");
    
    final IActiveBean bean1 = (IActiveBean) server1.getProxy(IActiveBean.class, "beanWithWaitNotify");
    final IActiveBean bean2 = (IActiveBean) server2.getProxy(IActiveBean.class, "beanWithWaitNotify");
    
    bean1.start();
    bean2.start();

    assertEquals("Preconditions " + getInfo(bean1, bean2), "0", bean1.getValue());
    assertEquals("Preconditions " + getInfo(bean1, bean2), "0", bean2.getValue());
    
    // added this sleep to make sure the 2 running thread made to the wait().
    Thread.sleep(1000L);

    bean1.setValue("1");
    waitForSuccess(1, new TestCallback() {
        public void check() throws Exception {
          assertEquals("Expected update1: "+ getInfo(bean1, bean2), "2", bean1.getValue());
          assertEquals("Expected update1: "+ getInfo(bean1, bean2), "2", bean2.getValue());
          
        }
      });
    
    // added this sleep to make sure the 2 running thread made to the wait().
    Thread.sleep(1000L);

    bean2.setValue("1");
    waitForSuccess(MAX_NOTIFY_DELAY, new TestCallback() {
        public void check() throws Exception {
          assertEquals("Expected update2: "+ getInfo(bean1, bean2), "4", bean1.getValue());
          assertEquals("Expected update2: "+ getInfo(bean1, bean2), "4", bean2.getValue());
        }
      });
    
    bean1.stop();
    bean2.stop();

    logger.info("End testWaitNotify()");
  }
  
  private void checkThreadCoordination(String beanName) throws Throwable {
    final IActiveBean bean1 = (IActiveBean) server1.getProxy(IActiveBean.class, beanName);
    final IActiveBean bean2 = (IActiveBean) server2.getProxy(IActiveBean.class, beanName);
    
    bean1.setValue("1");

    bean1.start();
    waitForSuccess(MAX_REPLICATION_DELAY, new TestCallback() {
        public void check() throws Exception {
          assertEquals("Expected update1 on bean1: "+ getInfo(bean1, bean2), "1", bean1.getValue());
        }
      });

    bean2.start();
    Thread.sleep(1000L);

    waitForSuccess(5, new TestCallback() {
        public void check() throws Exception {
          assertEquals("Expected update1 on bean1: "+ getInfo(bean1, bean2), "1", bean1.getValue());
          assertEquals("Expected update1 only on bean1: "+ getInfo(bean1, bean2), "0", bean2.getValue());
        }
      });

    bean1.stop();
    waitForSuccess(5, new TestCallback() {
        public void check() throws Exception {
          assertEquals("Expected update1 on bean2: "+ getInfo(bean1, bean2), "1", bean2.getValue());
        }
      });
    
    bean1.start();
    Thread.sleep(1000L);
    
    bean2.setValue("2");
    waitForSuccess(5, new TestCallback() {
        public void check() throws Exception {
          assertEquals("Expected update2 on bean2: "+ getInfo(bean1, bean2), "2", bean2.getValue());
          assertEquals("Expected update2 only on bean2: "+ getInfo(bean1, bean2), "1", bean1.getValue());
        }
      });

    bean2.stop();
    waitForSuccess(5, new TestCallback() {
        public void check() throws Exception {
          assertEquals("Expected update2 on bean1: "+ getInfo(bean1, bean2), "2", bean1.getValue());
        }
      });
    
    bean1.stop();
  }

  private String getInfo(IActiveBean bean1, IActiveBean bean2) {
    return (bean1.isActive() ? "started" : "waiting") + ":" + bean1.getValue() + "/"
           + (bean2.isActive() ? "started" : "waiting") + ":" + bean2.getValue();
  }
  
  
  private static class ThreadCoordinationTestSetup extends TwoSvrSetup {
    private ThreadCoordinationTestSetup() {
      super(ThreadCoordinationTest.class, "/tc-config-files/thread-coordination-tc-config.xml", "thread-coordination-test");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile("classpath:/com/tctest/spring/beanfactory-thread-coordination.xml");
      
      builder.addRemoteService("beanWithAutolock", "beanWithAutolock", IActiveBean.class);
      builder.addRemoteService("beanWithNamedLock", "beanWithNamedLock", IActiveBean.class);
      builder.addRemoteService("beanWithWaitNotify", "beanWithWaitNotify", IActiveBean.class);
    }

  }

  /**
   * JUnit test loader entry point
   */
  public static Test suite() {
    return new ThreadCoordinationTestSetup();
  }

}

