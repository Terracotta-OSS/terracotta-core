/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.object.BaseDSOTestCase;
import com.tc.util.Grep;
import com.tc.util.TcConfigBuilder;
import com.tctest.process.ExternalDsoServer;

import java.io.File;
import java.util.List;

public class UnSupportedServerStripingTest extends BaseDSOTestCase {

  /**
   * Test scenario when DSO server in open source mode, used with server striping
   */
  public void testStripingInOpensource() throws Exception {
    File workDir = new File(getTempDirectory(), "test1");
    workDir.mkdirs();

    TcConfigBuilder configBuilder = new TcConfigBuilder("/com/tctest/tc-2-server-groups-config.xml");
    configBuilder.randomizePorts();

    ExternalDsoServer server = new ExternalDsoServer(workDir, configBuilder.newInputStream(), "server1");

    server.startAndWait(30);
    assertFalse("Expected the server to fail due to unsupported feature", server.isRunning());
    server.stop();

    List<CharSequence> result = Grep.grep("Terracotta license key is required for Enterprise capabilities",
                                          server.getServerLog());
    System.out.println("Output found: " + result);
    assertTrue(result.size() > 0);
  }
}
