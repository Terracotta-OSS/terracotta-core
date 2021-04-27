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
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static java.util.Optional.empty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    Map<String, String> servers = new HashMap<String, String>() {{
      put("ACTIVE-COORDINATOR", "localhost:1234");
      put("PASSIVE-STANDBY", "localhost:1235");
    }};

    Map<String, ClientVoterManager> managers = Collections.synchronizedMap(
        servers.entrySet()
               .stream()
               .map((e) -> manager(e.getKey(), e.getValue(), new HashSet<>(servers.values())))
               .collect(toMap(ClientVoterManager::getTargetHostPort, identity())));

    new ActiveVoter(VOTER_ID, new CompletableFuture<>(), empty(), managers::get, servers.get("ACTIVE-COORDINATOR")).start();

    waitForLogMessage("New Topology detected");

    synchronized (managers) {
      disconnectManagers(managers.values());
    }

    waitForLogMessage("Attempting to re-register");

    systemOutRule.clearLog();

    ClientVoterManager passiveManager = managers.get(servers.get("PASSIVE-STANDBY"));
    promote(passiveManager);

    waitForLogMessage("Vote owner state: ACTIVE-COORDINATOR");

    Thread.sleep(5000L); // wait for reg retry
    verify(passiveManager, atLeastOnce()).registerVoter(VOTER_ID);
  }

  private void promote(ClientVoterManager passiveManager) throws TimeoutException {
    clearInvocations(passiveManager);
    when(passiveManager.getServerState()).thenReturn("ACTIVE-COORDINATOR");
    when(passiveManager.registerVoter(VOTER_ID)).thenReturn(0L);
    when(passiveManager.heartbeat(VOTER_ID)).thenReturn(0L);
    when(passiveManager.isConnected()).thenReturn(true);
  }

  private void waitForLogMessage(String message) throws TimeoutException {
    assertThatEventually(systemOutRule::getLog, containsString(message)).within(Duration.ofMillis(10000));
  }

  private void disconnectManagers(Collection<ClientVoterManager> managers) throws TimeoutException {
    for (ClientVoterManager manager : managers) {
      when(manager.heartbeat(VOTER_ID)).thenReturn(-1L);
      when(manager.isConnected()).thenAnswer((Answer<Boolean>)invocationOnMock -> {
        Thread.sleep(500);
        return false;
      });
      Mockito.doReturn(-1L).when(manager).registerVoter(VOTER_ID);
    }
  }

  private ClientVoterManager manager(String state, String serverAddress, Set<String> topology) {
    try {
      ClientVoterManager manager = mock(ClientVoterManager.class);
      when(manager.isConnected()).thenReturn(true);
      when(manager.getServerState()).thenReturn(state);
      when(manager.registerVoter(VOTER_ID)).thenReturn(0L);
      when(manager.getTopology()).thenReturn(topology);
      when(manager.heartbeat(VOTER_ID)).thenReturn(0L);
      when(manager.getTargetHostPort()).thenReturn(serverAddress);
      return manager;
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}