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

import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import org.terracotta.event.EventService;

import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

/**
 * Publishes physical socket events as {@link EventService} events, and counts the open sockets.
 *
 * <p>Registered on a {@link com.tc.net.core.TCConnectionManager}, it sees every socket created through that
 * manager, in any direction (inbound client, inbound diagnostic, inbound and outbound peer server), exactly as
 * {@code netstat} would. It fires {@link EventService.Event#CONNECTION_OPEN} once when a socket is connected and
 * {@link EventService.Event#CONNECTION_CLOSE} once when a connected socket is closed.
 *
 * <p>The connection layer guarantees that each event is delivered at most once per socket, and that a close event
 * is only delivered for a socket that was connected, so the open and close events always pair up. The channel and
 * transport layers are deliberately not used: a channel survives reconnects and one channel can be backed by many
 * sockets over its lifetime.
 */
public class ConnectionEventPublisher implements TCConnectionEventListener {
  private final EventService eventService;

  private final AtomicLong connections = new AtomicLong();

  /**
   * Creates a new listener that routes connection events to {@code eventService}.
   *
   * @param eventService the service to fire events on (must not be {@code null})
   */
  public ConnectionEventPublisher(EventService eventService) {
    this.eventService = requireNonNull(eventService);
  }

  /**
   * @return the current number of open physical connections seen by this listener, for the
   *         {@code CONNECTIONS} gauge
   */
  public long getConnectionCount() {
    return connections.get();
  }

  @Override
  public void connectEvent(TCConnectionEvent event) {
    connections.incrementAndGet();
    eventService.fireEvent(EventService.Event.CONNECTION_OPEN);
  }

  @Override
  public void closeEvent(TCConnectionEvent event) {
    connections.decrementAndGet();
    eventService.fireEvent(EventService.Event.CONNECTION_CLOSE);
  }

  @Override
  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    // not a connection state change
  }

  @Override
  public void endOfFileEvent(TCConnectionEvent event) {
    // the connection is closed right after this event, which triggers closeEvent
  }
}
