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
package com.tc.l2.state;

import org.junit.Test;

import com.tc.net.NodeID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SafeStartupManagerImplTest {

  @Test
  public void requestStartToActiveTransitionWithPeersJoining() {
    ConsistencyManager consistency = mock(ConsistencyManager.class);
    when(consistency.requestTransition(any(ServerMode.class), any(NodeID.class), eq(null), any(ConsistencyManager.Transition.class))).thenReturn(Boolean.TRUE);
    SafeStartupManagerImpl safeStartupManager = new SafeStartupManagerImpl(true, 2, consistency);
    assertThat(safeStartupManager.requestTransition(ServerMode.START, mock(NodeID.class), ConsistencyManager.Transition.MOVE_TO_ACTIVE), is(false));

    safeStartupManager.nodeJoined(mock(NodeID.class));  //First node joins
    assertThat(safeStartupManager.requestTransition(ServerMode.START, mock(NodeID.class), ConsistencyManager.Transition.MOVE_TO_ACTIVE), is(false));

    safeStartupManager.nodeJoined(mock(NodeID.class));  //Second node joins
    assertThat(safeStartupManager.requestTransition(ServerMode.START, mock(NodeID.class), ConsistencyManager.Transition.MOVE_TO_ACTIVE), is(true));
  }

  @Test
  public void requestStartToActiveTransitionWithExternalIntervention() {
    ConsistencyManager consistency = mock(ConsistencyManager.class);
    when(consistency.requestTransition(any(ServerMode.class), any(NodeID.class), eq(null), any(ConsistencyManager.Transition.class))).thenReturn(Boolean.TRUE);
    SafeStartupManagerImpl safeStartupManager = new SafeStartupManagerImpl(true, 2, consistency);
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
    verify(consistencyManager).requestTransition(eq(ServerMode.PASSIVE), any(NodeID.class), any(), eq(ConsistencyManager.Transition.MOVE_TO_ACTIVE));
  }
  
  @Test
  public void requestStartupTransisiton() {
    ConsistencyManager consistency = mock(ConsistencyManager.class);
    when(consistency.requestTransition(any(ServerMode.class), any(NodeID.class), eq(null), any(ConsistencyManager.Transition.class))).thenReturn(Boolean.TRUE); 
    SafeStartupManagerImpl safeStartupManager = new SafeStartupManagerImpl(true, 2, consistency);
    assertTrue(safeStartupManager.requestTransition(ServerMode.START, mock(NodeID.class), ConsistencyManager.Transition.CONNECT_TO_ACTIVE));
    verify(consistency).requestTransition(eq(ServerMode.START), any(NodeID.class), any(), eq(ConsistencyManager.Transition.CONNECT_TO_ACTIVE));
  }
  
  @Test
  public void allowLastTransitionDelegationForNonStartupTransition() {
    ConsistencyManager consistencyManager = mock(ConsistencyManager.class);
    SafeStartupManagerImpl safeStartupManager = new SafeStartupManagerImpl(true, 2, consistencyManager);
    safeStartupManager.allowLastTransition();
    verify(consistencyManager).allowLastTransition();
  }
}