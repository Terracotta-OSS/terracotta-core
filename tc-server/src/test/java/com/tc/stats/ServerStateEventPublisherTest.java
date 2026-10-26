/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2026
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
package com.tc.stats;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateManager;
import com.tc.util.State;
import org.junit.Test;
import org.mockito.InOrder;
import org.terracotta.event.EventService;

import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for ServerStateEventPublisher.
 */
public class ServerStateEventPublisherTest {

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullEventService() {
    new ServerStateEventPublisher(null);
  }

  @Test
  public void testPassiveStandbyToActiveFiresMemberStateThenFailover() {
    EventService eventService = mock(EventService.class);

    new ServerStateEventPublisher(eventService)
        .l2StateChanged(new StateChangedEvent(StateManager.PASSIVE_STANDBY, StateManager.ACTIVE_COORDINATOR));

    InOrder order = inOrder(eventService);
    order.verify(eventService).fireEvent(EventService.Event.MEMBER_STATE,
        singletonMap("state", StateManager.ACTIVE_COORDINATOR.getName()));
    verifyNoMoreInteractions(eventService);
  }

  @Test
  public void testStartToActiveIsNotFailover() {
    assertOnlyMemberState(StateManager.START_STATE, StateManager.ACTIVE_COORDINATOR);
  }

  @Test
  public void testSyncingToPassiveStandbyIsNotFailover() {
    assertOnlyMemberState(StateManager.PASSIVE_SYNCING, StateManager.PASSIVE_STANDBY);
  }

  @Test
  public void testStopIsNotFailover() {
    assertOnlyMemberState(null, StateManager.STOP_STATE);
  }

  private static void assertOnlyMemberState(State from, State to) {
    EventService eventService = mock(EventService.class);

    new ServerStateEventPublisher(eventService).l2StateChanged(new StateChangedEvent(from, to));

    inOrder(eventService).verify(eventService).fireEvent(EventService.Event.MEMBER_STATE,
        singletonMap("state", to.getName()));
    verifyNoMoreInteractions(eventService);
  }
}
