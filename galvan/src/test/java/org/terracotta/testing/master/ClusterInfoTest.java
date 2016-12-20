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

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


/**
 * Tests that {@link ClusterInfo} can be correctly encoded and decoded.
 */
public class ClusterInfoTest {
  @Test
  public void testEncoding() throws Exception {
    String testName = "test";
    int testServerPort = 1234, testGroupPort = 1235;

    Map<String, ServerInfo> servers = new HashMap<>();
    servers.put(testName, new ServerInfo(testName, testServerPort, testGroupPort));

    ClusterInfo clusterInfo = new ClusterInfo(servers);
    String encoded = clusterInfo.encode();
    ClusterInfo decoded = ClusterInfo.decode(encoded);

    ServerInfo serverInfo = decoded.getServersInfo().iterator().next();
    Assert.assertEquals(testName, serverInfo.getName());
    Assert.assertEquals(testServerPort, serverInfo.getServerPort());
    Assert.assertEquals(testGroupPort, serverInfo.getGroupPort());
  }
}
