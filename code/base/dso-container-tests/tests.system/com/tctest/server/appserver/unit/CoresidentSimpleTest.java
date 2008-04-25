/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.deployment.AbstractTwoServerCoresidentDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.Assert;
import com.tctest.webapp.servlets.CoresidentSimpleTestServlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import junit.framework.Test;

public class CoresidentSimpleTest extends AbstractTwoServerCoresidentDeploymentTest {

  private static final String CONTEXT = "simple";

  public CoresidentSimpleTest() {
    //  MNK-499
    if (appServerInfo().getId() == AppServerInfo.WEBLOGIC || appServerInfo().getId() == AppServerInfo.WASCE) {
      disableAllUntil("2008-05-15");
    }
  }

  public static Test suite() {
    return new CoresidentSimpleTestSetup();
  }

  public void testSimple() throws Exception {
    System.err.println("Printing...");
    print(server0, 0, 0);
    print(server1, 0, 0);
    print(server1, 1, 1);
    print(server0, 1, 1);

    System.err.println("Initializing...");
    initialize(server0, 0, 0);
    initialize(server1, 1, 1);

    System.err.println("Inserting...");

    // change map0 in partition-0 from server0
    insert(server0, 0, 0);
    Thread.sleep(1000);
    insert(server0, 0, 0);
    Thread.sleep(1000);

    // change map1 in partition-1 from server1
    insert(server1, 1, 1);
    Thread.sleep(1000);
    insert(server1, 1, 1);
    Thread.sleep(1000);
    insert(server1, 1, 1);
    Thread.sleep(1000);

    System.err.println("Asserting...");

    // assert change in partition-0 in map0 by server0 in server1
    assertSize(server1, 0, 0, 2);
    // assert change in partition-1 in map1 by server1 in server0
    assertSize(server0, 1, 1, 3);
    System.err.println("Done");
  }

  private void assertOk(final String response) throws Exception {
    String ok = getLastLine(response);
    Assert.assertEquals("OK", ok);
  }

  private String getLastLine(final String response) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(response.getBytes())));
    String line = br.readLine();
    String lastLine = line;
    while ((line = br.readLine()) != null)
      lastLine = line;
    return lastLine;
  }

  private void initialize(WebApplicationServer server, int partition, int map) throws Exception {
    final WebConversation conversation = new WebConversation();
    final WebResponse webResponse;
    final String response;
    webResponse = request(server, "cmd=" + "initialize" + "&partition=" + partition + "&map=" + map, conversation);
    response = webResponse.getText().trim();
    System.err.println("Response from server" + (server == server1 ? "1" : "0") + ": " + response);
    assertOk(response);
  }

  private void assertSize(WebApplicationServer server, int partition, int map, final int size) throws Exception {
    final WebConversation conversation = new WebConversation();
    final WebResponse webResponse;
    final String response;
    webResponse = request(server, "cmd=" + "assertSize" + "&partition=" + partition + "&map=" + map + "&size=" + size,
                          conversation);
    response = webResponse.getText().trim();
    System.err.println("Response from server" + (server == server1 ? "1" : "0") + ": " + response);
    assertOk(response);
  }

  private void insert(WebApplicationServer server, int partition, int map) throws Exception {
    final WebConversation conversation = new WebConversation();
    final WebResponse webResponse;
    final String response;
    webResponse = request(server, "cmd=" + "insert" + "&partition=" + partition + "&map=" + map, conversation);
    response = webResponse.getText().trim();
    System.err.println("Response from server" + (server == server1 ? "1" : "0") + ": " + response);
    assertOk(response);
  }

  private void print(final WebApplicationServer server, final int partition, final int map) throws Exception {
    WebConversation conversation = new WebConversation();
    WebResponse webResponse;
    String response;
    webResponse = request(server, "cmd=" + "print&partition=" + partition + "&map=" + map, conversation);
    response = webResponse.getText().trim();
    System.err.println("Response from server" + (server == server1 ? "1" : "0") + ": " + response);
    assertOk(response);
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + CONTEXT + "?" + params, con);
  }

  /**
   * ***** test setup *********
   */
  private static class CoresidentSimpleTestSetup extends TwoServerCoresidentTestSetup {

    public CoresidentSimpleTestSetup() {
      super(CoresidentSimpleTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("CoresidentSimpleTestServlet", "/" + CONTEXT + "/*", CoresidentSimpleTestServlet.class, null,
                         false);
      // builder.addDirectoryOrJARContainingClass(SynchronizedInt.class);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      String rootName = "sharedMap0";
      String fieldName = CoresidentSimpleTestServlet.class.getName() + ".sharedMap0";
      tcConfigBuilder.addRoot(fieldName, rootName);

      rootName = "sharedMap1";
      fieldName = CoresidentSimpleTestServlet.class.getName() + ".sharedMap1";
      tcConfigBuilder.addRoot(fieldName, rootName);

      // rootName = "count";
      // fieldName = CoresidentSimpleTestServlet.class.getName() + ".count";
      // tcConfigBuilder.addRoot(fieldName, rootName);

      String methodExpression = "* " + CoresidentSimpleTestServlet.class.getName() + ".*(..)";
      tcConfigBuilder.addAutoLock(methodExpression, "write");

    }

    protected boolean enableContainerDebug(final int serverId) {
      if (serverId == 0) { return false; }
      return false;
    }
  }

}