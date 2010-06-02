/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Test case for DEV-3168 -- makes sure that bogus class bytes passing through our instrumentation do not throw
 * exceptions
 */
public class InvalidClassBytesTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    String agentJar = makeAgentJar();

    t.getTransparentAppConfig().setAttribute("port", getDsoPort());
    t.getTransparentAppConfig().setAttribute("agent", agentJar);

    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  private String makeAgentJar() {
    try {
      File tempDir = getTempDirectory();
      File agent = new File(tempDir, "agent.jar");

      Manifest manifest = new Manifest();
      manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
      manifest.getMainAttributes().putValue("Premain-Class", InvalidClassBytesTestAgent.class.getName());

      JarOutputStream out = new JarOutputStream(new FileOutputStream(agent), manifest);
      out.putNextEntry(new ZipEntry(InvalidClassBytesTestAgent.class.getName().replace('.', '/').concat(".class")));
      out.write(InvalidClassBytesTestAgent.REAL);
      out.close();

      return agent.getAbsolutePath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final ApplicationConfig cfg;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      this.cfg = cfg;
    }

    @Override
    protected void runTest() throws Throwable {
      String hostName = "localhost";
      int portNumber = (Integer) cfg.getAttributeObject("port");
      String configFile = "localhost:" + portNumber;
      File workingDir = new File(".");

      List jvmArgs = new ArrayList();
      jvmArgs.add("-javaagent:" + cfg.getAttribute("agent"));

      ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, portNumber, Client.class, configFile,
                                                               Collections.EMPTY_LIST, workingDir, jvmArgs);
      client.start();

      int exit = client.waitFor();
      if (exit != 0) {
        fail("Client exited with code " + exit);
      }
    }
  }

  public static class Client {
    public static void main(String[] args) {
      assertDso();

      try {
        Class c = new Loader().loadInvalidClassBytes();

        if (!c.getName().equals(InvalidClassBytesTestAgent.class.getName())) {
          //
          throw new AssertionError("Unexpected class name: " + c.getName());
        }

        System.exit(0);
      } catch (Throwable t) {
        t.printStackTrace();
      }

      System.exit(1);
    }

    private static void assertDso() {
      if (!Boolean.getBoolean("tc.active")) { throw new AssertionError(); }
    }

    private static class Loader extends ClassLoader {
      Class loadInvalidClassBytes() {
        return defineClass(InvalidClassBytesTestAgent.class.getName(), InvalidClassBytesTestAgent.MAGIC, 0,
                           InvalidClassBytesTestAgent.MAGIC.length);
      }

    }

  }
}
