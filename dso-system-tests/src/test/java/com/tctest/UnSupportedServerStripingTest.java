/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tctest;

import com.tc.object.BaseDSOTestCase;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.Grep;
import com.tc.util.TcConfigBuilder;

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

    List<CharSequence> result = Grep.grep("'server striping' capability is not supported in Terracotta Open Source Version",
                                          server.getServerLog());
    System.out.println("Output found: " + result);
    assertTrue(result.size() > 0);
  }
}
