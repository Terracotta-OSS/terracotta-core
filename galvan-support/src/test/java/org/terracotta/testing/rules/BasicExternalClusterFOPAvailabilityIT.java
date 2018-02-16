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
package org.terracotta.testing.rules;

import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BasicExternalClusterFOPAvailabilityIT {

  @ClassRule
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(2).withFailoverPriorityVoterCount(-1).build();

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

}
