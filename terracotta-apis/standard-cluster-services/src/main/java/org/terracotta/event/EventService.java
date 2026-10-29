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
package org.terracotta.event;

import org.terracotta.entity.ServiceRegistry;

import java.util.Collection;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * SPI for observing server lifecycle events, such as a client joining or an entity being invoked.
 *
 * <p>Implementations are discovered through the {@link ServiceRegistry}. The
 * {@link #load(ServiceRegistry)} factory method returns a composite that forwards every event to
 * each registered implementation, so several plugins can observe the same event stream
 * independently.
 *
 * <p>The server calls {@link #fireEvent(Event)} or {@link #fireEvent(Event, Map)} when an
 * {@link Event} occurs. The optional {@code properties} map carries context for the event, such
 * as the entity class name. An implementation only needs to override
 * {@link #fireEvent(Event, Map)}: the single-argument overload delegates to it with an empty map.
 *
 * <p><b>Threading.</b> Events are fired synchronously on server threads, some of them on hot paths
 * (for example {@link Event#ENTITY_INVOKE} on every invocation, and {@link Event#CONNECTION_OPEN}
 * and {@link Event#CONNECTION_CLOSE} on network I/O threads). Implementations must return quickly,
 * must not block, and must not throw: the composite returned by {@link #load(ServiceRegistry)}
 * does not isolate failures.
 *
 * <p>For current-value gauges, such as the number of connected clients, see
 * {@link org.terracotta.metrics.MetricService}.
 *
 * <p>{@link #NOOP} discards every event. Use it in tests, or when no implementation is registered.
 */
public interface EventService {

  /**
   * Events that the server fires when a notable change occurs.
   *
   * <p>The optional {@code properties} map passed with some events may contain:
   * <ul>
   *   <li>{@code "type"} - the fully qualified entity class name. Present for
   *       {@link #ENTITY_INVOKE}, {@link #ENTITY_FETCH} and {@link #ENTITY_RELEASE}.</li>
   *   <li>{@code "state"} - the server state name, for example {@code "ACTIVE-COORDINATOR"},
   *       {@code "PASSIVE-STANDBY"}, {@code "PASSIVE-SYNCING"} or {@code "STOP-STATE"}.
   *       Present for {@link #MEMBER_STATE} and {@link #PEER_STATE}.</li>
   *   <li>{@code "peer"} - the name of the peer server. Present for {@link #PEER_JOIN},
   *       {@link #PEER_LEAVE} and {@link #PEER_STATE}.</li>
   * </ul>
   */
  enum Event {
    /**
     * A client application joined this server.  This tracks the client application, not a
     * socket: see {@link #CONNECTION_OPEN} for sockets.
     */
    CLIENT_JOIN,
    /** A client application left this server. */
    CLIENT_LEAVE,
    /**
     * A physical socket connection was established, as seen by {@code netstat}.
     * Fired once per socket, for inbound and outbound connections: client (L1),
     * diagnostic and peer-server.  A client that reconnects opens a new socket
     * and fires this event again.
     */
    CONNECTION_OPEN,
    /**
     * A physical socket connection that was open has closed.
     * Fired once per socket, for inbound and outbound connections.  Each
     * {@link #CONNECTION_CLOSE} pairs with an earlier {@link #CONNECTION_OPEN}.
     */
    CONNECTION_CLOSE,
    /** A client fetched (acquired a reference to) an entity. */
    ENTITY_FETCH,
    /** A client invoked a message on an entity. */
    ENTITY_INVOKE,
    /** A client released its reference to an entity. */
    ENTITY_RELEASE,
    /**
     * A peer server joined the stripe: a group connection to it was established.  Fired once per
     * peer connection, so a peer that reconnects fires it again.  For the lifecycle of the peer
     * itself, see {@link #PEER_STATE}.
     */
    PEER_JOIN,
    /** A peer server left the stripe: its group connection was removed. */
    PEER_LEAVE,
    /**
     * The state of a passive peer changed, as observed by the active.  Fired only on the active.
     * The {@code "state"} property carries the peer's new state and the {@code "peer"} property
     * carries the peer's name.  The states are {@code "PASSIVE-UNINITIALIZED"} (peer joined),
     * {@code "PASSIVE-SYNCING"} (sync started), {@code "PASSIVE-STANDBY"} (sync completed) and
     * {@code "STOP-STATE"} (peer left).  For the state of this server, see {@link #MEMBER_STATE}.
     */
    PEER_STATE,
    /**
     * The state of this server changed.  Fired on every local state transition, for example
     * {@code START-STATE -> PASSIVE-UNINITIALIZED -> PASSIVE-SYNCING -> PASSIVE-STANDBY}.
     * The {@code "state"} property carries the new state name.  For the state of another
     * server, see {@link #PEER_STATE}.
     */
    MEMBER_STATE,
    /** A voter registered with this server. */
    VOTER_JOIN,
    /** A voter de-registered from this server. */
    VOTER_LEAVE,
  }

  /**
   * Fires {@code event} with no additional properties.
   *
   * @param event the event to fire (must not be {@code null})
   */
  default void fireEvent(Event event) {
    fireEvent(event, emptyMap());
  }

  /**
   * Fires {@code event} with a set of supplemental {@code properties}.
   *
   * @param event      the event to fire (must not be {@code null})
   * @param properties optional key/value context (e.g. {@code "type" -> entityClassName})
   */
  default void fireEvent(Event event, Map<String, String> properties) {}

  /**
   * Returns a composite {@link EventService} that forwards every event to each
   * {@link EventService} available in {@code registry}.
   *
   * @param registry the service registry to search
   * @return the composite, or {@link #NOOP} if {@code registry} has no {@link EventService}
   *         (never {@code null})
   */
  static EventService load(ServiceRegistry registry) {
    final Collection<EventService> services = registry.getServices(() -> EventService.class);
    return services.isEmpty() ? NOOP : new EventService() {
      @Override
      public void fireEvent(Event event) {
        services.forEach(m -> m.fireEvent(event));
      }

      @Override
      public void fireEvent(Event event, Map<String, String> properties) {
        services.forEach(m -> m.fireEvent(event, properties));
      }
    };
  }

  /** A no-op singleton that silently discards all events. */
  EventService NOOP = new EventService() {};
}
