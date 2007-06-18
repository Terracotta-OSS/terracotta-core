/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.Server;
import com.tc.test.server.appserver.deployment.TestCallback;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tctest.spring.bean.RedeploymentBean;
import com.tctest.spring.integrationtests.SpringDeploymentTest;

import java.util.Date;

/**
 * Test ApplicationContext definition
 * 
 * LKC-1753: Test: Dynamic WAR deployment https://jira.terracotta.lan/jira//browse/LKC-1753
 * 
 * LKC-2340: Redeployed web applications will almost certainly run into ClassCastExceptions for shared app level objects
 * https://jira.terracotta.lan/jira/browse/LKC-2340
 * 
 * Hot deploy war Undeploy war Re-deploy war
 * 
 * Investigate how to do this - Cargo/tomcat does not support hot deployment
 * 
 * TODO currently it is not possible to hot deploy or undeploy wars on Tomcat
 */
public class RedeploymentTest extends SpringDeploymentTest {

  public RedeploymentTest() {
    disableAllUntil("2010-09-20");
  }

  public void testRedeployment() throws Throwable {
    Deployment deployment11 = createWarWithService("redeployment1.war", "redeploymentBean1");
    Deployment deployment21 = createWarWithService("redeployment2.war", "redeploymentBean1");

    long l1 = deployment11.getFileSystemPath().getFile().lastModified();
    logger.info(deployment11.getFileSystemPath() + " " + new Date(l1));

    final WebApplicationServer server1 = createServer(deployment11);
    final WebApplicationServer server2 = createServer(deployment21);

    verifyClustered("redeploymentBean1", 15, server1, server2);

    Deployment deployment12 = createWarWithService("redeployment1a.war", "redeploymentBean1");
    long l2 = deployment12.getFileSystemPath().getFile().lastModified();
    logger.info(deployment12.getFileSystemPath() + " " + new Date(l2));

    server1.undeployWar(deployment11, "redeployment");
    server1.deployWar(deployment12, "redeployment");

    try {
      server1.ping("/redeployment");
    } catch (Throwable e) {
      // ignore
    }
    try {
      Thread.sleep(1000L * 120);
    } catch (Exception ex) {
      // ignore
    }

    waitForSuccess(60, new TestCallback() {
      public void check() throws Exception {
        server1.getProxy(RedeploymentBean.class, "redeploymentBean1");
      }
    });

    verifyClustered("redeploymentBean1", 16, server1, server2);
  }

  private void verifyClustered(String serviceName, int n, Server server1, Server server2) throws Exception {
    RedeploymentBean bean1 = (RedeploymentBean) server1.getProxy(RedeploymentBean.class, serviceName);
    RedeploymentBean bean2 = (RedeploymentBean) server2.getProxy(RedeploymentBean.class, serviceName);

    bean1.setValue(n);
    assertEquals("Expected shared value", n, bean2.getValue());

    bean2.setValue(n + 12);
    assertEquals("Expected shared value", n + 12, bean1.getValue());
  }

  public Deployment createWarWithService(String warName, String serviceName) throws Exception {
    return makeDeploymentBuilder(warName).addDirectoryOrJARContainingClass(RedeploymentBean.class)
        .addDirectoryContainingResource("/tc-config-files/redeployment-tc-config.xml")
        .addBeanDefinitionFile("classpath:/com/tctest/spring/redeployment.xml")
        .addRemoteService(serviceName, "redeploymentBean", RedeploymentBean.class).makeDeployment();
  }

  private WebApplicationServer createServer(Deployment deployment) throws Exception {
    WebApplicationServer server = makeWebApplicationServer("/tc-config-files/redeployment-tc-config.xml");

    server.addWarDeployment(deployment, "redeployment");
    server.start();

    return server;
  }

}
