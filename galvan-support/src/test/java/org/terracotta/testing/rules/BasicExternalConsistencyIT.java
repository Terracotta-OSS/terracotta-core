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

import com.terracotta.diagnostic.Diagnostics;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;

import org.junit.ClassRule;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;

import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

/**
 *
 */
public class BasicExternalConsistencyIT {

  @ClassRule
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(3).withFailoverPriorityVoterCount(0).withClientReconnectWindowTime(30)
      .build();

  @Test
  public void testConnection() throws IOException, ConnectionException, Exception {
    CLUSTER.getClusterControl().terminateAllServers();
    CLUSTER.getClusterControl().startOneServer();
    String[] hp = CLUSTER.getClusterHostPorts();
    Diagnostics checker = findRunningServer(hp);
    int tries = 1;
    while (checker == null) {
      checker = findRunningServer(hp);
      if (tries++ == 3) {
        throw new AssertionError("no active server");
      }
    }
    TimeUnit.SECONDS.sleep(10);
    boolean stuck = Boolean.parseBoolean(checker.invoke("ConsistencyManager", "isStuck"));
    boolean blocked = Boolean.parseBoolean(checker.invoke("ConsistencyManager", "isBlocked"));
    Assert.assertTrue(stuck);
    Assert.assertTrue(blocked);
    String state = checker.getState();
    while (!state.startsWith("ACTIVE")) {
      System.out.println(state);
      checker.invoke("ConsistencyManager", "allowRequestedTransition");
      TimeUnit.SECONDS.sleep(10);
      state = checker.getState();
    }
    checker.invoke("ConsistencyManager", "allowRequestedTransition");
    Connection connected = CLUSTER.newConnection();
    System.out.println(connected.toString());
  }

  private Diagnostics findRunningServer(String[] hostports) {
    for (String hp : hostports) {
      String[] split = hp.split("\\:");
      Properties properties = new Properties();
      properties.setProperty(ConnectionPropertyNames.CONNECTION_TYPE, "diagnostic");
      properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "10000");
      try {
        System.out.println("trying " + hp);
        Connection connection = ConnectionFactory.connect(Collections.singleton(new InetSocketAddress(split[0], Integer.parseInt(split[1]))), properties);
        return connection.getEntityRef(Diagnostics.class, 1L, "root").fetchEntity(null);
      } catch (ConnectionException | NumberFormatException | EntityNotProvidedException | EntityNotFoundException | EntityVersionMismatchException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
