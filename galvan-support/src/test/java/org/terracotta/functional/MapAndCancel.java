/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.functional;


import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.map.ConcurrentClusteredMap;
import org.terracotta.entity.map.MapConfig;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class MapAndCancel {

  Logger LOGGER = LoggerFactory.getLogger(MapAndCancel.class);
  
  @ClassRule
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(1)
//          .withSystemProperty("logback.debug", "true")
          .withClientReconnectWindowTime(30)
          .withTcProperty("tc.messages.grouping.maxCount", "8196")
      .build();

  @Test @Ignore("not suitable fpr CI")
  public void testClusterHostPorts() throws Exception {
    long millis = System.currentTimeMillis();
    LOGGER.info("starting test");
    Connection c = CLUSTER.newConnection();
    EntityRef<ConcurrentClusteredMap, MapConfig, Void> map = c.getEntityRef(ConcurrentClusteredMap.class, 1L, "fire");
    map.create(new MapConfig(8, "fire"));
    ConcurrentClusteredMap<String, String> cmap = map.fetchEntity(null);
    cmap.setTypes(String.class, String.class);
   
    for (int x=0;x<1024 * 1024;x++) {
      cmap.insert(Integer.toString(x), "the quick brown fox jumped over the goat").cancel(false);
    }
    System.out.println("insert in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - millis) + "s ");
    int cancelCount = 0;
    for (int x=0;x<1024 * 1024;x++) {
      String v = cmap.get(Integer.toString(x));
      if (v != null) {
        System.out.println(x + " " + v);
      } else {
        cancelCount++;
      }
    }
    System.out.println("test run in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - millis) + "s " + cancelCount + " messages cancelled out of " + 
             (1024 * 1024) + " messages invoked");
  }

}
