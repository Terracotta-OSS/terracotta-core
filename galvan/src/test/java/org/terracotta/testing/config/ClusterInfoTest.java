/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClusterInfoTest {
  @Test
  public void testEncoding() {
    String testNameBase = "test";
    int portBase = 1000;
    int groupPortBase = 2000;
    int counter = 1;

    List<ServerInfo> servers = new ArrayList<>();
    servers.add(new ServerInfo(testNameBase + counter, portBase + counter, groupPortBase + counter++));
    servers.add(new ServerInfo(testNameBase + counter, portBase + counter, groupPortBase + counter++));
    servers.add(new ServerInfo(testNameBase + counter, portBase + counter, groupPortBase + counter++));
    servers.add(new ServerInfo(testNameBase + counter, portBase + counter, groupPortBase + counter++));
    servers.add(new ServerInfo(testNameBase + counter, portBase + counter, groupPortBase + counter++));

    ClusterInfo clusterInfo = new ClusterInfo(servers);
    String encoded = clusterInfo.encode();
    ClusterInfo decoded = ClusterInfo.decode(encoded);

    int count = 1;
    for (ServerInfo serverInfo : decoded.getServersInfo()) {
      assertEquals(testNameBase + count, serverInfo.getName());
      assertEquals(portBase + count, serverInfo.getServerPort());
      assertEquals(groupPortBase + count, serverInfo.getGroupPort());
      count++;
    }
  }
}
