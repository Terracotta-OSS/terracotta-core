/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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