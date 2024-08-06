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
package org.terracotta.functional;


import java.io.StringReader;
import java.net.InetSocketAddress;
import java.util.Properties;
import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.Diagnostics;
import org.terracotta.connection.DiagnosticsFactory;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.map.ConcurrentClusteredMap;
import org.terracotta.entity.map.MapConfig;
import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class ReconnectRejectIT {

  @Rule
  public final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(1).withClientReconnectWindowTime(30)
      .build();

  @Test
  public void testClusterHostPorts() throws Exception {
    Connection connection = CLUSTER.newConnection();
    String hp = CLUSTER.getClusterHostPorts()[0];
    String[] shp = hp.split(":");
    int port = Integer.parseInt(shp[1]);
    String id = null;
    try (Diagnostics d = DiagnosticsFactory.connect(InetSocketAddress.createUnresolved(shp[0], port), new Properties())) {
      Properties clients = new Properties();
      clients.load(new StringReader(d.invoke("Server", "getConnectedClients")));
      id = clients.getProperty("clients.0.id");
    }
    try (Diagnostics d = DiagnosticsFactory.connect(InetSocketAddress.createUnresolved(shp[0], port), new Properties())) {
      System.out.println(d.invokeWithArg("Server", "disconnectClient", id));
    }
    try {
      EntityRef<ConcurrentClusteredMap, MapConfig, Void>  ref = connection.getEntityRef(ConcurrentClusteredMap.class, 1L, "ROOT");
      ref.create(new MapConfig(1, "ROOT"));
      ConcurrentClusteredMap map = ref.fetchEntity(null);
      Assert.fail();
    } catch (ConnectionClosedException e) {
      //expected
      // There should be TRANSPORT_RECONNECTION_REJECTED_EVENT in the client logs
      e.printStackTrace();
    }
    
  }
}
