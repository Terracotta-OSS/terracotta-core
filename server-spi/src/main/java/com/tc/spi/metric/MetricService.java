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

public interface MetricService {
  enum Event {
    CLIENT_JOIN,
    CLIENT_LEAVE,
    CONNECTION_OPEN,
    CONNECTION_CLOSE,
    ENTITY_FETCH,
    ENTITY_INVOKE,
    ENTITY_RELEASE,
    MEMBER_JOIN,
    MEMBER_LEAVE,
    VOTER_JOIN,
    VOTER_LEAVE,
  }

  enum Metric {
    CLIENTS,
    ENTITY_FETCHES,
    MEMBERS,
    VOTERS
  }

  default void fireEvent(Event event) {
    fireEvent(event, emptyMap());
  }

  void fireEvent(Event event, Map<String, String> properties);

  void addMetricSupplier(Metric metric, LongSupplier supplier);

  void removeMetricSupplier(Metric metric);

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
