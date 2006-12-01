/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aj;

import com.tc.test.TestConfigObject;
import com.tctest.spring.integrationtests.framework.AbstractDeploymentTest;
import com.tctest.spring.integrationtests.framework.Deployment;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;
import com.tctest.spring.integrationtests.framework.Server;
import com.tctest.spring.integrationtests.framework.WebApplicationServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class AspectJTest extends AbstractDeploymentTest {

  private static final String REMOTE_SERVICE_NAME = "InstrumentedBean";

  private Deployment deployment;
  private String context = "test-aspectj";

  public AspectJTest() {
    this.disableVariant(TestConfigObject.SPRING_VARIANT, "128");
  }
  
  protected void setUp() throws Exception {
    super.setUp();

    DeploymentBuilder builder = makeDeploymentBuilder(context+".war");
    
    builder.addRemoteService(REMOTE_SERVICE_NAME, "instrumentedBean", IInstrumentedBean.class);
    
    builder.addDirectoryOrJARContainingClass(IInstrumentedBean.class);
    builder.addDirectoryOrJARContainingClass(org.aspectj.lang.Aspects.class);
    // builder.addDirectoryOrJARContainingClassOfSelectedVersion(org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect.class, new String[] {TestConfigObject.SPRING_VARIANT});  // spring advices
    
    builder.addBeanDefinitionFile("classpath:/com/tctest/spring/aj/beanfactory-aspectj.xml");
    builder.addDirectoryContainingResource("/tc-config-files/aspectj-tc-config.xml");
    
    deployment = builder.makeDeployment();
  }

  public void testSingleton2() throws Exception {
    List<WebApplicationServer> servers = new ArrayList<WebApplicationServer>();

    int nodeCount = 2;
    for (int i = 0; i < nodeCount; i++) {
      WebApplicationServer server = makeWebApplicationServer("/tc-config-files/aspectj-tc-config.xml");
      server.addWarDeployment(deployment, context);
      server.start();
      servers.add(server);
    }

    // ((WebApplicationServer) servers.get(0)).ping("/"+context);

    for (Iterator it1 = servers.iterator(); it1.hasNext();) {
      WebApplicationServer server1 = (WebApplicationServer) it1.next();
      for(Iterator it2 = servers.iterator(); it2.hasNext();) {
        WebApplicationServer server2 = (WebApplicationServer) it2.next();
        if(server1==server2) continue;

        assertShared(server1, server2, REMOTE_SERVICE_NAME);
        assertTransient(server1, server2, REMOTE_SERVICE_NAME);
      }
    }
  }

  
  private static void assertShared(Server server1, Server server2, String remoteServiceName) throws Exception {
    IInstrumentedBean bean1 = (IInstrumentedBean) server1.getProxy(IInstrumentedBean.class, remoteServiceName);
    IInstrumentedBean bean2 = (IInstrumentedBean) server2.getProxy(IInstrumentedBean.class, remoteServiceName);

    assertEquals("1", bean1.getProperty1());
    assertEquals("2", bean1.getProperty2());
    
    assertEquals(bean1.getValue(), bean2.getValue());
    
    bean1.setValue("AA1"+System.currentTimeMillis());
    assertEquals("Should be shared", bean1.getValue(), bean2.getValue());
    
    bean2.setValue("AA2"+System.currentTimeMillis());
    assertEquals("Should be shared", bean2.getValue(), bean1.getValue());
  }
  
  private static void assertTransient(Server server1, Server server2, String remoteServiceName) throws Exception {
    IInstrumentedBean bean1 = (IInstrumentedBean) server1.getProxy(IInstrumentedBean.class, remoteServiceName);
    IInstrumentedBean bean2 = (IInstrumentedBean) server2.getProxy(IInstrumentedBean.class, remoteServiceName);
    
    String originalValue = "aaa";
    assertEquals(originalValue, bean1.getTransientValue());
    assertEquals(originalValue, bean2.getTransientValue());
    
    bean1.setTransientValue("s1");
    assertEquals(originalValue, bean2.getTransientValue());
    
    bean2.setTransientValue("s2");
    assertEquals("s1", bean1.getTransientValue());
    assertEquals("s2", bean2.getTransientValue());

    bean1.setTransientValue(originalValue);
    bean2.setTransientValue(originalValue);
  }

//   public StandardTerracottaAppServerConfig buildTCConfig() {
//    StandardTerracottaAppServerConfig tcConfigBuilder = getConfigBuilder();
//    SpringConfigBuilder springConfigBuilder = tcConfigBuilder.getConfigBuilder().getApplication().getSpring();
//    SpringApplicationConfigBuilder application = springConfigBuilder.getApplications()[ 0];
//    application.setName("test-singleton");
//    SpringApplicationContextConfigBuilder applicationContext = application.getApplicationContexts()[ 0];
//    applicationContext.setPaths(new String[] { "*.xml"});
//    applicationContext.addBean("singleton");
//    tcConfigBuilder.build();
//    logger.debug(tcConfigBuilder.toString());
//    return tcConfigBuilder;
//  }

}
