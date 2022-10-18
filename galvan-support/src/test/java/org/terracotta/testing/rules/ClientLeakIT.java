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


import java.net.InetSocketAddress;
import java.util.Properties;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.Diagnostics;
import org.terracotta.connection.DiagnosticsFactory;


/**
 * This test is similar to BasicExternalClusterClassRuleIT, in that it uses the class rule to perform a basic test.
 * 
 * In this case, the basic test is watching how Galvan handles interaction with a basic active-passive cluster.
 */
// XXX: Currently ignored since this test depends on restartability of the server, which now requires a persistence service
//  to be plugged in (and there isn't one available, in open source).
public class ClientLeakIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientLeakIT.class);
  
  @ClassRule
  public static final Cluster CLUSTER = BasicExternalClusterBuilder.newCluster(1).build(); //logConfigExtensionResourceName("custom-logback-ext.xml").build();

  /**
   * This will ensure that a fail-over correctly happens.
   */
  @Test
  public void testClientLeakIsCleaned() throws Exception {
    CLUSTER.getClusterControl().startAllServers();
    CLUSTER.getClusterControl().waitForActive();
    Connection leak = CLUSTER.newConnection();
    CLUSTER.getClusterControl().terminateActive();
    Thread reconnect = lookForConnectionEstablisher();
    while (reconnect == null) {
      Thread.sleep(1000);
      reconnect = lookForConnectionEstablisher();
    }
    leak = null;
    while (reconnect.isAlive()) {
      System.out.println("reconnect thread " + reconnect.getName() + " is alive " + reconnect.isAlive());
      System.gc();
      Thread.sleep(1000);
    }
  }

  @Test
  public void testConnectionEstablisherDiesAfterJob() throws Exception {
    CLUSTER.getClusterControl().startAllServers();
    CLUSTER.getClusterControl().waitForActive();
    Connection leak = CLUSTER.newConnection();
    CLUSTER.getClusterControl().terminateActive();
    Thread reconnect = lookForConnectionEstablisher();
    while (reconnect == null) {
      Thread.sleep(1000);
      reconnect = lookForConnectionEstablisher();
    }
    CLUSTER.getClusterControl().startOneServer();
    CLUSTER.getClusterControl().waitForActive();
    while (reconnect.isAlive()) {
      System.out.println("reconnect thread " + reconnect.getName() + " is alive " + reconnect.isAlive());
      System.gc();
      Thread.sleep(1000);
    }
  }
  
  @Test
  public void testConnectionMakerDiesWithNoRef() throws Exception {
    CLUSTER.getClusterControl().startAllServers();
    CLUSTER.getClusterControl().waitForActive();
    CLUSTER.getClusterControl().terminateAllServers();
    Thread target = Thread.currentThread();
    new Thread(()->{
      try {
        while (lookForConnectionMaker() == null) {
          Thread.sleep(1000);
        }
        Thread.sleep(1000);
        target.interrupt();
      } catch (InterruptedException ie) {
        
      }
    }).start();
    Connection leak = null;
    try {
      leak = CLUSTER.newConnection();
    } catch (ConnectionException ce) {
      ce.printStackTrace();
      // expected
    }
    assertNull(leak);
    Thread maker = lookForConnectionMaker();
    if (maker != null) {
      for (int x=0;x<1000 && maker.isAlive();x++) {
        System.gc();
        Exception printer = new Exception("trying to join:" + x);
        printer.setStackTrace(maker.getStackTrace());
        printer.printStackTrace();
        maker.join(1000);
      }
      assertFalse(maker.isAlive());
    }
  }
    
  @Test
  public void testSEDADiesWithNoRef() throws Exception {
    CLUSTER.getClusterControl().startAllServers();
    CLUSTER.getClusterControl().waitForActive();

    String connectionName = "LEAKTESTCLIENT";
    Properties props = new Properties();
    props.setProperty(ConnectionPropertyNames.CONNECTION_NAME, connectionName);
    Connection leak = ConnectionFactory.connect(CLUSTER.getConnectionURI(), props);
    String cid = leak.toString();

    assertNotNull(leak);
    assertNotNull(lookForThreadWithName(connectionName));

    leak = null;
    
    while (lookForThreadWithName(connectionName) != null) {
      LOGGER.info(cid);
      System.gc();
      Thread.sleep(1_000);
    }
    
    String[] hp = CLUSTER.getClusterHostPorts();
    String[] server = hp[0].split(":");
    Diagnostics diag = DiagnosticsFactory.connect(InetSocketAddress.createUnresolved(server[0], Integer.parseInt(server[1])), props);

    assertNotNull(diag);
    assertNotNull(lookForThreadWithName(connectionName));
    
    diag = null;
    
    while (lookForThreadWithName(connectionName) != null) {
      LOGGER.info(cid);
      System.gc();
      Thread.sleep(1_000);
    }    
  }
    
  @Test
  public void testHealthCheckThreadDies() throws Exception {
    CLUSTER.getClusterControl().startAllServers();
    CLUSTER.getClusterControl().waitForActive();

    String connectionName = "LEAKTESTCLIENT";
    Properties props = new Properties();
    props.setProperty(ConnectionPropertyNames.CONNECTION_NAME, connectionName);
    Connection leak = ConnectionFactory.connect(CLUSTER.getConnectionURI(), props);
    String cid = leak.toString();

    assertNotNull(leak);
    assertNotNull(lookForThreadWithName(connectionName));

    leak = null;
    
    while (lookForThreadWithName(connectionName) != null) {
      LOGGER.info(cid);
      System.gc();
      Thread.sleep(1_000);
    }
    
    CLUSTER.getClusterControl().terminateAllServers();
    
    while (lookForThreadWithName("HealthCheck") != null) {
      LOGGER.info(cid);
      System.gc();
      Thread.sleep(1_000);
    }
  }
  
  private static Thread lookForThreadWithName(String name) {
    Thread[] list = new Thread[Thread.activeCount()];
    Thread.enumerate(list);
    for (Thread t : list) {
      if (t != null && t.getName().startsWith(name)) {
        return t;
      }
    }
    return null;
  } 
  
  private static Thread lookForConnectionMaker() {
    return lookForThreadWithName("Connection Maker");
  }  
  
  private static Thread lookForConnectionEstablisher() {
    return lookForThreadWithName("ConnectionEstablisher");
  }
}
