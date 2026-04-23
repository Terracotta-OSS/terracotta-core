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

import com.tc.text.PrettyPrintable;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

public interface MetricService extends StateDumpable, PrettyPrintable {
  void event(String name);

  void addSupplier(String name, LongSupplier supplier);

  void removeSupplier(String name);

  Map<String, Long> snapshot();

  @Override
  default void addStateTo(StateDumpCollector stateDumpCollector) {
    snapshot().forEach(stateDumpCollector::addState);
  }

  @Override
  default Map<String, ?> getStateMap() {
    return new TreeMap<>(snapshot());
  }

  static MetricService load(ServiceRegistry registry) {
    final Collection<MetricService> services = registry.getServices(() -> MetricService.class);
    return new MetricService() {
      @Override
      public void event(String name) { services.forEach(m -> m.event(name)); }
      @Override
      public void addSupplier(String name, LongSupplier supplier) { services.forEach(m -> m.addSupplier(name, supplier)); }
      @Override
      public void removeSupplier(String name) { services.forEach(m -> m.removeSupplier(name)); }
      @Override
      public Map<String, Long> snapshot() {
        return services.stream()
            .flatMap(m->m.snapshot().entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));
      }
    };
  }

  MetricService NOOP = new MetricService() {
    @Override
    public void event(String name) { }
    @Override
    public void addSupplier(String name, LongSupplier supplier) {}
    @Override
    public void removeSupplier(String name) {}
    @Override
    public Map<String, Long> snapshot() { return Collections.emptyMap(); }
  };
}
