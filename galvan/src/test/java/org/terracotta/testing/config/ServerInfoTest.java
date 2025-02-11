/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.testing.config;

import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;

public class ServerInfoTest {
  @Test
  public void testEncoding() {
    String testName = "test";
    int testServerPort = 1234;
    int testGroupPort = 1235;

    ServerInfo serverInfo = new ServerInfo(testName, testServerPort, testGroupPort);
    String encoded = serverInfo.encode();
    ServerInfo decoded = ServerInfo.decode(encoded);

    assertEquals(testName, decoded.getName());
    assertEquals(testServerPort, decoded.getServerPort());
    assertEquals(testGroupPort, decoded.getGroupPort());
    assertEquals(InetSocketAddress.createUnresolved("localhost", testServerPort), decoded.getAddress());
  }
}
