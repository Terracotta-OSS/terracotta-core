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
package org.terracotta.functional;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Diagnostics;
import org.terracotta.connection.DiagnosticsFactory;
import org.terracotta.testing.config.DefaultStartupCommandBuilder;
import org.terracotta.testing.config.ServerInfo;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class RelayFunctionIT {

  private static Logger LOGGER = LoggerFactory.getLogger(RelayFunctionIT.class);
  
  @ClassRule
  public static final Cluster CLUSTER1 = BasicExternalClusterBuilder.newCluster(2)
          .startupBuilder(RelayStartupCommandBuilder::new)
          .withStripeName("cluster1")
          .withClientReconnectWindowTime(30)
      .build();
  @ClassRule
  public static final Cluster CLUSTER2 = BasicExternalClusterBuilder.newCluster(2)
          .startupBuilder(RelayStartupCommandBuilder::new)
          .withStripeName("cluster2")
          .withClientReconnectWindowTime(30)
      .build();

  private static final AtomicReference<String> RELAY_SRC = new AtomicReference<>();
  private static final AtomicReference<String> RELAY_DST = new AtomicReference<>();

  @Test
  public void testClusterHostPorts() throws Exception {
// start both clusters to solidfy config and set the source and dest
    CLUSTER2.getClusterControl().waitForRunningPassivesInStandby();
    CLUSTER1.getClusterControl().waitForRunningPassivesInStandby();
//  shutdown both clusters and restart with relays
    CLUSTER2.getClusterControl().terminateAllServers();
    CLUSTER1.getClusterControl().terminateAllServers();
//  start both clusters
    CLUSTER2.getClusterControl().startAllServers();
    CLUSTER1.getClusterControl().startAllServers();
// shutdown the active of cluster 2
    CLUSTER2.getClusterControl().terminateActive();

    try (Diagnostics d = DiagnosticsFactory.connect(CLUSTER2.getClusterInfo().getServersInfo().get(1).getAddress(), null)) {
      String state = d.getState();
      int turns = 0;
      while (!state.equals("PASSIVE-REPLICA")) {
        System.out.println("waiting for PASSIVE-REPLICA currently " + state);
        TimeUnit.SECONDS.sleep(2);
        state = d.getState();
        if (turns++ > 60) {
          LOGGER.warn(d.getClusterState());
          throw new RuntimeException("timeout");
        }
      }
    }

    CLUSTER2.getClusterControl().waitForRunningPassivesInStandby();
// terminate all servers of the source cluster
    CLUSTER1.getClusterControl().terminateAllServers();
// connect to the RELAY-REPLICA and clear relay status and leaveGroup to start election    
    try (Diagnostics d = DiagnosticsFactory.connect(CLUSTER2.getClusterInfo().getServersInfo().get(1).getAddress(), null)) {
      String rst = d.invoke("RelayManager", "clearRelay");
      Assert.assertTrue(Boolean.parseBoolean(rst));
      String state = d.getState();
      while (!state.equals("ACTIVE-COORDINATOR")) {
        System.out.println("leaving group in " + state + " state " + d.invoke("TerracottaServer", "leaveGroup"));
        TimeUnit.SECONDS.sleep(2);
        state = d.getState();
      }
    }
    
//  wait for active of replica cluster
    CLUSTER2.getClusterControl().waitForActive();
    CLUSTER2.getClusterControl().startAllServers();
    CLUSTER2.getClusterControl().waitForRunningPassivesInStandby();
 }

  private static class RelayStartupCommandBuilder extends DefaultStartupCommandBuilder {

    public RelayStartupCommandBuilder() {
    }

    @Override
    public String[] build() {
      if (this.getStripeName().equals("cluster1") &&
              this.getServerName().equals(getStripeConfiguration().getClusterInfo().getServersInfo().get(1).getName())) {
        if (RELAY_DST.get() != null && RELAY_SRC.get() != null) {
          List<String> sup = new ArrayList<>(List.of(super.build()));
          sup.add("-source");
          sup.add(RELAY_DST.get());
          return sup.toArray(String[]::new);
        } else {
          ServerInfo server = this.getStripeConfiguration().getClusterInfo().getServersInfo().get(1);
          RELAY_SRC.set(server.getAddress().getHostString() + ":" + server.getGroupPort());
        }
      } else if (this.getStripeName().equals("cluster2") &&
              this.getServerName().equals(getStripeConfiguration().getClusterInfo().getServersInfo().get(1).getName())) {
        if (RELAY_SRC.get() != null && RELAY_DST.get() != null) {
          List<String> sup = new ArrayList<>(List.of(super.build()));
          sup.add("-destination");
          sup.add(RELAY_SRC.get());
          return sup.toArray(String[]::new);
        } else {
          ServerInfo server = this.getStripeConfiguration().getClusterInfo().getServersInfo().get(1);
          RELAY_DST.set(server.getAddress().getHostString() + ":" + server.getServerPort());
        }
      }
      return super.build();
    }
  }

}
