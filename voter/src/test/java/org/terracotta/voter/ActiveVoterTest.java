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
package org.terracotta.voter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static java.util.Optional.empty;
import java.util.Properties;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.terracotta.utilities.test.WaitForAssert.assertThatEventually;
import static org.terracotta.voter.ActiveVoter.TOPOLOGY_FETCH_TIME_PROPERTY;

public class ActiveVoterTest {

  private static final String VOTER_ID = UUID.randomUUID().toString();
  private static final long TOPOLOGY_FETCH_INTERVAL = 11000L;

  @Rule
  public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  static {
    System.setProperty(TOPOLOGY_FETCH_TIME_PROPERTY, "9000");
  }

  @Test
  public void testTopologyUpdate() throws TimeoutException, InterruptedException {
    Set<String> expectedTopology = new HashSet<>();
    expectedTopology.add("localhost:1234");
    expectedTopology.add("localhost:1235");

    ClientVoterManager firstClientVoterManager = mock(ClientVoterManager.class);
    ClientVoterManager otherClientVoterManager = mock(ClientVoterManager.class);
    when(firstClientVoterManager.isConnected()).thenReturn(true);
    when(otherClientVoterManager.isConnected()).thenReturn(true);
    when(firstClientVoterManager.getServerState()).thenReturn("ACTIVE-COORDINATOR");
    when(otherClientVoterManager.getServerState()).thenReturn("PASSIVE-STANDBY");
    when(firstClientVoterManager.registerVoter(VOTER_ID)).thenReturn(0L);
    when(otherClientVoterManager.registerVoter(VOTER_ID)).thenReturn(0L);
    when(firstClientVoterManager.getTopology()).thenReturn(expectedTopology);
    when(firstClientVoterManager.heartbeat(VOTER_ID)).thenReturn(0L);
    when(otherClientVoterManager.heartbeat(VOTER_ID)).thenReturn(0L);
    when(firstClientVoterManager.getTargetHostPort()).thenReturn("localhost:1234");
    when(otherClientVoterManager.getTargetHostPort()).thenReturn("localhost:1235");

    Function<String, ClientVoterManager> factory = hostPort -> {
      if (hostPort.equals("localhost:1234")) {
        return firstClientVoterManager;
      } else if (hostPort.equals("localhost:1235")) {
        return otherClientVoterManager;
      } else {
        ClientVoterManager mockClientVoterManager = mock(ClientVoterManager.class);
        when(mockClientVoterManager.getTargetHostPort()).thenReturn(hostPort);
        when(mockClientVoterManager.isConnected()).thenReturn(true);
        return mockClientVoterManager;
      }
    };
    ActiveVoter activeVoter = new ActiveVoter(VOTER_ID,
            new CompletableFuture<VoterStatus>(), empty(), factory, "localhost:1234", "localhost:1235");
    activeVoter.start();

    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(2));

    // Update Topology To Add Passive
    expectedTopology.add("localhost:1236");
    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(3));

    //Update Topology To Remove Passive
    expectedTopology.remove("localhost:1235");
    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(2));

    activeVoter.stop();
  }

  @Test
  public void testOverLappingHostPortsWhileAddingServers() throws TimeoutException, InterruptedException {
    Set<String> expectedTopology = new HashSet<>();
    expectedTopology.add("localhost:12345");

    ClientVoterManager firstClientVoterManager = mock(ClientVoterManager.class);
    when(firstClientVoterManager.isConnected()).thenReturn(true);
    when(firstClientVoterManager.getServerState()).thenReturn("ACTIVE-COORDINATOR");
    when(firstClientVoterManager.registerVoter(VOTER_ID)).thenReturn(0L);
    when(firstClientVoterManager.getTopology()).thenReturn(expectedTopology);
    when(firstClientVoterManager.heartbeat(VOTER_ID)).thenReturn(0L);
    when(firstClientVoterManager.getTargetHostPort()).thenReturn("localhost:12345");

    Function<String, ClientVoterManager> factory = hostPort -> {
      if (hostPort.equals("localhost:12345")) {
        return firstClientVoterManager;
      } else {
        ClientVoterManager mockClientVoterManager = mock(ClientVoterManager.class);
        when(mockClientVoterManager.getTargetHostPort()).thenReturn(hostPort);
        when(mockClientVoterManager.isConnected()).thenReturn(true);
        return mockClientVoterManager;
      }
    };
    ActiveVoter activeVoter = new ActiveVoter(VOTER_ID,
            new CompletableFuture<VoterStatus>(), empty(), factory, "localhost:12345");
    activeVoter.start();

    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(1));

    // Update Topology To Add Passive
    expectedTopology.add("localhost:1234");
    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(2));

    activeVoter.stop();
  }

  @Test
  public void testOverLappingHostPortsWhileRemovingServers() throws TimeoutException, InterruptedException {
    Set<String> expectedTopology = new HashSet<>();
    expectedTopology.add("localhost:12345");
    expectedTopology.add("localhost:1234");

    ClientVoterManager firstClientVoterManager = mock(ClientVoterManager.class);
    ClientVoterManager otherClientVoterManager = mock(ClientVoterManager.class);
    when(firstClientVoterManager.isConnected()).thenReturn(true);
    when(otherClientVoterManager.isConnected()).thenReturn(true);
    when(firstClientVoterManager.getServerState()).thenReturn("ACTIVE-COORDINATOR");
    when(otherClientVoterManager.getServerState()).thenReturn("PASSIVE-STANDBY");
    when(firstClientVoterManager.registerVoter(VOTER_ID)).thenReturn(0L);
    when(otherClientVoterManager.registerVoter(VOTER_ID)).thenReturn(0L);
    when(firstClientVoterManager.getTopology()).thenReturn(expectedTopology);
    when(firstClientVoterManager.heartbeat(VOTER_ID)).thenReturn(0L);
    when(otherClientVoterManager.heartbeat(VOTER_ID)).thenReturn(0L);
    when(firstClientVoterManager.getTargetHostPort()).thenReturn("localhost:12345");
    when(otherClientVoterManager.getTargetHostPort()).thenReturn("localhost:1234");

    Function<String, ClientVoterManager> factory = hostPort -> {
      if (hostPort.equals("localhost:12345")) {
        return firstClientVoterManager;
      } else if (hostPort.equals("localhost:1234")) {
        return otherClientVoterManager;
      } else {
        ClientVoterManager mockClientVoterManager = mock(ClientVoterManager.class);
        when(mockClientVoterManager.getTargetHostPort()).thenReturn(hostPort);
        when(mockClientVoterManager.isConnected()).thenReturn(true);
        return mockClientVoterManager;
      }
    };
    ActiveVoter activeVoter = new ActiveVoter(VOTER_ID,
            new CompletableFuture<VoterStatus>(), empty(), factory, "localhost:12345", "localhost:1234");
    activeVoter.start();

    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(2));

    // Update Topology To Remove Passive
    expectedTopology.remove("localhost:1234");
    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(1));

    activeVoter.stop();
  }

  @Test
  public void testWhenStaticPassivePortsRemoved() throws TimeoutException, InterruptedException {
    Set<String> expectedTopology = new HashSet<>();
    expectedTopology.add("localhost:1234");
    expectedTopology.add("localhost:1236");
    expectedTopology.add("localhost:1237");

    ClientVoterManager firstClientVoterManager = mock(ClientVoterManager.class);
    ClientVoterManager otherClientVoterManager = mock(ClientVoterManager.class);
    when(firstClientVoterManager.isConnected()).thenReturn(true);
    when(otherClientVoterManager.isConnected()).thenReturn(true);
    when(firstClientVoterManager.getServerState()).thenReturn("ACTIVE-COORDINATOR");
    when(otherClientVoterManager.getServerState()).thenReturn("PASSIVE-STANDBY");
    when(firstClientVoterManager.registerVoter(VOTER_ID)).thenReturn(0L);
    when(otherClientVoterManager.registerVoter(VOTER_ID)).thenReturn(0L);
    when(firstClientVoterManager.getTopology()).thenReturn(expectedTopology);
    when(firstClientVoterManager.heartbeat(VOTER_ID)).thenReturn(0L);
    when(otherClientVoterManager.heartbeat(VOTER_ID)).thenReturn(0L);
    when(firstClientVoterManager.getTargetHostPort()).thenReturn("localhost:1234");
    when(otherClientVoterManager.getTargetHostPort()).thenReturn("localhost:1235");

    Function<String, ClientVoterManager> factory = hostPort -> {
      if (hostPort.equals("localhost:1234")) {
        return firstClientVoterManager;
      } else if (hostPort.equals("localhost:1235")) {
        return otherClientVoterManager;
      } else {
        ClientVoterManager mockClientVoterManager = mock(ClientVoterManager.class);
        when(mockClientVoterManager.getTargetHostPort()).thenReturn(hostPort);
        when(mockClientVoterManager.isConnected()).thenReturn(true);
        return mockClientVoterManager;
      }
    };
    ActiveVoter activeVoter = new ActiveVoter(VOTER_ID,
            new CompletableFuture<VoterStatus>(), empty(), factory, "localhost:1234", "localhost:1235");
    activeVoter.start();

    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(3));
  }

  @Test
  public void testReregistrationWhenAllStaticHostPortsNotAvailable() throws TimeoutException, InterruptedException {
    Map<String, String> servers = new HashMap<String, String>() {
      {
        put("ACTIVE-COORDINATOR", "localhost:1234");
        put("PASSIVE-STANDBY", "localhost:1235");
      }
    };

    Map<String, MockedClientVoterManager> managers = Collections.synchronizedMap(
            servers.entrySet()
                    .stream()
                    .map((e) -> manager(e.getKey(), e.getValue(), new HashSet<>(servers.values())))
                    .collect(toMap(ClientVoterManager::getTargetHostPort, identity())));

    new ActiveVoter(VOTER_ID, new CompletableFuture<>(), empty(), managers::get, servers.get("ACTIVE-COORDINATOR")).start();
    
    waitForLogMessage("New Topology detected");

    disconnectManagers(managers.values());

    waitForLogMessage("Attempting to re-register");

    systemOutRule.clearLog();

    MockedClientVoterManager passiveManager = managers.get(servers.get("PASSIVE-STANDBY"));

    passiveManager.promote();

    waitForLogMessage("Vote owner state: ACTIVE-COORDINATOR");

    Thread.sleep(5000L); // wait for reg retry
//    verify(passiveManager, atLeastOnce()).registerVoter(eq(VOTER_ID));
  }

  private void promote(MockedClientVoterManager passiveManager) throws TimeoutException {
    passiveManager.promote();
  }

  private void waitForLogMessage(String message) throws TimeoutException {
    assertThatEventually(systemOutRule::getLog, containsString(message)).within(Duration.ofMillis(10000));
  }

  private void disconnectManagers(Collection<MockedClientVoterManager> managers) throws TimeoutException {
    for (MockedClientVoterManager manager : managers) {
      manager.connected = false;
    }
  }

  private MockedClientVoterManager manager(String state, String serverAddress, Set<String> topology) {
    return spy(new MockedClientVoterManager(state, serverAddress, topology));
  }

  private static class MockedClientVoterManager implements ClientVoterManager {

    private final String serverAddress;
    private volatile String state;
    private final Set<String> topology;
    volatile boolean connected = true;

    public MockedClientVoterManager(String initialState, String serverAddress, Set<String> topology) {
      this.state = initialState;
      this.serverAddress = serverAddress;
      this.topology = topology;
    }

    void promote() {
      state = "ACTIVE-COORDINATOR";
      connected = true;
    }

    @Override
    public String getTargetHostPort() {
      return serverAddress;
    }

    @Override
    public void connect(Optional<Properties> connectionProps) {

    }

    @Override
    public String getServerState() throws TimeoutException {
      return state;
    }

    @Override
    public String getServerConfig() throws TimeoutException {
      return null;
    }

    @Override
    public Set<String> getTopology() throws TimeoutException {
      return topology;
    }

    @Override
    public void close() {
      connected = false;
    }

    @Override
    public boolean isVoting() {
      return false;
    }

    @Override
    public void zombie() {

    }

    @Override
    public boolean isConnected() {
      return connected;
    }

    @Override
    public long registerVoter(String id) throws TimeoutException {
      return connected ? HEARTBEAT_RESPONSE : INVALID_VOTER_RESPONSE;
    }

    @Override
    public long heartbeat(String id) throws TimeoutException {
      return connected ? HEARTBEAT_RESPONSE : INVALID_VOTER_RESPONSE;
    }

    @Override
    public long vote(String id, long electionTerm) throws TimeoutException {
      return connected ? HEARTBEAT_RESPONSE : INVALID_VOTER_RESPONSE;
    }

    @Override
    public boolean overrideVote(String id) throws TimeoutException {
      return false;
    }

    @Override
    public boolean deregisterVoter(String id) throws TimeoutException {
      return false;
    }
    @Override
    public long getRegisteredVoterCount() throws TimeoutException {
      return 0;
    }

    @Override
    public long getRegisteredVoterLimit() throws TimeoutException {
      return 0;
    }
  }
}
