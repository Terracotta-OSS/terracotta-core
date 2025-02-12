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
package org.terracotta.example;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Properties;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.entity.map.ConcurrentClusteredMap;
import org.terracotta.entity.map.MapConfig;

/**
 *
 */
public class MapToy {
  public static void main(String[] args) throws Exception {
    String[] hostport = args[0].split(":");
    InetSocketAddress adddress = InetSocketAddress.createUnresolved(hostport[0], Integer.parseInt(hostport[1]));
    Connection c = ConnectionFactory.connect(Collections.singleton(adddress), new Properties());
    EntityRef<ConcurrentClusteredMap, MapConfig, Object> ref = c.getEntityRef(ConcurrentClusteredMap.class, 1L, "root");
    try {
      ref.create(new MapConfig(8, "root"));
    } catch (Exception e) {
      
    }
    ConcurrentClusteredMap<String, String> map = ref.fetchEntity(null);
    map.setTypes(String.class, String.class);
    System.out.println("setting hello=world:");
    map.insert("hello", "world").get();
    System.out.println(map.get("hello"));
  }
}
