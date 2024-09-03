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
package org.terracotta.entity.map.server;

import java.util.Map;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.ExecutionStrategy;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.entity.map.common.ClusteredMapCodec;
import org.terracotta.entity.map.common.MapConfig;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;

/**
 * TerracottaClusteredMapService
 */
public class TerracottaClusteredMapService implements EntityServerService<MapOperation, MapResponse> {
  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return typeName.equals("org.terracotta.entity.map.ConcurrentClusteredMap");
  }

  @Override
  public ActiveServerEntity<MapOperation, MapResponse> createActiveEntity(ServiceRegistry registry, byte[] configuration) {
    try {
      MapConfig config = new MapConfig(configuration);
      return new ActiveTerracottaClusteredMap(config.getName(), config.getConcurrency(), registry.getService(()->Map.class));
    } catch (ServiceException se) {
      return null;
    }
  }
  
  @Override
  public PassiveServerEntity<MapOperation, MapResponse> createPassiveEntity(ServiceRegistry registry, byte[] configuration) {
    try {
      MapConfig config = new MapConfig(configuration);
      return new PassiveTerracottaClusteredMap(config.getName(), config.getConcurrency(), registry.getService(()->Map.class));
    } catch (ServiceException se) {
      return null;
    }
  }

  @Override
  public ConcurrencyStrategy<MapOperation> getConcurrencyStrategy(byte[] configuration) {
    return new MapConcurrencyStrategy(new MapConfig(configuration).getConcurrency());
  }

  @Override
  public MessageCodec<MapOperation, MapResponse> getMessageCodec() {
    return new ClusteredMapCodec();
  }

  @Override
  public SyncMessageCodec<MapOperation> getSyncMessageCodec() {
    return new ClusteredMapSyncCodec();
  }

  @Override
  public ExecutionStrategy<MapOperation> getExecutionStrategy(byte[] configuration) {
    return new MapExecutionStrategy();
  }
  
  
}
