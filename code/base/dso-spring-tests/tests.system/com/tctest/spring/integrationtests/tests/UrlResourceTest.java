/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.SpringDeploymentTest;

import junit.framework.Test;

/**
 * Test loading application contexts from the UrlResource (CDV-557)
 * 
 * @author Eugene Kuleshov
 */
public class UrlResourceTest extends SpringDeploymentTest {

  private static final String CONFIG_FILE_FOR_TEST = "/tc-config-files/urlresource-tc-config.xml";
  private static final String REMOTE_SERVICE_NAME  = "Singleton";

  private String              context              = "test-singleton";

  public static Test suite() {
    return new ServerTestSetup(UrlResourceTest.class);
  }

  public void testUrlResource() throws Exception {
    Deployment deployment = makeDeployment();

    WebApplicationServer server1 = makeWebApplicationServer(CONFIG_FILE_FOR_TEST);
    server1.addWarDeployment(deployment, context);
    server1.start();

    WebApplicationServer server2 = makeWebApplicationServer(CONFIG_FILE_FOR_TEST);
    server2.addWarDeployment(deployment, context);
    server2.start();

    SingletonStateUtil.assertSingletonShared(server1, server2, REMOTE_SERVICE_NAME);
  }

  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(context + ".war");

    // classpath*:// tells Spring framework to use UrlResource
    builder.addBeanDefinitionFile("classpath*:/com/tctest/spring/beanfactory.xml");

    builder.addRemoteService(REMOTE_SERVICE_NAME, "singleton", ISingleton.class);

    builder.addDirectoryOrJARContainingClass(ISingleton.class);
    builder.addDirectoryContainingResource(CONFIG_FILE_FOR_TEST);

    return builder.makeDeployment();
  }

}
