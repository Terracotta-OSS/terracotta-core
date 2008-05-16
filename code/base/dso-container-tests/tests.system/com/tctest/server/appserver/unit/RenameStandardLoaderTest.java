/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.object.tools.BootJarTool;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.test.AppServerInfo;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.JARBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.externall1.StandardClasspathDummyClass;
import com.tctest.externall1.StandardLoaderApp;
import com.tctest.webapp.servlets.StandardLoaderServlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

public class RenameStandardLoaderTest extends AbstractOneServerDeploymentTest {

  private static final String CONTEXT = "simple";

  public RenameStandardLoaderTest() {
    // MNK-483
    if (appServerInfo().getId() == AppServerInfo.WEBSPHERE) {
      disableAllUntil("2008-06-15");
    }
  }

  public static Test suite() {
    return new RenameStandardLoaderSetup();
  }

  private File buildTestJar() throws Exception {
    File jarFile = getTempFile("myclass.jar");
    JARBuilder jar = new JARBuilder(jarFile.getName(), jarFile.getParentFile());
    jar.addResource("/com/tctest/externall1", "StandardClasspathDummyClass.class", "com/tctest/externall1");
    jar.finish();
    return jarFile;
  }

  protected void setUp() throws Exception {
    super.setUp();
    TestConfigObject.getInstance().addToAppServerClassPath(buildTestJar().getAbsolutePath());
    server0.start();
  }

  public void testClassLoader() throws Exception {

    WebConversation conversation = new WebConversation();
    WebResponse response1 = request(server0, "cmd=" + StandardLoaderServlet.GET_CLASS_LOADER_NAME, conversation);
    String classLoaderName = response1.getText().trim();
    System.out.println("Class Loader Name: " + classLoaderName);

    WebConversation conversation2 = new WebConversation();
    WebResponse response2 = request(server0, "cmd=" + StandardLoaderServlet.PUT_INNER_INSTANCE, conversation2);
    assertEquals("OK", response2.getText().trim());

    WebConversation conversation4 = new WebConversation();
    WebResponse response4 = request(server0, "cmd=" + StandardLoaderServlet.PUT_STANDARD_LOADER_OBJECT_INSTANCE,
                                    conversation4);
    assertEquals("OK", response4.getText().trim());

    int exitCode = spawnExtraL1(classLoaderName);
    assertEquals(0, exitCode);

    WebConversation conversation3 = new WebConversation();
    WebResponse response3 = request(server0, "cmd=" + StandardLoaderServlet.CHECK_APP_INNER_INSTANCE, conversation3);
    assertEquals("OK", response3.getText().trim());

  }

  private int spawnExtraL1(String loaderName) throws Exception {

    List vmArgs = new ArrayList();
    // uncomment below lines to debug spawned extra L1
    // vmArgs.add("-Xdebug");
    // vmArgs.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8001");

    vmArgs.add("-D" + BootJarTool.SYSTEM_CLASSLOADER_NAME_PROPERTY + "=" + loaderName);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(getServerManager().getServerTcConfig().getDsoHost(),
                                                             getServerManager().getServerTcConfig().getDsoPort(),
                                                             StandardLoaderApp.class, server0.getTcConfigFile()
                                                                 .getAbsolutePath(), new String[] {}, server0
                                                                 .getWorkingDirectory(), vmArgs, false);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    client.writeOutputTo(outputStream);
    client.start();

    final int exitCode = client.waitFor();

    // print(outputStream);
    BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray())));
    assertEquals("OK", getLastLine(br).trim());

    return exitCode;
  }

  private String getLastLine(BufferedReader br) throws IOException {
    String line = br.readLine();
    String lastLine = line;
    while ((line = br.readLine()) != null)
      lastLine = line;
    return lastLine;
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + CONTEXT + "?" + params, con);
  }

  /**
   * ***** test setup *********
   */
  private static class RenameStandardLoaderSetup extends OneServerTestSetup {

    public RenameStandardLoaderSetup() {
      super(RenameStandardLoaderTest.class, CONTEXT);
      setStart(false);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet("StandardLoaderServlet", "/" + CONTEXT + "/*", StandardLoaderServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      String rootName = "sharedMap";
      String fieldName = StandardLoaderServlet.class.getName() + ".sharedMap";
      tcConfigBuilder.addRoot(fieldName, rootName);

      String methodExpression = "* " + StandardLoaderServlet.class.getName() + ".*(..)";
      tcConfigBuilder.addAutoLock(methodExpression, "write");

      tcConfigBuilder.addInstrumentedClass(StandardLoaderServlet.class.getName() + "$Inner", false);
      tcConfigBuilder.addInstrumentedClass(StandardClasspathDummyClass.class.getName(), false);

      fieldName = StandardLoaderApp.class.getName() + ".sharedMap";
      tcConfigBuilder.addRoot(fieldName, rootName);
      methodExpression = "* " + StandardLoaderApp.class.getName() + ".*(..)";
      tcConfigBuilder.addAutoLock(methodExpression, "write");

      tcConfigBuilder.addInstrumentedClass(StandardLoaderApp.class.getName() + "$AppInnerClass", false);
    }
  }

}
