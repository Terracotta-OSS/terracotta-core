package com.tc.l2.state;

import org.junit.Test;

import com.tc.net.NodeID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SafeStartupManagerImplTest {

  @Test
  public void requestStartToActiveTransitionWithPeersJoining() {
    SafeStartupManagerImpl safeStartupManager = new SafeStartupManagerImpl(true, 2, mock(ConsistencyManager.class));
    assertThat(safeStartupManager.requestTransition(ServerMode.START, mock(NodeID.class), ConsistencyManager.Transition.MOVE_TO_ACTIVE), is(false));

    safeStartupManager.nodeJoined(mock(NodeID.class));  //First node joins
    assertThat(safeStartupManager.requestTransition(ServerMode.START, mock(NodeID.class), ConsistencyManager.Transition.MOVE_TO_ACTIVE), is(false));

    safeStartupManager.nodeJoined(mock(NodeID.class));  //Second node joins
    assertThat(safeStartupManager.requestTransition(ServerMode.START, mock(NodeID.class), ConsistencyManager.Transition.MOVE_TO_ACTIVE), is(true));
  }

  @Test
  public void requestStartToActiveTransitionWithExternalIntervention() {
    SafeStartupManagerImpl safeStartupManager = new SafeStartupManagerImpl(true, 2, mock(ConsistencyManager.class));
    assertThat(safeStartupManager.requestTransition(ServerMode.START, mock(NodeID.class), ConsistencyManager.Transition.MOVE_TO_ACTIVE), is(false));

    safeStartupManager.nodeJoined(mock(NodeID.class));  //First node joins
    assertThat(safeStartupManager.requestTransition(ServerMode.START, mock(NodeID.class), ConsistencyManager.Transition.MOVE_TO_ACTIVE), is(false));

    safeStartupManager.allowLastTransition();
    assertThat(safeStartupManager.requestTransition(ServerMode.START, mock(NodeID.class), ConsistencyManager.Transition.MOVE_TO_ACTIVE), is(true));
  }

  @Test
  public void requestNonStartupTransition() {
    ConsistencyManager consistencyManager = mock(ConsistencyManager.class);
    SafeStartupManagerImpl safeStartupManager = new SafeStartupManagerImpl(true, 2, consistencyManager);
    safeStartupManager.requestTransition(ServerMode.PASSIVE, mock(NodeID.class), ConsistencyManager.Transition.MOVE_TO_ACTIVE);
    verify(consistencyManager).requestTransition(eq(ServerMode.PASSIVE), any(NodeID.class), eq(ConsistencyManager.Transition.MOVE_TO_ACTIVE));
  }
}