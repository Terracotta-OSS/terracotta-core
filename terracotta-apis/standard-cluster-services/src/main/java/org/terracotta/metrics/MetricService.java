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
package org.terracotta.metrics;

import org.terracotta.entity.ServiceRegistry;
import org.terracotta.event.EventService;

import java.util.Collection;
import java.util.function.LongSupplier;

/**
 * SPI for reading current-value gauges of the server, such as the number of connected clients.
 *
 * <p>Implementations are discovered through the {@link ServiceRegistry}. The
 * {@link #load(ServiceRegistry)} factory method returns a composite that forwards every
 * registration to each registered implementation, so several plugins can read the same gauges
 * independently.
 *
 * <p>The server registers one {@link LongSupplier} per {@link Metric} with
 * {@link #addMetricSupplier(Metric, LongSupplier)} while it starts, and removes it with
 * {@link #removeMetricSupplier(Metric)} when it stops. An implementation reads the current value
 * by calling the supplier whenever it needs a sample.
 *
 * <p>For things that happen at a point in time, such as a client joining, see
 * {@link EventService}.
 *
 * <p>{@link #NOOP} discards every registration. Use it in tests, or when no implementation is
 * registered.
 */
public interface MetricService {
  /**
   * Gauges for which the server registers a {@link LongSupplier}.
   * Each constant is a current value that can rise and fall over time.
   */
  enum Metric {
    /**
     * Current number of open physical socket connections: opens minus closes.
     * Counts inbound and outbound connections: client (L1), diagnostic and peer-server.
     */
    CONNECTIONS,
    /** Current number of connected client applications. */
    CLIENTS,
    /** Current number of entity fetch references held by all clients. */
    ENTITY_FETCHES,
    /** Current number of peer servers connected. */
    PEERS,
    /** Current number of registered voters. */
    VOTERS
  }

  /**
   * Registers a {@link LongSupplier} that returns the current value of the gauge {@code metric}.
   * A later call with the same {@code metric} replaces the previous supplier.
   *
   * @param metric   the gauge to register (must not be {@code null})
   * @param supplier returns the current value of the gauge each time it is called
   */
  default void addMetricSupplier(Metric metric, LongSupplier supplier) {}

  /**
   * Removes the gauge supplier previously registered for {@code metric}, if any.
   *
   * @param metric the gauge whose supplier should be removed (must not be {@code null})
   */
  default void removeMetricSupplier(Metric metric) {}

  /**
   * Returns a composite {@link MetricService} that forwards every call to each
   * {@link MetricService} available in {@code registry}.
   *
   * @param registry the service registry to search
   * @return the composite, or {@link #NOOP} if {@code registry} has no {@link MetricService}
   *         (never {@code null})
   */
  static MetricService load(ServiceRegistry registry) {
    final Collection<MetricService> services = registry.getServices(() -> MetricService.class);
    return services.isEmpty() ? NOOP : new MetricService() {
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

  /** A no-op singleton that silently discards all supplier registrations and removals. */
  MetricService NOOP = new MetricService() {};
}
