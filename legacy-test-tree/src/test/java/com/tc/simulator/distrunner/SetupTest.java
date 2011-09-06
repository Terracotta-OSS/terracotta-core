/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.distrunner;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tcsimulator.ClientSpecImpl;
import com.tcsimulator.ConfigWriter;
import com.tcsimulator.Sandbox;
import com.tcsimulator.Setup;
import com.tcsimulator.distrunner.ServerSpec;
import com.tcsimulator.distrunner.ServerSpecImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

public class SetupTest extends TCTestCase {
  private static final String LICENSE_FILENAME = "license.lic";

  public void testBasic() throws Throwable {
    Collection clientSpecs = new ArrayList();
    String clientHostname = "myClientHostname";
    String clientTestHome = getTempDirectory().getAbsolutePath();
    int vmCount = 0;
    int executionCount = 0;
    int intensity = 1;
    clientSpecs.add(new ClientSpecImpl(clientHostname, clientTestHome, vmCount, executionCount, new ArrayList()));

    InetAddress host = InetAddress.getLocalHost();
    String localHostName = host.getHostName();

    String serverHostname = localHostName;
    String serverTestHome = getTempDirectory().getAbsolutePath();

    File licenseFile = new File(serverTestHome, LICENSE_FILENAME);
    FileUtils.touch(licenseFile);

    ServerSpec serverSpec = new ServerSpecImpl(serverHostname, serverTestHome, -1, -1, -1, new ArrayList(),
                                               ServerSpec.TEST_SERVER);

    String testAppClassName = TestApp.class.getName();
    /* Setup setup = */new Setup(new String[] {}, serverSpec, testAppClassName, clientSpecs, intensity, false);

    Sandbox sandbox = new Sandbox(new File(serverTestHome), Sandbox.TEST_SERVER);

    File configFile = new File(serverTestHome, sandbox.getConfigFile().getName());
    assertFalse(configFile.exists());
    assertNull(TestApp.visitCalls.poll(0));

    Collection classesToVisit = new ArrayList();
    classesToVisit.add(TestApp.class);
    ConfigWriter configWriter = new ConfigWriter(serverSpec, classesToVisit, sandbox);
    configWriter.writeConfigFile();

    assertNotNull(TestApp.visitCalls.poll(0));
    assertTrue(configFile.exists());
    dumpConfigFile(configFile);

  }

  private void dumpConfigFile(File configFile) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
    String line;
    while ((line = reader.readLine()) != null) {
      System.out.println(line);
    }
  }

  public static final class TestApp {
    public static final NoExceptionLinkedQueue visitCalls = new NoExceptionLinkedQueue();

    public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig cfg) {
      visitCalls.put(new Object[] { visitor, cfg });
    }
  }

}
