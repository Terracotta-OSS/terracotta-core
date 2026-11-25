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


import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.DiagnosticsFactory;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.map.ConcurrentClusteredMap;
import org.terracotta.entity.map.MapConfig;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class CreateEntityAndDump {

  Logger LOGGER = LoggerFactory.getLogger(CreateEntityAndDump.class);

  @ClassRule
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(1)
//          .withSystemProperty("logback.debug", "true")
          .withClientReconnectWindowTime(30)
          .withTcProperty("tc.messages.grouping.maxCount", "8196")
      .build();

  @Test @Ignore("not suitable for CI")
  public void testClusterHostPorts() throws Exception {
    long millis = System.currentTimeMillis();
    LOGGER.info("starting test");
    Connection c = CLUSTER.newConnection();
    EntityRef<ConcurrentClusteredMap, MapConfig, Void> map = c.getEntityRef(ConcurrentClusteredMap.class, 1L, "fire");
    map.create(new MapConfig(8, "fire"));
    ConcurrentClusteredMap<String, String> cmap = map.fetchEntity(null);
    cmap.setTypes(String.class, String.class);

    for (int i=0;i<10;i++) {
      for (int x=0;x<128;x++) {
        cmap.insert(Integer.toString(x), "the quick brown fox jumped over the goat " + x).get();
      }
      System.out.println("insert in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - millis) + "s ");

      Thread.sleep(10_000);
      String[] clusterHostPorts = CLUSTER.getClusterHostPorts();
      for (String hostPort: clusterHostPorts) {
        String[] hp = hostPort.split("[:]");
        InetSocketAddress inet = InetSocketAddress.createUnresolved(hp[0], Integer.parseInt(hp[1]));
        try (com.terracotta.diagnostic.Diagnostics d = (com.terracotta.diagnostic.Diagnostics)DiagnosticsFactory.connect(inet, new Properties())) {
          System.out.println(d.getClusterState());
        }
      }
    }
  }

}
