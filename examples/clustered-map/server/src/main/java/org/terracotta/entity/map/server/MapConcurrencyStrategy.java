/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package org.terracotta.entity.map.server;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.map.common.KeyedOperation;
import org.terracotta.entity.map.common.MapOperation;

/**
 *
 */
public class MapConcurrencyStrategy implements ConcurrencyStrategy<MapOperation> {
  
  private final int concurrency;

  public MapConcurrencyStrategy(int concurrency) {
    this.concurrency = concurrency;
  }

  @Override
  public int concurrencyKey(MapOperation operation) {
    switch (operation.operationType()) {
      case PUT:
      case PUT_IF_ABSENT:
      case PUT_IF_PRESENT:
      case GET:
      case CONDITIONAL_REMOVE:
      case CONDITIONAL_REPLACE:
      case CONTAINS_KEY:
      case REMOVE:
        return CompoundMap.segment(((KeyedOperation)operation).getKey().hashCode(), concurrency);
      default:
        return ConcurrencyStrategy.MANAGEMENT_KEY;
    }
  }

  @Override
  public Set<Integer> getKeysForSynchronization() {
    return IntStream.range(1, concurrency+1).mapToObj(Integer::valueOf).collect(Collectors.toSet());
  }
}
