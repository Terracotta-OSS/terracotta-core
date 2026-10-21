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
package com.tc.spi.metric;

import org.terracotta.entity.ServiceRegistry;

import java.util.Collection;
import java.util.Map;
import java.util.function.LongSupplier;

import static java.util.Collections.emptyMap;

/**
 * Core SPI for collecting server metrics.
 *
 * <p>Implementations are discovered via {@link ServiceRegistry} and wired in by the server's
 * service provider infrastructure. The {@link #load(ServiceRegistry)} factory method returns a
 * composite that fans events out to every registered implementation, so multiple plugins can
 * independently observe the same event stream.
 *
 * <p>There are two distinct integration points:
 * <ol>
 *   <li><b>Events</b> – {@link #fireEvent(Event)} / {@link #fireEvent(Event, Map)} – notify the
 *       metric layer that something happened (e.g. a client connected).  The optional
 *       {@code properties} map carries supplemental context such as the entity class name.</li>
 *   <li><b>Gauge suppliers</b> – {@link #addMetricSupplier(Metric, LongSupplier)} /
 *       {@link #removeMetricSupplier(Metric)} – register a live callback whose return value is
 *       the current snapshot of a particular gauge (e.g. the current connection count).</li>
 * </ol>
 *
 * <p>{@link #NOOP} is a no-op singleton suitable for use in tests and passive-mode contexts.
 */
public interface MetricService {

  /**
   * Events that the server fires when a notable lifecycle change occurs.
   *
   * <p>The optional {@code properties} map passed alongside some events may contain:
   * <ul>
   *   <li>{@code "type"} – the fully qualified entity class name, present for
   *       {@link #ENTITY_INVOKE}, {@link #ENTITY_FETCH}, and {@link #ENTITY_RELEASE}.</li>
   *   <li>{@code "state"} – the server state name (e.g. {@code "ACTIVE-COORDINATOR"},
   *       {@code "PASSIVE-STANDBY"}), present for {@link #MEMBER_STATE}.</li>
   * </ul>
   */
  enum Event {
    /** A new client application connected. */
    CLIENT_JOIN,
    /** A client application disconnected. */
    CLIENT_LEAVE,
    /**
     * A new physical transport connection was established.
     * Fired for all connection directions: inbound client (L1), inbound diagnostic,
     * inbound peer-server, and outbound peer-server connections.
     */
    CONNECTION_OPEN,
    /**
     * An existing physical transport connection was closed.
     * Fired for all connection directions: inbound client (L1), inbound diagnostic,
     * inbound peer-server, and outbound peer-server connections.
     */
    CONNECTION_CLOSE,
    /** A client fetched (acquired a reference to) an entity. */
    ENTITY_FETCH,
    /** A client invoked a message on an entity. */
    ENTITY_INVOKE,
    /** A client released its reference to an entity. */
    ENTITY_RELEASE,
    /** A peer server joined the stripe. */
    MEMBER_JOIN,
    /** A peer server left the stripe. */
    MEMBER_LEAVE,
    /**
     * The local server's state changed.
     * Fired on every state transition with a {@code "state"} property carrying
     * the new state name (e.g. {@code "ACTIVE-COORDINATOR"}, {@code "PASSIVE-STANDBY"}).
     * Consumers can use this to track failover, sync, and startup progressions.
     */
    MEMBER_STATE,
    /** A voter registered with this server. */
    VOTER_JOIN,
    /** A voter de-registered from this server. */
    VOTER_LEAVE,
    /** An active-to-passive failover occurred. */
    FAILOVER,
  }

  /**
   * Gauge types for which a {@link LongSupplier} can be registered.
   * Each constant maps to a current-value snapshot that increases or decreases over time.
   */
  enum Metric {
    /**
     * Current number of open physical transport connections.
     * Counts all directions: inbound client (L1), inbound diagnostic,
     * inbound peer-server, and outbound peer-server connections.
     */
    CONNECTIONS,
    /** Current number of connected client applications. */
    CLIENTS,
    /** Current number of entity fetch references held by all clients. */
    ENTITY_FETCHES,
    /** Current number of peer servers connected. */
    MEMBERS,
    /** Current number of registered voters. */
    VOTERS
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
  void fireEvent(Event event, Map<String, String> properties);

  /**
   * Registers a live {@link LongSupplier} for the given gauge {@code metric}.
   * A subsequent call with the same {@code metric} replaces the previous supplier.
   *
   * @param metric   the gauge to register (must not be {@code null})
   * @param supplier supplier that returns the current gauge value when invoked
   */
  void addMetricSupplier(Metric metric, LongSupplier supplier);

  /**
   * Removes the gauge supplier previously registered for {@code metric}, if any.
   *
   * @param metric the gauge whose supplier should be removed (must not be {@code null})
   */
  void removeMetricSupplier(Metric metric);

  /**
   * Returns a composite {@link MetricService} that fans all calls out to every instance
   * of {@link MetricService} available in {@code registry}.
   *
   * @param registry the service registry to search
   * @return composite service (never {@code null}; may be effectively no-op if no services found)
   */
  static MetricService load(ServiceRegistry registry) {
    final Collection<MetricService> services = registry.getServices(() -> MetricService.class);
    return new MetricService() {
      @Override
      public void fireEvent(Event event, Map<String, String> properties) {
        services.forEach(m -> m.fireEvent(event, properties));
      }

      @Override
      public void addMetricSupplier(Metric metric, LongSupplier supplier) {
        services.forEach(m -> m.addMetricSupplier(metric, supplier));
      }

      @Override
      public void removeMetricSupplier(Metric metric) {
        services.forEach(m -> m.removeMetricSupplier(metric));
      }
    };
  }

  /** A no-op singleton that silently discards all events and supplier registrations. */
  MetricService NOOP = new MetricService() {
    @Override
    public void fireEvent(Event event, Map<String, String> properties) {
    }

    @Override
    public void addMetricSupplier(Metric metric, LongSupplier supplier) {
    }

    @Override
    public void removeMetricSupplier(Metric metric) {
    }
  };
}
