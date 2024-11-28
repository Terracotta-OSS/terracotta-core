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

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.Properties;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.Diagnostics;
import org.terracotta.connection.DiagnosticsFactory;
import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class DiagnosticFunctionIT {

  @Rule
  public final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(1).withClientReconnectWindowTime(30)
          .in(Paths.get(System.getProperty("galvan.dir")))
      .build();

  @Test
  public void testClusterHostPorts() throws Exception {
    String[] clusterHostPorts = CLUSTER.getClusterHostPorts();
    for (String hostPort: clusterHostPorts) {
      String[] hp = hostPort.split("[:]");
      InetSocketAddress inet = InetSocketAddress.createUnresolved(hp[0], Integer.parseInt(hp[1]));
      try (com.terracotta.diagnostic.Diagnostics d = (com.terracotta.diagnostic.Diagnostics)DiagnosticsFactory.connect(inet, new Properties())) {
        System.out.println(d.getThreadDump());
        System.out.println(d.invoke("Server", "isAcceptingClients"));
        System.out.println(d.get("Server", "Version"));
        System.out.println(d.get("Server", "CurrentChannelProperties"));
        System.out.println(d.list("Server"));
        System.out.println(d.getClusterState());
      }
    }

    String[] hp = clusterHostPorts[0].split("[:]");
    InetSocketAddress inet = InetSocketAddress.createUnresolved(hp[0], Integer.parseInt(hp[1]));
    new Thread(()->{
      try (Diagnostics d = DiagnosticsFactory.connect(inet, new Properties())) {
        Thread.sleep(15_000);
        d.forceTerminateServer();
      } catch (InterruptedException ie) {

      } catch (ConnectionException ce) {

      }
    }).start();
    CLUSTER.expectCrashes(true);
    long start = System.currentTimeMillis();
    try (Diagnostics d = DiagnosticsFactory.connect(inet, new Properties())) {
      while (System.currentTimeMillis() - start < 30_000) {
        System.out.println(d.getState());
        Thread.sleep(1_000);
      }
      fail();
    } catch (ConnectionException | ConnectionClosedException e) {
      System.out.println("success:" + (System.currentTimeMillis() - start));
    }
  }
  
  @Test
  public void testCyclingConnections() throws Exception {
    String[] clusterHostPorts = CLUSTER.getClusterHostPorts();
    for (int x=0;x<500;x++) {
      for (String hostPort: clusterHostPorts) {
        String[] hp = hostPort.split("[:]");
        InetSocketAddress inet = InetSocketAddress.createUnresolved(hp[0], Integer.parseInt(hp[1]));
        try (com.terracotta.diagnostic.Diagnostics d = (com.terracotta.diagnostic.Diagnostics)DiagnosticsFactory.connect(inet, new Properties())) {
          System.out.println(d.invoke("Server", "isAcceptingClients"));
          System.out.println(d.get("Server", "Version"));
          System.out.println(d.get("Server", "CurrentChannelProperties"));
          System.out.println(d.list("Server"));
        }
      }
    }
  }
}
