/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.terracotta.functional;

import java.net.InetSocketAddress;
import java.util.Properties;
import org.junit.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.connection.DiagnosticsFactory;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class ExtendedServerIT {
  static {
    System.setProperty("tc.server-jar", "extended-server");
  }
  
  @Rule
  public final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(1).withClientReconnectWindowTime(30)
      .build();

  @Test
  public void testConnection() throws Exception {
    String[] clusterHostPorts = CLUSTER.getClusterHostPorts();
    for (String hostPort: clusterHostPorts) {
      String[] hp = hostPort.split("[:]");
      InetSocketAddress inet = InetSocketAddress.createUnresolved(hp[0], Integer.parseInt(hp[1]));
      try (com.terracotta.diagnostic.Diagnostics d = (com.terracotta.diagnostic.Diagnostics)DiagnosticsFactory.connect(inet, new Properties())) {
        Assert.assertTrue(d.get("Server", "Version").startsWith("Extended Server Example"));
        System.out.println(d.list("Server"));
      }
    }
  }
}
