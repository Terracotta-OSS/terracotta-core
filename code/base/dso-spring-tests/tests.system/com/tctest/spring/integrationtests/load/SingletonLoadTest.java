/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.load;

import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.SpringDeploymentTest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

public class SingletonLoadTest extends SpringDeploymentTest {
  private static final String REMOTE_SERVICE_NAME           = "Singleton";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/singleton-tc-config.xml";
  private static final int    NUM_ITERATION                 = 500;

  private String              CONTEXT                       = "test-singleton";

  private Deployment          deployment;

  protected void setUp() throws Exception {
    super.setUp();
    if (deployment == null) deployment = makeDeployment();
  }

  public void testTwoNodeSingletonLoad() throws Exception {
    runNodes(2);
  }

  public void testFourNodeSingletonLoad() throws Exception {
    runNodes(4);
  }

  public void testEightNodeSingletonLoad() throws Exception {
    runNodes(8);
  }

  /*
   * Time needed to update the Singleton object's counter NUM_ITERATION times is measured. All nodes are utilized to
   * update the counter in round-robin fashion.
   */
  private void runNodes(int nodeCount) throws Exception {
    List servers = new ArrayList();
    List singletons = new ArrayList();

    try {
      for (int i = 0; i < nodeCount; i++) {
        WebApplicationServer server = makeWebApplicationServer(CONFIG_FILE_FOR_TEST);
        server.addWarDeployment(deployment, CONTEXT);
        server.start();
        servers.add(server);
        singletons.add(server.getProxy(ISingleton.class, REMOTE_SERVICE_NAME));
      }
  
      // ((WebApplicationServer) servers.get(0)).ping(URL);
  
      long startTime = System.currentTimeMillis();
      // round-robin
      for (int i = 0; i < NUM_ITERATION; i++) {
        ((ISingleton) singletons.get(i % nodeCount)).incrementCounter();
      }
      long endTime = System.currentTimeMillis();
      long totalTime = (endTime - startTime);
  
      // check clustering
      for (Iterator iter = servers.iterator(); iter.hasNext();) {
        WebApplicationServer cur = (WebApplicationServer) iter.next();
        ISingleton isp = (ISingleton) cur.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
        Assert.assertEquals(NUM_ITERATION, isp.getCounter());
      }
  
      printData(nodeCount, totalTime);

    } finally {
      for (Iterator it = servers.iterator(); it.hasNext();) {
        ((WebApplicationServer) it.next()).stopIgnoringExceptions();
      }
    }
  }

  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(CONTEXT + ".war");
    addBeanDefinitions(builder);
    configureRemoteInterfaces(builder);
    addClassesAndLibraries(builder);
    return builder.makeDeployment();
  }

  private void addBeanDefinitions(DeploymentBuilder builder) {
    builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
  }

  private void configureRemoteInterfaces(DeploymentBuilder builder) {
    builder.addRemoteService(REMOTE_SERVICE_NAME, "singleton", ISingleton.class);
  }

  private void addClassesAndLibraries(DeploymentBuilder builder) {
    builder.addDirectoryOrJARContainingClass(ISingleton.class);
    builder.addDirectoryContainingResource(CONFIG_FILE_FOR_TEST);
  }


  private void printData(int nodeCount, long totalTime) {
    System.out.println("**%% TERRACOTTA TEST STATISTICS %%**: nodes=" + nodeCount + " iteration=" + NUM_ITERATION
                       + " time=" + totalTime + " nanoseconds  avg=" + (totalTime / NUM_ITERATION)
                       + " nanoseconds/iteration");
  }
}
