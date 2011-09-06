/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

public class LockShareUnlockTestApp extends AbstractErrorCatchingTransparentApp {

  public static final String      CONFIG_FILE = "config-file";
  public static final String      PORT_NUMBER = "port-number";
  public static final String      HOST_NAME   = "host-name";
  public static final String      JMX_PORT    = "jmx-port";

  private static Set<Object>      root        = new HashSet<Object>();

  private final ApplicationConfig appConfig;

  public LockShareUnlockTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.appConfig = config;
  }

  public void runTest() throws Exception {
    final String hostName = appConfig.getAttribute(HOST_NAME);
    final int port = Integer.parseInt(appConfig.getAttribute(PORT_NUMBER));
    final File configFile = new File(appConfig.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client");
    FileUtils.forceMkdir(workingDir);

    List<String> jvmArgs = new ArrayList<String>();
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, LockShareUnlockTestApp.class,
                                                             configFile.getAbsolutePath(), Collections.EMPTY_LIST,
                                                             workingDir, jvmArgs);
    client.start();
    System.err.println("Started New Client");
    client.mergeSTDERR();
    client.mergeSTDOUT();

    System.out.println(Thread.currentThread().getName() + ": Waiting for client to finish.");
    int exitCode = client.waitFor();
    Assert.assertTrue("Client terminated with failure", exitCode != 0);
  }

  public static void main(String[] args) {
    System.err.println("Testing : [UnbalancedRead, Write]");
    testUnbalancedReadWrite();

    System.err.println("Testing : [UnbalancedWrite, Write]");
    testUnbalancedWriteWrite();
    Assert.fail();
  }

  private static void testUnbalancedReadWrite() {
    Object o = new Object();
    synchronized (o) {
      addToRoot(o);
    }
  }

  private static void testUnbalancedWriteWrite() {
    Object o = new Object();
    try {
      synchronized (o) {
        addToRoot(o);
      }
      Assert.fail("Should have thrown UnlockedSharedObjectException");
    } catch (UnlockedSharedObjectException e) {
      // expected
    }
  }

  private static void addToRoot(Object o) {
    synchronized (root) {
      root.add(o);
    }
  }
}