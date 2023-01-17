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

import java.net.InetSocketAddress;
import java.util.Properties;

import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.Diagnostics;
import org.terracotta.connection.DiagnosticsFactory;
import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.testing.rules.BasicExternalClusterBuilder;
import org.terracotta.testing.rules.Cluster;

/**
 *
 */
public class PassiveDeathIT {

  Logger LOGGER = LoggerFactory.getLogger(PassiveDeathIT.class);
  
  @ClassRule
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(3)
          .withFailoverPriorityVoterCount(0)
//          .withSystemProperty("logback.debug", "true")
          .withClientReconnectWindowTime(30)
      .build();

  @Test
  public void testPassiveCrash() throws Exception {
    LOGGER.info("starting test");
    String[] clusterHostPorts = CLUSTER.getClusterHostPorts();
    CLUSTER.expectCrashes(true);
    CLUSTER.getClusterControl().waitForRunningPassivesInStandby();
    CLUSTER.getClusterControl().terminateActive();
    CLUSTER.getClusterControl().waitForActive();
    Properties prop = new Properties();
    prop.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "1000");
    for (String hostPort: clusterHostPorts) {
      String[] hp = hostPort.split("[:]");
      InetSocketAddress inet = InetSocketAddress.createUnresolved(hp[0], Integer.parseInt(hp[1]));
      try (Diagnostics d = DiagnosticsFactory.connect(inet, prop)) {
        if (d.getState().equals("PASSIVE-STANDBY")) {
          System.out.println(d.restartServer());

          String state = "UKNOWN";
          while (!state.equals("PASSIVE-STANDBY")) {
            try (Diagnostics dd = DiagnosticsFactory.connect(inet, new Properties())) {
              state = dd.getState();
              System.out.println(state);
            } catch (ConnectionClosedException ce) {
              System.out.println("no connection");
            } catch (ConnectionException ce) {
              Thread.sleep(500);
            }
          }
        }
      } catch (ConnectionException to) {
        System.out.println("skip down server");
      }

    }
  }

}
