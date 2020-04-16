/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
package org.terracotta.testing.rules;

import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BasicExternalClusterFOPAvailabilityIT {

  @ClassRule
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(2)
          .withFailoverPriorityVoterCount(-1).withConsistentStartup(true).build();

  @Test
  public void testDirectConnection() throws Exception {
    CLUSTER.getClusterControl().waitForActive();
    CLUSTER.getClusterControl().waitForRunningPassivesInStandby();
    CLUSTER.getClusterControl().terminateActive();

    //Fail-over will not happen since the cluster is tuned for consistency and there aren't enough voters to vote for the active to continue as active.

    CompletableFuture<Void> connectionFuture = CompletableFuture.runAsync(() -> {
      try {
        CLUSTER.getClusterControl().waitForActive();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    connectionFuture.get(10, TimeUnit.SECONDS);
  }

  @Test(expected = TimeoutException.class)
  public void testConsistentStartup() throws Exception {
    CLUSTER.getClusterControl().terminateAllServers();

    CLUSTER.getClusterControl().startOneServer();

    CompletableFuture<Void> connectionFuture = CompletableFuture.runAsync(() -> {
      try {
        CLUSTER.getClusterControl().waitForActive();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    // The started up server should not have become active as a consistent start was requested and only one server was started up
    connectionFuture.get(10, TimeUnit.SECONDS);
  }
}
