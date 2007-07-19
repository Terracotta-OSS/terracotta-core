/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.derby.drda.NetworkServerControl;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tctest.webapp.servlets.ContainerHibernateTestServlet;

import java.io.PrintWriter;
import java.util.Date;

import junit.framework.Test;

public class ContainerHibernateTest extends AbstractTwoServerDeploymentTest {
  private static final String CONFIG_FILE_FOR_TEST = "/tc-config-files/hibernate-tc-config.xml";

  public static Test suite() {
    return new ContainerHibernateTestSetup();
  }

  public ContainerHibernateTest() {
    // MNK-287
    if (shouldDisable()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  public boolean shouldDisable() {
    return super.shouldDisable()
           || NewAppServerFactory.WASCE.equals(TestConfigObject.getInstance().appserverFactoryName())
           || NewAppServerFactory.WEBSPHERE.equals(TestConfigObject.getInstance().appserverFactoryName());
  }

  public void testHibernate() throws Exception {
    WebConversation conversation = new WebConversation();

    WebResponse response1 = request(server1, "server=server0", conversation);
    assertEquals("OK", response1.getText().trim());

    WebResponse response2 = request(server2, "server=server1", conversation);
    assertEquals("OK", response2.getText().trim());
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/events/ContainerHibernateTestServlet?" + params, con);
  }

  private static class ContainerHibernateTestSetup extends TwoServerTestSetup {
    private NetworkServerControl derbyServer;

    private ContainerHibernateTestSetup() {
      super(ContainerHibernateTest.class, CONFIG_FILE_FOR_TEST, "events");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addDirectoryOrJARContainingClass(org.hibernate.SessionFactory.class); // hibernate*.jar
      builder.addDirectoryOrJARContainingClass(org.dom4j.Node.class); // domj4*.jar
      builder.addDirectoryOrJARContainingClass(net.sf.cglib.core.ClassInfo.class); // cglib-nodep*.jar
      builder.addDirectoryOrJARContainingClass(javax.transaction.Transaction.class); // jta*.jar
      builder.addDirectoryOrJARContainingClass(org.apache.commons.collections.Buffer.class); // commons-collections*.jar
      builder.addDirectoryOrJARContainingClass(org.apache.derby.jdbc.ClientDriver.class); // derby*.jar
      builder.addDirectoryOrJARContainingClass(antlr.Tool.class); // antlr*.jar

      builder.addResource("/com/tctest/server/appserver/unit", "hibernate.cfg.xml", "WEB-INF/classes");
      builder.addResource("/com/tctest/server/appserver/unit", "Event.hbm.xml", "WEB-INF/classes");
      builder.addResource("/com/tctest/server/appserver/unit", "Person.hbm.xml", "WEB-INF/classes");
      builder.addResource("/com/tctest/server/appserver/unit", "PhoneNumber.hbm.xml", "WEB-INF/classes");

      builder.addServlet("ContainerHibernateTestServlet", "/ContainerHibernateTestServlet/*",
                         ContainerHibernateTestServlet.class, null, true);
    }

    public void setUp() throws Exception {
      derbyServer = new NetworkServerControl();
      derbyServer.start(new PrintWriter(System.out));
      int tries = 0;
      while (tries < 5) {
        try {
          Thread.sleep(500);
          derbyServer.ping();
          break;
        } catch (Exception e) {
          tries++;
        }
      }
      if (tries == 5) { throw new Exception("Failed to start Derby!"); }

      super.setUp();
    }

    public void tearDown() throws Exception {
      super.tearDown();
      derbyServer.shutdown();
    }

  }
}
