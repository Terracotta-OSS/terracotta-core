/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.config.schema.builder.SpringApplicationConfigBuilder;
import com.tc.config.schema.builder.SpringApplicationContextConfigBuilder;
import com.tc.config.schema.builder.SpringConfigBuilder;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.Server;
import com.tc.test.server.appserver.deployment.SpringTerracottaAppServerConfig;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.SpringDeploymentTest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 
 * This is a simple smoke test for the Spring testing framework extensions.
 * Don't make it more elaborate, i.e. spawn more than 2 servers etc
 */
public class SpringTestingFrameworkSmokeTest extends SpringDeploymentTest {

  private static final String APP_NAME = "test-singleton";
  private static final String REMOTE_SERVICE_NAME           = "Singleton";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/singleton-tc-config.xml";

  private StandardTerracottaAppServerConfig tcConfig;
  private Deployment deployment;

  String context = APP_NAME;
  String url = "/" + context;

  protected void setUp() throws Exception {
    super.setUp();
    if (tcConfig == null) tcConfig = buildTCConfig();
    if (deployment == null) deployment = makeDeployment();
   }
  
  public void testSingleton2() throws Exception {
    runNodes(2);
  }

  private void runNodes(int nodeCount) throws Exception {
    List servers = new ArrayList();

    for (int i = 0; i < nodeCount; i++) {
      WebApplicationServer server = makeWebApplicationServer(tcConfig);
      server.addWarDeployment(deployment, context);
      server.start();
      servers.add(server);
    }

//    ((WebApplicationServer) servers.get(0)).ping(url);

    WebApplicationServer prev = null;
    for (Iterator iter = servers.iterator(); iter.hasNext();) {
      if (prev == null) {
        prev = (WebApplicationServer) iter.next();
      } else {
        WebApplicationServer server = (WebApplicationServer) iter.next();
        SingletonStateUtil.assertSingletonShared(prev, server, REMOTE_SERVICE_NAME);
        assertTransientState(prev, server);
        prev = server;
      }
    }
  }

  public static void assertTransientState(Server server1, Server server2) throws Exception {
    ISingleton singleton1 = (ISingleton) server1.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
    ISingleton singleton2 = (ISingleton) server2.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
    String originalValue = "aaa";

    assertEquals(originalValue, singleton1.getTransientValue());
    assertEquals(originalValue, singleton2.getTransientValue());
    singleton1.setTransientValue("s1");
    assertEquals(originalValue, singleton2.getTransientValue());
    singleton2.setTransientValue("s2");
    assertEquals("s1", singleton1.getTransientValue());
    assertEquals("s2", singleton2.getTransientValue());

    // reset - for 2,4,8-node testing
    singleton1.setTransientValue(originalValue);
    singleton2.setTransientValue(originalValue);
  }

  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(APP_NAME + ".war");
    
    builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
    
    builder.addRemoteService(REMOTE_SERVICE_NAME, "singleton", ISingleton.class);
    
    builder.addDirectoryOrJARContainingClass(ISingleton.class);
    builder.addDirectoryContainingResource(CONFIG_FILE_FOR_TEST);
    
    return builder.makeDeployment();
  }

  public StandardTerracottaAppServerConfig buildTCConfig() throws Exception {
    StandardTerracottaAppServerConfig tcConfigBuilder = new SpringTerracottaAppServerConfig(serverManager.getTcConfigFile(CONFIG_FILE_FOR_TEST).getFile()) ;
    SpringConfigBuilder springConfigBuilder = tcConfigBuilder.getConfigBuilder().getApplication().getSpring();
    
    SpringApplicationConfigBuilder application = springConfigBuilder.getApplications()[0];
    application.setName(APP_NAME);
    
    SpringApplicationContextConfigBuilder applicationContext = application.getApplicationContexts()[0];
    applicationContext.setPaths(new String[] {"*.xml"});
    applicationContext.addBean("singleton");
    tcConfigBuilder.build();
    logger.debug(tcConfigBuilder.toString());
    return tcConfigBuilder;
  }
}
