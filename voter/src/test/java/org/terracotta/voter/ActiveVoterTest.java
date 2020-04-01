package org.terracotta.voter;

import org.junit.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.terracotta.voter.ActiveVoter.TOPOLOGY_FETCH_TIME_PROPERTY;

public class ActiveVoterTest {
  private static final long TOPOLOGY_FETCH_INTERVAL = 11000L;

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
    when(firstClientVoterManager.registerVoter("mvoter")).thenReturn(0L);
    when(otherClientVoterManager.registerVoter("mvoter")).thenReturn(0L);
    when(firstClientVoterManager.getTopology()).thenReturn(expectedTopology);
    when(firstClientVoterManager.heartbeat("mvoter")).thenReturn(0L);
    when(otherClientVoterManager.heartbeat("mvoter")).thenReturn(0L);
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
        return mockClientVoterManager;
      }
    };
    ActiveVoter activeVoter = new ActiveVoter("mvoter",
        new CompletableFuture<VoterStatus>(), Optional.empty(), factory, "localhost:1234", "localhost:1235");
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
    when(firstClientVoterManager.registerVoter("mvoter")).thenReturn(0L);
    when(firstClientVoterManager.getTopology()).thenReturn(expectedTopology);
    when(firstClientVoterManager.heartbeat("mvoter")).thenReturn(0L);
    when(firstClientVoterManager.getTargetHostPort()).thenReturn("localhost:12345");

    Function<String, ClientVoterManager> factory = hostPort -> {
      if (hostPort.equals("localhost:12345")) {
        return firstClientVoterManager;
      } else {
        ClientVoterManager mockClientVoterManager = mock(ClientVoterManager.class);
        when(mockClientVoterManager.getTargetHostPort()).thenReturn(hostPort);
        return mockClientVoterManager;
      }
    };
    ActiveVoter activeVoter = new ActiveVoter("mvoter",
        new CompletableFuture<VoterStatus>(), Optional.empty(), factory, "localhost:12345");
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
    when(firstClientVoterManager.registerVoter("mvoter")).thenReturn(0L);
    when(otherClientVoterManager.registerVoter("mvoter")).thenReturn(0L);
    when(firstClientVoterManager.getTopology()).thenReturn(expectedTopology);
    when(firstClientVoterManager.heartbeat("mvoter")).thenReturn(0L);
    when(otherClientVoterManager.heartbeat("mvoter")).thenReturn(0L);
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
        return mockClientVoterManager;
      }
    };
    ActiveVoter activeVoter = new ActiveVoter("mvoter",
        new CompletableFuture<VoterStatus>(), Optional.empty(), factory, "localhost:12345", "localhost:1234");
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
    when(firstClientVoterManager.registerVoter("mvoter")).thenReturn(0L);
    when(otherClientVoterManager.registerVoter("mvoter")).thenReturn(0L);
    when(firstClientVoterManager.getTopology()).thenReturn(expectedTopology);
    when(firstClientVoterManager.heartbeat("mvoter")).thenReturn(0L);
    when(otherClientVoterManager.heartbeat("mvoter")).thenReturn(0L);
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
        return mockClientVoterManager;
      }
    };
    ActiveVoter activeVoter = new ActiveVoter("mvoter",
        new CompletableFuture<VoterStatus>(), Optional.empty(), factory, "localhost:1234", "localhost:1235");
    activeVoter.start();

    Thread.sleep(TOPOLOGY_FETCH_INTERVAL);
    assertThat(activeVoter.getExistingTopology(), is(expectedTopology));
    assertThat(activeVoter.getHeartbeatFutures().size(), is(3));
  }
}