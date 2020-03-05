/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.master;

import java.io.OutputStream;
import java.io.PipedOutputStream;
import org.junit.Assert;
import org.junit.Test;
import org.terracotta.testing.config.ClusterInfo;

import java.util.regex.Pattern;
import static org.mockito.Mockito.mock;


/**
 * Tests that {@link ClusterInfo} can be correctly encoded and decoded.
 */
public class ClientEventManagerTest {
  @Test
  public void testRegEx() throws Exception {
    String testName = "2017-03-10 06:34:08,691 [L2_L1:TCWorkerComm # 0_R] WARN com.tc.net.protocol.transport.ServerMessageTransport - ConnectionID(0.9a8a9dc8a58b491aa85d3545696b40cc.bddcd2ad-c5fb-4bdb-b842-3544ea630095-15ab8a41b2c.User)[]: CLOSE EVENT : com.tc.net.core.TCConnectionImpl@1627999186: connected: false, closed: true local=127.0.0.1:12080 remote=127.0.0.1:55490 connect=[Fri Mar 10 06:34:08 PST 2017] idle=0ms [746 read, 461 write]. STATUS : ESTABLISHED";
    Pattern rgx = Pattern.compile("WARN\\scom\\.tc\\.net");
    Assert.assertTrue(rgx.matcher(testName).find());
  }
  
  @Test
  public void testWarnCheck() throws Exception {
    IMultiProcessControl ctrl = mock(IMultiProcessControl.class);
    PipedOutputStream output = mock(PipedOutputStream.class);
    OutputStream sink = System.out;
    
    ClientEventManager mgr = new ClientEventManager(ctrl, output, sink, true);
    String testName = "2017-03-10 06:34:08,691 [L2_L1:TCWorkerComm # 0_R] WARN com.tc.net.protocol.transport.ServerMessageTransport - ConnectionID(0.9a8a9dc8a58b491aa85d3545696b40cc.bddcd2ad-c5fb-4bdb-b842-3544ea630095-15ab8a41b2c.User)[]: CLOSE EVENT : com.tc.net.core.TCConnectionImpl@1627999186: connected: false, closed: true local=127.0.0.1:12080 remote=127.0.0.1:55490 connect=[Fri Mar 10 06:34:08 PST 2017] idle=0ms [746 read, 461 write]. STATUS : ESTABLISHED\n";
    //  should not fail
    mgr.getEventingStream().write(testName.getBytes());
    testName = "2017-03-10 06:34:15,580 [main] WARN com.tc.object.config.schema.L2ConfigObject - The specified hostname \"localhost\" may not work correctly if clients and operator console are connecting from other hosts. Replace \"localhost\" with an appropriate hostname in configuration.\n";
    try {
      mgr.getEventingStream().write(testName.getBytes());
      Assert.fail();
    } catch (Exception e) {
// expected
      e.printStackTrace();
    }
// reset
    mgr = new ClientEventManager(ctrl, output, sink, true);
    testName = "2017-03-10 06:34:15,612 [main] INFO com.terracottatech.console - Available Max Runtime Memory: 123MB\n";
    //  should not fail
    mgr.getEventingStream().write(testName.getBytes());
  }
}
