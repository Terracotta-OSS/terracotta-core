/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
package org.terracotta.functional;


import java.util.HashMap;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.map.ConcurrentClusteredMap;
import org.terracotta.entity.map.MapConfig;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class ActiveMessengerFunctionIT {

  @Rule
  public final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(2).withClientReconnectWindowTime(30)
      .build();

  @Test
  public void testActiveMessenger() throws Exception {
    Connection connection = CLUSTER.newConnection();
    String hp = CLUSTER.getClusterHostPorts()[0];
    String[] shp = hp.split(":");
    int port = Integer.parseInt(shp[1]);

    EntityRef<ConcurrentClusteredMap, MapConfig, Void>  ref = connection.getEntityRef(ConcurrentClusteredMap.class, 1L, "ROOT");
    ref.create(new MapConfig(1, "ROOT"));
    ConcurrentClusteredMap map = ref.fetchEntity(null);
    map.putMultiple(new HashMap<>());
  }
}
