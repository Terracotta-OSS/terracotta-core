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
package org.terracotta.entity.map;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.map.common.ClusteredMapCodec;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;

@SuppressWarnings("rawtypes")
public class TerracottaClusteredMapClientService implements EntityClientService<ConcurrentClusteredMap, MapConfig, MapOperation, MapResponse, Object> {
  @Override
  public boolean handlesEntityType(Class<ConcurrentClusteredMap> cls) {
    return cls == ConcurrentClusteredMap.class;
  }

  @Override
  public byte[] serializeConfiguration(MapConfig configuration) {
    return configuration.getBytes();
  }

  @Override
  public MapConfig deserializeConfiguration(byte[] configuration) {
    return new MapConfig(configuration);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ConcurrentClusteredMap create(EntityClientEndpoint endpoint, Object userData) {
    return new TerracottaClusteredMap(endpoint);
  }

  @Override
  public MessageCodec<MapOperation, MapResponse> getMessageCodec() {
    return new ClusteredMapCodec();
  }
}
