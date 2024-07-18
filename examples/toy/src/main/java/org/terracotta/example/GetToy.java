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
public class GetToy {
  public static void main(String[] args) throws Exception {
    String[] hostport = args[0].split(":");
    InetSocketAddress adddress = InetSocketAddress.createUnresolved(hostport[0], Integer.parseInt(hostport[1]));
    Connection c = ConnectionFactory.connect(Collections.singleton(adddress), new Properties());
    EntityRef<ConcurrentClusteredMap, MapConfig, Object> ref = c.getEntityRef(ConcurrentClusteredMap.class, 1L, "root");
    ConcurrentClusteredMap<String, String> map = ref.fetchEntity(null);
    map.setTypes(String.class, String.class);
    System.out.println(map.get("hello"));
  }
}
