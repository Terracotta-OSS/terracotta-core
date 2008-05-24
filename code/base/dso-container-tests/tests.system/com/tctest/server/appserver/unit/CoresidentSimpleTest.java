/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.test.server.appserver.deployment.AbstractTwoServerCoresidentDeploymentTest;
import com.tc.test.server.appserver.deployment.CoresidentServerTestSetup;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.Assert;
import com.tctest.externall1.PartitionManagerTestApp;
import com.tctest.webapp.servlets.CoresidentSimpleTestServlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

public class CoresidentSimpleTest extends AbstractTwoServerCoresidentDeploymentTest {

  private static final String              CONTEXT   = "simple";
  private static CoresidentServerTestSetup testSetup = new CoresidentSimpleTestSetup();

  public CoresidentSimpleTest() {
    //
  }

  public static Test suite() {
    return testSetup;
  }

  private int spawnExtraCoresidentL1(final OutputStream outputStream, final String cmd, final int partition,
                                     final int map, String extraArgs) throws Exception {
    List vmArgs = new ArrayList();
    vmArgs.add("-Dpartition.num=" + partition);
    vmArgs.add("-Dmap.num=" + map);
    vmArgs.add("-Dcmd=" + cmd);
    if (extraArgs != null) vmArgs.add(extraArgs);

    ExtraL1ProcessControl client = new ExtraL1ProcessControl(testSetup.getServerManagers()[0].getServerTcConfig()
        .getDsoHost(), testSetup.getServerManagers()[0].getServerTcConfig().getDsoPort(),
                                                             PartitionManagerTestApp.class, server0.getTcConfigFile()
                                                                 .getAbsolutePath(), new String[] {}, server0
                                                                 .getWorkingDirectory(), vmArgs, false);
    client.setCoresidentMode(server0.getCoresidentConfigFile().getAbsolutePath());
    client.writeOutputTo(outputStream);
    client.start();

    final int exitCode = client.waitFor();
    return exitCode;
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

    int exitCode = -1;

    // assert size of map0 in partition-0
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    exitCode = spawnExtraCoresidentL1(bos, "assertSize", 0, 0, "-Dsize=2");
    System.err.println("Client extra l1 output: " + bos.toString());
    assertL1Output(exitCode, bos);

    // assert map1 is null in partition-0
    bos = new ByteArrayOutputStream();
    exitCode = spawnExtraCoresidentL1(bos, "assertNull", 0, 1, null);
    System.err.println("Client extra l1 output: " + bos.toString());
    assertL1Output(exitCode, bos);

    // assert map0 is null in partition-1
    bos = new ByteArrayOutputStream();
    exitCode = spawnExtraCoresidentL1(bos, "assertNull", 1, 0, null);
    System.err.println("Client extra l1 output: " + bos.toString());
    assertL1Output(exitCode, bos);

    // assert size of map1 in partition -1
    bos = new ByteArrayOutputStream();
    exitCode = spawnExtraCoresidentL1(bos, "assertSize", 1, 1, "-Dsize=3");
    System.err.println("Client extra l1 output: " + bos.toString());
    assertL1Output(exitCode, bos);
  }

  private void assertL1Output(final int exitCode, final ByteArrayOutputStream bos) throws Exception {
    Assert.assertEquals("OK", getLastLine(bos.toString()));
    Assert.assertEquals(0, exitCode);
  }

  private void assertOk(final String response) throws Exception {
    Assert.assertEquals("OK", getLastLine(response));
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
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      String rootName = "sharedMap0";
      String fieldName = CoresidentSimpleTestServlet.class.getName() + ".sharedMap0";
      tcConfigBuilder.addRoot(fieldName, rootName);
      fieldName = PartitionManagerTestApp.class.getName() + ".sharedMap0";
      tcConfigBuilder.addRoot(fieldName, rootName);

      rootName = "sharedMap1";
      fieldName = CoresidentSimpleTestServlet.class.getName() + ".sharedMap1";
      tcConfigBuilder.addRoot(fieldName, rootName);
      fieldName = PartitionManagerTestApp.class.getName() + ".sharedMap1";
      tcConfigBuilder.addRoot(fieldName, rootName);

      String methodExpression = "* " + CoresidentSimpleTestServlet.class.getName() + ".*(..)";
      tcConfigBuilder.addAutoLock(methodExpression, "write");
      tcConfigBuilder.addInstrumentedClass(PartitionManagerTestApp.class.getName(), false);

    }

    protected boolean enableContainerDebug(final int serverId) {
      if (serverId == 0) { return false; }
      return false;
    }
  }

}