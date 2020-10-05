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
package org.terracotta.testing.rules;

import org.junit.ClassRule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.entity.EntityRef;


import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import org.terracotta.connection.Diagnostics;


public class DynamicPassiveRemovalIT {

  @ClassRule
  public static final Cluster CLUSTER =
      BasicExternalClusterBuilder.newCluster(4).withFailoverPriorityVoterCount(0).build();

  @Test
  public void testPassiveRemoval() throws Exception {
    CLUSTER.getClusterControl().waitForActive();
    CLUSTER.getClusterControl().waitForRunningPassivesInStandby();

    String[] hostPorts = CLUSTER.getClusterHostPorts();

    // kill one passive
    CLUSTER.getClusterControl().terminateOnePassive();
    String deadPassive = findDeadPassive(hostPorts);
    int split = deadPassive.indexOf(':');
    String host = deadPassive.substring(0,split);
    int port = Integer.parseInt(deadPassive.substring(split + 1));
//  this is lazy, beacause we don't know the group port, use zero.
//  don't really need it for remove
    removeDeadPassiveFromTopology(hostPorts, deadPassive, port, 0);

    // kill the active so that one of the passives promotes
    CLUSTER.getClusterControl().terminateActive();

    // waitForActive would get stuck forever If the dead passive was not removed from the topology earlier
    CLUSTER.getClusterControl().waitForActive();
  }

  private void removeDeadPassiveFromTopology(String[] hostPorts, String deadPassive, int port, int group) throws Exception {
    for (String hostPort: hostPorts) {
      if (hostPort.equals(deadPassive)) continue;
      try (Connection connection = ConnectionFactory.connect(URI.create( "diagnostic://" + hostPort),
                                                             new Properties())) {
        EntityRef<Diagnostics, Object, Object> entityRef = connection.getEntityRef(Diagnostics.class, 1L, "root");
        Diagnostics diagnostics = entityRef.fetchEntity(null);
        diagnostics.invokeWithArg("TopologyMBean", "removePassive", deadPassive + ":" + port + ":" + group);
      }
    }
  }

  private String findDeadPassive(String[] hostPorts) throws Exception {
    for (String hostPort: hostPorts) {
      Properties properties = new Properties();
      properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "10000");
      try (Connection connection = ConnectionFactory.connect(URI.create( "diagnostic://" + hostPort), properties)) {
      } catch (ConnectionException exception) {
        if (exception.getCause() instanceof TimeoutException) {
          return hostPort;
        }
      }
    }

    return null;
  }
}
