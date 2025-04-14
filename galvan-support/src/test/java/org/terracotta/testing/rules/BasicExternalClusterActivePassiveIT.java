/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package org.terracotta.testing.rules;

import java.io.IOException;
import java.net.URI;

import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Rule;

/**
 *
 * @author cdennis
 */
public class BasicExternalClusterActivePassiveIT {

  @Rule
  public final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(3).withClientReconnectWindowTime(30)
      .withTcProperty("server.entity.processor.threads", "16").inline(false)
      .build();

  @Test
  public void testFailover() throws IOException, ConnectionException, Exception {
    CLUSTER.getClusterControl().waitForRunningPassivesInStandby();
    CLUSTER.getClusterControl().terminateActive();
    CLUSTER.getClusterControl().startOneServer();
    CLUSTER.getClusterControl().waitForActive();
    
    try (Connection connection = CLUSTER.newConnection()) {
      System.out.println("testFailover " + connection);
    }
  }

  @Test
  public void testFailoverWithLiveConnection() throws IOException, ConnectionException, Exception {
    try (Connection connection = CLUSTER.newConnection()) {
      CLUSTER.getClusterControl().waitForRunningPassivesInStandby();
      CLUSTER.getClusterControl().terminateActive();
      CLUSTER.getClusterControl().waitForActive();
      CLUSTER.getClusterControl().startOneServer();
      System.out.println("testFailoverWithLiveConnection " + connection);
    }
  }

  @Test
  public void testClusterHostPorts() throws Exception {
    String[] clusterHostPorts = CLUSTER.getClusterHostPorts();
    assertThat(clusterHostPorts.length, is(3));
    for (String hostPort: clusterHostPorts) {
      URI uri = new URI("tc://" + hostPort);
      assertThat(uri.getHost(), is("localhost"));
    }
  }

}
