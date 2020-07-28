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
 *
 */
package org.terracotta.testing.rules;

import com.terracotta.diagnostic.Diagnostics;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.voter.ActiveVoter;
import org.terracotta.voter.TCVoter;
import org.terracotta.voter.TCVoterImpl;
import org.terracotta.voter.VoterStatus;
import org.terracotta.voter.ClientVoterManagerImpl;

import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.voter.ActiveVoter.TOPOLOGY_FETCH_TIME_PROPERTY;

public class BasicExternalClusterFOPConsistencyVoterIT {
  private static final long TOPOLOGY_FETCH_INTERVAL = 11000L;
  @Rule
  public final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(2).withFailoverPriorityVoterCount(1).build();

  static {
    System.setProperty(TOPOLOGY_FETCH_TIME_PROPERTY, "9000");
  }

  @Test
  public void testDirectConnection() throws Exception {
    CLUSTER.getClusterControl().waitForActive();
    CLUSTER.getClusterControl().waitForRunningPassivesInStandby();

    TCVoter voter = new TCVoterImpl();
    Future<VoterStatus> voterStatusFuture = voter.register("MyCluster", CLUSTER.getClusterHostPorts());
    VoterStatus voterStatus = voterStatusFuture.get();
    voterStatus.awaitRegistrationWithAll(10, TimeUnit.SECONDS);

    CLUSTER.getClusterControl().terminateActive();

    CompletableFuture<Void> connectionFuture = CompletableFuture.runAsync(() -> {
      try {
        CLUSTER.getClusterControl().waitForActive();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    connectionFuture.get(10, TimeUnit.SECONDS);
  }

  @Test
  public void testToplogyUpdateAfterPassiveRemoval() throws Exception {
    CLUSTER.getClusterControl().waitForActive();
    CLUSTER.getClusterControl().waitForRunningPassivesInStandby();

    CompletableFuture<VoterStatus> voterStatusCompletableFuture = new CompletableFuture<>();
    ActiveVoter activeVoter = new ActiveVoter("mvoter", voterStatusCompletableFuture, Optional.empty(), ClientVoterManagerImpl::new, CLUSTER.getClusterHostPorts());
    activeVoter.start();

    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    String[] hostPorts = CLUSTER.getClusterHostPorts();
    Set<String> expectedTopology = new HashSet<>();
    for (int i = 0; i < hostPorts.length; ++i) {
      expectedTopology.add(hostPorts[i]);
    }

    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(hostPorts.length));

    CLUSTER.getClusterControl().terminateOnePassive();
    String deadPassive = findDeadPassive(hostPorts);
    removeDeadPassiveFromTopology(hostPorts, deadPassive);

    expectedTopology.remove(deadPassive);
    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    
    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(hostPorts.length - 1));

    activeVoter.stop();
  }

  private void removeDeadPassiveFromTopology(String[] hostPorts, String deadPassive) throws Exception {
    for (String hostPort : hostPorts) {
      if (hostPort.equals(deadPassive)) continue;
      try (Connection connection = ConnectionFactory.connect(URI.create("diagnostic://" + hostPort),
          new Properties())) {
        EntityRef<Diagnostics, Object, Object> entityRef = connection.getEntityRef(Diagnostics.class, 1L, "root");
        Diagnostics diagnostics = entityRef.fetchEntity(null);
        diagnostics.invokeWithArg("TopologyMBean", "removePassive", deadPassive + ":0");
      }
    }
  }

  private String findDeadPassive(String[] hostPorts) throws Exception {
    for (String hostPort : hostPorts) {
      Properties properties = new Properties();
      properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "10000");
      try (Connection connection = ConnectionFactory.connect(URI.create("diagnostic://" + hostPort), properties)) {
      } catch (ConnectionException exception) {
        if (exception.getCause() instanceof TimeoutException) {
          return hostPort;
        }
      }
    }

    return null;
  }
}
