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
package org.terracotta.functional;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;
import javax.management.MBeanServer;

import org.junit.ClassRule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class InformationConnectionIT {

  @ClassRule
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(1).withClientReconnectWindowTime(30)
      .build();

  @Test
  public void testClusterHostPorts() throws Exception {
    String[] clusterHostPorts = CLUSTER.getClusterHostPorts();
    for (String hostPort: clusterHostPorts) {
      String[] hp = hostPort.split("[:]");
      InetSocketAddress inet = InetSocketAddress.createUnresolved(hp[0], Integer.parseInt(hp[1]));
      Properties props = new Properties();
      try {
        Connection c = ConnectionFactory.connect(Collections.singleton(inet), props);
        EntityRef<Entity, Void, Void> ref = c.getEntityRef(Entity.class, 1L, "test");
        ref.create(null);
      } catch (Exception e) {
        CLUSTER.getClusterControl().terminateAllServers();
        System.gc();
        Path f = Files.createTempFile("heap", ".hprof");
        String dump = f.toString();
        Files.delete(f);
        dumpHeap(dump, true);
      }
    }
  }

  public static void dumpHeap(String filePath, boolean live) throws IOException {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      try {
        HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
          server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
        mxBean.dumpHeap(filePath, live);
      } catch (Exception e) {
        e.printStackTrace();
      }
  }
}
