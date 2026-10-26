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
import com.tc.l2.state.StateChangeListener;
import org.terracotta.event.EventService;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Publishes local server state changes as {@link EventService} events.
 *
 * <p>Every transition fires {@link EventService.Event#MEMBER_STATE}.
 */
public class ServerStateEventPublisher implements StateChangeListener {
  private final EventService eventService;

  /**
   * Creates a new listener that routes state changes to {@code eventService}.
   *
   * @param eventService the service to fire events on (must not be {@code null})
   */
  public ServerStateEventPublisher(EventService eventService) {
    this.eventService = requireNonNull(eventService);
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    eventService.fireEvent(EventService.Event.MEMBER_STATE, Map.of(
        "old", sce.getOldState().getName(),
        "state", sce.getCurrentState().getName()));
  }
}
